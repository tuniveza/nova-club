// Change this to match your own project's package name
package com.novacane.novaclub

// ---------- MEMBERSHIP TIERS ----------
// Each tier has a display name and a monthly price in pounds
enum class Tier(val label: String, val monthlyPrice: Int) {
    NON_MEMBER("Non-member", 0),      // Tier 0: pay as you go
    SYNDICATE("Base Syndicate", 29),  // Tier 1: £29 a month
    PRO("Pro Engineer", 79),          // Tier 2: £79 a month
    ELITE("Elite Club", 189)          // Tier 3: £189 a month
}

// What each tier gets. Fill in the real perks here when they're decided.
fun tierPerks(tier: Tier): List<String> = when (tier) {
    // Non-members pay per session and get the new-customer bonus
    Tier.NON_MEMBER -> listOf("Pay per session", "Bonus hours on your first booking", "Hybrid Loop setup: £$HYBRID_LOOP_FEE")
    // Syndicate members still pay for the Hybrid Loop setup
    Tier.SYNDICATE -> listOf("Monthly membership", "Hybrid Loop setup: £$HYBRID_LOOP_FEE")
    // Pro gets the setup free
    Tier.PRO -> listOf("Free Hybrid Loop setup")
    // Elite gets the setup free and can roll hours over
    Tier.ELITE -> listOf("Free Hybrid Loop setup", "Unused hours roll over once approved")
}

// ---------- SESSION TYPES AND PRICES ----------
// baseRate is the normal hourly price (PLACEHOLDER: put the real rates in)
// floorRate is the lowest a flash sale is ever allowed to go
enum class SessionType(val label: String, val baseRate: Int, val floorRate: Int) {
    DRY_HIRE("Dry hire", 30, 20),      // floor is £20 an hour
    ENGINEERED("Engineered", 50, 35)   // floor is £35 an hour
}

// Flat fee for setting up the analog rack (PLACEHOLDER: set the real fee)
const val HYBRID_LOOP_FEE = 25
// Every revision after the first free one costs this much
const val EXTRA_REVISION_FEE = 25
// How many free revisions each mix gets
const val FREE_REVISIONS = 1
// The gear the Hybrid Loop setup covers
const val RACK_GEAR = "ngOmnibus, CL 1B, 1176 and Pultec"

// Pro and Elite get the Hybrid Loop setup for free
fun hybridLoopIsFree(tier: Tier): Boolean = tier == Tier.PRO || tier == Tier.ELITE

// Never let a discounted price drop below the floor for that session type
fun flashPrice(type: SessionType, discounted: Int): Int = maxOf(discounted, type.floorRate)

// The onboarding bonus for non-members, based on hours booked
// Returns null when they haven't booked enough for a bonus
fun onboardingBonus(hours: Int): String? = when {
    hours >= 8 -> "Free mix and master credit"   // 8 hours or more
    hours >= 4 -> "2 free hours"                 // 4 to 7 hours
    hours >= 2 -> "1 free hour"                  // 2 to 3 hours
    else -> null                                 // under 2 hours: nothing
}

// Adds up the price shown on the booking screen
// (the real charge should always be worked out again on the server)
fun sessionTotal(type: SessionType, hours: Int, tier: Tier, hybridLoop: Boolean): Int {
    // Hourly rate times number of hours
    val sessionCost = type.baseRate * hours
    // Add the setup fee only if they asked for it and their tier doesn't include it
    val loopCost = if (hybridLoop && !hybridLoopIsFree(tier)) HYBRID_LOOP_FEE else 0
    // Put the two together
    return sessionCost + loopCost
}

// ---------- SAMPLE DATA (replaced by your backend later) ----------

// One empty slot on sale
data class FlashSlot(
    val day: String,          // e.g. "Tonight"
    val time: String,         // e.g. "8pm to 11pm"
    val type: SessionType,    // dry hire or engineered
    val hoursAway: Int,       // which alert step this is: 24, 12 or 6
    val price: Int,           // flash price per hour
    val soldOut: Boolean      // true once someone has booked it
)

// Pretend flash sales so the screen has something to show
val sampleFlashSlots = listOf(
    FlashSlot("Tomorrow", "2pm to 5pm", SessionType.DRY_HIRE, 24, flashPrice(SessionType.DRY_HIRE, 22), false),
    FlashSlot("Tonight", "8pm to 11pm", SessionType.ENGINEERED, 6, flashPrice(SessionType.ENGINEERED, 38), false),
    FlashSlot("Tomorrow", "10am to 1pm", SessionType.ENGINEERED, 12, flashPrice(SessionType.ENGINEERED, 30), true)
)

// One mix draft sent back by an engineer
data class MixDraft(
    val title: String,        // the song name
    val version: Int,         // draft number
    val invoicePaid: Boolean, // watermark stays on until this is true
    val revisionsUsed: Int    // how many revisions they've asked for
)

// Pretend drafts so the Mixes screen has something to show
val sampleDrafts = listOf(
    MixDraft("Late Train", 2, false, 0),
    MixDraft("Clyde Vale", 3, true, 1)
)

// Pretend Elite rollover hours waiting for the admin to approve
const val SAMPLE_ROLLOVER_HOURS = 3
// Pretend hours booked so far this month
const val SAMPLE_HOURS_THIS_MONTH = 7

// One thing that happened on the membership
data class MembershipEvent(
    val title: String,        // what happened
    val detail: String,       // a bit more about it
    val whenText: String      // when, e.g. "2 days ago"
)

// Pretend recent membership activity, newest first (replaced by your backend later)
val sampleMembershipActivity = listOf(
    MembershipEvent("Session booked", "Studio A, engineered, 4 hours", "Today"),
    MembershipEvent("Rollover requested", "$SAMPLE_ROLLOVER_HOURS unused hours, waiting for studio approval", "2 days ago"),
    MembershipEvent("Hybrid Loop setup", "Patched in free with your membership", "Last week"),
    MembershipEvent("Membership renewed", "Elite Club, £${Tier.ELITE.monthlyPrice}", "1st of the month")
)

// One strip in the Home screen's stripe list: a title, a line underneath, and an optional picture from res/drawable
data class StripItem(
    val title: String,        // e.g. "Hybrid Loop"
    val subtitle: String,     // e.g. what it is or costs
    val image: Int? = null    // a picture in res/drawable, e.g. R.drawable.studio_desk (none by default)
)

// The strips on Home. Add an image to any of them once you've got photos in res/drawable.
val sampleStrips = listOf(
    StripItem("Hybrid Loop", "$RACK_GEAR, patched in before you arrive"),
    StripItem("Remote mixing", "Send your stems, get drafts back, $FREE_REVISIONS free revision"),
    StripItem("Flash sales", "Empty slots from £${SessionType.DRY_HIRE.floorRate} an hour"),
    StripItem("Nova Club", "Memberships from £${Tier.SYNDICATE.monthlyPrice} a month")
)

// ---------- SESSIONS BY CATEGORY ----------
// The three kinds of session on Home, each with its own label and look
enum class SessionCategory(val label: String, val tagline: String) {
    ONGOING("On air", "In session right now"),        // happening in the studio now
    AVAILABLE("Open sessions", "Ready to book"),        // free slots at the normal price
    DEALS("Flash deals", "Going fast")                  // empty slots on sale
}

// One session in a category
data class StudioSession(
    val title: String,                 // e.g. "Studio A, until 9pm"
    val detail: String,                // e.g. the type and price
    val category: SessionCategory,     // which group it goes in
    val progress: Float? = null,       // for sessions on air: how far through they are (0 to 1)
    val soldOut: Boolean = false       // for deals: someone has already booked it
)

// Pretend sessions so each category has something to show (replaced by your backend later).
// The deals come straight from the flash sales above.
val sampleSessions: List<StudioSession> = listOf(
    StudioSession("Studio A, until 9pm", "${SessionType.ENGINEERED.label}, 2 of 5 hours in", SessionCategory.ONGOING, progress = 0.4f),
    StudioSession("Live room, until 6pm", "${SessionType.DRY_HIRE.label}, nearly done", SessionCategory.ONGOING, progress = 0.85f),
    StudioSession("Tomorrow, 4pm to 8pm", "${SessionType.ENGINEERED.label}, £${SessionType.ENGINEERED.baseRate} an hour", SessionCategory.AVAILABLE),
    StudioSession("Thursday, 2pm to 6pm", "${SessionType.DRY_HIRE.label}, £${SessionType.DRY_HIRE.baseRate} an hour", SessionCategory.AVAILABLE)
) + sampleFlashSlots.map { slot ->
    StudioSession(
        "${slot.day}, ${slot.time}",
        if (slot.soldOut) "${slot.type.label}, sold out" else "${slot.type.label}, £${slot.price}/hr, was £${slot.type.baseRate}",
        SessionCategory.DEALS,
        soldOut = slot.soldOut
    )
}
