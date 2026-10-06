// Change this to match your own project's package name
package com.novacane.novaclub

// Lets us draw a coloured border round a card
import androidx.compose.foundation.BorderStroke
// Lets us paint a background colour behind something
import androidx.compose.foundation.background
// Layout tools: rows, columns, boxes, spacing, sizing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
// A scrolling list that only draws what's on screen
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
// The moon on the membership card goes round on a never-ending timer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlin.math.PI
// Asking for the notification permission
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
// The vertical deal carousel: a pager that snaps card to card, and tools to shrink and fade the cards
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
// The sigil line card: positions and a gentle buzz
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import kotlin.math.abs
// Noticing when the app comes back from Settings
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
// Lets us draw an outline round a shape
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
// Icons (all from the core set, no extra library needed)
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
// Material 3 parts
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
// State tools: remembering values between redraws
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// Positioning and styling tools
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
// The app's sound and calendar sync: handing them to every screen, and running them only while the app is in front
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.repeatOnLifecycle
// The booking calendar: loading the studio's bookings in the background, and colouring the chosen chip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Keeping the calendar in sync: waiting between reads, and the re-check before paying
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// The Club tab's tiles: matching heights, and scrolling only if the phone is very short
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
// The membership screen: sliding in, the back gesture, the back arrow, tapping the card, and its glow
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
// The list of files in res/ (the sigil lives in res/drawable)
import uk.co.novacane.novaclub.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================================
// THE APP SHELL: bottom bar + whichever screen is selected
// =====================================================================

// One button on the bottom bar
data class NavTab(val label: String, val icon: ImageVector)

// The five tabs, left to right
val navTabs = listOf(
    NavTab("Home", Icons.Filled.Home),        // membership + flash sales
    NavTab("Book", Icons.Filled.DateRange),   // book a session
    NavTab("Mixes", Icons.Filled.PlayArrow),  // remote mixing portal
    NavTab("Club", Icons.Filled.Star),        // membership tiers
    NavTab("Deal", Icons.Filled.ShoppingCart) // swipeable flash sale carousel
)

// The whole app. Call this from MainActivity.
@Composable
fun NovaClubApp() {
    // Which tab is showing (0 = Home)
    var selectedTab by remember { mutableIntStateOf(0) }
    // The user's tier. Switch it on the Club tab to see prices change.
    var tier by remember { mutableStateOf(Tier.ELITE) }
    // Whether the membership screen is open (it opens from the card on Home)
    var showMembership by remember { mutableStateOf(false) }
    // A day for the Book screen to jump to (set by tapping the "Just booked" banner)
    var bookFocus by remember { mutableStateOf<Day?>(null) }

    // The app's sound (ambient track and effects) and the studio calendar, shared by every screen
    val context = LocalContext.current
    val sound = remember { NovaSound(context) }
    val sync = remember { StudioCalendarSync(context, availabilitySource()) }
    DisposableEffect(Unit) { onDispose { sound.release() } }

    // Animated space behind everything (the stars lean towards your finger)
    CosmosBackground {
    CompositionLocalProvider(LocalNovaSound provides sound, LocalStudioCalendar provides sync) {
    val clock = rememberSecondsClock()
    // While the app is in front: play the ambient track and keep the calendar in sync. Both stop when it goes to the back.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            sound.onForeground()
            try { sync.run { clock.value } } finally { sound.onBackground() }
        }
    }
    // When the calendar changes: a sound, and a banner (tap it to see the day)
    var banner by remember { mutableStateOf<String?>(null) }
    var bannerBooked by remember { mutableStateOf(true) }
    LaunchedEffect(sync.latest?.id) {
        val change = sync.latest ?: return@LaunchedEffect
        // Booked: a chime in. Cancelled: a soft fall away. Rescheduled: away from the old time, then in at the new one.
        val c = change.changes
        when {
            c.booked.isNotEmpty() -> sound.play(Sfx.BOOKING_IN)
            c.moved.isNotEmpty() -> launch { sound.play(Sfx.BOOKING_OUT); delay(450); sound.play(Sfx.BOOKING_IN) }
            else -> sound.play(Sfx.BOOKING_OUT)
        }
        banner = describeChanges(change.changes)
        bannerBooked = change.changes.added.isNotEmpty()
        delay(5500)
        banner = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
    // Scaffold gives us a slot for the bottom bar and handles spacing
    Scaffold(
        containerColor = Color.Transparent,                // see-through, so space shows behind
        contentColor = MaterialTheme.colorScheme.onBackground,  // default text colour: white (see-through has no text colour of its own)
        // Draw the bottom navigation bar
        bottomBar = {
            // The tab bar: a floating glass dock with a glowing orb on the chosen tab
            NovaNavBar(
                labels = navTabs.map { it.label },
                icons = navTabs.map { it.icon },
                selected = selectedTab,
                onSelect = {
                    if (it != selectedTab || showMembership) sound.play(Sfx.SWITCH)
                    selectedTab = it; showMembership = false                     // switch tab (closing the membership screen)
                }
            )
        }
    ) { innerPadding ->
        // A box that keeps screens clear of the bottom bar
        Box(modifier = Modifier.padding(innerPadding)) {
            // The membership screen slides up over the tabs when it opens, and fades away when it closes
            AnimatedContent(
                targetState = showMembership,
                transitionSpec = { (slideInVertically { it / 4 } + fadeIn()) togetherWith fadeOut() },
                label = "membership"
            ) { membershipOpen ->
                if (membershipOpen) {
                    MembershipScreen(
                        tier = tier,
                        onBack = { showMembership = false; sound.play(Sfx.SWITCH) },              // back to Home
                        onChangePlan = { showMembership = false; selectedTab = 3; sound.play(Sfx.SWITCH) }   // over to the Club tab's plans
                    )
                } else {
                    // Show the screen that matches the selected tab
                    when (selectedTab) {
                        0 -> HomeScreen(tier, onOpenMembership = { showMembership = true; sound.play(Sfx.SWITCH) })  // Home
                        1 -> BookScreen(tier, focus = bookFocus, onFocused = { bookFocus = null })                  // Book
                        2 -> MixesScreen()                                   // Mixes
                        3 -> ClubScreen(tier, onChoose = { tier = it; sound.play(Sfx.CONFIRM) })                 // Club
                        else -> DealScreen()                                 // Deal
                    }
                }
            }
        }
    }
    // The celebration over everything: a comet and nova burst for a new booking, golden stardust when one frees up
    BookingCelebration(sync.latest)
    // "Just booked · …" sliding in at the top. Tap it to jump to that day on the Book screen and see it land.
    BookingToast(
        banner, booked = bannerBooked,
        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 6.dp, start = 16.dp, end = 16.dp),
        onClick = {
            val day = sync.latest?.changes?.let { (it.added + it.removed).firstOrNull()?.day }
            if (day != null) {
                bookFocus = day; selectedTab = 1; showMembership = false
                sync.replay(clock.value)                                   // play the animation again, now the day is on show
                sound.play(Sfx.SWITCH)
            }
            banner = null
        }
    )
    }   // end of the Box
    }   // end of the sound and calendar
    }   // end of CosmosBackground
}

// =====================================================================
// SHARED PIECES used on several screens
// =====================================================================

// Big title at the top of each screen, with a quieter line under it
@Composable
fun ScreenTitle(title: String, subtitle: String) {
    // Stack the two lines, as wide as the screen, with both lines in the middle
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),   // full width, small gap underneath
        horizontalAlignment = Alignment.CenterHorizontally           // put each line in the middle
    ) {
        // The big title, centred, in capitals with wide spacing like the website's headings
        Text(title.uppercase(), fontSize = 30.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.sp, textAlign = TextAlign.Center)
        // The smaller line, centred, in the website's typewriter font
        Text(subtitle.uppercase(), fontFamily = SourceCodePro, fontSize = 12.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        // A fading line with a little star on it
        StarDivider(modifier = Modifier.padding(top = 10.dp))
    }
}

// =====================================================================
// DEAL: a swipeable carousel of this week's flash sales
// =====================================================================

@Composable
fun DealScreen() {
    // Title at the top, carousel filling the rest of the screen
    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        // Title
        Box(modifier = Modifier.padding(horizontal = 16.dp)) { ScreenTitle("Deals", "Swipe up through this week's offers") }
        // The carousel, taking all the space that's left
        DealCarousel(modifier = Modifier.weight(1f))
    }
}

// The backgrounds the deal cards take turns with: each a different corner of the nebula
private val dealGradients = listOf(
    NovaCardGradient,                                                                  // magenta to indigo, like the membership card
    Brush.linearGradient(listOf(NovaPurple, NovaCobalt)),                              // purple to cobalt
    Brush.linearGradient(listOf(NovaPink.copy(alpha = 0.85f), NovaPurple, NovaIndigo)) // hot pink to indigo
)

// A vertical carousel: the middle card full size, the ones above and below shrinking and fading as they move away.
// (Material 3's carousels only go sideways, so this is built from Compose's VerticalPager, which snaps card to card.)
@Composable
fun DealCarousel(modifier: Modifier = Modifier) {
    // The deals to show (the flash sales from NovaData.kt)
    val deals = sampleFlashSlots
    // Which card we're on, and how far through a swipe we are
    val pagerState = rememberPagerState { deals.size }
    // How tall each card is
    val cardHeight = 220.dp

    // BoxWithConstraints tells us how much height we've got, so we can centre the current card
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Empty space above the first card and below the last, so even they can sit in the middle
        val edge = ((maxHeight - cardHeight) / 2).coerceAtLeast(0.dp)
        VerticalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(cardHeight),                                                  // every card the same height
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = edge, bottom = edge),  // centres the current card
            pageSpacing = 14.dp,                                                                    // gap between cards
            modifier = Modifier.fillMaxSize()
        ) { page ->
            DealCard(
                deal = deals[page],
                index = page,
                modifier = Modifier.graphicsLayer {
                    // How far this card is from the middle (0 = in the middle, 1 = one card away or more)
                    val distance = abs(pagerState.currentPage - page + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
                    // Shrink and fade as it moves away
                    val scale = 1f - 0.14f * distance
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - 0.5f * distance
                }
            )
        }
        // Dots down the right-hand side showing which card you're on
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(deals.size) { i ->
                val current = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(if (current) 7.dp else 5.dp)                                         // the current dot is bigger
                        .clip(CircleShape)
                        .background(if (current) NovaPink else NovaMuted.copy(alpha = 0.4f))         // and pink
                )
            }
        }
    }
}

// One deal card: a Novacane gradient with a faint sigil, and the deal written in the bottom-left corner
@Composable
fun DealCard(deal: FlashSlot, index: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.extraLarge)                                   // big rounded corners
            .background(dealGradients[index % dealGradients.size])                   // take turns with the gradients
    ) {
        // The sigil, big and faint, hanging off the top-right corner
        Image(
            painter = painterResource(R.drawable.novacane_logo),
            contentDescription = null,                                       // just decoration
            contentScale = ContentScale.Crop,
            alpha = 0.14f,
            modifier = Modifier.size(170.dp).align(Alignment.TopEnd).offset(x = 50.dp, y = (-30).dp)
        )
        // The deal itself, centred along the bottom (one line each)
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // "FLASH SALE" or "SOLD OUT"
            Text(
                if (deal.soldOut) "SOLD OUT" else "FLASH SALE",
                fontFamily = SourceCodePro, fontSize = 11.sp, letterSpacing = 2.sp,
                color = if (deal.soldOut) NovaWhite else NovaBlush,     // white, as red is hard to read on the pink cards
                fontWeight = if (deal.soldOut) FontWeight.SemiBold else null, maxLines = 1, softWrap = false
            )
            // The price, big
            Text("£${deal.price}/hr", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = NovaWhite, maxLines = 1, softWrap = false)
            // When
            Text("${deal.day}, ${deal.time}", color = NovaWhite, maxLines = 1, softWrap = false)
            // What, and the normal price
            Text("${deal.type.label}, normally £${deal.type.baseRate}", fontSize = 13.sp, color = NovaBlush, maxLines = 1, softWrap = false)
        }
    }
}

// A section heading with a small gold star in front, centred across the screen
@Composable
fun SectionHeading(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,                  // star and words together in the middle
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
    ) {
        // The star
        Sparkle(size = 14.dp)
        // A small gap
        Spacer(modifier = Modifier.width(10.dp))
        // The words
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

// A standard Nova card: dark plum glass, softly rounded, with a thin magenta-purple edge
@Composable
fun NovaCard(border: BorderStroke? = BorderStroke(1.dp, NovaEdgeGradient), content: @Composable () -> Unit) {
    // Material card with our colours
    Card(
        modifier = Modifier.fillMaxWidth(),                                          // full width
        shape = RoundedCornerShape(14.dp),                                           // soft corners
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)), // see-through panel, so stars show behind
        border = border                                                              // thin magenta-purple edge (unless a screen asks for a different one)
    ) {
        // Inner padding so text doesn't touch the edges, with whatever's inside sitting in the middle
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.TopCenter) { content() }
    }
}

// =====================================================================
// HOME: the membership strip at the top, the logo, then a switch between areas (on air, open, deals, studio)
// =====================================================================

@Composable
fun HomeScreen(tier: Tier, onOpenMembership: () -> Unit) {
    val sound = LocalNovaSound.current
    // Lets us buzz the phone gently when a stripe is tapped
    val haptics = LocalHapticFeedback.current
    // Which area is showing (kept if the screen is rebuilt, e.g. when the phone turns)
    var area by rememberSaveable { mutableStateOf(HomeArea.ON_AIR) }

    Column(modifier = Modifier.fillMaxSize()) {
        // The membership strip, pinned at the top. Tap it to open the membership screen.
        MembershipStrip(tier = tier, onOpen = onOpenMembership, modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp))

        // Everything else scrolls underneath it
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // The Novacane sigil, centred, as a glowing planet with two moons going round it
            item { SigilOrbit(painterResource(R.drawable.novacane_logo), scale = 0.72f) }   // a little smaller, to leave room for the strips
            // Title (types itself out) with the location underneath, both centred
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    // "NOVA CLUB", typed out letter by letter
                    TypewriterText("Nova Club")
                    // The small line underneath, in the typewriter font like the other screens
                    Text("NOVACANE STUDIOS, FOREST HILL", fontFamily = SourceCodePro, fontSize = 12.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    // A fading line with a little star on it
                    // Music and sound effects, on or off
                    if (sound != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            SoundPill("♪", "Ambient", on = sound.ambientOn) { sound.setAmbient(!sound.ambientOn) }
                            SoundPill("✦", "Sounds", on = sound.effectsOn) { sound.setEffects(!sound.effectsOn) }
                        }
                    }
                    StarDivider(modifier = Modifier.padding(top = 10.dp))
                }
            }
            // The switch between areas: a rack of pads, each showing how many are in it
            item {
                AreaSwitcher(
                    selected = area,
                    onSelect = { if (it != area) sound?.play(Sfx.SWITCH); area = it },
                    counts = HomeArea.entries.associateWith { a ->
                        a.category?.let { c -> sampleSessions.count { it.category == c && !it.soldOut } } ?: sampleStrips.size
                    }
                )
            }
            // The chosen area. Switching slides the new one in from the side it's on, and the old one out the other way.
            item {
                AnimatedContent(
                    targetState = area,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal          // moving right along the switcher?
                        (slideInHorizontally(tween(380)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(380))) togetherWith
                            (slideOutHorizontally(tween(260)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(260)))
                    },
                    label = "area"
                ) { shown ->
                    HomeAreaContent(shown, onTap = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); sound?.play(Sfx.TAP) })
                }
            }
        }
    }
}

// A small on/off pill for the music or the sound effects: glowing pink when on, dim when off
@Composable
fun SoundPill(symbol: String, label: String, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shape)
            .background(if (on) NovaMagenta.copy(alpha = 0.3f) else NovaVoid.copy(alpha = 0.5f))
            .border(1.dp, if (on) NovaPink.copy(alpha = 0.6f) else NovaMuted.copy(alpha = 0.25f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(symbol, fontSize = 12.sp, color = if (on) NovaPink else NovaMuted.copy(alpha = 0.6f))
        Text(" ${label.uppercase()} ${if (on) "ON" else "OFF"}", fontFamily = SourceCodePro, fontSize = 9.sp, letterSpacing = 1.sp, color = if (on) NovaBlush else NovaMuted.copy(alpha = 0.6f))
    }
}

// What's in one area of Home: its strips (its name, tagline and count are on the pads above)
@Composable
fun HomeAreaContent(area: HomeArea, onTap: () -> Unit) {
    val look = areaLook(area)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        val category = area.category
        if (category != null) {
            // A session area: on air, open or deals
            val sessions = sampleSessions.filter { it.category == category }
            // Deals: the switch for flash sale notifications sits with them
            if (area == HomeArea.DEALS) FlashAlertsCard()
            // One galaxy strip per session, in the area's colours and sigils
            sessions.forEachIndexed { i, session ->
                StripeCard(
                    title = session.title,
                    subtitle = session.detail,
                    color = look.color,
                    accent = look.accent,
                    progress = session.progress,                      // on air: how far through it is
                    seed = i + category.ordinal * 3,                  // so each strip's sky is different
                    leftSigil = look.leftSigil,
                    rightSigil = look.rightSigil,
                    modifier = if (session.soldOut) Modifier.alpha(0.5f) else Modifier,   // sold-out deals fade back
                    onClick = onTap                                   // a little buzz (swap for opening the booking)
                )
            }
        } else {
            // The studio area: what the studio offers
            sampleStrips.forEachIndexed { i, strip ->
                StripeCard(
                    title = strip.title,
                    subtitle = strip.subtitle,
                    image = strip.image?.let { painterResource(it) },     // only if the item has a picture
                    color = stripeColors[i % stripeColors.size],
                    seed = i,                                             // so each panel, its stars and its two sigils are different
                    onClick = onTap                                       // a little buzz (swap for opening something)
                )
            }
        }
    }
}

// =====================================================================
// MEMBERSHIP: the full card, this month's numbers, perks and recent activity
// =====================================================================

@Composable
fun MembershipScreen(tier: Tier, onBack: () -> Unit, onChangePlan: () -> Unit) {
    // The phone's back gesture closes this screen rather than leaving the app
    BackHandler(onBack = onBack)
    // Colours for the perk strips
    val perkColors = listOf(NovaMagenta, NovaPurple, NovaCobalt)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // A back arrow in the top-left corner
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home", tint = NovaBlush)
                }
            }
        }
        // Title
        item { ScreenTitle("Your membership", "Nova Club, Novacane Studios") }
        // The full membership card
        item { MembershipFaceplate(tier) }
        // This month's numbers, side by side
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("$SAMPLE_HOURS_THIS_MONTH", "hours booked\nthis month", NovaPink, Modifier.weight(1f))
                StatTile(if (tier == Tier.ELITE) "$SAMPLE_ROLLOVER_HOURS" else "–", "hours\nrolling over", NovaCorona, Modifier.weight(1f))
                StatTile(if (hybridLoopIsFree(tier)) "Free" else "£$HYBRID_LOOP_FEE", "Hybrid Loop\nsetup", Color(0xFF9FB4FF), Modifier.weight(1f))
            }
        }
        // Perks, as galaxy strips
        item { SectionHeading("Your perks") }
        itemsIndexed(tierPerks(tier)) { i, perk ->
            StripeCard(
                title = perk,
                subtitle = "Included with ${tier.label}",
                color = perkColors[i % perkColors.size],
                seed = i + 5                                          // different skies and sigils from the Home strips
            )
        }
        // Recent membership activity, newest first
        item { SectionHeading("Recent activity") }
        item {
            NovaCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sampleMembershipActivity.forEachIndexed { i, event ->
                        // A faint star divider between events
                        if (i > 0) StarDivider()
                        Text(event.title, fontWeight = FontWeight.SemiBold)
                        Text(event.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(event.whenText.uppercase(), fontFamily = SourceCodePro, fontSize = 10.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
        // Change plan: goes to the Club tab's list of plans
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(onClick = onChangePlan) { Text("Change plan") }
            }
        }
    }
}

// One number with a label under it, in a small glassy tile
@Composable
fun StatTile(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))              // the same see-through panel as the cards
            .border(1.dp, Brush.linearGradient(listOf(color.copy(alpha = 0.6f), NovaPurple.copy(alpha = 0.3f))), shape)
            .padding(vertical = 14.dp, horizontal = 6.dp)
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// The colours the stripes take turns with (deep ones, so the white writing stays readable)
private val stripeColors = listOf(NovaMagenta, NovaPurple, NovaCobalt, Color(0xFF9C1757))

// Asks for permission to send notifications, then sends a flash sale alert
@Composable
fun FlashAlertsCard() {
    // Android's context: needed for anything that talks to the system (permissions, notifications)
    val context = LocalContext.current
    // Whether notifications are allowed right now
    var allowed by remember { mutableStateOf(canNotify(context)) }
    // Whether they've already said no (then the system won't ask again, so we point them to Settings)
    var refused by remember { mutableStateOf(false) }
    // Check again every time the app comes back to the front (they may have changed it in Settings)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val watcher = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) allowed = canNotify(context) }
        lifecycle.addObserver(watcher)                       // start watching
        onDispose { lifecycle.removeObserver(watcher) }      // stop when the card leaves the screen
    }
    // Shows Android's "Allow Nova Club to send you notifications?" box, and tells us the answer
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted && canNotify(context)        // remember the answer
        refused = !granted                             // they said no
        // Said yes: send the next open slot straight away so they can see what an alert looks like
        if (allowed) sampleFlashSlots.firstOrNull { !it.soldOut }?.let { showFlashSaleAlert(context, it) }
    }

    NovaCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Heading
            Text("Flash sale alerts", fontWeight = FontWeight.SemiBold)
            // What they'll get
            Text("A notification 24, 12 and 6 hours before an empty slot goes cheap", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            // Gap
            Spacer(modifier = Modifier.height(10.dp))
            when {
                // Allowed: offer a test alert using the next open slot
                allowed -> OutlinedButton(onClick = {
                    sampleFlashSlots.firstOrNull { !it.soldOut }?.let { showFlashSaleAlert(context, it) }
                }) { Text("Send a test alert") }
                // Said no, or switched off in Settings: only Settings can turn them back on
                refused || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> OutlinedButton(onClick = {
                    openNotificationSettings(context)
                }) { Text("Turn on in Settings") }
                // Android 13+ and not asked yet: ask
                else -> Button(onClick = {
                    askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Turn on alerts") }
            }
        }
    }
}

// The membership card, styled like a piece of rack gear in the Novacane gradient, with the sigil as a ringed planet
@Composable
fun MembershipFaceplate(tier: Tier) {
    // The plate's shape: tight corners, like rack gear
    val plate = RoundedCornerShape(6.dp)
    // The moon going round the sigil: once round (0 to 2π) every 16 seconds
    val moon = rememberInfiniteTransition(label = "plateMoon").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "moon"
    )
    // Outer box holding the decorations and the writing
    Box(
        modifier = Modifier
            .fillMaxWidth()                                                               // full width
            .clip(plate)                                                                  // trim anything hanging off the edges (the watermark)
            .background(NovaCardGradient)                                                 // magenta-to-indigo gradient plate
            .border(1.dp, Brush.linearGradient(listOf(NovaPink, NovaPurple)), plate)      // thin pink-to-purple border
            .faceplateTrim(painterResource(R.drawable.novacane_logo)) { moon.value }      // star field, ringed sigil with its moon, corner brackets
            .padding(20.dp)                                                               // breathing room
    ) {
        // The writing on the plate, centred
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Small line saying what this is, with the sigil's slashes in front
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Three slanted strokes from the sigil's N
                SigilSlashes(color = NovaPink)
                // A small gap
                Spacer(modifier = Modifier.width(8.dp))
                // The words, in the typewriter font
                Text("YOUR MEMBERSHIP", fontFamily = SourceCodePro, fontSize = 12.sp, letterSpacing = 2.sp, color = NovaBlush)
            }
            // The tier name, big, in capitals like the website's headings
            Text(tier.label.uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp, color = NovaWhite)
            // The price, or "Pay per session" for non-members
            Text(
                if (tier.monthlyPrice > 0) "£${tier.monthlyPrice} a month" else "Pay per session",
                color = NovaWhite
            )
            // Elite only: show rollover hours waiting for approval
            if (tier == Tier.ELITE) {
                // Small gap
                Spacer(modifier = Modifier.height(12.dp))
                // How many hours are waiting
                Text("$SAMPLE_ROLLOVER_HOURS hours rolling over", fontWeight = FontWeight.SemiBold, color = NovaWhite)
                // Status line: the studio has to approve them in Nova Hub
                Text("Waiting for studio approval", color = NovaBlush)
            }
            // Bouncing level meter, centred underneath (it used to sit top-right, but would cover the centred writing)
            Spacer(modifier = Modifier.height(12.dp))
            VuMeter(barColor = NovaBlush)                                                // pale pink bars on the gradient
        }
    }
}

// One flash sale slot
@Composable
fun FlashSlotCard(slot: FlashSlot) {
    // Standard card
    NovaCard {
        // Everything stacked and centred: details, then price, then button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // e.g. "Tonight, 8pm to 11pm"
            Text("${slot.day}, ${slot.time}", fontWeight = FontWeight.SemiBold)
            // e.g. "Engineered, normally £50 an hour"
            Text("${slot.type.label}, normally £${slot.type.baseRate} an hour", color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Which alert this is (24, 12 or 6 hours before)
            Text("Starts in ${slot.hoursAway} hours", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            // Gap
            Spacer(modifier = Modifier.height(8.dp))
            // If someone already booked it, say so in red
            if (slot.soldOut) {
                Text("Sold out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            } else {
                // Otherwise show the flash price
                Text("£${slot.price}/hr", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                // And a button to book it (does nothing yet)
                Button(onClick = { }) { Text("Book it") }
            }
        }
    }
}

// =====================================================================
// BOOK: pick a session type, a day on the moon calendar, a time on the day's orbit, and see the price.
// Everything is linked: the hours, session type and Hybrid Loop setup all change which days and times are free.
// =====================================================================

@Composable
fun BookScreen(tier: Tier, focus: Day? = null, onFocused: () -> Unit = {}) {   // focus: a day to jump to (e.g. from the booking banner)
    val sound = LocalNovaSound.current
    // Which session type is picked
    var type by remember { mutableStateOf(SessionType.ENGINEERED) }
    // How many hours
    var hours by remember { mutableIntStateOf(2) }
    // Whether they want the analog rack set up (it needs setup time before the session)
    var hybridLoop by remember { mutableStateOf(false) }
    // Today in London, the month showing, the chosen day and the chosen start hour
    val today = remember { Day.today() }
    var month by remember { mutableStateOf(today.firstOfMonth()) }
    var selected by remember { mutableStateOf<Day?>(null) }
    var start by remember { mutableStateOf<Int?>(null) }

    // The studio's bookings come from the app-wide calendar sync, which keeps the months on show up to date
    val sync = LocalStudioCalendar.current!!
    val clock = rememberSecondsClock()
    LaunchedEffect(month) { sync.watch(month) }
    // Check the calendar every few seconds while this screen is open
    DisposableEffect(sync) {
        sync.bookScreenOpened()
        onDispose { sync.bookScreenClosed() }
    }
    val busy = sync.busyFor(month)                                           // null while the month is loading
    // Jump to a day when asked (e.g. tapping "Just booked · …")
    LaunchedEffect(focus) {
        if (focus != null) { month = focus.firstOfMonth(); selected = focus; onFocused() }
    }
    // What happened when "Book and pay" was last pressed
    var bookingNote by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Everything below is worked out from the choices above, so it all updates together
    val prep = if (hybridLoop) HYBRID_LOOP_PREP_HOURS else 0f
    val byDay = remember(busy) { busy.orEmpty().groupBy { it.day } }
    // The start times that fit on a day (for these hours and setup)
    fun startsOn(day: Day, h: Int = hours, p: Float = prep) = startTimes(h, byDay[day].orEmpty(), p, earliestStart(day))
    // How open a day is (for the moons), or null if it can't be booked
    fun dayOpenness(day: Day): Float? = if (busy == null || day < today) null else openness(hours, byDay[day].orEmpty(), prep, earliestStart(day))
    // After any change, move the session to the nearest time that still fits (or none, if the day has no room)
    fun refit(day: Day? = selected, h: Int = hours, p: Float = prep) {
        start = day?.let { nearestStart(start ?: (STUDIO_OPEN_HOUR + 4), startsOn(it, h, p)) }
    }
    // When the month's bookings arrive or change: keep the chosen day if it's in this month (even if it's just filled
    // up, so you can see the new booking land), otherwise choose the first day with room
    LaunchedEffect(busy) {
        if (busy == null) return@LaunchedEffect
        val keep = selected?.takeIf { it.sameMonthAs(month) }
        selected = keep ?: (0 until month.daysInMonth).map { month.plusDays(it) }.firstOrNull { it >= today && startsOn(it).isNotEmpty() }
        refit()
    }

    // Live changes from the calendar, animated for a couple of seconds on the moons and the day's orbit
    val live = sync.latest
    var animating by remember { mutableStateOf(false) }
    LaunchedEffect(live?.id, live?.at) {
        if (live != null) { animating = true; delay((CHANGE_ANIMATION_SECONDS * 1000).toLong() + 200); animating = false }
    }
    val flashes = if (!animating || live == null) emptyList() else
        live.changes.added.map { BookingFlash(it, added = true, at = live.at) } + live.changes.removed.map { BookingFlash(it, added = false, at = live.at) }
    val shifts = if (!animating || live == null) emptyList() else {
        // What the month looked like just before the change, to show each moon sliding from its old brightness
        val before = live.before.groupBy { it.day }
        (live.changes.added + live.changes.removed).map { it.day }.distinct().map { d ->
            MoonShift(d, from = if (d < today) null else openness(hours, before[d].orEmpty(), prep, earliestStart(d)), booked = live.changes.added.any { it.day == d }, at = live.at)
        }
    }

    val day = selected
    val dayBusy = day?.let { byDay[it] }.orEmpty()
    val sessionFits = day != null && start != null && fits(start!!.toFloat(), hours, dayBusy, prep, earliestStart(day))
    val accent = sessionAccent(type)
    val bonus = onboardingBonus(hours)
    val total = sessionTotal(type, hours, tier, hybridLoop)

    // Everything on one page: title, session type, the moon calendar, the day's orbit (with hours and Hybrid Loop),
    // and the price with the pay button. (It only scrolls on very short phones, so nothing gets cut off.)
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // A compact title
        Text("BOOK A SESSION", fontSize = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.sp)
        Text("OPEN 10AM TO 11PM, EVERY DAY", fontFamily = SourceCodePro, fontSize = 10.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Session type: also sets the calendar's colour and the price
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
            SessionType.entries.forEach { option ->
                FilterChip(
                    selected = option == type,
                    onClick = { type = option; sound?.play(Sfx.TAP) },
                    label = { Text("${option.label}, £${option.baseRate}/hr") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = sessionAccent(option).copy(alpha = 0.25f),     // the chosen chip glows in its colour
                        selectedLabelColor = sessionAccent(option)
                    )
                )
            }
        }

        // The moon calendar
        CosmicCalendar(
            month = month,
            today = today,
            selected = day,
            openness = ::dayOpenness,
            accent = accent,
            loading = busy == null,
            onSelect = { picked -> selected = picked; refit(picked); sound?.play(Sfx.TAP) },
            onMonthChange = { step -> month = month.plusMonths(step); sound?.play(Sfx.SWITCH) },
            shifts = shifts
        )

        // The chosen day's orbit, with the hours and the Hybrid Loop switch underneath
        DayPanel(
            day = day,
            busy = dayBusy,
            start = start,
            hours = hours,
            prep = prep,
            fits = sessionFits,
            freeStarts = day?.let { startsOn(it).size } ?: 0,
            accent = accent,
            hybridLoop = hybridLoop,
            hybridNote = if (hybridLoopIsFree(tier)) "included" else "£$HYBRID_LOOP_FEE",
            onStartChange = { start = it },
            onHoursChange = { step ->
                val newHours = (hours + step).coerceIn(1, STUDIO_CLOSE_HOUR - STUDIO_OPEN_HOUR)
                hours = newHours
                sound?.play(if (step > 0) Sfx.STEP_UP else Sfx.STEP_DOWN)
                refit(h = newHours)                                  // the session stretches or shrinks, and moves if it no longer fits
            },
            onHybridChange = { on ->
                hybridLoop = on
                sound?.play(if (on) Sfx.TOGGLE_ON else Sfx.TOGGLE_OFF)
                refit(p = if (on) HYBRID_LOOP_PREP_HOURS else 0f)     // setup needs time before, so the session may move
            },
            flashes = flashes.filter { it.block.day == day }
        )

        // The booking and the price on the left, pay on the right
        NovaCard {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        if (sessionFits) "${day!!.label()} · ${hourLabel(start!!.toFloat())}–${hourLabel((start!! + hours).toFloat())}".uppercase()
                        else "CHOOSE A FREE TIME",
                        fontFamily = SourceCodePro, fontSize = 10.sp, letterSpacing = 1.sp, color = if (sessionFits) accent else NovaSignal, maxLines = 1
                    )
                    Text("£$total", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("${type.label}, $hours ${if (hours == 1) "hour" else "hours"}${if (hybridLoop) " + Hybrid Loop" else ""}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                    // Non-members: the first booking bonus for these hours
                    if (tier == Tier.NON_MEMBER && bonus != null) Text("First booking bonus: $bonus", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, maxLines = 1)
                }
                // Pay: first checks the calendar once more, in case someone has just booked this time
                Button(
                    enabled = sessionFits && !checking,
                    onClick = {
                        val chosenDay = day!!
                        val chosenStart = start!!
                        scope.launch {
                            checking = true
                            val fresh = sync.read(month) { clock.value }                 // also animates anything that's changed
                            val stillFree = fits(chosenStart.toFloat(), hours, fresh.filter { it.day == chosenDay }, prep, earliestStart(chosenDay))
                            bookingNote = if (stillFree) "Still free. Checkout isn't connected yet, so nothing has been booked."
                                          else "Someone has just booked that time. Pick another one."
                            sound?.play(if (stillFree) Sfx.CONFIRM else Sfx.CLASH)
                            if (!stillFree) refit()                       // slide to the nearest time that's still free
                            checking = false
                        }
                    }
                ) { Text(if (checking) "Checking…" else if (sessionFits) "Book and pay" else "Pick a time") }
            }
        }
        bookingNote?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }

        // Where the bookings came from, and when they were last checked
        val checkedAt = if (sync.lastChecked == 0L) "" else " · checked " + java.text.SimpleDateFormat("h:mm:ss a", java.util.Locale.UK).apply { timeZone = STUDIO_TIME_ZONE }.format(java.util.Date(sync.lastChecked))
        Text(sync.note + checkedAt, fontSize = 9.sp, color = NovaMuted.copy(alpha = 0.7f))
    }
    }   // end of the Box
}

// =====================================================================
// MIXES: order a mix, listen to drafts, ask for revisions
// =====================================================================

@Composable
fun MixesScreen() {
    // Whether they've just bought a mixing package
    var justOrdered by remember { mutableStateOf(false) }

    // Scrolling list
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title
        item { ScreenTitle("Mixes", "Remote mixing and mastering") }

        // Order a mix card
        item {
            NovaCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Heading
                    Text("Order a mix", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    // How sending stems works (no uploads in the app)
                    Text(
                        "After you pay, we'll email you how to prepare your stems and a link to send them by WeTransfer or Dropbox.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Gap
                    Spacer(modifier = Modifier.height(12.dp))
                    // Before ordering: show the button. After: show the confirmation.
                    if (justOrdered) {
                        Text("Ordered. Check your inbox for the stem guide.", color = MaterialTheme.colorScheme.secondary)
                    } else {
                        Button(onClick = { justOrdered = true }) { Text("Buy mixing package") }
                    }
                }
            }
        }

        // Heading for drafts
        item { SectionHeading("Your drafts") }

        // One card per draft
        items(sampleDrafts) { draft -> DraftCard(draft) }
    }
}

// One mix draft with player, payment and revision buttons
@Composable
fun DraftCard(draft: MixDraft) {
    // Whether the invoice is paid (starts from the sample data)
    var paid by remember { mutableStateOf(draft.invoicePaid) }
    // How many revisions they've used
    var revisionsUsed by remember { mutableIntStateOf(draft.revisionsUsed) }

    NovaCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Play button (hook up a real player later)
            IconButton(onClick = { }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play draft", tint = MaterialTheme.colorScheme.secondary)
            }
            // Song name and version
            Text(draft.title, fontWeight = FontWeight.SemiBold)
            Text("Draft ${draft.version}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            // If unpaid, show that the audio is watermarked
            if (!paid) {
                AssistChip(
                    onClick = { },                                                     // does nothing
                    label = { Text("Watermarked") },                                   // the word
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) } // padlock
                )
            }

            // Gap
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: pay and revision buttons, centred
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                // Only show "Pay invoice" if it isn't paid
                if (!paid) {
                    Button(onClick = { paid = true }) { Text("Pay invoice") }
                    // Gap between buttons
                    Spacer(modifier = Modifier.width(8.dp))
                }
                // Free revision if they have one left, otherwise the £25 one
                if (revisionsUsed < FREE_REVISIONS) {
                    OutlinedButton(onClick = { revisionsUsed++ }) { Text("Request free revision") }
                } else {
                    OutlinedButton(onClick = { }) { Text("Revision, £$EXTRA_REVISION_FEE") }
                }
            }
        }
    }
}

// =====================================================================
// CLUB: the four tiers
// =====================================================================

@Composable
fun ClubScreen(currentTier: Tier, onChoose: (Tier) -> Unit) {
    // Everything on one screen: the title, then the four plans as a 2 x 2 grid of tiles.
    // (It only scrolls on very short phones, so nothing gets cut off.)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Title
        ScreenTitle("Membership", "Pick the plan that fits how you work")
        // Two rows of two tiles; tiles in the same row are the same height
        Tier.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                pair.forEach { tier ->
                    TierTile(tier, isCurrent = tier == currentTier, onChoose = { onChoose(tier) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// One plan, as a compact tile: its body in the night sky, name, class and price, its perks, and a button at the bottom
@Composable
fun TierTile(tier: Tier, isCurrent: Boolean, onChoose: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,                                      // the button sits at the bottom
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))                // the same see-through panel as the cards
            .border(
                if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, NovaEdgeGradient),  // thick magenta on your plan
                shape
            )
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // The tier's body in the night sky: moon, planet, star or supernova
            TierBadge(tier)
            Text(tier.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("CLASS: ${tierBody(tier).uppercase()}", fontFamily = SourceCodePro, fontSize = 9.sp, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.tertiary)
            Text(if (tier.monthlyPrice > 0) "£${tier.monthlyPrice}/mo" else "Free", fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            // Each perk, small
            tierPerks(tier).forEach { perk ->
                Text(perk, fontSize = 11.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        // "Your plan" (greyed out) or "Choose"
        val small = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        if (isCurrent) {
            OutlinedButton(onClick = { }, enabled = false, contentPadding = small, modifier = Modifier.height(34.dp)) { Text("Your plan", fontSize = 12.sp) }
        } else {
            Button(onClick = onChoose, contentPadding = small, modifier = Modifier.height(34.dp)) { Text("Choose", fontSize = 12.sp) }
        }
    }
}
