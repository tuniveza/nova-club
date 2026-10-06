// Same package name as NovaScreens.kt, so the screens can use these effects
package com.novacane.novaclub

// Animation tools
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
// Layout tools
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
// Material parts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// State tools
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
// Styling tools
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
// The area pads: rounded corners on drawn shapes
import androidx.compose.ui.geometry.CornerRadius
// The navigation dock: tab icons, a bouncy slide, and keeping clear of the phone's own navigation bar
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Drawing tools: points, sizes, gradients, outlines, and finger tracking
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
// Sigil line: tap and drag handling, flowing dashes, and keeping callbacks fresh
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.rememberUpdatedState
// Stripe cards: tapping, outlines, the uneven image tile, text shadow, and the shared clock
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.State
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
// Galaxy panels and cosmic sigils: blending colours, cutting one shape out of another, circles as boxes
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.positionChange
// Sigil constellation ribbons: trimming to their shape, and moving/turning them along the line
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
// Separate drawing layers, so animations only redraw themselves
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
// Sigil constellation: trimming to a box, smooth corners on outlines, and the angle of a line
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.atan2
// Maths for orbits, twinkling and gravity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
// Fixed "random" numbers, so the sky is the same every time
import kotlin.random.Random
// Lets us wait between letters
import kotlinx.coroutines.delay

// A full circle, in radians
private const val TAU = (2 * PI).toFloat()

// 1. CENTRED TITLE: full width + text told to sit in the middle
@Composable
fun CenteredTitle(title: String, subtitle: String) {
    // Stack the lines, and centre everything horizontally
    Column(
        modifier = Modifier.fillMaxWidth(),                    // take the whole width
        horizontalAlignment = Alignment.CenterHorizontally     // put children in the middle
    ) {
        // Big title, centred
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        // Small line, centred
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// 2. TYPEWRITER TEXT: types itself out one letter at a time
@Composable
fun TypewriterText(
    text: String,                 // the full sentence
    fontSize: Int = 30,           // size in sp
    letterDelayMs: Long = 70      // pause between letters (bigger = slower)
) {
    // How much of the text is showing right now
    var shown by remember { mutableStateOf("") }
    // Runs once when this appears (and again if the text changes)
    LaunchedEffect(text) {
        // Start empty
        shown = ""
        // Add one letter at a time
        for (i in text.indices) {
            shown = text.take(i + 1)   // show the first i+1 letters
            delay(letterDelayMs)       // wait a moment
        }
    }
    // Draw whatever's showing so far, centred
    Text(
        shown.uppercase(),                    // the letters typed so far, in capitals like the website's headings
        fontSize = fontSize.sp,               // how big
        fontWeight = FontWeight.Medium,       // medium weight, like the other titles
        letterSpacing = 6.sp,                 // extra-wide spacing, like a mission name on a spacecraft
        textAlign = TextAlign.Center,         // in the middle
        modifier = Modifier.fillMaxWidth()    // as wide as the screen, so "middle" means the screen's middle
    )
}

// 3. GLOW: gently fades up and down, like a valve warming
@Composable
fun Glow(content: @Composable () -> Unit) {
    // A timer that runs forever
    val transition = rememberInfiniteTransition(label = "glow")
    // A value that drifts between 0.55 and 1.0 and back
    val glowAlpha by transition.animateFloat(
        initialValue = 0.55f,                                                // dimmest
        targetValue = 1f,                                                    // brightest
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse), // 1.6s each way
        label = "glowAlpha"                                                  // a name for debugging tools
    )
    // Draw the content with that changing see-through-ness
    Box(modifier = Modifier.alpha(glowAlpha)) { content() }
}

// 4. VU METER: a row of bouncing bars, like a level meter on a desk
@Composable
fun VuMeter(
    barColor: Color = MaterialTheme.colorScheme.onPrimary,  // bar colour
    barCount: Int = 12                                      // how many bars
) {
    // One shared timer for all the bars
    val transition = rememberInfiniteTransition(label = "vu")
    // A row of bars, all sitting on the bottom edge
    Row(
        modifier = Modifier.height(28.dp),                     // meter height
        horizontalArrangement = Arrangement.spacedBy(3.dp),    // gap between bars
        verticalAlignment = Alignment.Bottom                   // bars grow upwards
    ) {
        // Make each bar
        repeat(barCount) { i ->
            // Each bar bounces at a slightly different speed so they don't move together
            val level by transition.animateFloat(
                initialValue = 0.15f,                                                    // lowest
                targetValue = 1f,                                                        // highest
                animationSpec = infiniteRepeatable(tween(400 + i * 97 % 500), RepeatMode.Reverse), // speed for this bar
                label = "bar$i"                                                          // a name for debugging tools
            )
            // The bar itself: thin, rounded, height follows the level
            Box(
                modifier = Modifier
                    .width(4.dp)                                       // thin
                    .fillMaxHeight(level)                              // how tall right now
                    .background(barColor, RoundedCornerShape(2.dp))    // colour and soft ends
            )
        }
    }
}

// 5. COSMOS BACKGROUND: deep space with drifting nebulae, twinkling stars and shooting stars,
//    above a planet's horizon lit by a rising star. Stars nearby lean towards your finger.

// One star. Positions are shares of the screen (0 to 1), so the sky fits any phone.
private class Star(
    val x: Float,          // across (0 = left, 1 = right)
    val y: Float,          // down (0 = top, 1 = bottom)
    val depth: Float,      // 0 = far away (small, dim, slow), 1 = close (big, bright, quicker)
    val twinkle: Float,    // how fast it twinkles
    val phase: Float,      // where in its twinkle it starts, so they don't all blink together
    val tint: Color        // mostly starlight, with the odd pink or gold one
)

// The same sky every time the app opens (the seed fixes the "random" numbers)
private val sky: List<Star> = Random(7).let { rnd ->
    List(120) {
        Star(
            x = rnd.nextFloat(),                              // anywhere across
            y = rnd.nextFloat() * 0.84f,                      // above the planet
            depth = rnd.nextFloat().let { it * it },          // squared, so most are far away and only a few are close
            twinkle = 0.5f + rnd.nextFloat() * 2f,            // between slow and quick
            phase = rnd.nextFloat() * TAU,                    // a random start in the twinkle
            tint = when (rnd.nextInt(12)) {                   // 1 in 12 pink, 1 in 12 gold, 1 in 12 blush
                0 -> NovaPink
                1 -> NovaCorona
                2 -> NovaBlush
                else -> NovaStarlight
            }
        )
    }
}

// A soft, round cloud of coloured gas: bright in the middle, fading to nothing at the edge
private fun DrawScope.nebula(color: Color, strength: Float, centre: Offset, radius: Float) {
    drawRect(Brush.radialGradient(listOf(color.copy(alpha = strength), Color.Transparent), centre, radius))
}

@Composable
fun CosmosBackground(content: @Composable () -> Unit) {
    // Where the finger last was on the screen
    var finger by remember { mutableStateOf(Offset.Zero) }
    // Whether a finger is touching (or a stylus is hovering) right now
    var active by remember { mutableStateOf(false) }
    // How strongly the sky reacts: eases from 0 to 1 when touched, and back when let go
    val strength by animateFloatAsState(
        targetValue = if (active) 1f else 0f,     // full pull while touching, none otherwise
        animationSpec = tween(700),               // fade in and out over 0.7 seconds
        label = "touchStrength"                   // a name for debugging tools
    )
    // Seconds since the sky appeared (it never loops, so shooting stars can be told apart). This is also the clock the
    // rest of the app animates by, handed down below.
    val clock = rememberOwnClock()

    // A full-screen box: space drawn behind, the app drawn on top
    Box(
        modifier = Modifier
            .fillMaxSize()                                         // cover the whole screen
            .pointerInput(Unit) {                                  // listen to fingers anywhere on screen
                awaitPointerEventScope {                           // start listening
                    while (true) {                                 // keep listening forever
                        // Peek at each touch before the buttons see it (we only watch, we don't steal it)
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        // The first finger on the screen
                        val change = event.changes.first()
                        // Remember where it is
                        finger = change.position
                        // A finger counts while pressed; a stylus or mouse counts while hovering over the app
                        active = change.pressed || (change.type != PointerType.Touch && event.type != PointerEventType.Exit)
                    }
                }
            }
    ) {
        // The drawing surface for the whole sky, on a layer of its own so it can redraw without redrawing the app on top
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer()) {
            // Read the clock here, so only the drawing repeats each frame (not the whole screen)
            val t = clock.value
            val w = size.width
            val h = size.height

            // --- Deep space ---
            drawRect(NovaVoid)

            // --- Nebulae: three slow clouds in the brand colours, drifting past each other ---
            nebula(NovaMagenta, 0.34f, Offset(w * (0.18f + 0.07f * sin(t * 0.21f)), h * 0.16f), w * 0.95f)                       // magenta, top-left
            nebula(Color(0xFF5A1C86), 0.45f, Offset(w * (0.92f - 0.07f * sin(t * 0.17f)), h * (0.42f + 0.05f * sin(t * 0.29f))), w) // purple, right
            nebula(NovaCobalt, 0.30f, Offset(w * (0.08f + 0.05f * cos(t * 0.13f)), h * 0.62f), w * 0.8f)                          // cobalt, low left

            // --- A soft pink glow under the finger, only while touching ---
            if (strength > 0f) nebula(NovaPink, 0.28f * strength, finger, size.minDimension * 0.6f)

            // --- Stars ---
            val reach = size.minDimension * 0.45f                     // how far the finger's pull reaches
            val hair = 0.6.dp.toPx()                                  // width of the flare lines on close stars
            sky.forEach { star ->
                // Drift slowly right (close stars a little quicker), wrapping round at the edge
                var x = ((star.x + t * 0.003f * (0.2f + star.depth)) % 1f) * w
                var y = star.y * h
                // Gravity: how close the finger is (1 = right on it, 0 = out of reach)
                val dx = finger.x - x
                val dy = finger.y - y
                val near = (1f - sqrt(dx * dx + dy * dy) / reach).coerceAtLeast(0f) * strength
                // Lean towards the finger, closer stars more so
                val lean = near * (0.12f + 0.25f * star.depth)
                x += dx * lean
                y += dy * lean
                // Twinkle between about half and full brightness
                val glimmer = 0.55f + 0.45f * sin(t * star.twinkle + star.phase)
                // Far stars are dim, close ones bright, and stars near the finger light up
                val alpha = ((0.25f + 0.75f * star.depth) * glimmer + 0.5f * near).coerceIn(0f, 1f)
                // Close stars are bigger, and swell near the finger
                val radius = (0.6f + 1.6f * star.depth).dp.toPx() * (1f + 0.6f * near)
                drawCircle(star.tint.copy(alpha = alpha), radius, Offset(x, y))
                // The closest stars get a little cross-shaped flare, like a photo through a telescope
                if (star.depth > 0.7f) {
                    val arm = radius * 4f * glimmer                     // flare length breathes with the twinkle
                    val flare = star.tint.copy(alpha = alpha * 0.5f)    // fainter than the star
                    drawLine(flare, Offset(x - arm, y), Offset(x + arm, y), hair)   // across
                    drawLine(flare, Offset(x, y - arm), Offset(x, y + arm), hair)   // up and down
                }
            }

            // --- Shooting stars: one every few seconds, each on its own path ---
            val period = 6.5f                                         // seconds between shooting stars
            val p = (t % period) / 1.1f                               // progress across the sky (takes 1.1 seconds; over 1 means it's gone)
            if (p < 1f) {
                val rnd = Random(floor(t / period).toInt())           // a different path for each one
                val start = Offset(w * (0.35f + 0.6f * rnd.nextFloat()), h * (0.05f + 0.35f * rnd.nextFloat()))  // somewhere in the top half
                val heading = 0.45f + 0.4f * rnd.nextFloat()          // angle in radians, heading down and to the left
                val dir = Offset(-cos(heading), sin(heading))         // that angle as a direction
                val fade = sin(p * PI.toFloat())                      // fades in, then out
                val head = start + dir * (w * 0.55f * p)              // the bright tip
                val tail = head - dir * (w * 0.22f * fade)            // the trail behind it, longest halfway across
                drawLine(
                    brush = Brush.linearGradient(listOf(Color.Transparent, NovaStarlight.copy(alpha = 0.9f * fade)), tail, head),
                    start = tail, end = head, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round
                )
                drawCircle(NovaStarlight.copy(alpha = fade), 1.6.dp.toPx(), head)   // the tip itself
            }

            // --- The planet: a huge curve along the bottom, its edge lit by a rising star ---
            val planetR = w * 1.7f                                    // radius: much wider than the screen, so it reads as a horizon
            val horizon = h * 0.80f                                   // where the top of the planet sits
            val core = Offset(w * 0.5f, horizon + planetR)            // the planet's centre, far below the screen
            // Where the star is rising: creeps very slowly along the horizon
            val sunX = w * (0.66f + 0.04f * sin(t * 0.05f))
            val sun = Offset(sunX, core.y - sqrt(planetR * planetR - (sunX - core.x) * (sunX - core.x)))
            // The star's brightness swells and settles, like a nova
            val pulse = 0.85f + 0.15f * sin(t * 1.3f)
            // Atmosphere: a magenta haze hugging the horizon
            val haze = planetR * 1.09f
            drawCircle(
                brush = Brush.radialGradient(
                    0.90f to Color.Transparent,                         // clear inside
                    planetR / haze to NovaMagenta.copy(alpha = 0.45f),  // brightest right at the planet's edge
                    1f to Color.Transparent,                            // fading out into space
                    center = core, radius = haze
                ),
                radius = haze, center = core
            )
            // The planet's night side: dark plum at the edge, sinking to black
            drawCircle(
                brush = Brush.verticalGradient(listOf(NovaPlum, NovaVoid), startY = horizon, endY = horizon + h * 0.18f),
                radius = planetR, center = core
            )
            // The rising star's glow, spilling over the horizon
            nebula(NovaCorona, 0.50f * pulse, sun, w * 0.45f)
            // The rim light: brightest gold where the star is, fading to pink and then almost nothing
            val at = sunX / w
            drawCircle(
                brush = Brush.horizontalGradient(
                    0f to NovaPink.copy(alpha = 0.08f),
                    at - 0.25f to NovaPink.copy(alpha = 0.45f),
                    at to NovaCorona.copy(alpha = 0.95f),
                    at + 0.25f to NovaPink.copy(alpha = 0.45f),
                    1f to NovaPink.copy(alpha = 0.08f),
                    startX = 0f, endX = w
                ),
                radius = planetR, center = core, style = Stroke(1.5.dp.toPx())
            )
            // A thin flare stretched along the horizon, like a lens catching the light
            val flareHalf = w * 0.35f * pulse
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, NovaCorona.copy(alpha = 0.7f * pulse), Color.Transparent),
                    startX = sunX - flareHalf, endX = sunX + flareHalf
                ),
                start = Offset(sunX - flareHalf, sun.y), end = Offset(sunX + flareHalf, sun.y), strokeWidth = 1.dp.toPx()
            )
            // The star itself: a hot white point
            drawCircle(NovaStarlight.copy(alpha = pulse), 2.5.dp.toPx(), sun)
        }
        // The app itself, drawn on top of space, sharing the clock
        CompositionLocalProvider(LocalNovaClock provides clock) { content() }
    }
}

// 7. SIGIL SLASHES: three slanted strokes, borrowed from the strokes that make the N in the Novacane sigil
@Composable
fun SigilSlashes(color: Color = NovaPink) {
    // A tiny drawing surface, a bit wider than it is tall
    Canvas(modifier = Modifier.size(width = 20.dp, height = 11.dp)) {
        // How wide each stroke is
        val stroke = size.width / 6f
        // How far each stroke leans over (same lean as the sigil's N)
        val lean = size.height * 0.55f
        // Draw three strokes side by side
        repeat(3) { i ->
            // Where this stroke's bottom-left corner sits
            val x = i * stroke * 1.9f
            // A slanted four-sided shape: bottom-left, top-left, top-right, bottom-right
            val path = Path().apply {
                moveTo(x, size.height)                    // bottom-left
                lineTo(x + lean, 0f)                      // top-left (leaning right)
                lineTo(x + lean + stroke, 0f)             // top-right
                lineTo(x + stroke, size.height)           // bottom-right
                close()                                   // join back up
            }
            // Fill it in, each stroke a little fainter than the last
            drawPath(path, color.copy(alpha = 1f - i * 0.25f))
        }
    }
}

// ---------- RING HELPERS: shared by the orbiting sigil, the faceplate and the tier badges ----------

// Draw half of a tilted ring: the back half (hidden behind whatever it circles) or the front half
private fun DrawScope.ringHalf(centre: Offset, rx: Float, ry: Float, tilt: Float, color: Color, width: Float, front: Boolean) {
    rotate(tilt, centre) {                                       // tip the ring over by `tilt` degrees
        drawArc(
            color = color,
            startAngle = if (front) 0f else 180f,                // front = the lower half, back = the upper half
            sweepAngle = 180f,                                   // half a circle
            useCenter = false,                                   // just the curve, no pie slice
            topLeft = Offset(centre.x - rx, centre.y - ry),      // the ring's bounding box
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(width)
        )
    }
}

// Where a moon sits on a tilted ring, `angle` radians round from the right-hand side
private fun ringPoint(centre: Offset, rx: Float, ry: Float, tilt: Float, angle: Float): Offset {
    val px = rx * cos(angle)                                     // across, before tilting
    val py = ry * sin(angle)                                     // up and down, before tilting
    val tip = tilt * PI.toFloat() / 180f                         // the tilt in radians
    return Offset(centre.x + px * cos(tip) - py * sin(tip), centre.y + px * sin(tip) + py * cos(tip))
}

// A moon: a small dot with a soft halo
private fun DrawScope.moon(at: Offset, radius: Float, color: Color) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent), at, radius * 4f), radius * 4f, at)  // halo
    drawCircle(color, radius, at)                                                                                          // the moon
}

// 8. FACEPLATE TRIM: Novacane decorations painted behind a card. The sigil watermark becomes a ringed
//    planet with a moon going round it, in a field of faint stars, framed by telescope-sight corner brackets.
//    `orbit` is the moon's angle in radians; pass a lambda so only the drawing repeats as it moves.

// Faint stars scattered over the plate: across, down, and brightness (all 0 to 1)
private val plateSpecks = Random(11).let { rnd -> List(30) { Triple(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()) } }

fun Modifier.faceplateTrim(sigil: Painter, orbit: () -> Float = { 0.8f }): Modifier = drawBehind {
    // --- Faint stars across the whole plate ---
    plateSpecks.forEach { (x, y, b) ->
        drawCircle(NovaStarlight.copy(alpha = 0.12f + 0.4f * b), (0.5f + b).dp.toPx(), Offset(x * size.width, y * size.height))
    }

    // --- The sigil watermark, big and faint, partly hanging off the right edge ---
    val markSize = size.height * 1.25f                           // a bit taller than the card
    val markLeft = size.width - markSize * 0.70f                 // push it right so about a third is cropped off
    val markTop = (size.height - markSize) / 2f                  // centred top to bottom
    // --- A tilted ring round the watermark, so the sigil reads as a planet ---
    val centre = Offset(markLeft + markSize / 2f, size.height / 2f)
    val rx = markSize * 0.50f                                    // ring width (kept just round the sigil, clear of the writing)
    val ry = markSize * 0.13f                                    // ring height (flat, seen nearly edge-on)
    val tilt = -16f                                              // tipped down to the left
    val ringColor = NovaBlush.copy(alpha = 0.16f)
    val line = 1.dp.toPx()
    ringHalf(centre, rx, ry, tilt, ringColor, line, front = false)                // back half, behind the planet
    translate(left = markLeft, top = markTop) {
        with(sigil) { draw(Size(markSize, markSize), alpha = 0.10f) }           // the logo, very see-through
    }
    ringHalf(centre, rx, ry, tilt, ringColor, line, front = true)                 // front half, in front of the planet
    // The moon, gold, riding the ring
    val angle = orbit()
    moon(ringPoint(centre, rx, ry, tilt, angle), 2.5.dp.toPx(), NovaCorona.copy(alpha = if (sin(angle) > 0f) 0.9f else 0.35f))  // dimmer when it's behind

    // --- Corner brackets with the ends cut on a slant, like the sigil's strokes (and a telescope's sight) ---
    val inset = 8.dp.toPx()                                      // how far in from the edge
    val arm = 12.dp.toPx()                                       // how long each arm is
    val cut = 3.dp.toPx()                                        // how much the slanted tip leans
    val bracketLine = 1.5.dp.toPx()                              // line thickness
    val trim = NovaBlush.copy(alpha = 0.8f)                      // pale pink
    // For each corner: which way is "inwards" sideways (sx) and up/down (sy)
    listOf(
        Offset(inset, inset) to Offset(1f, 1f),                                  // top-left
        Offset(size.width - inset, inset) to Offset(-1f, 1f),                    // top-right
        Offset(inset, size.height - inset) to Offset(1f, -1f),                   // bottom-left
        Offset(size.width - inset, size.height - inset) to Offset(-1f, -1f)      // bottom-right
    ).forEach { (corner, dir) ->
        // One L-shape: along the top/bottom, round the corner, down/up the side, with slanted tips
        val bracket = Path().apply {
            moveTo(corner.x + dir.x * arm, corner.y + dir.y * cut)   // slanted tip of the sideways arm
            lineTo(corner.x + dir.x * (arm - cut), corner.y)         // into the arm
            lineTo(corner.x, corner.y)                               // the corner itself
            lineTo(corner.x, corner.y + dir.y * (arm - cut))         // down/up the side arm
            lineTo(corner.x + dir.x * cut, corner.y + dir.y * arm)   // slanted tip of the side arm
        }
        drawPath(bracket, trim, style = Stroke(width = bracketLine)) // draw it as a thin line
    }
}

// 9. SIGIL ORBIT: the Novacane sigil as a glowing planet, with two moons on tilted rings going round it

// Draw both rings and moons, either the parts behind the sigil or the parts in front
private fun DrawScope.sigilOrbits(turn: Float, front: Boolean, scale: Float) {
    val r = 40.dp.toPx() * scale                                                 // roughly the sigil's radius
    // Each ring: width, height, tilt in degrees, the moon's angle, its colour and size
    val outer = ringPoint(center, r * 2.1f, r * 0.45f, -12f, turn)
    val inner = ringPoint(center, r * 1.6f, r * 0.32f, 18f, -turn * 1.7f + 2f)
    ringHalf(center, r * 2.1f, r * 0.45f, -12f, NovaBlush.copy(alpha = if (front) 0.55f else 0.25f), 1.dp.toPx(), front)
    ringHalf(center, r * 1.6f, r * 0.32f, 18f, NovaPink.copy(alpha = if (front) 0.45f else 0.2f), 1.dp.toPx(), front)
    // A moon is in front when it's on the lower half of its ring
    if ((sin(turn) > 0f) == front) moon(outer, 3.dp.toPx(), NovaPink)
    if ((sin(-turn * 1.7f + 2f) > 0f) == front) moon(inner, 2.dp.toPx(), NovaCorona)
}

@Composable
fun SigilOrbit(sigil: Painter, modifier: Modifier = Modifier, scale: Float = 1f) {   // scale: 1 = full size, smaller to save room
    // One shared timer for the moons and the glow
    val transition = rememberInfiniteTransition(label = "orbit")
    // Goes once round (0 to 2π) every 12 seconds, at a steady speed
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = TAU,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "turn"
    )
    // The corona swells and settles
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "corona"
    )
    // Three layers on top of each other: the glow and back of the rings, the sigil, then the front of the rings
    Box(modifier = modifier.fillMaxWidth().height(140.dp * scale), contentAlignment = Alignment.Center) {
        // Behind the sigil (on its own layer, so redrawing it doesn't redraw anything else)
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer()) {
            val glow = 80.dp.toPx() * scale * pulse                                  // how far the corona reaches
            drawCircle(
                Brush.radialGradient(listOf(NovaCorona.copy(alpha = 0.30f), NovaMagenta.copy(alpha = 0.18f), Color.Transparent), center, glow),
                glow
            )
            sigilOrbits(turn, front = false, scale = scale)
        }
        // The sigil itself
        Image(painter = sigil, contentDescription = "Novacane Studios", modifier = Modifier.size(84.dp * scale))
        // In front of the sigil
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer()) { sigilOrbits(turn, front = true, scale = scale) }
    }
}

// 10. SPARKLE AND STAR DIVIDER: a four-pointed star, and a fading hairline with one in the middle

// A four-pointed star filling the given box, with its sides curving in through the middle
internal fun sparklePath(box: Size, at: Offset = Offset.Zero): Path = Path().apply {
    val cx = at.x + box.width / 2f                               // centre across
    val cy = at.y + box.height / 2f                              // centre down
    moveTo(cx, at.y)                                             // top point
    quadraticTo(cx, cy, at.x + box.width, cy)                    // curve in, out to the right point
    quadraticTo(cx, cy, cx, at.y + box.height)                   // to the bottom point
    quadraticTo(cx, cy, at.x, cy)                                // to the left point
    quadraticTo(cx, cy, cx, at.y)                                // and back to the top
    close()
}

@Composable
fun Sparkle(color: Color = NovaCorona, size: Dp = 12.dp) {
    Canvas(modifier = Modifier.size(size)) { drawPath(sparklePath(this.size), color) }
}

@Composable
fun StarDivider(modifier: Modifier = Modifier) {
    // The line and the sparkle stacked, both centred
    Box(modifier = modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.7f).height(1.dp).background(NovaHorizonGradient))   // the fading line
        Sparkle()                                                                                    // the star on it
    }
}

// 11. TIER BADGE: each membership tier is a body in the night sky, getting brighter as you go up

// What each tier is, from the smallest to the brightest
fun tierBody(tier: Tier): String = when (tier) {
    Tier.NON_MEMBER -> "Moon"
    Tier.SYNDICATE -> "Planet"
    Tier.PRO -> "Star"
    Tier.ELITE -> "Supernova"
}

@Composable
fun TierBadge(tier: Tier, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(40.dp)) {
        val c = center
        val r = size.minDimension / 2f
        when (tier) {
            // A small moon, lit from the top-left, with two craters
            Tier.NON_MEMBER -> {
                drawCircle(Brush.radialGradient(listOf(NovaBlush, NovaMuted, NovaPlumDeep), c + Offset(-r * 0.25f, -r * 0.25f), r * 0.7f), r * 0.42f, c)
                drawCircle(NovaMuted.copy(alpha = 0.6f), r * 0.09f, c + Offset(r * 0.12f, -r * 0.08f))
                drawCircle(NovaMuted.copy(alpha = 0.5f), r * 0.06f, c + Offset(-r * 0.14f, r * 0.16f))
            }
            // A purple planet with a ring round it
            Tier.SYNDICATE -> {
                ringHalf(c, r * 0.95f, r * 0.26f, -20f, NovaBlush.copy(alpha = 0.6f), 1.5.dp.toPx(), front = false)
                drawCircle(Brush.radialGradient(listOf(NovaPink, NovaPurple, NovaIndigo), c + Offset(-r * 0.2f, -r * 0.2f), r * 0.75f), r * 0.5f, c)
                ringHalf(c, r * 0.95f, r * 0.26f, -20f, NovaBlush, 1.5.dp.toPx(), front = true)
            }
            // A bright pink star with a glow
            Tier.PRO -> {
                drawCircle(Brush.radialGradient(listOf(NovaPink.copy(alpha = 0.55f), Color.Transparent), c, r), r, c)
                drawPath(sparklePath(Size(r * 1.5f, r * 1.5f), c - Offset(r * 0.75f, r * 0.75f)), NovaBlush)
                drawCircle(NovaStarlight, r * 0.12f, c)
            }
            // A supernova: a gold corona, eight rays, and a shock ring racing outwards
            Tier.ELITE -> {
                drawCircle(Brush.radialGradient(listOf(NovaCorona.copy(alpha = 0.7f), NovaMagenta.copy(alpha = 0.35f), Color.Transparent), c, r), r, c)
                repeat(8) { i ->
                    val a = i * TAU / 8f                                  // this ray's direction
                    val dir = Offset(cos(a), sin(a))
                    val reach = if (i % 2 == 0) r * 0.98f else r * 0.62f  // long and short rays, alternating
                    drawLine(NovaCorona.copy(alpha = 0.85f), c + dir * (r * 0.25f), c + dir * reach, 1.2.dp.toPx(), StrokeCap.Round)
                }
                drawCircle(NovaPink.copy(alpha = 0.5f), r * 0.72f, c, style = Stroke(1.dp.toPx()))
                drawCircle(NovaStarlight, r * 0.18f, c)
            }
        }
    }
}

// 12. SIGIL CONSTELLATION: sigils (the Novacane logo as little planets, each with a corona and an orbiting moon)
//     joined by ribbons of images with uneven, drifting edges. Drag any sigil to move it; tap one to trigger something.

// One join between two sigils, and how it looks
data class SigilLink(
    val from: Int,                                                   // which sigil it starts at (its place in the list)
    val to: Int,                                                     // which sigil it ends at
    val color: Color,                                                // the ribbon's colour (and the glow round its sigils)
    val pictures: List<Painter> = emptyList(),                       // images scrolling along inside the ribbon
    val thickness: Dp = 30.dp,                                       // how thick the ribbon is
    val speed: Dp = 22.dp,                                           // how far the images scroll each second
    val roughness: Float = 0.35f                                     // how far the edges wobble in and out (0 = smooth)
)

@Composable
fun SigilConstellation(
    sigils: List<Offset>,                                            // where each sigil is, in pixels
    links: List<SigilLink>,                                          // which sigils are joined, and how
    onSigilTap: (Int) -> Unit,                                       // called with the sigil's number when it's tapped
    onSigilDrag: (Int, Offset) -> Unit,                              // called while a sigil is dragged, with how far it moved
    modifier: Modifier = Modifier,                                   // lets the caller size and position it
    sigilImage: Painter? = null                                      // the picture for each sigil (null draws a spinning diamond instead)
) {
    // Seconds since it appeared, ticking every frame (one clock for every ribbon, so they can each go their own speed)
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val begin = withFrameNanos { it }
        while (true) withFrameNanos { seconds = (it - begin) / 1_000_000_000f }
    }

    val currentSigils by rememberUpdatedState(sigils)                // always the latest positions, without restarting touch handling
    val currentOnTap by rememberUpdatedState(onSigilTap)             // always the latest tap action
    val currentOnDrag by rememberUpdatedState(onSigilDrag)           // always the latest drag action

    // Which sigil (if any) is under a point: the nearest one within reach, or -1
    fun hit(p: Offset, reach: Float): Int =
        currentSigils.indices.minByOrNull { (currentSigils[it] - p).getDistance() }
            ?.takeIf { (currentSigils[it] - p).getDistance() < reach } ?: -1

    Canvas(
        modifier
            .fillMaxSize()                                           // fill the space given
            .pointerInput(Unit) {                                    // listen for taps
                val reach = 32.dp.toPx()                             // how close a finger must be to hit a sigil
                detectTapGestures { tap -> hit(tap, reach).takeIf { it >= 0 }?.let { currentOnTap(it) } }
            }
            .pointerInput(Unit) {                                    // listen for drags
                val reach = 32.dp.toPx()                             // how close a finger must be to grab a sigil
                awaitEachGesture {                                   // handle one touch at a time, from finger down to finger up
                    val down = awaitFirstDown(requireUnconsumed = false)  // wait for a finger to touch
                    val grabbed = hit(down.position, reach)          // which sigil was grabbed (-1 for empty space)
                    if (grabbed < 0) return@awaitEachGesture         // empty space: leave the touch alone so the page can scroll
                    // Wait until the finger has moved far enough to count as a drag (a tap ends here instead)
                    val first = awaitTouchSlopOrCancellation(down.id) { change, over ->
                        change.consume()                             // claim the touch, so the page doesn't scroll
                        currentOnDrag(grabbed, over)                 // move the sigil by that first bit of movement
                    } ?: return@awaitEachGesture                     // lifted without dragging: it was a tap
                    drag(first.id) { change ->                       // keep following the finger until it lifts
                        currentOnDrag(grabbed, change.positionChange())  // how far to move it (read this first: it reads as zero once consumed)
                        change.consume()                             // stop other things reacting to this touch
                    }
                }
            }
    ) {
        val t = seconds                                              // read the clock here, so only the drawing repeats each frame
        // The ribbons first, so the sigils sit on top of their ends
        links.forEachIndexed { i, link ->
            if (link.from in sigils.indices && link.to in sigils.indices) {
                imageRibbon(sigils[link.from], sigils[link.to], link, t, seed = i * 3.1f)
            }
        }
        // Then the sigils, each glowing in the colour of the first ribbon that reaches it
        sigils.forEachIndexed { i, at ->
            val color = links.firstOrNull { it.from == i || it.to == i }?.color ?: NovaPink
            cosmicSigil(at, 16.dp.toPx(), t, i, color, sigilImage)
        }
    }
}

// The original two-sigil line, now drawn as a one-ribbon constellation. `lineWidth` is the ribbon's thickness.
@Composable
fun SigilLine(
    start: Offset,                                                   // where the line begins
    end: Offset,                                                     // where the line ends
    onSigilTap: (Int) -> Unit,                                       // called with 0 (start sigil) or 1 (end sigil) when tapped
    onSigilDrag: (Int, Offset) -> Unit,                              // called while a sigil is dragged, with how far it moved
    color: Color = Color(0xFFB14CFF),                                // ribbon and sigil colour
    modifier: Modifier = Modifier,                                   // lets the caller size and position it
    lineWidth: Dp = 30.dp,                                           // how thick the ribbon is
    cards: List<Painter> = emptyList(),                              // images scrolling along inside it
    sigilImage: Painter? = null                                      // the picture for each sigil (null draws a spinning diamond)
) = SigilConstellation(
    sigils = listOf(start, end),
    links = listOf(SigilLink(0, 1, color, cards, lineWidth)),
    onSigilTap = onSigilTap,
    onSigilDrag = onSigilDrag,
    modifier = modifier,
    sigilImage = sigilImage
)

// Specks of light for the ribbons' texture, the same every time: where along (0 to 1), how far across (-1 to 1),
// size, brightness, and where in its flicker it starts
private val lineSpecks = Random(23).let { rnd ->
    List(40) { floatArrayOf(rnd.nextFloat(), rnd.nextFloat() * 2f - 1f, rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()) }
}

// One ribbon between two sigils: a band with uneven, drifting edges (like torn paper or smoke), filled with the
// link's colour, images scrolling along it, a sheen sweeping through, and a sprinkle of flickering specks.
private fun DrawScope.imageRibbon(start: Offset, end: Offset, link: SigilLink, seconds: Float, seed: Float) {
    val along = end - start
    val length = along.getDistance()
    if (length < 1f) return                                          // the sigils are on top of each other: nothing to draw
    val half = link.thickness.toPx() / 2f                            // half the ribbon's thickness
    val pi = PI.toFloat()
    val wave = seconds * 0.9f                                        // slowly shifts the edges' wobble
    val steps = (length / 5.dp.toPx()).toInt().coerceIn(12, 120)     // more points on longer ribbons, so the edges stay detailed

    // How far out the edge is at `x` pixels along, on one side. Three waves of different sizes make it look random,
    // and it pinches in towards the sigils at each end.
    fun edge(x: Float, side: Float): Float {
        val u = x / length
        val s = seed + side * 7.3f                                   // each side gets its own pattern
        val wobble = 0.55f * sin(u * 7f * pi + s + wave * 2f) +
            0.30f * sin(u * 19f * pi + s * 2f - wave * 3f) +
            0.15f * sin(u * 43f * pi + s * 3f + wave * 4f)
        val pinch = 0.45f + 0.55f * sin(u * pi).coerceAtLeast(0f).pow(0.5f)
        return half * (1f - link.roughness * 0.57f + link.roughness * wobble) * pinch
    }

    withTransform({
        translate(start.x, start.y)                                  // measure from the start sigil...
        rotate(atan2(along.y, along.x) * 180f / pi, Offset.Zero)     // ...with "along the ribbon" pointing right
    }) {
        // The ribbon's outline: along the top edge, then back along the bottom
        val outline = Path()
        for (i in 0..steps) {
            val x = length * i / steps
            if (i == 0) outline.moveTo(x, -edge(x, 1f)) else outline.lineTo(x, -edge(x, 1f))
        }
        for (i in steps downTo 0) {
            val x = length * i / steps
            outline.lineTo(x, edge(x, -1f))
        }
        outline.close()

        // A soft haze round the outside, following the uneven edge
        drawPath(outline, link.color.copy(alpha = 0.22f), style = Stroke(half * 0.9f, join = StrokeJoin.Round))

        // Everything inside is trimmed to the ribbon's shape
        clipPath(outline) {
            // Background: the link's colour across the middle, deep indigo at the edges
            drawRect(
                Brush.verticalGradient(listOf(NovaIndigo, link.color, NovaIndigo), startY = -half * 1.3f, endY = half * 1.3f),
                Offset(0f, -half * 1.3f), Size(length, half * 2.6f)
            )
            // The images, in a row, scrolling from the start sigil towards the end one
            if (link.pictures.isNotEmpty()) {
                val tile = half * 1.6f                                // each image's size
                val spacing = tile * 1.3f                             // distance from one image to the next
                val scrolled = seconds * link.speed.toPx()            // how far the row has moved in total
                val shift = scrolled % spacing                        // how far past the last whole step
                val passed = floor(scrolled / spacing).toInt()        // how many whole steps, so each image keeps its picture as it moves
                for (n in -1..(length / spacing).toInt() + 1) {
                    val x = n * spacing + shift                       // this image's centre along the ribbon
                    val picture = link.pictures[Math.floorMod(n - passed, link.pictures.size)]
                    val src = picture.intrinsicSize
                    // Scale to fill the square and centre it (like ContentScale.Crop), so photos don't get squashed
                    val fill = if (src.isSpecified && src.width > 0f && src.height > 0f) maxOf(tile / src.width, tile / src.height) else 1f
                    val drawn = if (src.isSpecified) Size(src.width * fill, src.height * fill) else Size(tile, tile)
                    clipRect(x - tile / 2f, -tile / 2f, x + tile / 2f, tile / 2f) {
                        translate(x - drawn.width / 2f, -drawn.height / 2f) { with(picture) { draw(drawn, alpha = 0.95f) } }
                    }
                }
            }
            // A sheen of light sweeping along every few seconds
            val sweep = ((seconds / 3.5f + seed) % 1f) * (length + half * 4f) - half * 2f
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, NovaStarlight.copy(alpha = 0.22f), Color.Transparent), startX = sweep - half * 1.5f, endX = sweep + half * 1.5f),
                Offset(sweep - half * 1.5f, -half * 1.3f), Size(half * 3f, half * 2.6f)
            )
            // Flickering specks drifting along, for a grainy, see-through texture
            lineSpecks.forEach { s ->
                val x = ((s[0] + seconds * 0.08f) % 1f) * length
                val flicker = 0.5f + 0.5f * sin(seconds * 6f + s[4] * 6.28f)
                drawCircle(NovaStarlight.copy(alpha = 0.45f * s[3] * flicker), (0.5f + 1.3f * s[2]).dp.toPx(), Offset(x, s[1] * half * 0.9f))
            }
        }
        // A thin pale-pink edge following the uneven outline
        drawPath(outline, NovaBlush.copy(alpha = 0.7f), style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
    }
}

// A sigil as a little planet: a corona in its colour, a dark disc, the Novacane logo, and a tilted orbit ring with a
// moon going round (passing behind the logo, then in front). Without a picture it falls back to the spinning diamond.
private fun DrawScope.cosmicSigil(c: Offset, radius: Float, seconds: Float, index: Int, color: Color, logo: Painter?) {
    val pulse = 1f + 0.15f * sin(seconds * 2.6f + index)             // the corona breathes, each sigil out of step
    if (logo == null) {
        drawSigil(c, radius, seconds * 60f, pulse, color)            // the original diamond
        return
    }
    val rx = radius * 1.75f                                          // orbit ring width
    val ry = radius * 0.55f                                          // orbit ring height (seen nearly edge-on)
    val tilt = -20f + index * 17f                                    // each sigil's ring tipped differently
    val angle = seconds * (1.2f + 0.15f * index) + index             // where the moon is on its ring
    val moonAt = ringPoint(c, rx, ry, tilt, angle)
    val moonInFront = sin(angle) > 0f                                // on the lower half of the ring = in front

    // Corona: the sigil's colour fading out, with a touch of gold in the middle
    val glow = radius * 2.4f * pulse
    drawCircle(Brush.radialGradient(listOf(NovaCorona.copy(alpha = 0.35f), color.copy(alpha = 0.35f), Color.Transparent), c, glow), glow, c)
    // Back of the ring, and the moon if it's behind
    ringHalf(c, rx, ry, tilt, color.copy(alpha = 0.55f), 1.5.dp.toPx(), front = false)
    if (!moonInFront) moon(moonAt, 2.5.dp.toPx(), NovaCorona)
    // A dark disc so the logo stands out over the ribbon's end, then the logo
    drawCircle(NovaVoid.copy(alpha = 0.85f), radius * 1.05f, c)
    translate(c.x - radius * 1.2f, c.y - radius * 1.2f) { with(logo) { draw(Size(radius * 2.4f, radius * 2.4f)) } }
    // Front of the ring, and the moon if it's in front
    ringHalf(c, rx, ry, tilt, color, 1.5.dp.toPx(), front = true)
    if (moonInFront) moon(moonAt, 2.5.dp.toPx(), NovaCorona)
}

fun DrawScope.drawSigil(c: Offset, size: Float, spin: Float, pulse: Float, color: Color) {
    rotate(spin, pivot = c) {                                        // rotate everything inside around the sigil's centre
        val diamond = Path().apply {                                 // build a diamond outline
            moveTo(c.x, c.y - size)                                  // top point
            lineTo(c.x + size, c.y)                                  // right point
            lineTo(c.x, c.y + size)                                  // bottom point
            lineTo(c.x - size, c.y)                                  // left point
            close()                                                  // join back to the top
        }
        drawPath(diamond, color, style = Stroke(width = 3.5.dp.toPx())) // draw the diamond as a bold outline
    }
    drawCircle(color, radius = size * 0.35f * pulse, center = c)     // pulsing solid core in the middle
}

// 13. STRIPE CARD: one list item. A galaxy panel (deep space, nebulae, a side-on galaxy and twinkling stars) runs between
//     two cosmic sigils, with one image and a title and subtitle on it. Stack several for a list.

// The app's shared animation clock, handed down from the cosmos background so everything animates on the same beat
val LocalNovaClock = staticCompositionLocalOf<State<Float>?> { null }

// A clock: seconds since it started, ticking about 30 times a second. That's plenty for slow, cosmic movement, and
// half the work of redrawing every frame, which keeps the app responsive on slower phones.
@Composable
private fun rememberOwnClock(): State<Float> {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val begin = withFrameNanos { it }                                         // the first frame's time
        while (true) {
            seconds.floatValue = withFrameNanos { it - begin } / 1_000_000_000f    // nanoseconds to seconds
            delay(18)                                                             // skip a frame before the next tick
        }
    }
    return seconds
}

// The shared clock if there is one (there always is inside the app), otherwise a clock of its own.
// Read `.value` inside drawing code, so only the drawing repeats.
@Composable
fun rememberSecondsClock(): State<Float> = LocalNovaClock.current ?: rememberOwnClock()

// A rounded square with gently uneven sides, for the image on a stripe card
private val WobblyShape = GenericShape { size, _ ->
    val count = 36                                                   // points around the edge
    val pts = List(count) { k ->
        val a = k * 2f * PI.toFloat() / count                        // angle round the centre
        val c = cos(a)
        val s = sin(a)
        // Between a circle and a square, so it reads as a rounded tile
        val r = 1f / ((abs(c) / (size.width / 2f)).pow(4f) + (abs(s) / (size.height / 2f)).pow(4f)).pow(0.25f)
        val wobble = 1f + 0.05f * sin(3f * a + 1.3f) + 0.035f * sin(5f * a - 0.7f)   // the bulges and dips
        Offset(size.width / 2f + c * r * wobble * 0.94f, size.height / 2f + s * r * wobble * 0.94f)  // 0.94 keeps it inside its box
    }
    // Join the points with smooth curves
    val first = (pts.last() + pts[0]) / 2f
    moveTo(first.x, first.y)
    pts.forEachIndexed { i, p ->
        val mid = (p + pts[(i + 1) % count]) / 2f
        quadraticTo(p.x, p.y, mid.x, mid.y)
    }
    close()
}

// ---------- GALAXY PANELS ----------

// Stars for the galaxy panels, the same every time: along (0 to 1), across (-1 to 1), size, brightness, twinkle start.
// Each panel shifts and flips them, so no two panels share a sky.
private val panelStars = Random(77).let { rnd ->
    List(36) { floatArrayOf(rnd.nextFloat(), rnd.nextFloat() * 2f - 1f, rnd.nextFloat().let { it * it }, rnd.nextFloat(), rnd.nextFloat() * 6.28f) }
}

// A horizontal panel of deep space from `left` to `right`, centred on `midY`: soft nebulae in the panel's colour,
// a galaxy seen side-on, and gently twinkling stars. Its edges ripple very slightly and pinch in under the sigils.
private fun DrawScope.galaxyPanel(
    left: Float, right: Float, midY: Float, half: Float, color: Color, seconds: Float, seed: Int,
    wash: List<Color>? = null,                                       // optional colours washed across the panel, left to right
    sheen: Color = NovaStarlight.copy(alpha = 0.06f)                 // the colour of the light that sweeps through
) {
    val length = right - left
    if (length < 1f) return
    val pi = PI.toFloat()
    val wave = seconds * 0.25f                                       // the edges drift very slowly
    val steps = (length / 12.dp.toPx()).toInt().coerceIn(12, 40)     // points along each edge (enough for a gentle ripple)

    // How far out the edge is at `x`, on one side: a gentle ripple, pinching in towards each end
    fun edge(x: Float, side: Float): Float {
        val u = (x - left) / length
        val s = seed * 3.1f + side * 7.3f                            // each side gets its own pattern
        val ripple = 0.6f * sin(u * 5f * pi + s + wave * 2f) + 0.4f * sin(u * 13f * pi + s * 2f - wave * 3f)
        val pinch = 0.55f + 0.45f * sin(u * pi).coerceAtLeast(0f).pow(0.4f)
        return half * (0.94f + 0.06f * ripple) * pinch
    }
    val outline = Path()
    for (i in 0..steps) {                                            // along the top...
        val x = left + length * i / steps
        if (i == 0) outline.moveTo(x, midY - edge(x, 1f)) else outline.lineTo(x, midY - edge(x, 1f))
    }
    for (i in steps downTo 0) {                                      // ...and back along the bottom
        val x = left + length * i / steps
        outline.lineTo(x, midY + edge(x, -1f))
    }
    outline.close()


    clipPath(outline) {
        // Deep space, warmed towards the panel's colour through the middle
        drawRect(
            Brush.verticalGradient(listOf(NovaVoid, lerp(NovaVoid, color, 0.30f), NovaVoid), startY = midY - half, endY = midY + half),
            Offset(left, midY - half * 1.2f), Size(length, half * 2.4f)
        )
        // An optional colour wash across the whole panel
        if (wash != null) drawRect(Brush.horizontalGradient(wash, startX = left, endX = right), Offset(left, midY - half * 1.2f), Size(length, half * 2.4f))
        // Nebulae: three soft clouds, drifting very slowly
        val drift = sin(seconds * 0.08f + seed) * half * 0.6f
        listOf(
            Triple(0.20f, -0.35f, color.copy(alpha = 0.55f)),                       // the panel's own colour
            Triple(0.55f, 0.40f, lerp(color, NovaCobalt, 0.5f).copy(alpha = 0.45f)), // a cooler, bluer cloud
            Triple(0.85f, -0.15f, NovaCorona.copy(alpha = 0.12f))                     // a faint warm glow
        ).forEachIndexed { i, (u0, v, tint) ->
            val u = 0.1f + 0.8f * ((u0 + seed * 0.23f) % 1f)          // each panel's clouds in different places
            val at = Offset(left + length * u + if (i % 2 == 0) drift else -drift, midY + v * half)
            val radius = half * (1.4f + 0.4f * i)
            drawCircle(Brush.radialGradient(listOf(tint, Color.Transparent), at, radius), radius, at)
        }
        // A galaxy seen side-on: a soft glow squashed into a long, tilted oval
        val core = Offset(left + length * (0.3f + 0.4f * ((seed * 0.41f) % 1f)), midY)
        withTransform({
            rotate(-14f + (seed % 3) * 12f, core)                    // tipped a little, differently per panel
            scale(2.8f, 0.42f, core)                                 // stretched long and thin
        }) {
            drawCircle(
                Brush.radialGradient(listOf(NovaStarlight.copy(alpha = 0.20f), color.copy(alpha = 0.16f), Color.Transparent), core, half * 0.9f),
                half * 0.9f, core
            )
        }
        // Stars, twinkling gently
        val hair = 0.5.dp.toPx()
        panelStars.forEach { s ->
            val u = (s[0] + seed * 0.37f) % 1f                       // shifted along for each panel
            val v = if (seed % 2 == 0) s[1] else -s[1]               // and flipped on every other one
            val at = Offset(left + length * u, midY + v * half * 0.95f)
            val twinkle = 0.75f + 0.25f * sin(seconds * 0.9f + s[4]) // only a quarter dimmer at its faintest
            val alpha = (0.25f + 0.6f * s[3]) * twinkle
            val radius = (0.45f + 1.1f * s[2]).dp.toPx()
            drawCircle(NovaStarlight.copy(alpha = alpha), radius, at)
            if (s[2] > 0.72f) {                                      // the biggest few get a faint cross-shaped flare
                val arm = radius * 3.5f
                drawLine(NovaStarlight.copy(alpha = alpha * 0.35f), at - Offset(arm, 0f), at + Offset(arm, 0f), hair)
                drawLine(NovaStarlight.copy(alpha = alpha * 0.35f), at - Offset(0f, arm), at + Offset(0f, arm), hair)
            }
        }
        // A faint sheen drifting through every nine seconds or so
        val sweep = left + ((seconds / 9f + seed * 0.29f) % 1f) * (length + half * 4f) - half * 2f
        drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, sheen, Color.Transparent), startX = sweep - half * 2f, endX = sweep + half * 2f),
            Offset(sweep - half * 2f, midY - half * 1.2f), Size(half * 4f, half * 2.4f)
        )
    }
    // The faintest edge, just enough to catch the light
    drawPath(outline, NovaStarlight.copy(alpha = 0.12f), style = Stroke(1.dp.toPx(), join = StrokeJoin.Round))
}

// ---------- COSMIC SIGILS ----------
// The Novacane mark is a ring broken where the N's slanted stroke cuts through it. Each of these keeps that broken
// ring or slant and turns it into something in space.

// The kinds of cosmic sigil
enum class CosmicSigil {
    ECLIPSE,        // a dark moon with a gold corona and the N's slash of light across it
    RINGED_PLANET,  // a planet whose ring is tilted at the N's slant
    PULSAR,         // a bright core sweeping two beams along the N's slant
    CRESCENT,       // a crescent moon inside the broken ring
    NOVA,           // a four-pointed burst of light inside the broken ring
    BLACK_HOLE,     // a black disc, its glowing ring, and an accretion disc at the N's slant
    BINARY,         // two stars circling each other inside the broken ring
    SPIRAL,         // a little spiral galaxy inside the broken ring
    HALO            // the Novacane mark in pure white light: a glowing white broken ring round the N's three strokes
}

private const val SLANT = -58f                                       // the angle of the N's diagonal stroke, in degrees

// A soft round glow: bright in the middle, clear at the edge
internal fun DrawScope.softGlow(at: Offset, radius: Float, color: Color) {
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), at, radius), radius, at)
}

// The Novacane ring: a circle with two gaps where the N's stroke would cut through it, turned `turn` degrees
internal fun DrawScope.brokenRing(c: Offset, r: Float, color: Color, turn: Float, width: Float) {
    val gap = 30f                                                    // how wide each gap is, in degrees
    listOf(SLANT, SLANT + 180f).forEach { at ->
        drawArc(color, at + gap / 2f + turn, 180f - gap, false, Offset(c.x - r, c.y - r), Size(r * 2f, r * 2f), style = Stroke(width, cap = StrokeCap.Round))
    }
}

// The N's diagonal: a slanted bar running from bottom-left to top-right
internal fun DrawScope.nSlash(c: Offset, r: Float, color: Color) {
    drawPath(Path().apply {
        moveTo(c.x - 0.50f * r, c.y + 0.55f * r)                     // bottom-left
        lineTo(c.x - 0.18f * r, c.y + 0.55f * r)
        lineTo(c.x + 0.50f * r, c.y - 0.55f * r)                     // top-right
        lineTo(c.x + 0.18f * r, c.y - 0.55f * r)
        close()
    }, color)
}

// Draw one cosmic sigil of radius `r` at `c`. Everything moves slowly and only a little.
private fun DrawScope.drawCosmicSigil(c: Offset, r: Float, seconds: Float, style: CosmicSigil, color: Color) {
    val line = 1.6.dp.toPx()                                         // stroke thickness
    val breathe = 1f + 0.05f * sin(seconds * 1.2f + style.ordinal)   // a slow, small swell
    val turn = seconds * 6f                                          // a slow turn, in degrees
    val light = lerp(color, NovaStarlight, 0.6f)                     // a pale version of the panel's colour
    softGlow(c, r * 2.1f * breathe, color.copy(alpha = 0.28f))       // a halo in the panel's colour behind every sigil

    when (style) {
        CosmicSigil.ECLIPSE -> {
            softGlow(c, r * 1.5f * breathe, NovaCorona.copy(alpha = 0.35f))         // the corona
            drawCircle(NovaVoid, r * 0.7f, c)                                         // the dark moon
            drawCircle(NovaCorona.copy(alpha = 0.85f), r * 0.7f, c, style = Stroke(line))  // its bright rim
            nSlash(c, r * 0.62f, NovaCorona.copy(alpha = 0.5f))                       // the N's slash of light
            brokenRing(c, r, light.copy(alpha = 0.75f), turn, line)
        }
        CosmicSigil.RINGED_PLANET -> {
            val tilt = SLANT + 30f                                                    // the ring follows the N's slant
            ringHalf(c, r * 1.25f, r * 0.36f, tilt, light.copy(alpha = 0.45f), line, front = false)   // back of the ring
            drawCircle(Brush.radialGradient(listOf(light, color, NovaIndigo), c - Offset(r * 0.25f, r * 0.25f), r * 0.75f), r * 0.55f, c)  // the planet
            ringHalf(c, r * 1.25f, r * 0.36f, tilt, light.copy(alpha = 0.9f), line, front = true)     // front of the ring
            val angle = seconds * 0.5f                                                // a tiny moon, slowly going round
            if (sin(angle) > 0f) moon(ringPoint(c, r * 1.25f, r * 0.36f, tilt, angle), 1.6.dp.toPx(), NovaCorona)
        }
        CosmicSigil.PULSAR -> {
            rotate(SLANT + 8f * sin(seconds * 0.4f), c) {                             // the beams sway gently either side of the slant
                val w = r * 0.12f
                listOf(1f, -1f).forEach { dir ->                                      // one beam each way
                    drawPath(Path().apply {
                        moveTo(c.x, c.y - w); lineTo(c.x, c.y + w); lineTo(c.x + dir * r * 1.7f, c.y); close()
                    }, Brush.horizontalGradient(listOf(light.copy(alpha = 0.8f), Color.Transparent), startX = c.x, endX = c.x + dir * r * 1.7f))
                }
            }
            softGlow(c, r * 0.8f * breathe, NovaCorona.copy(alpha = 0.6f))           // the hot core's glow
            drawCircle(NovaStarlight, r * 0.18f, c)                                   // the core
            brokenRing(c, r, light.copy(alpha = 0.4f), turn, line)
        }
        CosmicSigil.CRESCENT -> {
            val disc = Path().apply { addOval(Rect(c, r * 0.68f)) }                    // the full moon...
            val bite = Path().apply { addOval(Rect(c + Offset(r * 0.32f, -r * 0.22f), r * 0.6f)) }  // ...minus a bite
            val crescent = Path().apply { op(disc, bite, PathOperation.Difference) }
            drawPath(crescent, Brush.radialGradient(listOf(NovaStarlight, light, color), c - Offset(r * 0.4f, -r * 0.2f), r * 1.1f))
            nSlash(c + Offset(r * 0.32f, -r * 0.22f), r * 0.3f, light.copy(alpha = 0.6f))  // a small slash of light in the dark part
            brokenRing(c, r, light.copy(alpha = 0.6f), -turn, line)                   // turning the other way
        }
        CosmicSigil.NOVA -> {
            softGlow(c, r * 1.2f * breathe, NovaCorona.copy(alpha = 0.35f))
            drawPath(sparklePath(Size(r * 1.7f, r * 1.7f), c - Offset(r * 0.85f, r * 0.85f)), NovaStarlight.copy(alpha = 0.95f))  // the burst
            rotate(45f, c) { drawPath(sparklePath(Size(r, r), c - Offset(r * 0.5f, r * 0.5f)), light.copy(alpha = 0.55f)) }      // a smaller diagonal one
            brokenRing(c, r, light.copy(alpha = 0.7f), turn, line)
        }
        CosmicSigil.BLACK_HOLE -> {
            val tilt = SLANT + 38f                                                    // the disc follows the N's slant
            ringHalf(c, r * 1.2f, r * 0.34f, tilt, light.copy(alpha = 0.5f), line * 2f, front = false)  // back of the glowing disc
            drawCircle(NovaVoid, r * 0.45f, c)                                         // the black hole
            drawCircle(NovaCorona.copy(alpha = 0.85f), r * 0.47f, c, style = Stroke(line * 0.8f))       // the thin ring of trapped light
            drawArc(light.copy(alpha = 0.45f), 200f, 140f, false, Offset(c.x - r * 0.75f, c.y - r * 0.62f), Size(r * 1.5f, r * 1.1f), style = Stroke(line))  // light bent over the top
            ringHalf(c, r * 1.2f, r * 0.34f, tilt, NovaCorona.copy(alpha = 0.9f), line * 2f, front = true)   // front of the disc
        }
        CosmicSigil.BINARY -> {
            val angle = seconds * 0.6f                                                // the pair slowly circles
            val offset = Offset(cos(angle), sin(angle) * 0.6f) * (r * 0.38f)          // a slightly flattened orbit
            softGlow(c + offset, r * 0.55f, NovaCorona.copy(alpha = 0.5f))
            drawCircle(NovaStarlight, r * 0.16f, c + offset)                          // the bigger, whiter star
            softGlow(c - offset, r * 0.5f, light.copy(alpha = 0.5f))
            drawCircle(light, r * 0.12f, c - offset)                                  // its smaller, coloured partner
            brokenRing(c, r, light.copy(alpha = 0.7f), turn, line)
        }
        CosmicSigil.HALO -> {
            softGlow(c, r * 1.9f * breathe, NovaStarlight.copy(alpha = 0.35f))      // a wide white glow
            softGlow(c, r * 0.9f, Color.White.copy(alpha = 0.4f))                     // and a brighter one in the middle
            brokenRing(c, r * 0.95f, Color.White, turn * 0.5f, line * 1.4f)          // the white broken ring, turning slowly
            nSlash(c, r * 0.62f, Color.White)                                         // the N's diagonal...
            nSlash(c + Offset(-r * 0.42f, r * 0.12f), r * 0.36f, Color.White.copy(alpha = 0.85f))  // ...with a shorter stroke either side
            nSlash(c + Offset(r * 0.42f, -r * 0.12f), r * 0.36f, Color.White.copy(alpha = 0.85f))
        }
        CosmicSigil.SPIRAL -> {
            softGlow(c, r * 0.7f, NovaCorona.copy(alpha = 0.45f))                    // the bright middle
            val spin = seconds * 0.25f                                                // the arms turn very slowly
            repeat(2) { arm ->
                repeat(14) { k ->
                    val a = k * 0.42f + arm * PI.toFloat() + spin                     // further round for each dot
                    val reach = r * (0.12f + 0.06f * k)                               // and further out
                    val at = c + Offset(cos(a), sin(a) * 0.75f) * reach
                    drawCircle(light.copy(alpha = 0.9f - 0.05f * k), (1.6f - 0.07f * k).dp.toPx(), at)
                }
            }
            drawCircle(NovaStarlight, r * 0.12f, c)                                   // the core
            brokenRing(c, r * 1.05f, light.copy(alpha = 0.45f), turn, line)
        }
    }
}

@Composable
fun StripeCard(
    title: String,                                                   // the bold line
    subtitle: String,                                                // the line underneath
    color: Color,                                                    // the panel's colour (darker colours keep the white text readable)
    modifier: Modifier = Modifier,                                   // lets the caller size and position it
    image: Painter? = null,                                          // an optional picture on the left (none by default)
    accent: Color = NovaStarlight,                                   // the colour of the progress bar
    height: Dp = 86.dp,                                              // how tall the strip is
    progress: Float? = null,                                         // how far through something is (0 to 1): draws a glowing bar along the bottom
    seed: Int = 0,                                                   // makes each strip's panel, stars and sigils different
    leftSigil: CosmicSigil = CosmicSigil.entries[(seed * 2) % CosmicSigil.entries.size],      // the sigil at the left end
    rightSigil: CosmicSigil = CosmicSigil.entries[(seed * 2 + 1) % CosmicSigil.entries.size], // the sigil at the right end
    onClick: () -> Unit = {}                                         // what tapping the strip does
) {
    val clock = rememberSecondsClock()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .graphicsLayer()                                         // the artwork gets its own layer...
            .drawBehind {
                val t = clock.value                                  // read the clock here, so only the drawing repeats each frame
                val inset = 26.dp.toPx()                             // how far in from each side the sigils sit
                val start = Offset(inset, size.height / 2f)
                val end = Offset(size.width - inset, size.height / 2f)
                galaxyPanel(start.x, end.x, start.y, size.height * 0.38f, color, t, seed)   // the galaxy panel between them
                // How far through it is: a faint track along the bottom, lit up to the progress point with a glowing head
                if (progress != null) {
                    val y = start.y + size.height * 0.27f
                    val from = start.x + 40.dp.toPx()
                    val to = end.x - 40.dp.toPx()
                    val at = from + (to - from) * progress.coerceIn(0f, 1f)
                    drawLine(NovaStarlight.copy(alpha = 0.12f), Offset(from, y), Offset(to, y), 2.dp.toPx(), StrokeCap.Round)
                    drawLine(accent.copy(alpha = 0.85f), Offset(from, y), Offset(at, y), 2.dp.toPx(), StrokeCap.Round)
                    softGlow(Offset(at, y), 7.dp.toPx() * (1f + 0.15f * sin(t * 2.4f)), accent.copy(alpha = 0.6f))
                    drawCircle(NovaStarlight, 2.dp.toPx(), Offset(at, y))
                }
                drawCosmicSigil(start, 15.dp.toPx(), t, leftSigil, color)              // left sigil
                drawCosmicSigil(end, 15.dp.toPx(), t, rightSigil, color)               // right sigil
            }
            .padding(horizontal = 52.dp),                            // keep the writing clear of the sigils
        contentAlignment = Alignment.Center
    ) {
        // A soft dark shadow under the writing, so it reads over the stars and nebulae
        val shadow = Shadow(NovaVoid, blurRadius = 8f)
        // ...and the writing gets another, kept as a ready-made picture, so it isn't redrawn every time the artwork moves
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            // The image (if there is one), in a tile with uneven edges
            if (image != null) {
                Image(
                    painter = image,
                    contentDescription = null,                       // decoration: the title says what it is
                    contentScale = ContentScale.Crop,                // fill the tile without squashing
                    modifier = Modifier
                        .size(50.dp)
                        .clip(WobblyShape)
                        .background(NovaVoid.copy(alpha = 0.6f))
                        .border(1.dp, NovaStarlight.copy(alpha = 0.25f), WobblyShape)   // the faintest edge
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            // The writing, centred in the space that's left
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = NovaWhite, maxLines = 1, style = LocalTextStyle.current.copy(shadow = shadow))
                Text(subtitle, fontSize = 12.sp, lineHeight = 15.sp, color = NovaBlush, maxLines = 2, overflow = TextOverflow.Ellipsis, style = LocalTextStyle.current.copy(shadow = shadow))
            }
        }
    }
}

// 14. HOME AREAS: Home is split into areas you switch between, each with its own vibe:
//     On air (happening now): a red recording light that ripples outwards, and a pill whose edge glows on and off.
//     Open (ready to book): calm cobalt, a small world in the broken ring with a moon slowly going round.
//     Deals (going fast): gold and crimson, a twinkling four-pointed burst, and a shimmer sweeping across.
//     Studio (what we offer): soft purple, the Novacane broken ring and slash, with an edge that breathes slowly.

// How each category looks: the panel colour, a brighter accent, and the two sigils on its strips
data class CategoryLook(val color: Color, val accent: Color, val leftSigil: CosmicSigil, val rightSigil: CosmicSigil)

fun categoryLook(category: SessionCategory): CategoryLook = when (category) {
    SessionCategory.ONGOING -> CategoryLook(Color(0xFF8E1B3A), NovaSignal, CosmicSigil.PULSAR, CosmicSigil.BINARY)
    SessionCategory.AVAILABLE -> CategoryLook(NovaCobalt, Color(0xFF9FB4FF), CosmicSigil.RINGED_PLANET, CosmicSigil.SPIRAL)
    SessionCategory.DEALS -> CategoryLook(Color(0xFF9C1757), NovaCorona, CosmicSigil.NOVA, CosmicSigil.ECLIPSE)
}

// The areas on Home, in switcher order. The first three are the session categories; the last is the studio's services.
enum class HomeArea(val short: String, val label: String, val tagline: String, val category: SessionCategory?) {
    ON_AIR("On air", SessionCategory.ONGOING.label, SessionCategory.ONGOING.tagline, SessionCategory.ONGOING),
    OPEN("Open", SessionCategory.AVAILABLE.label, SessionCategory.AVAILABLE.tagline, SessionCategory.AVAILABLE),
    DEALS("Deals", SessionCategory.DEALS.label, SessionCategory.DEALS.tagline, SessionCategory.DEALS),
    STUDIO("Studio", "The studio", "What we offer", null)
}

// How each area looks (the session areas borrow their category's look; the studio is soft purple)
fun areaLook(area: HomeArea): CategoryLook =
    area.category?.let { categoryLook(it) } ?: CategoryLook(NovaPurple, NovaBlush, CosmicSigil.ECLIPSE, CosmicSigil.SPIRAL)

// The animated icon for each area: clear, musical pictures with a touch of space
//   On air: a microphone with sound rippling out either side.  Open: a calendar with a twinkling star on a free day.
//   Deals: a swinging price tag with a sparkle.  Studio: three mixing-desk faders sliding up and down.
private fun DrawScope.areaEmblem(area: HomeArea, seconds: Float, look: CategoryLook) {
    val c = center
    val r = size.minDimension / 2f
    val line = 1.6.dp.toPx()
    when (area) {
        HomeArea.ON_AIR -> {
            softGlow(c, r, look.accent.copy(alpha = 0.3f))
            // Sound rippling out either side
            repeat(2) { k ->
                val ripple = ((seconds / 1.4f) + k * 0.5f) % 1f                      // 0 to 1, then again
                val rr = r * (0.6f + 0.4f * ripple)
                val box = Offset(c.x - rr, c.y - r * 0.2f - rr)
                drawArc(look.accent.copy(alpha = (1f - ripple) * 0.7f), -40f, 80f, false, box, Size(rr * 2f, rr * 2f), style = Stroke(line, cap = StrokeCap.Round))   // right
                drawArc(look.accent.copy(alpha = (1f - ripple) * 0.7f), 140f, 80f, false, box, Size(rr * 2f, rr * 2f), style = Stroke(line, cap = StrokeCap.Round))  // left
            }
            // The microphone: a rounded head with grille lines, a cradle, a stem and a base
            val headW = r * 0.5f
            val headH = r * 0.85f
            val top = c.y - r * 0.75f
            drawRoundRect(look.accent, Offset(c.x - headW / 2f, top), Size(headW, headH), CornerRadius(headW / 2f))
            repeat(3) { k ->
                val y = top + headH * (0.3f + 0.2f * k)
                drawLine(NovaVoid.copy(alpha = 0.45f), Offset(c.x - headW * 0.3f, y), Offset(c.x + headW * 0.3f, y), 1.dp.toPx())
            }
            val cradleTop = top + headH * 0.3f
            val cradleH = headH * 0.75f
            drawArc(look.accent, 0f, 180f, false, Offset(c.x - r * 0.42f, cradleTop), Size(r * 0.84f, cradleH), style = Stroke(line))
            drawLine(look.accent, Offset(c.x, cradleTop + cradleH), Offset(c.x, c.y + r * 0.8f), line)
            drawLine(look.accent, Offset(c.x - r * 0.3f, c.y + r * 0.8f), Offset(c.x + r * 0.3f, c.y + r * 0.8f), line, StrokeCap.Round)
        }
        HomeArea.OPEN -> {
            softGlow(c, r, look.accent.copy(alpha = 0.3f))
            // A calendar page
            val w = r * 1.45f
            val h = r * 1.3f
            val tl = Offset(c.x - w / 2f, c.y - h / 2f + r * 0.1f)
            drawRoundRect(look.accent.copy(alpha = 0.3f), tl, Size(w, h * 0.27f), CornerRadius(r * 0.16f))         // the coloured top
            drawRoundRect(look.accent, tl, Size(w, h), CornerRadius(r * 0.16f), style = Stroke(line))             // the outline
            listOf(0.3f, 0.7f).forEach { u ->                                                                     // the two binding rings
                drawLine(look.accent, Offset(tl.x + w * u, tl.y - r * 0.14f), Offset(tl.x + w * u, tl.y + r * 0.12f), 2.dp.toPx(), StrokeCap.Round)
            }
            // The days: dots, with one free day shining as a twinkling star
            for (row in 0..1) for (col in 0..2) {
                val p = Offset(tl.x + w * (0.22f + 0.28f * col), tl.y + h * (0.52f + 0.26f * row))
                if (row == 0 && col == 1) {
                    val s = r * 0.5f * (0.85f + 0.15f * sin(seconds * 3f))
                    drawPath(sparklePath(Size(s, s), p - Offset(s / 2f, s / 2f)), NovaStarlight)
                } else {
                    drawCircle(look.accent.copy(alpha = 0.6f), 1.4.dp.toPx(), p)
                }
            }
        }
        HomeArea.DEALS -> {
            softGlow(c, r, look.accent.copy(alpha = 0.4f))
            // A price tag, swinging gently
            rotate(-30f + 6f * sin(seconds * 1.5f), c) {
                val tag = Path().apply {
                    moveTo(c.x - r * 0.8f, c.y)                       // the point
                    lineTo(c.x - r * 0.38f, c.y - r * 0.42f)
                    lineTo(c.x + r * 0.78f, c.y - r * 0.42f)
                    lineTo(c.x + r * 0.78f, c.y + r * 0.42f)
                    lineTo(c.x - r * 0.38f, c.y + r * 0.42f)
                    close()
                }
                drawPath(tag, look.accent)
                drawCircle(NovaVoid, r * 0.1f, Offset(c.x - r * 0.42f, c.y))                                          // the hole
                drawLine(NovaVoid.copy(alpha = 0.45f), Offset(c.x - r * 0.05f, c.y - r * 0.14f), Offset(c.x + r * 0.55f, c.y - r * 0.14f), line, StrokeCap.Round)  // writing on the tag
                drawLine(NovaVoid.copy(alpha = 0.45f), Offset(c.x - r * 0.05f, c.y + r * 0.14f), Offset(c.x + r * 0.35f, c.y + r * 0.14f), line, StrokeCap.Round)
            }
            // A sparkle winking at its corner
            val wink = r * 0.5f * (0.4f + 0.6f * (0.5f + 0.5f * sin(seconds * 4f)))
            drawPath(sparklePath(Size(wink, wink), c + Offset(r * 0.62f, -r * 0.7f) - Offset(wink / 2f, wink / 2f)), NovaStarlight)
        }
        HomeArea.STUDIO -> {
            softGlow(c, r, look.color.copy(alpha = 0.55f))
            // Three faders: a track each, with a knob sliding slowly up and down
            listOf(-0.5f, 0f, 0.5f).forEachIndexed { k, dx ->
                val x = c.x + dx * r
                drawLine(look.accent.copy(alpha = 0.45f), Offset(x, c.y - r * 0.78f), Offset(x, c.y + r * 0.78f), line, StrokeCap.Round)
                val y = c.y + r * 0.55f * sin(seconds * (0.8f + 0.3f * k) + k * 1.7f)
                drawRoundRect(look.accent, Offset(x - r * 0.2f, y - r * 0.12f), Size(r * 0.4f, r * 0.24f), CornerRadius(r * 0.06f))
            }
        }
    }
}

// What each pad says under its name, e.g. "2 live now"
private fun areaDescription(area: HomeArea, count: Int?): String {
    val n = count?.toString() ?: "–"
    return when (area) {
        HomeArea.ON_AIR -> "$n live now"
        HomeArea.OPEN -> "$n to book"
        HomeArea.DEALS -> "$n on sale"
        HomeArea.STUDIO -> "$n services"
    }
}

// The switch between Home's areas, styled like a row of drum-machine pads in a rack. Each pad has a clear icon, its name
// and what's in it, all centred. The chosen one glows from inside, lights its LED and bounces a little level meter;
// a waveform made of stars runs behind the pads in the chosen area's colour.
@Composable
fun AreaSwitcher(
    selected: HomeArea,                                              // the area showing now
    onSelect: (HomeArea) -> Unit,                                    // called when a pad is tapped
    modifier: Modifier = Modifier,
    counts: Map<HomeArea, Int> = emptyMap()                          // how many are in each area
) {
    val clock = rememberSecondsClock()
    val chosen = areaLook(selected)
    val rack = RoundedCornerShape(22.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(rack)
            .background(Brush.verticalGradient(listOf(NovaPlum.copy(alpha = 0.85f), NovaVoid.copy(alpha = 0.9f))))
            .border(1.dp, Brush.horizontalGradient(listOf(chosen.accent.copy(alpha = 0.35f), NovaPurple.copy(alpha = 0.25f), chosen.accent.copy(alpha = 0.35f))), rack)
            .graphicsLayer()                                          // on its own layer, so its animation doesn't redraw the rest of Home
            .drawBehind { starWave(clock.value, chosen.accent) }      // the waveform of stars behind the pads
            .padding(6.dp)
    ) {
        HomeArea.entries.forEach { area ->
            AreaPad(area, on = area == selected, description = areaDescription(area, counts[area]), clock = clock, modifier = Modifier.weight(1f)) { onSelect(area) }
        }
    }
}

// A waveform drawn as a line of twinkling stars: quiet at the ends, swelling in the middle, slowly rolling
private fun DrawScope.starWave(seconds: Float, color: Color) {
    val mid = size.height / 2f
    val amp = size.height * 0.28f                                    // how tall the wave gets
    val count = 48                                                   // stars along it
    val pi = PI.toFloat()
    for (i in 0..count) {
        val u = i / count.toFloat()                                  // how far across (0 to 1)
        val envelope = sin(u * pi)                                   // quiet at both ends
        val swell = 0.6f + 0.4f * sin(seconds * 0.9f + u * 3f)       // the loudness rises and falls
        val y = mid + amp * envelope * swell * sin(u * 6f * pi + seconds * 2.2f)
        val twinkle = 0.5f + 0.5f * sin(seconds * 3f + i)
        drawCircle(color.copy(alpha = (0.12f + 0.25f * twinkle) * envelope), (0.8f + 0.8f * twinkle).dp.toPx(), Offset(size.width * u, y))
    }
}

// One pad in the rack: icon, name and description, centred
@Composable
private fun AreaPad(area: HomeArea, on: Boolean, description: String, clock: State<Float>, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val look = areaLook(area)
    // How lit the pad is: eases between 0 (resting) and 1 (chosen)
    val lit by animateFloatAsState(if (on) 1f else 0f, tween(300), label = "pad")
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(92.dp)
            .clip(shape)
            .background(NovaVoid.copy(alpha = 0.55f))
            .drawWithContent {
                val t = clock.value
                // A glow from inside the pad, in its colour, when chosen
                drawRect(Brush.radialGradient(listOf(look.color.copy(alpha = 0.85f * lit), Color.Transparent), center, size.maxDimension * 0.75f))
                // Deals: a gold shimmer sweeping across the chosen pad
                if (area == HomeArea.DEALS && lit > 0f) {
                    val x = ((t / 2.4f) % 1f) * size.width * 1.8f - size.width * 0.4f
                    drawRect(Brush.linearGradient(listOf(Color.Transparent, look.accent.copy(alpha = 0.3f * lit), Color.Transparent), Offset(x - 20.dp.toPx(), 0f), Offset(x + 20.dp.toPx(), size.height)))
                }
                drawContent()
                // The edge: faint when resting, bright when chosen (On air's pulses like a studio light; Studio's breathes slowly)
                val beat = when (area) {
                    HomeArea.ON_AIR -> 0.6f + 0.4f * sin(t * 3f)
                    HomeArea.STUDIO -> 0.75f + 0.25f * sin(t * 1.1f)
                    else -> 1f
                }
                drawRoundRect(look.accent.copy(alpha = 0.15f + 0.55f * lit * beat), cornerRadius = CornerRadius(16.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // The icon, dimmed a little when resting
            Canvas(modifier = Modifier.size(34.dp).alpha(0.55f + 0.45f * lit)) { areaEmblem(area, clock.value, look) }
            // The name
            Text(area.short.uppercase(), fontFamily = SourceCodePro, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.sp, color = lerp(NovaBlush.copy(alpha = 0.8f), look.accent, lit), maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            // What's in it
            Text(description, fontSize = 10.sp, lineHeight = 12.sp, color = NovaMuted.copy(alpha = 0.7f + 0.3f * lit), maxLines = 1)
            // A little level meter that only shows on the chosen pad
            Canvas(modifier = Modifier.padding(top = 3.dp).size(width = 22.dp, height = 5.dp).alpha(lit)) {
                val t = clock.value
                val bars = 5
                val w = size.width / (bars * 2 - 1)                  // bar width, with equal gaps
                repeat(bars) { k ->
                    val level = 0.3f + 0.7f * abs(sin(t * (2.2f + k * 0.7f) + k))
                    val h = size.height * level
                    drawRoundRect(look.accent, Offset(k * 2 * w, size.height - h), Size(w, h), CornerRadius(w / 2f))
                }
            }
        }
        // The LED in the top-right corner: lit when chosen (On air's blinks)
        Canvas(modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(5.dp).graphicsLayer()) {
            val blink = if (area == HomeArea.ON_AIR) 0.7f + 0.3f * sin(clock.value * 4f) else 1f
            drawCircle(look.accent.copy(alpha = 0.18f + 0.82f * lit * blink))
        }
    }
}

// 15. MEMBERSHIP CARD STRIP: the membership card as a slim holographic card, unlike the galaxy strips. Dark glass with a
//     holographic foil drifting across, fine engraved orbit lines (like the lines on a banknote), a metal edge in the
//     tier's colour with a glint travelling along it (gold for Elite), the tier's body from the Club tab on the left,
//     and the Novacane mark in white light on the right.

// Each tier's metal, for the card's edge: dull, bright, dull, so a glint shows where the bright part is
private fun tierMetal(tier: Tier): List<Color> = when (tier) {
    Tier.NON_MEMBER -> listOf(NovaMuted, NovaStarlight, NovaMuted)
    Tier.SYNDICATE -> listOf(NovaPurple, NovaBlush, NovaPurple)
    Tier.PRO -> listOf(NovaPink, NovaStarlight, NovaPink)
    Tier.ELITE -> listOf(Color(0xFFC9A66B), Color.White, NovaCorona)
}

@Composable
fun MembershipStrip(tier: Tier, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val clock = rememberSecondsClock()
    val shadow = Shadow(NovaVoid, blurRadius = 8f)                  // keeps the writing crisp over the foil
    val card = RoundedCornerShape(20.dp)
    val metal = tierMetal(tier)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(card)
            .clickable(onClick = onOpen)
            .graphicsLayer()                                         // the card's artwork on its own layer
            .drawBehind {
                val t = clock.value                                  // read the clock here, so only the drawing repeats each frame
                // Dark glass
                drawRect(Brush.linearGradient(listOf(Color(0xFF1A0B24), NovaPlumDeep, Color(0xFF0B0718)), Offset.Zero, Offset(size.width, size.height)))
                // A soft magenta glow behind the tier badge
                softGlow(Offset(size.height * 0.55f, size.height / 2f), size.height * 1.1f, NovaMagenta.copy(alpha = 0.28f))
                // Holographic foil: a band of shifting colours drifting slowly across
                val drift = (t / 7f % 1f) * size.width * 2f - size.width * 0.5f
                drawRect(Brush.linearGradient(
                    listOf(Color.Transparent, NovaMagenta.copy(alpha = 0.32f), NovaPurple.copy(alpha = 0.28f), NovaCobalt.copy(alpha = 0.3f), NovaCorona.copy(alpha = 0.2f), Color.Transparent),
                    start = Offset(drift - size.width * 0.6f, 0f), end = Offset(drift + size.width * 0.6f, size.height)
                ))
                // Fine engraved orbit lines round the Novacane mark
                val mark = Offset(size.width - 42.dp.toPx(), size.height / 2f)
                rotate(-20f, mark) {
                    repeat(9) { k ->
                        val rx = 26.dp.toPx() + k * 15.dp.toPx()
                        val ry = rx * 0.42f
                        drawOval(NovaStarlight.copy(alpha = 0.1f - k * 0.009f), Offset(mark.x - rx, mark.y - ry), Size(rx * 2f, ry * 2f), style = Stroke(0.7.dp.toPx()))
                    }
                }
                // The Novacane mark in white light
                drawCosmicSigil(mark, 19.dp.toPx(), t, CosmicSigil.HALO, NovaPurple)
            }
            .drawWithContent {
                drawContent()
                // The metal edge, with a glint travelling across it every few seconds
                val g = (clock.value / 4f % 1f) * size.width * 1.6f - size.width * 0.3f
                drawRoundRect(
                    Brush.linearGradient(listOf(metal[0].copy(alpha = 0.5f), metal[1], metal[2].copy(alpha = 0.5f)), start = Offset(g - size.width * 0.25f, 0f), end = Offset(g + size.width * 0.25f, size.height)),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(1.4.dp.toPx())
                )
            }
            .padding(start = 18.dp)
    ) {
        // The tier's body in the night sky: moon, planet, star or supernova (as on the Club tab)
        TierBadge(tier)
        // The writing, centred in the middle
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {   // the writing kept as a ready-made picture
            Row(verticalAlignment = Alignment.CenterVertically) {
                SigilSlashes(color = metal[1].copy(alpha = 0.9f))
                Spacer(modifier = Modifier.width(6.dp))
                Text("YOUR MEMBERSHIP", fontFamily = SourceCodePro, fontSize = 10.sp, letterSpacing = 2.sp, color = lerp(metal[0], NovaWhite, 0.4f), style = LocalTextStyle.current.copy(shadow = shadow))
            }
            Text(tier.label.uppercase(), fontSize = 19.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp, color = NovaWhite, maxLines = 1, style = LocalTextStyle.current.copy(shadow = shadow))
            val price = if (tier.monthlyPrice > 0) "£${tier.monthlyPrice} a month" else "Pay per session"
            val rollover = if (tier == Tier.ELITE) " · $SAMPLE_ROLLOVER_HOURS hours rolling over" else ""
            Text("$price$rollover", fontSize = 11.5.sp, color = NovaBlush, maxLines = 1, style = LocalTextStyle.current.copy(shadow = shadow))
        }
        // Room for the white Novacane mark on the right
        Spacer(modifier = Modifier.width(76.dp))
    }
}

// 16. NAVIGATION DOCK: the tab bar as a floating glass dock. Faint stars twinkle in it; the chosen tab sits in a glowing
//     orb with an orbit ring and a moon going round; a comet of light along the top edge glides to whichever tab you pick.

// Faint stars for the dock: across, down, brightness
private val dockStars = Random(91).let { rnd -> List(26) { Triple(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()) } }

@Composable
fun NovaNavBar(labels: List<String>, icons: List<ImageVector>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val clock = rememberSecondsClock()
    // Where the glow and comet are, in tabs (slides with a little bounce when you pick another)
    val slot by animateFloatAsState(selected.toFloat(), spring(dampingRatio = 0.75f, stiffness = 300f), label = "navSlot")
    val dock = RoundedCornerShape(28.dp)
    Box(modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).graphicsLayer()) {   // its own layer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(dock)
                .drawBehind {
                    val t = clock.value
                    // Dark glass
                    drawRect(Brush.verticalGradient(listOf(NovaPlum.copy(alpha = 0.94f), NovaVoid.copy(alpha = 0.97f))))
                    // Faint twinkling stars
                    dockStars.forEach { (x, y, b) ->
                        drawCircle(NovaStarlight.copy(alpha = (0.08f + 0.25f * b) * (0.6f + 0.4f * sin(t + b * 6f))), (0.5f + b).dp.toPx(), Offset(x * size.width, y * size.height))
                    }
                    // A soft nebula glow under the chosen tab, sliding with it
                    val w = size.width / labels.size
                    val cx = w * (slot + 0.5f)
                    softGlow(Offset(cx, size.height * 0.45f), w * 0.9f, NovaMagenta.copy(alpha = 0.32f))
                    // The comet along the top edge
                    drawLine(
                        Brush.horizontalGradient(listOf(Color.Transparent, NovaPink, Color.Transparent), startX = cx - w * 0.4f, endX = cx + w * 0.4f),
                        Offset(cx - w * 0.4f, 1.dp.toPx()), Offset(cx + w * 0.4f, 1.dp.toPx()), 2.dp.toPx()
                    )
                }
                .border(1.dp, Brush.horizontalGradient(listOf(NovaPink.copy(alpha = 0.35f), NovaPurple.copy(alpha = 0.3f), NovaCobalt.copy(alpha = 0.35f))), dock)
        ) {
            labels.forEachIndexed { i, label ->
                // How lit this tab is: eases between 0 and 1
                val lit by animateFloatAsState(if (i == selected) 1f else 0f, tween(300), label = "navLit")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) }
                ) {
                    // The icon, in a glowing orb with an orbit ring and moon when chosen
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(38.dp).drawBehind {
                            if (lit > 0f) {
                                drawCircle(Brush.radialGradient(listOf(NovaPink.copy(alpha = 0.55f * lit), NovaPurple.copy(alpha = 0.35f * lit), Color.Transparent), center, size.minDimension * 0.62f))
                                val rx = size.width * 0.64f
                                val ry = size.height * 0.22f
                                rotate(-18f, center) {
                                    drawOval(NovaBlush.copy(alpha = 0.45f * lit), Offset(center.x - rx, center.y - ry), Size(rx * 2f, ry * 2f), style = Stroke(1.dp.toPx()))
                                }
                                drawCircle(NovaCorona.copy(alpha = lit), 2.dp.toPx(), ringPoint(center, rx, ry, -18f, clock.value * 1.6f))
                            }
                        }
                    ) {
                        Icon(icons[i], contentDescription = label, tint = lerp(NovaMuted.copy(alpha = 0.75f), Color.White, lit), modifier = Modifier.size(22.dp))
                    }
                    // The name
                    Text(label, fontFamily = SourceCodePro, fontSize = 10.sp, letterSpacing = 1.sp, color = lerp(NovaMuted.copy(alpha = 0.7f), NovaPink, lit), maxLines = 1)
                }
            }
        }
    }
}

// 6. ANIMATED PRICE: number rolls up or down instead of jumping
@Composable
fun AnimatedPrice(amount: Int, modifier: Modifier = Modifier) {
    // Smoothly slide from the old number to the new one
    val shownAmount by animateIntAsState(
        targetValue = amount,                 // where we're heading
        animationSpec = tween(450),           // takes under half a second
        label = "price"                       // a name for debugging tools
    )
    // Draw it with a pound sign
    Text("£$shownAmount", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}
