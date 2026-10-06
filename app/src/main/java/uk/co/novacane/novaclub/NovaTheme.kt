// Same package name as NovaScreens.kt, so the screens can use these colours
package com.novacane.novaclub

// Pulls in the Material 3 theme building blocks
import androidx.compose.material3.MaterialTheme
// The default text sizes, which we copy and restyle with our fonts
import androidx.compose.material3.Typography
// Lets us build a dark colour scheme
import androidx.compose.material3.darkColorScheme
// Lets a function hold UI
import androidx.compose.runtime.Composable
// A "brush" paints with a gradient instead of one flat colour
import androidx.compose.ui.graphics.Brush
// The Color type
import androidx.compose.ui.graphics.Color
// Tools for loading font files
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
// Lets us set letter spacing in sp
import androidx.compose.ui.unit.sp
// Centres every line of text by default
import androidx.compose.ui.text.style.TextAlign
// The list of files in res/ (fonts live in res/font)
import uk.co.novacane.novaclub.R

// ---------- THE PALETTE ----------
// Taken from novacane.co.uk and its NovaBot chat panel. Swap the hex codes here if the brand changes.

// Warm near-black, the website's page background (main background)
val NovaBlack = Color(0xFF100806)
// Dark plum, the NovaBot panel's body colour (used for cards)
val NovaPlum = Color(0xFF1C1129)
// Deeper plum, used for the bottom bar
val NovaPlumDeep = Color(0xFF150D1F)
// Novacane magenta, the website's "Book a session" button (main buttons)
val NovaMagenta = Color(0xFFB01D68)
// Novacane purple, the middle of the website's gradients
val NovaPurple = Color(0xFF7A1F86)
// Deep indigo, the dark end of the website's gradients
val NovaIndigo = Color(0xFF25194D)
// Hot pink, the website's highlight colour (prices and important text)
val NovaPink = Color(0xFFFF5FA8)
// Pale pink, the website's soft text on purple
val NovaBlush = Color(0xFFFFD1EA)
// Plain white, the website's main text colour
val NovaWhite = Color(0xFFFFFFFF)
// Dusty pink-lilac for quieter secondary text
val NovaMuted = Color(0xFFC4A6C1)
// Signal red, for "sold out" and the watermark warning
val NovaSignal = Color(0xFFE5484D)

// ---------- THE COSMOS ----------
// Extra colours for the space look. The brand colours above become the nebulae.

// Deep space: a cold violet-black, darker than the website's page (main background)
val NovaVoid = Color(0xFF06040D)
// Cobalt nebula, a bluer cousin of the indigo, so the sky has some depth
val NovaCobalt = Color(0xFF2B2A8C)
// Starlight: a cool, slightly lilac white for the stars
val NovaStarlight = Color(0xFFF3EEFF)
// Corona gold: the warm glow round the Novacane sigil, used for the rising star
val NovaCorona = Color(0xFFF2D9A0)

// ---------- THE GRADIENTS ----------
// The NovaBot header sweep: magenta top-left, through purple, to indigo bottom-right (membership card)
val NovaCardGradient = Brush.linearGradient(
    0f to Color(0xFF9C1757),      // deep magenta corner
    0.55f to Color(0xFF5E1B82),   // purple just past the middle
    1f to NovaIndigo              // indigo corner
)
// A thin magenta-to-purple edge, like the outline on the website's chat boxes (card borders)
val NovaEdgeGradient = Brush.linearGradient(
    listOf(NovaMagenta.copy(alpha = 0.45f), NovaPurple.copy(alpha = 0.45f))   // see-through so it stays subtle
)
// A hairline that fades in from nothing, peaks in pale pink, and fades out again (dividers under titles)
val NovaHorizonGradient = Brush.horizontalGradient(
    listOf(Color.Transparent, NovaPink.copy(alpha = 0.55f), Color.Transparent)
)

// ---------- THE FONTS ----------
// Jost: a free lookalike of Futura PT, the website's main font (files in res/font)
val Jost = FontFamily(
    Font(R.font.jost_regular, FontWeight.Normal),     // everyday weight
    Font(R.font.jost_medium, FontWeight.Medium),      // slightly heavier
    Font(R.font.jost_semibold, FontWeight.SemiBold),  // heavier again
    Font(R.font.jost_bold, FontWeight.Bold)           // boldest
)
// Source Code Pro: the website's typewriter-style font for buttons and small labels
val SourceCodePro = FontFamily(
    Font(R.font.source_code_pro_regular, FontWeight.Normal),     // everyday weight
    Font(R.font.source_code_pro_semibold, FontWeight.SemiBold)   // heavier
)

// Material's standard text sizes, which we restyle below
private val Base = Typography()

// Every text style in the app: Jost for reading, Source Code Pro for buttons, chips and tabs. All centred.
val NovaType = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Jost, textAlign = TextAlign.Center),       // huge text
    displayMedium = Base.displayMedium.copy(fontFamily = Jost, textAlign = TextAlign.Center),     // very big text
    displaySmall = Base.displaySmall.copy(fontFamily = Jost, textAlign = TextAlign.Center),       // big text
    headlineLarge = Base.headlineLarge.copy(fontFamily = Jost, textAlign = TextAlign.Center),     // large headings
    headlineMedium = Base.headlineMedium.copy(fontFamily = Jost, textAlign = TextAlign.Center),   // medium headings
    headlineSmall = Base.headlineSmall.copy(fontFamily = Jost, textAlign = TextAlign.Center),     // small headings
    titleLarge = Base.titleLarge.copy(fontFamily = Jost, textAlign = TextAlign.Center),           // large titles
    titleMedium = Base.titleMedium.copy(fontFamily = Jost, textAlign = TextAlign.Center),         // medium titles
    titleSmall = Base.titleSmall.copy(fontFamily = Jost, textAlign = TextAlign.Center),           // small titles
    bodyLarge = Base.bodyLarge.copy(fontFamily = Jost, textAlign = TextAlign.Center),             // normal text (the default for Text)
    bodyMedium = Base.bodyMedium.copy(fontFamily = Jost, textAlign = TextAlign.Center),           // smaller text
    bodySmall = Base.bodySmall.copy(fontFamily = Jost, textAlign = TextAlign.Center),             // smallest text
    labelLarge = Base.labelLarge.copy(fontFamily = SourceCodePro, textAlign = TextAlign.Center),  // buttons and chips
    labelMedium = Base.labelMedium.copy(fontFamily = SourceCodePro, textAlign = TextAlign.Center, letterSpacing = 1.sp), // bottom bar words
    labelSmall = Base.labelSmall.copy(fontFamily = SourceCodePro, textAlign = TextAlign.Center)   // tiny labels
)

// ---------- THE COLOUR SCHEME ----------
// Tells Material 3 which palette colour to use for each job
private val NovaColors = darkColorScheme(
    primary = NovaMagenta,               // buttons, switches, card outlines
    onPrimary = NovaWhite,               // text sitting on top of magenta
    secondary = NovaPink,                // highlight text: prices, "included", confirmations
    onSecondary = NovaBlack,             // text sitting on pink
    secondaryContainer = NovaPurple,     // selected tab bubble and selected chips
    onSecondaryContainer = NovaWhite,    // icon or text inside that bubble
    tertiary = NovaCorona,               // the warm gold of the rising star
    onTertiary = NovaVoid,               // text sitting on gold
    background = NovaVoid,               // screen background: deep space
    onBackground = NovaWhite,            // text on the background
    surface = NovaPlum,                  // card background (the cards draw it see-through so stars show behind)
    onSurface = NovaWhite,               // text on cards
    surfaceVariant = NovaPlumDeep,       // bottom bar
    onSurfaceVariant = NovaMuted,        // quieter text
    outline = NovaMagenta,               // outlined button borders
    error = NovaSignal                   // warnings
)

// Wrap the whole app in this so every screen picks up the Nova colours and fonts
@Composable
fun NovaClubTheme(content: @Composable () -> Unit) {
    // Hand our colours and fonts to Material 3, then draw whatever is inside
    MaterialTheme(colorScheme = NovaColors, typography = NovaType, content = content)
}
