package com.novacane.novaclub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Checks the booking calendar's rules: what fits, what's open, and London times
class BookingLogicTest {
    private val day = Day(2026, 9, 6)   // Tuesday 6 October 2026 (months count from 0)

    @Test
    fun aSessionFitsInsideOpeningHoursOnAnEmptyDay() {
        assertTrue(fits(10f, 2, emptyList()))
        assertTrue(fits(21f, 2, emptyList()))           // 9pm to 11pm: ends right at closing
        assertFalse(fits(22f, 2, emptyList()))          // would run past 11pm
        assertFalse(fits(9f, 2, emptyList()))           // before opening
    }

    @Test
    fun aSessionCantOverlapABookingButCanTouchIt() {
        val busy = listOf(BusyBlock(day, 12f, 14f))
        assertFalse(fits(13f, 2, busy))                 // overlaps
        assertFalse(fits(11f, 2, busy))                 // 11 to 1 overlaps 12 to 2
        assertTrue(fits(10f, 2, busy))                  // 10 to 12, ends as the booking starts
        assertTrue(fits(14f, 2, busy))                  // 2 to 4, starts as the booking ends
    }

    @Test
    fun hybridLoopSetupNeedsTimeBeforeTheSession() {
        val busy = listOf(BusyBlock(day, 12f, 14f))
        assertTrue(fits(14f, 2, busy, prep = 0f))
        assertFalse(fits(14f, 2, busy, prep = 0.5f))    // setup from 1:30 clashes with the booking
        assertFalse(fits(10f, 2, emptyList(), prep = 0.5f))   // setup would start before opening
    }

    @Test
    fun startTimesAndOpennessFollowTheBookings() {
        val busy = listOf(BusyBlock(day, 12f, 14f))
        assertEquals(listOf(10) + (14..21).toList(), startTimes(2, busy))
        assertEquals(0f, openness(2, listOf(BusyBlock(day, 10f, 23f))), 0f)   // fully booked
        assertEquals(1f, openness(2, emptyList()), 0f)                         // wide open
    }

    @Test
    fun longerSessionsHaveFewerStartTimes() {
        val busy = listOf(BusyBlock(day, 15f, 16f))
        assertTrue(startTimes(4, busy).size < startTimes(1, busy).size)
    }

    @Test
    fun nearestStartPrefersTheSameOrLaterTime() {
        assertEquals(14, nearestStart(14, listOf(10, 14, 15)))
        assertEquals(15, nearestStart(14, listOf(13, 15)))    // tie: the later one
        assertEquals(10, nearestStart(14, listOf(10)))
        assertNull(nearestStart(14, emptyList()))
    }

    @Test
    fun googleTimesAreReadInLondonTime() {
        // 1pm to 3pm UTC on 6 October is 2pm to 4pm in London (summer time)
        val blocks = splitIntoDays(parseCalendarTime("2026-10-06T13:00:00Z"), parseCalendarTime("2026-10-06T15:00:00Z"))
        assertEquals(listOf(BusyBlock(day, 14f, 16f)), blocks)
    }

    @Test
    fun aLateSessionIsSplitAcrossMidnight() {
        val blocks = splitIntoDays(parseCalendarTime("2026-10-06T22:00:00+01:00"), parseCalendarTime("2026-10-07T02:00:00+01:00"))
        assertEquals(listOf(BusyBlock(day, 22f, 24f), BusyBlock(Day(2026, 9, 7), 0f, 2f)), blocks)
    }

    @Test
    fun daysKnowTheirWeekdayAndMonth() {
        assertEquals(1, day.weekdayMondayFirst)          // Tuesday
        assertEquals(31, day.daysInMonth)
        assertEquals(Day(2026, 10, 1), day.plusMonths(1))
        assertEquals("Tue 6 Oct", day.label())
    }

    @Test
    fun aNewBookingAndACancellationAreSpotted() {
        val kept = BusyBlock(day, 10f, 12f)
        val gone = BusyBlock(day, 14f, 16f)
        val new = BusyBlock(day, 18f, 21f)
        val changes = diffBookings(listOf(kept, gone), listOf(kept, new))
        assertEquals(listOf(new), changes.booked)
        assertEquals(listOf(gone), changes.cancelled)
        assertTrue(changes.moved.isEmpty())
        assertTrue(diffBookings(listOf(kept), listOf(kept)).isEmpty)
    }

    @Test
    fun aBookingRightAfterAnotherIsJustTheNewTime() {
        // Google joins back-to-back bookings into one block, so 2–4pm becomes 2–6pm when 4–6pm is booked
        val changes = diffBookings(listOf(BusyBlock(day, 14f, 16f)), listOf(BusyBlock(day, 14f, 18f)))
        assertEquals(listOf(BusyBlock(day, 16f, 18f)), changes.booked)
        assertTrue(changes.cancelled.isEmpty())
        // ...and cancelling it again frees just that time
        val back = diffBookings(listOf(BusyBlock(day, 14f, 18f)), listOf(BusyBlock(day, 14f, 16f)))
        assertEquals(listOf(BusyBlock(day, 16f, 18f)), back.cancelled)
        assertTrue(back.booked.isEmpty())
    }

    @Test
    fun aRescheduleIsSeenAsOneMove() {
        val thursday = day.plusDays(2)
        val moved = diffBookings(listOf(BusyBlock(day, 14f, 16f)), listOf(BusyBlock(thursday, 18f, 20f)))
        assertEquals(listOf(Moved(BusyBlock(day, 14f, 16f), BusyBlock(thursday, 18f, 20f))), moved.moved)
        assertTrue(moved.booked.isEmpty() && moved.cancelled.isEmpty())
        // Moved an hour later, overlapping itself: still the whole booking
        val nudged = diffBookings(listOf(BusyBlock(day, 14f, 16f)), listOf(BusyBlock(day, 15f, 17f)))
        assertEquals(listOf(Moved(BusyBlock(day, 14f, 16f), BusyBlock(day, 15f, 17f))), nudged.moved)
    }

    @Test
    fun changesAreDescribedForTheBanner() {
        assertEquals("Just booked · Tue 6 Oct, 2pm–4pm", describeChanges(BookingChanges(listOf(BusyBlock(day, 14f, 16f)), emptyList())))
        assertEquals("Cancelled · Tue 6 Oct, 6pm–8pm", describeChanges(BookingChanges(emptyList(), listOf(BusyBlock(day, 18f, 20f)))))
        assertEquals("Rescheduled · Tue 6 Oct, 2pm → 3pm", describeChanges(BookingChanges(emptyList(), emptyList(),
            listOf(Moved(BusyBlock(day, 14f, 16f), BusyBlock(day, 15f, 17f))))))
        assertEquals("Rescheduled · Tue 6 Oct → Thu 8 Oct, 6pm–8pm", describeChanges(BookingChanges(emptyList(), emptyList(),
            listOf(Moved(BusyBlock(day, 14f, 16f), BusyBlock(day.plusDays(2), 18f, 20f))))))
        assertEquals("2 booked · 1 cancelled", describeChanges(BookingChanges(
            listOf(BusyBlock(day, 10f, 11f), BusyBlock(day, 12f, 13f)), listOf(BusyBlock(day, 18f, 20f))
        )))
    }

    @Test
    fun theWorkerWinsOverAnOlderCopyOfGoogleCalendar() {
        // The app's copy of Google still shows a booking the worker says was cancelled (2–4pm), hasn't got a new one
        // yet (6–8pm), and has a studio event the worker's answer doesn't include (11am–12pm)
        val google = listOf(BusyBlock(day, 11f, 12f), BusyBlock(day, 14f, 16f))
        val studio = StudioBookings(busy = listOf(BusyBlock(day, 18f, 20f)), cancelled = listOf(BusyBlock(day, 14f, 16f)))
        assertEquals(listOf(BusyBlock(day, 11f, 12f), BusyBlock(day, 18f, 20f)), combineCalendars(studio, google))
    }

    @Test
    fun aNewBookingWhereOneWasCancelledStaysBusy() {
        val studio = StudioBookings(busy = listOf(BusyBlock(day, 14f, 16f)), cancelled = listOf(BusyBlock(day, 14f, 16f)))
        assertEquals(listOf(BusyBlock(day, 14f, 16f)), combineCalendars(studio, listOf(BusyBlock(day, 14f, 16f))))
    }

    @Test
    fun datesAreSentToTheWorkerAsYearMonthDay() {
        assertEquals("2026-10-06", day.iso())
        assertEquals("2027-01-01", Day(2027, 0, 1).iso())
    }

    @Test
    fun hoursAreWrittenTheWayPeopleSayThem() {
        assertEquals("2pm", hourLabel(14f))
        assertEquals("12pm", hourLabel(12f))
        assertEquals("10:30am", hourLabel(10.5f))
        assertEquals("11pm", hourLabel(23f))
    }
}
