// The booking calendar, drawn on canvases in the Nova Club cosmic style:
//   CosmicCalendar: a month where each day is a moon. How lit it is shows how open the day is for the session you've
//                   set up (full moon = wide open, crescent = filling up, new moon = fully booked).
//   DayOrbit:       one day's opening hours as an orbit from 10am to 11pm, with bookings as dark blocks, the Hybrid
//                   Loop setup time, and your session as a glowing bar you can tap or drag along.
package com.novacane.novaclub

// Layout, drawing, touch and text tools
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.roundToInt

// ---------- LIVE CHANGES ----------
// When the calendar is re-read and something has changed, these say what to animate and when it happened
// (`at` is the app's animation clock, in seconds).

// A booking that has just appeared (`added`) or just gone, to animate on the day's orbit
data class BookingFlash(val block: BusyBlock, val added: Boolean, val at: Float)

// A day whose moon has just changed: how lit it was before, and whether it got busier (`booked`) or freer
data class MoonShift(val day: Day, val from: Float?, val booked: Boolean, val at: Float)

// How long the animations run, in seconds
const val CHANGE_ANIMATION_SECONDS = 2.4f

// Easing: quick at first, gently settling
private fun easeOutCubic(t: Float): Float { val u = 1f - t.coerceIn(0f, 1f); return 1f - u * u * u }
// Easing: overshoots a little, then settles back, like a spring
private fun easeOutBack(t: Float): Float {
    val x = t.coerceIn(0f, 1f) - 1f
    return 1f + 2.7f * x * x * x + 1.7f * x * x
}

// The colour each session type paints the calendar in
fun sessionAccent(type: SessionType): Color = when (type) {
    SessionType.DRY_HIRE -> Color(0xFF9FB4FF)                         // cool cobalt-white
    SessionType.ENGINEERED -> NovaPink                                // Novacane hot pink
}

// Writes `text` centred on a point
private fun DrawScope.centredText(measurer: TextMeasurer, text: String, at: Offset, style: TextStyle) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
}

// One day's moon. `lit` (0 to 1) is how much of it shines; `rim` is its outline colour.
private fun DrawScope.dayMoon(c: Offset, r: Float, lit: Float, accent: Color, rim: Color) {
    clipPath(Path().apply { addOval(Rect(c, r)) }) {
        drawCircle(NovaPlumDeep, r, c)                                                        // the dark side
        if (lit > 0f) {
            // The lit side: pale, tinted by the session's colour, brightest towards the top-left
            drawCircle(Brush.radialGradient(listOf(NovaStarlight, lerp(accent, NovaStarlight, 0.35f)), c - Offset(r * 0.3f, r * 0.3f), r * 1.5f), r, c)
            // The shadow, slid across: the more open the day, the further it's slid away
            if (lit < 1f) drawCircle(NovaPlumDeep, r, c + Offset(r * 2f * lit, 0f))
        }
    }
    drawCircle(rim, r, c, style = Stroke(1.dp.toPx()))
}

// The glassy panel the calendar and timeline sit on
private fun Modifier.glassPanel(accent: Color): Modifier {
    val shape = RoundedCornerShape(22.dp)
    return this
        .fillMaxWidth()
        .clip(shape)
        .background(Brush.verticalGradient(listOf(NovaPlum.copy(alpha = 0.85f), NovaVoid.copy(alpha = 0.9f))))
        .border(1.dp, Brush.linearGradient(listOf(accent.copy(alpha = 0.4f), NovaPurple.copy(alpha = 0.3f), NovaCobalt.copy(alpha = 0.3f))), shape)
}

@Composable
fun CosmicCalendar(
    month: Day,                                     // any day in the month to show
    today: Day,
    selected: Day?,                                 // the chosen day (gets the turning Novacane ring)
    openness: (Day) -> Float?,                      // how open a day is (0 to 1), or null if it can't be booked
    accent: Color,                                  // the session type's colour
    loading: Boolean,                               // still reading the calendar
    onSelect: (Day) -> Unit,                        // a day was tapped
    onMonthChange: (Int) -> Unit,                   // -1 for the month before, +1 for the month after
    modifier: Modifier = Modifier,
    shifts: List<MoonShift> = emptyList()           // days whose moons have just changed (animated)
) {
    val measurer = rememberTextMeasurer()
    val clock = rememberSecondsClock()
    val first = month.firstOfMonth()
    val offset = first.weekdayMondayFirst                                   // empty cells before the 1st
    val days = first.daysInMonth
    val rows = (offset + days + 6) / 7
    val cellHeight = 37.dp
    // Always the latest values inside the touch handler
    val currentOpenness by rememberUpdatedState(openness)
    val currentOnSelect by rememberUpdatedState(onSelect)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.glassPanel(accent).padding(horizontal = 8.dp, vertical = 2.dp)) {
        // The month, with arrows either side (no going back before this month)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { onMonthChange(-1) }, enabled = first > today.firstOfMonth(), modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = if (first > today.firstOfMonth()) NovaBlush else NovaMuted.copy(alpha = 0.3f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("${MONTH_NAMES[first.month].uppercase()} ${first.year}", fontFamily = SourceCodePro, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 3.sp, color = accent)
                Text(if (loading) "Reading the studio calendar…" else "full moon = wide open · new moon = booked", fontSize = 9.sp, color = NovaMuted)
            }
            IconButton(onClick = { onMonthChange(1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = NovaBlush)
            }
        }
        // The days of the week
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, fontFamily = SourceCodePro, fontSize = 10.sp, color = NovaMuted.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
            }
        }
        // The month of moons
        Box(modifier = Modifier.fillMaxWidth().height(cellHeight * rows)) {
            // Everything that only changes when you change something (redrawn only then)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(first) {
                        detectTapGestures { tap ->
                            val col = (tap.x / (size.width / 7f)).toInt().coerceIn(0, 6)
                            val row = (tap.y / cellHeight.toPx()).toInt()
                            val index = row * 7 + col - offset
                            if (index in 0 until days) {
                                val day = Day(first.year, first.month, index + 1)
                                if ((currentOpenness(day) ?: 0f) > 0f) currentOnSelect(day)   // only days with a free time
                            }
                        }
                    }
            ) {
                val cellW = size.width / 7f
                val cellH = cellHeight.toPx()
                val r = 7.5.dp.toPx()
                val now = if (shifts.isNotEmpty()) clock.value else 0f            // only animate while something has changed
                for (i in 0 until days) {
                    val pos = offset + i
                    val c = Offset(cellW * (pos % 7 + 0.5f), cellH * (pos / 7) + cellH * 0.36f)
                    val day = Day(first.year, first.month, i + 1)
                    val target = openness(day)
                    // A moon that's just changed slides from its old brightness to its new one
                    val shift = shifts.firstOrNull { it.day == day }
                    val open = if (shift == null || target == null) target
                               else androidx.compose.ui.util.lerp(shift.from ?: target, target, easeOutCubic((now - shift.at) / 1.4f))
                    val bookable = open != null
                    val full = open == 0f
                    if (day == selected) softGlow(c, r * 2.8f, accent.copy(alpha = 0.4f))   // a glow behind the chosen day
                    dayMoon(
                        c, r, lit = open ?: 0f, accent = accent,
                        rim = when {
                            !bookable -> NovaMuted.copy(alpha = 0.18f)              // gone, or still loading
                            full -> NovaSignal.copy(alpha = 0.45f)                  // fully booked: a faint red edge
                            else -> accent.copy(alpha = 0.5f)
                        }
                    )
                    if (day == today) drawCircle(NovaCorona.copy(alpha = 0.9f), r + 3.dp.toPx(), c, style = Stroke(1.dp.toPx()))   // today: a gold ring
                    centredText(
                        measurer, "${i + 1}", Offset(c.x, cellH * (pos / 7) + cellH * 0.82f),
                        TextStyle(
                            fontFamily = SourceCodePro, fontSize = 9.sp,
                            fontWeight = if (day == selected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                day == selected -> accent
                                !bookable -> NovaMuted.copy(alpha = 0.3f)
                                full -> NovaMuted.copy(alpha = 0.55f)
                                else -> NovaStarlight.copy(alpha = 0.85f)
                            }
                        )
                    )
                }
            }
            // The chosen day's turning Novacane ring, and ripples on days that have just changed, on their own layer
            val ripples = shifts.filter { it.day.sameMonthAs(first) }
            if ((selected != null && selected.sameMonthAs(first)) || ripples.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize().graphicsLayer()) {
                    val cellW = size.width / 7f
                    val cellH = cellHeight.toPx()
                    fun centreOf(d: Day): Offset {
                        val pos = offset + d.day - 1
                        return Offset(cellW * (pos % 7 + 0.5f), cellH * (pos / 7) + cellH * 0.36f)
                    }
                    val t = clock.value
                    if (selected != null && selected.sameMonthAs(first)) brokenRing(centreOf(selected), 12.dp.toPx(), accent, t * 30f, 1.5.dp.toPx())
                    ripples.forEach { shift ->
                        val age = t - shift.at
                        if (age in 0f..CHANGE_ANIMATION_SECONDS) {
                            val c = centreOf(shift.day)
                            val r = 7.5.dp.toPx()
                            val colour = if (shift.booked) NovaSignal else NovaCorona      // red when it got busier, gold when it freed up
                            // Two rings rippling out, one after the other
                            repeat(2) { k ->
                                val q = ((age - k * 0.35f) / 1.6f).coerceIn(0f, 1f)
                                if (q > 0f && q < 1f) drawCircle(colour.copy(alpha = (1f - q) * 0.75f), r * (1f + 2.6f * easeOutCubic(q)), c, style = Stroke((2f - q).dp.toPx()))
                            }
                            // Freed up: four little sparkles flying out
                            if (!shift.booked) {
                                val q = (age / 1.8f).coerceIn(0f, 1f)
                                repeat(4) { k ->
                                    val a = k * PI.toFloat() / 2f + PI.toFloat() / 4f
                                    val at = c + Offset(kotlin.math.cos(a), kotlin.math.sin(a)) * (r * (1.2f + 2.4f * easeOutCubic(q)))
                                    val s = r * 0.9f * (1f - q)
                                    if (s > 0.5f) drawPath(sparklePath(Size(s, s), at - Offset(s / 2f, s / 2f)), NovaStarlight.copy(alpha = 1f - q))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayOrbit(
    busy: List<BusyBlock>,                          // that day's bookings
    start: Int?,                                    // the chosen start hour (null if nothing fits)
    hours: Int,                                     // how long the session is
    prep: Float,                                    // Hybrid Loop setup time before it (0 if not wanted)
    fits: Boolean,                                  // whether the session fits where it is
    accent: Color,                                  // the session type's colour
    onStartChange: (Int) -> Unit,                   // the session was tapped or dragged to a new start hour
    modifier: Modifier = Modifier,
    flashes: List<BookingFlash> = emptyList()       // bookings that have just appeared or gone on this day (animated)
) {
    val measurer = rememberTextMeasurer()
    val clock = rememberSecondsClock()
    val currentOnStart by rememberUpdatedState(onStartChange)
    val currentHours by rememberUpdatedState(hours)
    val currentPrep by rememberUpdatedState(prep)
    val span = (STUDIO_CLOSE_HOUR - STUDIO_OPEN_HOUR).toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            // Turns a finger position into a start hour, centring the session on the finger
            .pointerInput(Unit) {
                val edge = 14.dp.toPx()
                fun pick(x: Float) {
                    val hour = STUDIO_OPEN_HOUR + (x - edge) / (size.width - edge * 2) * span - currentHours / 2f
                    val lowest = STUDIO_OPEN_HOUR + kotlin.math.ceil(currentPrep).toInt()
                    currentOnStart(hour.roundToInt().coerceIn(lowest, STUDIO_CLOSE_HOUR - currentHours))
                }
                detectTapGestures { pick(it.x) }
            }
            .pointerInput(Unit) {
                val edge = 14.dp.toPx()
                detectHorizontalDragGestures { change, _ ->
                    val hour = STUDIO_OPEN_HOUR + (change.position.x - edge) / (size.width - edge * 2) * span - currentHours / 2f
                    val lowest = STUDIO_OPEN_HOUR + kotlin.math.ceil(currentPrep).toInt()
                    currentOnStart(hour.roundToInt().coerceIn(lowest, STUDIO_CLOSE_HOUR - currentHours))
                }
            }
    ) {
        val edge = 14.dp.toPx()
        val width = size.width - edge * 2
        fun x(hour: Float) = edge + (hour - STUDIO_OPEN_HOUR) / span * width
        val trackY = 42.dp.toPx()

        // The orbit: a faint track, glowing softly in the session's colour
        drawLine(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.1f), accent.copy(alpha = 0.35f), accent.copy(alpha = 0.1f)), startX = edge, endX = edge + width),
            Offset(edge, trackY), Offset(edge + width, trackY), 3.dp.toPx(), StrokeCap.Round)
        // Hour ticks, with a time under every other one
        for (h in STUDIO_OPEN_HOUR..STUDIO_CLOSE_HOUR) {
            val tx = x(h.toFloat())
            drawLine(NovaStarlight.copy(alpha = 0.25f), Offset(tx, trackY + 6.dp.toPx()), Offset(tx, trackY + 10.dp.toPx()), 1.dp.toPx())
            if ((h - STUDIO_OPEN_HOUR) % 2 == 0) centredText(measurer, hourLabel(h.toFloat()), Offset(tx, trackY + 21.dp.toPx()), TextStyle(fontFamily = SourceCodePro, fontSize = 9.sp, color = NovaMuted))
        }
        // Bookings: dark blocks with a faint red edge and hatching. A booking that's just arrived springs into place.
        val now = if (flashes.isNotEmpty()) clock.value else 0f            // only animate while something has changed
        val fullH = 16.dp.toPx()
        // A booking that's just arrived (even one joined onto another) is split off so it can land on its own
        val arrivals = flashes.filter { it.added }.map { it.block }
        val pieces = minusBlocks(busy, arrivals).map { it to 1f } + arrivals.map { a ->
            a to easeOutBack((now - flashes.first { it.added && it.block == a }.at - 0.35f) / 0.7f)   // lands just after the comet
        }
        pieces.forEach { (b, grow) ->
            val left = x(b.start.coerceAtLeast(STUDIO_OPEN_HOUR.toFloat()))
            val right = x(b.end.coerceAtMost(STUDIO_CLOSE_HOUR.toFloat()))
            val blockH = fullH * grow.coerceAtLeast(0f)
            if (right > left && blockH > 0.5f) {
                val top = trackY - blockH / 2f
                drawRoundRect(NovaPlumDeep.copy(alpha = 0.95f), Offset(left, top), Size(right - left, blockH), CornerRadius(5.dp.toPx()))
                clipPath(Path().apply { addRect(Rect(left, top, right, top + blockH)) }) {
                    var hx = left - blockH
                    while (hx < right) {
                        drawLine(NovaSignal.copy(alpha = 0.22f), Offset(hx, top + blockH), Offset(hx + blockH, top), 1.dp.toPx())
                        hx += 6.dp.toPx()
                    }
                }
                drawRoundRect(NovaSignal.copy(alpha = 0.45f), Offset(left, top), Size(right - left, blockH), CornerRadius(5.dp.toPx()), style = Stroke(1.dp.toPx()))
                if (right - left > 44.dp.toPx()) centredText(measurer, "BOOKED", Offset((left + right) / 2f, trackY), TextStyle(fontFamily = SourceCodePro, fontSize = 8.sp, color = NovaSignal.copy(alpha = 0.8f)))
            }
        }
        // The live changes on top: comets for new bookings, stardust for ones that have gone
        flashes.forEach { f ->
            val age = now - f.at
            if (age < 0f || age > CHANGE_ANIMATION_SECONDS) return@forEach
            val left = x(f.block.start.coerceAtLeast(STUDIO_OPEN_HOUR.toFloat()))
            val right = x(f.block.end.coerceAtMost(STUDIO_CLOSE_HOUR.toFloat()))
            if (right <= left) return@forEach
            val centre = Offset((left + right) / 2f, trackY)
            if (f.added) {
                // A comet streaking down onto the orbit
                if (age < 0.45f) {
                    val q = easeOutCubic(age / 0.45f)
                    val from = centre + Offset(-70.dp.toPx(), -40.dp.toPx())
                    val head = from + (centre - from) * q
                    val tail = from + (centre - from) * (q * 0.55f)
                    drawLine(Brush.linearGradient(listOf(Color.Transparent, NovaStarlight), tail, head), tail, head, 2.dp.toPx(), StrokeCap.Round)
                    drawCircle(NovaStarlight, 2.5.dp.toPx(), head)
                }
                // A red flash and a ring rippling out as it lands
                val p = ((age - 0.35f) / 1.6f).coerceIn(0f, 1f)
                if (age > 0.35f) {
                    softGlow(centre, (right - left) * 0.6f + 34.dp.toPx() * (1f - p), NovaSignal.copy(alpha = 0.55f * (1f - p)))
                    val w = (right - left) / 2f + 22.dp.toPx() * easeOutCubic(p)
                    val h = fullH / 2f + 14.dp.toPx() * easeOutCubic(p)
                    drawOval(NovaSignal.copy(alpha = 0.7f * (1f - p)), centre - Offset(w, h), Size(w * 2f, h * 2f), style = Stroke(1.5.dp.toPx()))
                }
            } else {
                // The booking fading and shrinking away...
                val p = (age / 1.8f).coerceIn(0f, 1f)
                val ghostH = fullH * (1f - 0.6f * p)
                drawRoundRect(NovaSignal.copy(alpha = 0.5f * (1f - p)), Offset(left, trackY - ghostH / 2f), Size(right - left, ghostH), CornerRadius(5.dp.toPx()), style = Stroke(1.dp.toPx()))
                // ...as it breaks up into stardust that drifts up and twinkles out
                val rnd = kotlin.random.Random(f.block.hashCode())
                repeat(34) { k ->
                    val x0 = left + rnd.nextFloat() * (right - left)
                    val y0 = trackY + (rnd.nextFloat() - 0.5f) * fullH
                    val vx = (rnd.nextFloat() - 0.5f) * 60.dp.toPx()
                    val vy = -(15.dp.toPx() + rnd.nextFloat() * 55.dp.toPx())
                    val size0 = (0.8f + rnd.nextFloat() * 1.6f).dp.toPx()
                    val tint = when (k % 3) { 0 -> NovaCorona; 1 -> NovaStarlight; else -> NovaPink }
                    val twinkle = 0.6f + 0.4f * kotlin.math.sin(age * 12f + k)
                    val fade = (1f - p).let { it * it }
                    drawCircle(tint.copy(alpha = (fade * twinkle).coerceIn(0f, 1f)), size0, Offset(x0 + vx * age, y0 + vy * age))
                }
            }
        }

        if (start != null) {
            val s = start.toFloat()
            val sessionH = 22.dp.toPx()
            val top = trackY - sessionH / 2f
            // The Hybrid Loop setup time just before, as a dashed gold slot
            if (prep > 0f) {
                val pl = x(s - prep)
                drawRoundRect(NovaCorona.copy(alpha = 0.22f), Offset(pl, trackY - 6.dp.toPx()), Size(x(s) - pl, 12.dp.toPx()), CornerRadius(3.dp.toPx()))
                drawRoundRect(NovaCorona.copy(alpha = 0.7f), Offset(pl, trackY - 6.dp.toPx()), Size(x(s) - pl, 12.dp.toPx()), CornerRadius(3.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
            }
            // The session: a glowing bar in the session's colour, or red if it clashes with a booking
            val left = x(s)
            val right = x(s + hours)
            val colour = if (fits) accent else NovaSignal
            softGlow(Offset((left + right) / 2f, trackY), (right - left) * 0.7f + 16.dp.toPx(), colour.copy(alpha = 0.3f))
            drawRoundRect(
                Brush.horizontalGradient(listOf(colour.copy(alpha = if (fits) 0.9f else 0.55f), lerp(colour, NovaStarlight, 0.35f).copy(alpha = if (fits) 0.95f else 0.6f)), startX = left, endX = right),
                Offset(left, top), Size(right - left, sessionH), CornerRadius(sessionH / 2f)
            )
            // A bright star at each end
            drawCircle(NovaStarlight, 2.5.dp.toPx(), Offset(left + sessionH / 2f, trackY))
            drawCircle(NovaStarlight, 2.5.dp.toPx(), Offset(right - sessionH / 2f, trackY))
            // The times above it
            centredText(
                measurer, if (fits) "${hourLabel(s)} – ${hourLabel(s + hours)}" else "CLASHES WITH A BOOKING",
                Offset(((left + right) / 2f).coerceIn(70.dp.toPx(), size.width - 70.dp.toPx()), 12.dp.toPx()),
                TextStyle(fontFamily = SourceCodePro, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = colour)
            )
        } else {
            centredText(measurer, "NO $hours-HOUR GAP ON THIS DAY", Offset(size.width / 2f, 12.dp.toPx()), TextStyle(fontFamily = SourceCodePro, fontSize = 11.sp, color = NovaSignal))
        }
    }
}

// The day's timeline with a one-line header, and the hours and Hybrid Loop switch side by side underneath, on a glass panel
@Composable
fun DayPanel(
    day: Day?,
    busy: List<BusyBlock>,
    start: Int?,
    hours: Int,
    prep: Float,
    fits: Boolean,
    freeStarts: Int,                                // how many start times fit on this day
    accent: Color,
    hybridLoop: Boolean,                            // whether the Hybrid Loop rack is wanted
    hybridNote: String,                             // e.g. "included" or "£25"
    onStartChange: (Int) -> Unit,
    onHoursChange: (Int) -> Unit,                   // -1 or +1
    onHybridChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    flashes: List<BookingFlash> = emptyList()       // bookings that have just appeared or gone on this day
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.glassPanel(accent).padding(horizontal = 6.dp, vertical = 8.dp)) {
        // Which day, and how many start times are free on it
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text((day?.label() ?: "Pick a day").uppercase(), fontFamily = SourceCodePro, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 2.sp, color = accent)
            if (day != null) Text("  ·  $freeStarts ${if (freeStarts == 1) "start" else "starts"} free · tap or drag to move", fontSize = 10.sp, color = NovaMuted)
        }
        // The orbit of the day
        DayOrbit(busy, start, hours, prep, fits, accent, onStartChange, flashes = flashes)
        // Hours on the left, Hybrid Loop on the right
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            // Hours: − n +, so you can see the session stretch and shrink above
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                IconButton(onClick = { onHoursChange(-1) }, enabled = hours > 1, modifier = Modifier.size(40.dp)) {
                    Text("−", fontSize = 22.sp, color = if (hours > 1) accent else NovaMuted.copy(alpha = 0.3f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
                    Text("$hours", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(if (hours == 1) "HOUR" else "HOURS", fontFamily = SourceCodePro, fontSize = 8.sp, letterSpacing = 2.sp, color = NovaMuted)
                }
                IconButton(onClick = { onHoursChange(1) }, enabled = hours < STUDIO_CLOSE_HOUR - STUDIO_OPEN_HOUR, modifier = Modifier.size(40.dp)) {
                    Text("+", fontSize = 22.sp, color = accent)
                }
            }
            // Hybrid Loop: needs setup time before the session, shown as the dashed gold slot on the orbit
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Hybrid Loop", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("+${(HYBRID_LOOP_PREP_HOURS * 60).toInt()} min setup · $hybridNote", fontSize = 9.sp, color = NovaCorona.copy(alpha = 0.85f))
                }
                Switch(checked = hybridLoop, onCheckedChange = onHybridChange)
            }
        }
    }
}

// Remembers the banner's last message (not watched by the screen, so updating it doesn't redraw anything)
private class HeldText { var text: String = "" }

// The banner that slides in when the calendar changes: a comet for a new booking, a sparkle when something frees up
@Composable
fun BookingToast(message: String?, booked: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {   // tap it to jump to the day
    val clock = rememberSecondsClock()
    // Keep the last message while it slides away
    val held = remember { HeldText() }
    if (message != null) held.text = message
    androidx.compose.animation.AnimatedVisibility(
        visible = message != null,
        enter = androidx.compose.animation.slideInVertically { -it } + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically { -it } + androidx.compose.animation.fadeOut(),
        modifier = modifier
    ) {
        val colour = if (booked) NovaSignal else NovaCorona
        val shape = RoundedCornerShape(50)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(shape)
                .background(NovaVoid)                                                    // solid, so nothing shows through
                .background(Brush.horizontalGradient(listOf(colour.copy(alpha = 0.35f), Color.Transparent, Color.Transparent)))
                .border(1.dp, colour.copy(alpha = 0.6f), shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // The little animated icon: a comet for a booking, a twinkling sparkle for a free slot
            Canvas(modifier = Modifier.size(20.dp).graphicsLayer()) {
                val t = clock.value
                if (booked) {
                    val head = center + Offset(4.dp.toPx(), 3.dp.toPx())
                    val tail = center + Offset(-8.dp.toPx(), -7.dp.toPx())
                    drawLine(Brush.linearGradient(listOf(Color.Transparent, colour), tail, head), tail, head, 2.dp.toPx(), StrokeCap.Round)
                    softGlow(head, 7.dp.toPx() * (0.85f + 0.15f * kotlin.math.sin(t * 5f)), colour.copy(alpha = 0.6f))
                    drawCircle(NovaStarlight, 2.5.dp.toPx(), head)
                } else {
                    val s = size.minDimension * (0.75f + 0.25f * kotlin.math.sin(t * 4f))
                    softGlow(center, size.minDimension * 0.6f, colour.copy(alpha = 0.5f))
                    drawPath(sparklePath(Size(s, s), center - Offset(s / 2f, s / 2f)), NovaStarlight)
                }
            }
            Text(held.text, fontFamily = SourceCodePro, fontSize = 11.sp, color = NovaStarlight, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
