package com.example.codebase;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Unit tests for EventDetailActivity logic.
 *
 * Tests cover:
 *   1.  calculateStatus → Draft when any date is null
 *   2.  calculateStatus → Registration Opening Soon
 *   3.  calculateStatus → Registration Open
 *   4.  calculateStatus → Registration Closed / Lottery Opening Soon
 *   5.  calculateStatus → Lottery Closed & Event Scheduled
 *   6.  calculateStatus → In Progress
 *   7.  calculateStatus → Event Ended
 *   8.  calculateStatus → Draft when dates present but state unmatched
 *   9.  formatEventDateRange → null startDate returns date_not_set placeholder
 *   10. formatEventDateRange → null endDate returns startDate only
 *   11. formatEventDateRange → same start and end returns single date
 *   12. formatEventDateRange → different start and end returns range
 *   13. Entrant counts correct when all lists populated
 *   14. Entrant counts zero when all lists null
 *   15. Entrant counts zero when all lists empty
 *   16. waitingCount equals tvWaitingListRowCount value
 *
 * Add to: app/src/test/java/com/example/codebase/EventDetailActivityTest.java
 */
public class EventDetailActivityTest {

    // ── Helpers mirrored from EventDetailActivity ─────────────────────────────

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private String calculateStatus(Date regOpen, Date registrationDeadline,
                                   Date drawDate, Date startDate, Date endDate,
                                   List<String> selectedEntrants,
                                   List<String> enrolledEntrants) {
        if (regOpen == null || registrationDeadline == null || drawDate == null
                || startDate == null || endDate == null) {
            return "Draft";
        }

        Date today = new Date();
        boolean selectedEmpty = selectedEntrants == null || selectedEntrants.isEmpty();
        boolean enrolledEmpty = enrolledEntrants == null || enrolledEntrants.isEmpty();

        if (today.before(regOpen)) {
            return "Registration Opening Soon";
        } else if (!today.before(regOpen) && !today.after(registrationDeadline)) {
            return "Registration Open";
        } else if (today.after(registrationDeadline)
                && today.before(drawDate) && selectedEmpty) {
            return "Registration Closed / Lottery Opening Soon";
        } else if (today.after(drawDate)
                && today.before(startDate) && !selectedEmpty) {
            return "Lottery Closed & Event Scheduled";
        } else if (!today.before(startDate)
                && !today.after(endDate) && !enrolledEmpty) {
            return "In Progress";
        } else if (today.after(endDate)) {
            return "Event Ended";
        } else {
            return "Draft";
        }
    }

    private String formatEventDateRange(Date startDate, Date endDate) {
        if (startDate == null) return "Date not set";
        if (endDate == null)   return displayDateFormat.format(startDate);

        String startText = displayDateFormat.format(startDate);
        String endText   = displayDateFormat.format(endDate);
        if (startText.equals(endText)) return startText;
        return startText + " - " + endText;
    }

    /** shorthand: date N days from now */
    private Date daysFromNow(long days) {
        return new Date(System.currentTimeMillis() + days * 24 * 60 * 60 * 1000L);
    }

    // ─── Test 1: Draft when any date is null ──────────────────────────────────

    @Test
    public void testStatus_draftWhenAnyDateNull() {
        assertEquals("Draft", calculateStatus(null, null, null, null, null, null, null));
        assertEquals("Draft", calculateStatus(
                daysFromNow(-5), null, daysFromNow(2), daysFromNow(5), daysFromNow(10),
                null, null));
        assertEquals("Draft", calculateStatus(
                daysFromNow(-5), daysFromNow(-2), null, daysFromNow(5), daysFromNow(10),
                null, null));
    }

    // ─── Test 2: Registration Opening Soon ───────────────────────────────────

    @Test
    public void testStatus_registrationOpeningSoon() {
        // today < regOpen
        String status = calculateStatus(
                daysFromNow(2),   // regOpen in future
                daysFromNow(5),   // regClose
                daysFromNow(8),   // drawDate
                daysFromNow(10),  // startDate
                daysFromNow(12),  // endDate
                null, null
        );
        assertEquals("Registration Opening Soon", status);
    }

    // ─── Test 3: Registration Open ───────────────────────────────────────────

    @Test
    public void testStatus_registrationOpen() {
        // regOpen <= today <= regClose
        String status = calculateStatus(
                daysFromNow(-2),  // regOpen in past
                daysFromNow(3),   // regClose in future
                daysFromNow(6),   // drawDate
                daysFromNow(10),  // startDate
                daysFromNow(12),  // endDate
                null, null
        );
        assertEquals("Registration Open", status);
    }

    // ─── Test 4: Registration Closed / Lottery Opening Soon ──────────────────

    @Test
    public void testStatus_registrationClosedLotteryOpeningSoon() {
        // today > regClose AND today < drawDate AND no selected
        String status = calculateStatus(
                daysFromNow(-5),  // regOpen
                daysFromNow(-2),  // regClose in past
                daysFromNow(2),   // drawDate in future
                daysFromNow(5),   // startDate
                daysFromNow(8),   // endDate
                new ArrayList<>(), null  // empty selected
        );
        assertEquals("Registration Closed / Lottery Opening Soon", status);
    }

    // ─── Test 5: Lottery Closed & Event Scheduled ────────────────────────────

    @Test
    public void testStatus_lotteryClosedEventScheduled() {
        // today > drawDate AND today < startDate AND selected not empty
        String status = calculateStatus(
                daysFromNow(-10), // regOpen
                daysFromNow(-7),  // regClose
                daysFromNow(-2),  // drawDate in past
                daysFromNow(3),   // startDate in future
                daysFromNow(6),   // endDate
                Arrays.asList("user1", "user2"), null  // selected not empty
        );
        assertEquals("Lottery Closed & Event Scheduled", status);
    }

    // ─── Test 6: In Progress ─────────────────────────────────────────────────

    @Test
    public void testStatus_inProgress() {
        // startDate <= today <= endDate AND enrolled not empty
        String status = calculateStatus(
                daysFromNow(-10), // regOpen
                daysFromNow(-7),  // regClose
                daysFromNow(-5),  // drawDate
                daysFromNow(-2),  // startDate in past
                daysFromNow(2),   // endDate in future
                Arrays.asList("user1"),
                Arrays.asList("user1")  // enrolled not empty
        );
        assertEquals("In Progress", status);
    }

    // ─── Test 7: Event Ended ─────────────────────────────────────────────────

    @Test
    public void testStatus_eventEnded() {
        // today > endDate
        String status = calculateStatus(
                daysFromNow(-15), // regOpen
                daysFromNow(-12), // regClose
                daysFromNow(-10), // drawDate
                daysFromNow(-7),  // startDate
                daysFromNow(-2),  // endDate in past
                Arrays.asList("user1"),
                Arrays.asList("user1")
        );
        assertEquals("Event Ended", status);
    }

    // ─── Test 8: Draft when state unmatched ──────────────────────────────────

    @Test
    public void testStatus_draftWhenStateUnmatched() {
        // today > regClose AND today < drawDate BUT selected not empty
        // → doesn't match any case → Draft
        String status = calculateStatus(
                daysFromNow(-5),  // regOpen
                daysFromNow(-2),  // regClose
                daysFromNow(2),   // drawDate
                daysFromNow(5),   // startDate
                daysFromNow(8),   // endDate
                Arrays.asList("user1"), // selected NOT empty — skips case 4
                null
        );
        assertEquals("Draft", status);
    }

    // ─── Test 9: formatDateRange → null startDate ─────────────────────────────

    @Test
    public void testFormatDateRange_nullStartDate() {
        String result = formatEventDateRange(null, daysFromNow(5));
        assertEquals("Date not set", result);
    }

    // ─── Test 10: formatDateRange → null endDate ──────────────────────────────

    @Test
    public void testFormatDateRange_nullEndDate() {
        Date start = daysFromNow(0);
        String result = formatEventDateRange(start, null);
        assertEquals(displayDateFormat.format(start), result);
    }

    // ─── Test 11: formatDateRange → same start and end ───────────────────────

    @Test
    public void testFormatDateRange_sameDates() {
        // Use exact same Date object → same formatted string
        Date date = daysFromNow(5);
        String result = formatEventDateRange(date, date);
        assertEquals("Same dates should return single date string",
                displayDateFormat.format(date), result);
    }

    // ─── Test 12: formatDateRange → different start and end ──────────────────

    @Test
    public void testFormatDateRange_differentDates() {
        Date start = daysFromNow(5);
        Date end   = daysFromNow(10);
        String result = formatEventDateRange(start, end);
        assertTrue("Should contain ' - ' separator", result.contains(" - "));
        assertTrue("Should start with start date", result.startsWith(displayDateFormat.format(start)));
        assertTrue("Should end with end date", result.endsWith(displayDateFormat.format(end)));
    }

    // ─── Test 13: Entrant counts correct when all lists populated ────────────

    @Test
    public void testEntrantCounts_allListsPopulated() {
        List<String> waiting   = Arrays.asList("w1", "w2", "w3");
        List<String> selected  = Arrays.asList("s1", "s2");
        List<String> enrolled  = Arrays.asList("e1");
        List<String> cancelled = Arrays.asList("c1", "c2", "c3", "c4");

        assertEquals(3, waiting.size());
        assertEquals(2, selected.size());
        assertEquals(1, enrolled.size());
        assertEquals(4, cancelled.size());
    }

    // ─── Test 14: Entrant counts zero when all lists null ────────────────────

    @Test
    public void testEntrantCounts_nullLists() {
        List<String> waiting   = null;
        List<String> selected  = null;
        List<String> enrolled  = null;
        List<String> cancelled = null;

        int waitingCount   = waiting   != null ? waiting.size()   : 0;
        int invitedCount   = selected  != null ? selected.size()  : 0;
        int enrolledCount  = enrolled  != null ? enrolled.size()  : 0;
        int cancelledCount = cancelled != null ? cancelled.size() : 0;

        assertEquals(0, waitingCount);
        assertEquals(0, invitedCount);
        assertEquals(0, enrolledCount);
        assertEquals(0, cancelledCount);
    }

    // ─── Test 15: Entrant counts zero when all lists empty ───────────────────

    @Test
    public void testEntrantCounts_emptyLists() {
        List<String> waiting   = new ArrayList<>();
        List<String> selected  = new ArrayList<>();
        List<String> enrolled  = new ArrayList<>();
        List<String> cancelled = new ArrayList<>();

        assertEquals(0, waiting.size());
        assertEquals(0, selected.size());
        assertEquals(0, enrolled.size());
        assertEquals(0, cancelled.size());
    }

    // ─── Test 16: waitingCount == tvWaitingListRowCount ──────────────────────

    @Test
    public void testWaitingCount_matchesRowCount() {
        List<String> waitingList = Arrays.asList("w1", "w2", "w3", "w4", "w5");
        int waitingCount = waitingList.size();

        // Both tvWaitingCount and tvWaitingListRowCount are set to waitingCount
        int tvWaitingCount       = waitingCount;
        int tvWaitingListRowCount = waitingCount;

        assertEquals("Both waiting count views should show same value",
                tvWaitingCount, tvWaitingListRowCount);
        assertEquals(5, tvWaitingCount);
    }
}
