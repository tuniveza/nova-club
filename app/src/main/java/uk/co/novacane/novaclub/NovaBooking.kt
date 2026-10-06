// The booking calendar's brain: days, studio hours, when the studio is already booked, and which sessions fit.
// Plain Kotlin with no screen code, so it can be tested on its own (see BookingLogicTest).
package com.novacane.novaclub

// Talking to Google Calendar over the internet, and reading its answer
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject
// Dates and times (the older Java tools, which work on every Android version the app supports)
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
// Maths and the sample data's fixed "random" numbers
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random

// ---------- STUDIO HOURS ----------
const val STUDIO_OPEN_HOUR = 10                 // opens at 10am
const val STUDIO_CLOSE_HOUR = 23                // closes at 11pm
// How long before a session the Hybrid Loop rack needs patching in (PLACEHOLDER: set the real time)
const val HYBRID_LOOP_PREP_HOURS = 0.5f
// The studio's time zone, so times always mean London time, whatever the phone is set to
val STUDIO_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Europe/London")

// ---------- KEEPING IN SYNC WITH THE STUDIO ----------
// The app reads the studio's bookings through Novabot's worker (STUDIO_WORKER_URL):
//   - The worker reads the studio's Google Calendar and its own bookings (each booking including the changeover time
//     kept free after it) and hands the app busy times only, no names. A booking made, moved or cancelled through
//     Novabot shows up the next time the app checks, within a second or two.
//   - Bookings that were just cancelled or moved are reported as cancelled for an hour, so the app can stop showing
//     them straight away.
//   - The studio's Google Calendar is read directly too, as a backup if the worker can't be reached. Novabot writes
//     its bookings into Google itself, straight away, but the app reuses Google's answer for a few seconds, so times
//     the worker says were just cancelled or moved are ignored in Google.
//   - While the app is open it checks every CALENDAR_SYNC_FAST_SECONDS on the Book screen and every
//     CALENDAR_SYNC_SECONDS elsewhere, straight away when the app comes back to the front or the Book screen opens,
//     and once more right before "Book and pay", so it can't offer a time that's just been taken. If the calendar
//     can't be reached, it waits CALENDAR_SYNC_RETRY_SECONDS before trying again.
const val CALENDAR_SYNC_SECONDS = 3               // anywhere else in the app
const val CALENDAR_SYNC_FAST_SECONDS = 1          // while the Book screen is open
const val CALENDAR_SYNC_RETRY_SECONDS = 5          // after a failed read (no signal, calendar unreachable)

// Novabot's worker, which reads the studio's bookings for the app (blank = Google Calendar only)
const val STUDIO_WORKER_URL = "https://novacane-worker.novacane-studio.workers.dev"
// How long the Google Calendar answer is reused (the worker has the latest, so this backup can lag a little)
const val GOOGLE_CALENDAR_REUSE_SECONDS = 15

// ---------- GOOGLE CALENDAR ----------
// Leave these blank to use the built-in sample bookings. To use the studio's real calendar:
//   1. In Google Calendar, open the studio calendar's settings. Under "Access permissions", tick
//      "Make available to public" and choose "See only free/busy (hide details)", so no client names are shown.
//   2. Copy the "Calendar ID" from "Integrate calendar" into GOOGLE_CALENDAR_ID.
//   3. In Google Cloud Console, turn on the Google Calendar API, create an API key, and restrict it to the
//      Calendar API and this Android app. Put it in local.properties as
//      novaclub.googleCalendarApiKey=YOUR_KEY (see README).
const val GOOGLE_CALENDAR_ID = "info@novacane.co.uk"
// The key comes from local.properties (novaclub.googleCalendarApiKey=...), which git ignores, so it
// never ends up in the repository. Blank = built-in sample bookings.
val GOOGLE_CALENDAR_API_KEY: String = uk.co.novacane.novaclub.BuildConfig.GOOGLE_CALENDAR_API_KEY

// ---------- DAYS ----------
// One calendar day in London. `month` counts from 0 (January) to 11 (December), as Java's calendar does.
data class Day(val year: Int, val month: Int, val day: Int) : Comparable<Day> {
    override fun compareTo(other: Day): Int = compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

    // This day as a Java calendar, at midnight London time
    fun toCalendar(): Calendar = Calendar.getInstance(STUDIO_TIME_ZONE, Locale.UK).apply {
        clear()
        set(year, month, day, 0, 0, 0)
    }

    fun plusDays(n: Int): Day = toCalendar().apply { add(Calendar.DAY_OF_MONTH, n) }.toDay()
    fun firstOfMonth(): Day = Day(year, month, 1)
    fun plusMonths(n: Int): Day = toCalendar().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, n) }.toDay()
    val daysInMonth: Int get() = toCalendar().getActualMaximum(Calendar.DAY_OF_MONTH)
    // 0 = Monday ... 6 = Sunday (Java counts Sunday = 1, Monday = 2 ... Saturday = 7)
    val weekdayMondayFirst: Int get() = (toCalendar().get(Calendar.DAY_OF_WEEK) + 5) % 7
    // Midnight at the start of this day, in milliseconds
    fun startMillis(): Long = toCalendar().timeInMillis
    fun sameMonthAs(other: Day): Boolean = year == other.year && month == other.month

    companion object {
        // Today, in London
        fun today(nowMillis: Long = System.currentTimeMillis()): Day =
            Calendar.getInstance(STUDIO_TIME_ZONE, Locale.UK).apply { timeInMillis = nowMillis }.toDay()
    }
}

fun Calendar.toDay(): Day = Day(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH))

val WEEKDAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
val MONTH_NAMES = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

// e.g. "Tue 6 Oct"
fun Day.label(): String = "${WEEKDAY_NAMES[weekdayMondayFirst]} $day ${MONTH_NAMES[month].take(3)}"

// e.g. 14 -> "2pm", 10.5 -> "10:30am", 12 -> "12pm", 24 -> "12am"
fun hourLabel(hour: Float): String {
    val whole = hour.toInt()
    val minutes = ((hour - whole) * 60f + 0.5f).toInt()
    val suffix = if (whole % 24 < 12) "am" else "pm"
    val clock = when (val h = whole % 12) { 0 -> 12; else -> h }
    return if (minutes == 0) "$clock$suffix" else "$clock:${minutes.toString().padStart(2, '0')}$suffix"
}

// ---------- BOOKINGS ALREADY IN THE CALENDAR ----------
// A time the studio is already booked: on one day, from `start` to `end` in hours after midnight (e.g. 14.5 = 2:30pm)
data class BusyBlock(val day: Day, val start: Float, val end: Float)

// The hour of the day (with minutes as a fraction) at a moment in time, in London
private fun hourOfDay(millis: Long): Float {
    val c = Calendar.getInstance(STUDIO_TIME_ZONE, Locale.UK).apply { timeInMillis = millis }
    return c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60f
}

// Turns a booking between two moments into one block per day it touches (a late session can run past midnight)
fun splitIntoDays(startMillis: Long, endMillis: Long): List<BusyBlock> {
    if (endMillis <= startMillis) return emptyList()
    val firstDay = Day.today(startMillis)
    val lastDay = Day.today(endMillis)
    val blocks = mutableListOf<BusyBlock>()
    var d = firstDay
    while (d <= lastDay) {
        val from = if (d == firstDay) hourOfDay(startMillis) else 0f
        val to = if (d == lastDay) hourOfDay(endMillis) else 24f
        if (to > from) blocks += BusyBlock(d, from, to)
        d = d.plusDays(1)
    }
    return blocks
}

// ---------- WHAT FITS ----------
// Whether a session starting at `start` for `hours` fits: inside opening hours, not before `earliest`, and clear of
// every booking (including `prep` hours beforehand for setting up the Hybrid Loop rack)
fun fits(start: Float, hours: Int, busy: List<BusyBlock>, prep: Float = 0f, earliest: Float = STUDIO_OPEN_HOUR.toFloat()): Boolean {
    val from = start - prep
    val to = start + hours
    if (from < STUDIO_OPEN_HOUR || to > STUDIO_CLOSE_HOUR || start < earliest) return false
    return busy.none { it.start < to && it.end > from }          // touching end-to-start is fine; any overlap isn't
}

// Every on-the-hour start time that fits on a day
fun startTimes(hours: Int, busy: List<BusyBlock>, prep: Float = 0f, earliest: Float = STUDIO_OPEN_HOUR.toFloat()): List<Int> =
    (STUDIO_OPEN_HOUR..STUDIO_CLOSE_HOUR - hours).filter { fits(it.toFloat(), hours, busy, prep, earliest) }

// How open a day is for a session of this length: the share of start times that fit (1 = wide open, 0 = fully booked)
fun openness(hours: Int, busy: List<BusyBlock>, prep: Float = 0f, earliest: Float = STUDIO_OPEN_HOUR.toFloat()): Float {
    val possible = STUDIO_CLOSE_HOUR - hours - STUDIO_OPEN_HOUR + 1
    if (possible <= 0) return 0f
    return startTimes(hours, busy, prep, earliest).size / possible.toFloat()
}

// The earliest a session can start on a day: opening time, or (today) the next whole hour from now.
// Days already gone can't be booked at all.
fun earliestStart(day: Day, nowMillis: Long = System.currentTimeMillis()): Float {
    val today = Day.today(nowMillis)
    return when {
        day < today -> Float.MAX_VALUE
        day == today -> maxOf(STUDIO_OPEN_HOUR.toFloat(), ceil(hourOfDay(nowMillis) + 0.01f))
        else -> STUDIO_OPEN_HOUR.toFloat()
    }
}

// Of the start times that fit, the closest to the one wanted (on a tie, the later one), or null if none fit
fun nearestStart(wanted: Int, starts: List<Int>): Int? =
    starts.minByOrNull { abs(it - wanted) * 2 + if (it < wanted) 1 else 0 }

// ---------- WHAT CHANGED SINCE LAST TIME ----------
// The calendar only says which times are busy, not who booked them, and back-to-back bookings arrive as one long block.
// So changes are worked out from the time itself: time that's newly busy was booked, time that's newly free was
// cancelled, and the same length freed in one place and taken in another was a booking rescheduled.
data class Moved(val from: BusyBlock, val to: BusyBlock)

data class BookingChanges(
    val booked: List<BusyBlock>,
    val cancelled: List<BusyBlock>,
    val moved: List<Moved> = emptyList()
) {
    val isEmpty: Boolean get() = booked.isEmpty() && cancelled.isEmpty() && moved.isEmpty()
    // For the animations: every time that's just been taken, and every time that's just come free
    val added: List<BusyBlock> get() = booked + moved.map { it.to }
    val removed: List<BusyBlock> get() = cancelled + moved.map { it.from }
}

// The time on each day covered by these blocks, with overlapping and touching blocks joined into one
fun mergeBlocks(blocks: List<BusyBlock>): List<BusyBlock> =
    blocks.sortedWith(compareBy({ it.day }, { it.start })).fold(mutableListOf()) { joined, b ->
        val last = joined.lastOrNull()
        if (last != null && last.day == b.day && b.start <= last.end) joined[joined.lastIndex] = last.copy(end = maxOf(last.end, b.end))
        else joined += b
        joined
    }

// The time covered by `a` that isn't covered by `b` (pieces shorter than a minute are left out)
fun minusBlocks(a: List<BusyBlock>, b: List<BusyBlock>): List<BusyBlock> {
    val cuts = mergeBlocks(b).groupBy { it.day }
    return mergeBlocks(a).flatMap { block ->
        var pieces = listOf(block.start to block.end)
        cuts[block.day].orEmpty().forEach { cut ->
            pieces = pieces.flatMap { (s, e) -> listOf(s to minOf(e, cut.start), maxOf(s, cut.end) to e).filter { it.second > it.first } }
        }
        pieces.filter { it.second - it.first > 1f / 60f }.map { BusyBlock(block.day, it.first, it.second) }
    }
}

fun diffBookings(old: List<BusyBlock>, new: List<BusyBlock>): BookingChanges {
    val booked = minusBlocks(new, old).toMutableList()
    val cancelled = minusBlocks(old, new).toMutableList()
    val moved = mutableListOf<Moved>()
    val oldWhole = mergeBlocks(old)
    val newWhole = mergeBlocks(new)
    fun length(b: BusyBlock) = b.end - b.start
    fun BusyBlock.within(list: List<BusyBlock>) = list.firstOrNull { it.day == day && it.start <= start && it.end >= end } ?: this
    cancelled.toList().forEach { freed ->
        // The same length taken somewhere else (preferring the same day): a reschedule
        val taken = booked.filter { abs(length(it) - length(freed)) < 0.02f }.minByOrNull { if (it.day == freed.day) 0 else 1 } ?: return@forEach
        booked -= taken; cancelled -= freed
        // Moved by less than its own length (e.g. 2–4pm to 3–5pm), only the edges change, so show the whole booking
        val from = freed.within(oldWhole)
        val to = taken.within(newWhole)
        moved += if (abs(length(from) - length(to)) < 0.02f) Moved(from, to) else Moved(freed, taken)
    }
    return BookingChanges(booked, cancelled, moved)
}

// A one-line description of what changed, for the banner, e.g. "Just booked · Thu 8 Oct, 2pm–4pm"
fun describeChanges(changes: BookingChanges): String {
    fun span(b: BusyBlock) = "${hourLabel(b.start)}–${hourLabel(b.end)}"
    val total = changes.booked.size + changes.cancelled.size + changes.moved.size
    if (total == 1) {
        changes.booked.firstOrNull()?.let { return "Just booked · ${it.day.label()}, ${span(it)}" }
        changes.cancelled.firstOrNull()?.let { return "Cancelled · ${it.day.label()}, ${span(it)}" }
        val m = changes.moved.first()
        return if (m.from.day == m.to.day) "Rescheduled · ${m.to.day.label()}, ${hourLabel(m.from.start)} → ${hourLabel(m.to.start)}"
        else "Rescheduled · ${m.from.day.label()} → ${m.to.day.label()}, ${span(m.to)}"
    }
    val parts = mutableListOf<String>()
    if (changes.booked.isNotEmpty()) parts += "${changes.booked.size} booked"
    if (changes.moved.isNotEmpty()) parts += "${changes.moved.size} rescheduled"
    if (changes.cancelled.isNotEmpty()) parts += "${changes.cancelled.size} cancelled"
    return parts.joinToString(" · ")
}

// ---------- WHERE THE BOOKINGS COME FROM ----------
interface AvailabilitySource {
    val name: String                                         // shown under the calendar, e.g. "Google Calendar"
    // Every booking from the start of `from` up to (not including) `until`. Runs over the internet, so call it off
    // the main thread.
    fun busyBetween(from: Day, until: Day): List<BusyBlock>
}

// Made-up bookings, the same every time for the same day, until a real calendar is connected
object SampleAvailability : AvailabilitySource {
    override val name = "sample bookings"
    override fun busyBetween(from: Day, until: Day): List<BusyBlock> {
        val blocks = mutableListOf<BusyBlock>()
        var d = from
        while (d < until) {
            val rnd = Random(d.year * 10_000 + d.month * 100 + d.day)
            if (rnd.nextInt(9) == 0) {
                blocks += BusyBlock(d, STUDIO_OPEN_HOUR.toFloat(), STUDIO_CLOSE_HOUR.toFloat())   // about one day in nine fully booked
            } else {
                repeat(rnd.nextInt(4)) {                                                         // otherwise up to three bookings
                    val start = STUDIO_OPEN_HOUR + rnd.nextInt(STUDIO_CLOSE_HOUR - STUDIO_OPEN_HOUR - 1)
                    val length = 1 + rnd.nextInt(4)
                    blocks += BusyBlock(d, start.toFloat(), minOf(start + length, STUDIO_CLOSE_HOUR).toFloat())
                }
            }
            d = d.plusDays(1)
        }
        return blocks
    }
}

// The studio's real Google Calendar, read with Google's "free/busy" lookup (it only ever returns busy times, no details)
class GoogleCalendarAvailability(private val calendarId: String, private val apiKey: String) : AvailabilitySource {
    override val name = "Google Calendar"

    override fun busyBetween(from: Day, until: Day): List<BusyBlock> {
        val utc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        // What we're asking: the busy times on this calendar between these two moments
        val request = JSONObject()
            .put("timeMin", utc.format(Date(from.startMillis())))
            .put("timeMax", utc.format(Date(until.startMillis())))
            .put("timeZone", STUDIO_TIME_ZONE.id)
            .put("items", JSONArray().put(JSONObject().put("id", calendarId)))
        val url = URL("https://www.googleapis.com/calendar/v3/freeBusy?key=" + URLEncoder.encode(apiKey, "UTF-8"))
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000                          // give up after 10 seconds
            readTimeout = 10_000
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(request.toString().toByteArray()) }
            if (connection.responseCode !in 200..299) throw IOException("Google Calendar replied ${connection.responseCode}")
            val reply = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val calendar = reply.getJSONObject("calendars").getJSONObject(calendarId)
            if (calendar.has("errors")) throw IOException("Google Calendar couldn't read the calendar: ${calendar.getJSONArray("errors")}")
            val busy = calendar.optJSONArray("busy") ?: JSONArray()
            return (0 until busy.length()).flatMap { i ->
                val b = busy.getJSONObject(i)
                splitIntoDays(parseCalendarTime(b.getString("start")), parseCalendarTime(b.getString("end")))
            }
        } finally {
            connection.disconnect()
        }
    }
}

// Reads a time like "2026-10-06T13:00:00Z" or "2026-10-06T14:00:00+01:00" into milliseconds
fun parseCalendarTime(text: String): Long =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(text.replace(Regex("\\.\\d+"), ""))!!.time

// e.g. "2026-10-06", the way the worker wants dates
fun Day.iso(): String = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)

// What the studio's calendar says right now, through Novabot's worker: every booking and blocked-off time, plus
// bookings just cancelled or moved away (so they can be ignored in the app's slightly older copy of Google Calendar)
class StudioBookings(val busy: List<BusyBlock>, val cancelled: List<BusyBlock>)

fun readStudioViaWorker(workerUrl: String, from: Day, until: Day): StudioBookings {
    val url = URL("$workerUrl/club/busy?from=${from.iso()}&until=${until.iso()}")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 5_000                               // give up quickly, so Google Calendar can step in
        readTimeout = 10_000
        useCaches = false                                    // always the latest
    }
    try {
        if (connection.responseCode !in 200..299) throw IOException("The studio calendar replied ${connection.responseCode}")
        return readWorkerReply(connection.inputStream.bufferedReader().use { it.readText() })
    } finally {
        connection.disconnect()
    }
}

// Reads the worker's answer: {"busy": [[start, end], ...], "cancelled": [[start, end], ...]} in milliseconds
fun readWorkerReply(text: String): StudioBookings {
    val reply = JSONObject(text)
    fun blocks(name: String): List<BusyBlock> {
        val list = reply.optJSONArray(name) ?: return emptyList()
        return (0 until list.length()).flatMap { i -> list.getJSONArray(i).let { splitIntoDays(it.getLong(0), it.getLong(1)) } }
    }
    return StudioBookings(blocks("busy"), blocks("cancelled"))
}

// The studio's bookings: from the worker the moment they change, plus anything else busy in Google Calendar
class StudioAvailability(private val workerUrl: String, private val google: AvailabilitySource?) : AvailabilitySource {
    override val name = "studio calendar"
    // Google's last answer for each stretch of dates, and when it came
    private val googleSeen = mutableMapOf<Pair<Day, Day>, Pair<Long, List<BusyBlock>>>()

    // Google Calendar's busy times (reused for a few seconds), or null if it can't be reached and never has been
    private fun googleBusy(from: Day, until: Day): List<BusyBlock>? {
        val calendar = google ?: return emptyList()
        val key = from to until
        val now = System.currentTimeMillis()
        val last = synchronized(googleSeen) { googleSeen[key] }
        if (last != null && now - last.first < GOOGLE_CALENDAR_REUSE_SECONDS * 1000L) return last.second
        val fresh = runCatching { calendar.busyBetween(from, until) }.getOrNull() ?: return last?.second
        synchronized(googleSeen) { googleSeen[key] = now to fresh }
        return fresh
    }

    override fun busyBetween(from: Day, until: Day): List<BusyBlock> {
        val studio = runCatching { readStudioViaWorker(workerUrl, from, until) }
        val fromGoogle = googleBusy(from, until)
        // The worker can't be reached: Google Calendar on its own, or give up if that fails too
        val bookings = studio.getOrNull() ?: return fromGoogle ?: throw studio.exceptionOrNull()!!
        return combineCalendars(bookings, fromGoogle.orEmpty())
    }
}

// Everything the worker says is busy, plus whatever's busy in Google Calendar apart from times the worker says just
// came free
fun combineCalendars(studio: StudioBookings, google: List<BusyBlock>): List<BusyBlock> =
    mergeBlocks(studio.busy + minusBlocks(google, studio.cancelled))

// The studio's bookings through the worker (with Google Calendar alongside) if set up above, otherwise Google Calendar on its own,
// otherwise the sample bookings
fun availabilitySource(): AvailabilitySource {
    val google = if (GOOGLE_CALENDAR_ID.isNotBlank() && GOOGLE_CALENDAR_API_KEY.isNotBlank()) GoogleCalendarAvailability(GOOGLE_CALENDAR_ID, GOOGLE_CALENDAR_API_KEY) else null
    return when {
        STUDIO_WORKER_URL.isNotBlank() -> StudioAvailability(STUDIO_WORKER_URL, google)
        google != null -> google
        else -> SampleAvailability
    }
}
