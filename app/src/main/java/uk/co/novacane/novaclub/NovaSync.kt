// Keeps Nova Club in step with the studio calendar from anywhere in the app (not just the Book screen), remembers what
// it saw last time (so bookings made while the app was closed are celebrated when it opens), and draws the
// full-screen celebration when a booking lands or frees up.
package com.novacane.novaclub

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Something that's just changed in the calendar, with when it happened on the animation clock, and the bookings as they
// were just before (so the moons can slide from how they looked). `id` goes up each time, so the same change is never
// mistaken for a new one.
data class LiveChange(val id: Int, val changes: BookingChanges, val before: List<BusyBlock>, val at: Float)

class StudioCalendarSync(context: Context, private val source: AvailabilitySource) {
    private val prefs = context.applicationContext.getSharedPreferences("nova_calendar", Context.MODE_PRIVATE)

    // The bookings for each month being watched, as shown (keyed by the 1st of the month)
    private val months = mutableStateMapOf<Day, List<BusyBlock>>()
    // The bookings for each month as last read from the real calendar: what changes are measured against
    private val seen = mutableMapOf<Day, List<BusyBlock>>()
    // The months to keep in sync: this month and next always, plus any the Book screen is showing
    private val watched = mutableStateListOf<Day>()
    // Wakes the sync loop early (for a newly watched month, or when asked)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    // How many Book screens are showing: while there's one, the calendar is checked more often
    private var bookScreens = 0
    private var lastReadFailed = false

    // The Book screen calls this when it opens: check right away, then every second until it closes
    fun bookScreenOpened() { bookScreens++; wake.trySend(Unit) }
    fun bookScreenClosed() { bookScreens = (bookScreens - 1).coerceAtLeast(0) }
    private var counter = 0

    var latest by mutableStateOf<LiveChange?>(null)              // the most recent change (for the animations)
        private set
    var lastChecked by mutableLongStateOf(0L)                    // when the calendar was last read
        private set
    var note by mutableStateOf("")                               // where the bookings come from, for the Book screen
        private set

    fun busyFor(month: Day): List<BusyBlock>? = months[month.firstOfMonth()]

    fun watch(month: Day) {
        val m = month.firstOfMonth()
        if (m !in watched) { watched += m; wake.trySend(Unit) }
    }

    // Plays the latest change again (e.g. after jumping to its day, so its animation can be seen there)
    fun replay(now: Float) { latest = latest?.copy(at = now) }

    // Reads one month (e.g. to double-check a time before paying), reporting anything that's changed
    suspend fun read(month: Day, now: () -> Float): List<BusyBlock> {
        readAll(listOf(month.firstOfMonth()), now)
        return months.getValue(month.firstOfMonth())
    }

    // Reads these months, compares them with what we had before (or with what was saved last visit), and reports
    // everything that's changed across them as one change, so a booking moved from one month to another is seen
    // as rescheduled rather than as one cancelled and one new
    private suspend fun readAll(list: List<Day>, now: () -> Float) {
        val before = mutableListOf<BusyBlock>()
        val after = mutableListOf<BusyBlock>()
        var failed = false
        // Every month at once, so checking three months takes no longer than checking one
        val results = coroutineScope {
            list.map { m -> async(Dispatchers.IO) { m to runCatching { source.busyBetween(m, m.plusMonths(1)) } } }.awaitAll()
        }
        results.forEach { (m, result) ->
            failed = failed || result.isFailure
            val fresh = result.getOrNull()
            if (fresh != null) {
                val old = seen[m] ?: loadSnapshot(m)                     // what we last knew, even from a previous visit
                if (old != null) { before += old; after += fresh }
                seen[m] = fresh
                saveSnapshot(m, fresh)
            }
            // Something to show even when the calendar can't be reached
            months[m] = fresh ?: months[m] ?: SampleAvailability.busyBetween(m, m.plusMonths(1))
        }
        note = when {
            source === SampleAvailability -> "Showing sample bookings · connect the studio calendar in NovaBooking.kt"
            !failed -> "Live from the studio calendar"
            seen.isNotEmpty() -> "Couldn't reach the studio calendar just now, so these may be out of date"
            else -> "Couldn't reach the studio calendar, so these are sample bookings"
        }
        lastChecked = System.currentTimeMillis()
        lastReadFailed = failed
        val changes = diffBookings(before, after)
        if (!changes.isEmpty) latest = LiveChange(++counter, changes, before, now())
    }

    // Keeps every watched month in sync: straight away, then every second or few (or sooner if woken).
    // Run it only while the app is in front.
    suspend fun run(now: () -> Float) {
        while (true) {
            // This month and next are always watched (it rolls on by itself when the month changes); months gone are dropped
            val thisMonth = Day.today().firstOfMonth()
            listOf(thisMonth, thisMonth.plusMonths(1)).forEach { if (it !in watched) watched += it }
            watched.removeAll { it < thisMonth }
            readAll(watched.toList(), now)
            val wait = when {
                lastReadFailed -> CALENDAR_SYNC_RETRY_SECONDS    // back off while the calendar can't be reached
                bookScreens > 0 -> CALENDAR_SYNC_FAST_SECONDS     // someone's choosing a time: keep it fresh
                else -> CALENDAR_SYNC_SECONDS
            }
            withTimeoutOrNull(wait * 1000L) { wake.receive() }
        }
    }

    // ---------- Remembering what was seen, between visits ----------
    private fun key(m: Day) = "seen_${m.year}_${m.month}"

    private fun saveSnapshot(m: Day, blocks: List<BusyBlock>) {
        prefs.edit().putString(key(m), blocks.joinToString(";") { "${it.day.day},${it.start},${it.end}" }).apply()
    }

    private fun loadSnapshot(m: Day): List<BusyBlock>? {
        val text = prefs.getString(key(m), null) ?: return null
        if (text.isEmpty()) return emptyList()
        return text.split(";").mapNotNull { part ->
            val bits = part.split(",")
            if (bits.size != 3) null else BusyBlock(Day(m.year, m.month, bits[0].toInt()), bits[1].toFloat(), bits[2].toFloat())
        }
    }
}

// The calendar sync, handed to every screen
val LocalStudioCalendar = staticCompositionLocalOf<StudioCalendarSync?> { null }

// ---------- THE CELEBRATION ----------
// Drawn over the whole app for a few seconds when the calendar changes:
//   A new booking: a comet streaks across the sky and bursts into a nova (rings, rays and scattering sparks).
//   A booking gone: a gentle shower of golden stardust drifts down.
const val CELEBRATION_SECONDS = 3.4f

@Composable
fun BookingCelebration(change: LiveChange?, modifier: Modifier = Modifier) {
    val clock = rememberSecondsClock()
    // Only draw (and redraw) while it's playing
    var playing by remember { mutableStateOf(false) }
    LaunchedEffect(change?.id, change?.at) {
        if (change != null) { playing = true; delay((CELEBRATION_SECONDS * 1000).toLong()); playing = false }
    }
    if (!playing || change == null) return
    Canvas(modifier = modifier.fillMaxSize().graphicsLayer()) {
        val age = clock.value - change.at
        if (age < 0f || age > CELEBRATION_SECONDS) return@Canvas
        val w = size.width
        val h = size.height
        if (change.changes.added.isNotEmpty()) {
            val from = Offset(-w * 0.1f, h * 0.06f)
            val to = Offset(w * 0.62f, h * 0.3f)
            // The comet: a bright head with a long, fading tail
            if (age < 0.8f) {
                val q = easeOut(age / 0.8f)
                val head = from + (to - from) * q
                val tail = from + (to - from) * (q * 0.35f).coerceAtLeast(0f)
                drawLine(Brush.linearGradient(listOf(Color.Transparent, NovaPink.copy(alpha = 0.5f)), tail, head), tail, head, 9.dp.toPx(), StrokeCap.Round)
                drawLine(Brush.linearGradient(listOf(Color.Transparent, NovaStarlight), tail, head), tail, head, 2.5.dp.toPx(), StrokeCap.Round)
                softGlow(head, 26.dp.toPx(), NovaStarlight.copy(alpha = 0.7f))
            }
            // The nova where it lands
            val p = ((age - 0.7f) / (CELEBRATION_SECONDS - 0.7f)).coerceIn(0f, 1f)
            if (age > 0.7f) {
                val fade = 1f - p
                softGlow(to, (60.dp.toPx() + 140.dp.toPx() * easeOut(p)), NovaCorona.copy(alpha = 0.45f * fade))
                softGlow(to, 30.dp.toPx() * (1f + p), NovaPink.copy(alpha = 0.5f * fade))
                repeat(2) { k ->
                    val q = ((age - 0.7f - k * 0.25f) / 1.8f).coerceIn(0f, 1f)
                    if (q > 0f && q < 1f) drawCircle(if (k == 0) NovaCorona.copy(alpha = (1f - q) * 0.8f) else NovaPink.copy(alpha = (1f - q) * 0.6f),
                        20.dp.toPx() + 170.dp.toPx() * easeOut(q), to, style = Stroke((3f * (1f - q) + 0.5f).dp.toPx()))
                }
                // Rays shooting out
                repeat(12) { k ->
                    val a = k * 2f * PI.toFloat() / 12f + 0.2f
                    val dir = Offset(cos(a), sin(a))
                    val inner = 18.dp.toPx() + 60.dp.toPx() * easeOut(p)
                    val outer = inner + (if (k % 2 == 0) 70f else 40f) * density * (1f - p * 0.5f)
                    drawLine(NovaCorona.copy(alpha = 0.7f * fade), to + dir * inner, to + dir * outer, 1.5.dp.toPx(), StrokeCap.Round)
                }
                // Sparks scattering
                val rnd = Random(change.id)
                repeat(26) { k ->
                    val a = rnd.nextFloat() * 2f * PI.toFloat()
                    val speed = (60f + rnd.nextFloat() * 160f) * density
                    val at = to + Offset(cos(a), sin(a)) * (speed * easeOut(p))
                    val s = (1f + rnd.nextFloat() * 2.5f) * density * fade
                    val tint = when (k % 3) { 0 -> NovaCorona; 1 -> NovaPink; else -> NovaStarlight }
                    if (s > 0.3f) drawCircle(tint.copy(alpha = fade), s, at)
                }
                // A bright core
                drawCircle(NovaStarlight.copy(alpha = fade), 4.dp.toPx() * (1f - p * 0.7f), to)
            }
        }
        if (change.changes.removed.isNotEmpty()) {
            // Golden stardust drifting down across the screen
            val p = (age / CELEBRATION_SECONDS).coerceIn(0f, 1f)
            val rnd = Random(change.id * 31 + 7)
            repeat(46) { k ->
                val x0 = rnd.nextFloat() * w
                val y0 = -rnd.nextFloat() * h * 0.2f
                val fall = (40f + rnd.nextFloat() * 90f) * density
                val sway = sin(age * (1.5f + rnd.nextFloat()) + k) * 14f * density
                val at = Offset(x0 + sway, y0 + fall * age + h * 0.12f * easeOut(p))
                val twinkle = 0.5f + 0.5f * sin(age * 9f + k)
                val fade = (1f - p) * (p * 6f).coerceAtMost(1f)
                val s = (0.8f + rnd.nextFloat() * 1.8f) * density
                val tint = if (k % 4 == 0) NovaStarlight else NovaCorona
                drawCircle(tint.copy(alpha = (fade * twinkle).coerceIn(0f, 1f)), s, at)
                if (k % 7 == 0) {
                    val sz = s * 5f
                    drawPath(sparklePath(Size(sz, sz), at - Offset(sz / 2f, sz / 2f)), NovaStarlight.copy(alpha = (fade * twinkle * 0.8f).coerceIn(0f, 1f)))
                }
            }
        }
    }
}

// Quick at first, then settling
private fun easeOut(t: Float): Float { val u = 1f - t.coerceIn(0f, 1f); return 1f - u * u * u }
