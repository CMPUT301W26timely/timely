package com.example.codebase;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for CancelledEntrantsActivity logic.
 *
 * Tests cover:
 *   1. All cancelled entrants shown in ALL tab
 *   2. Declined = cancelled ∩ (selected - enrolled)
 *   3. Cancelled = cancelled - declined
 *   4. Empty lists handled safely
 *   5. Entrant in both selected and enrolled → NOT declined
 *   6. CancelledEntrant model fields set correctly
 *
 * Add to: app/src/test/java/com/example/codebase/CancelledEntrantsActivityTest.java
 * Run with: Right-click → Run 'CancelledEntrantsActivityTest'
 */
public class CancelledEntrantsActivityTest {

    // ─── Helper — simulate the logic from processDocument() ──────────────────

    private static final int TAB_ALL       = 0;
    private static final int TAB_DECLINED  = 1;
    private static final int TAB_CANCELLED = 2;

    private List<CancelledEntrantsActivity.CancelledEntrant> allList;
    private List<CancelledEntrantsActivity.CancelledEntrant> declinedList;
    private List<CancelledEntrantsActivity.CancelledEntrant> cancelledList;

    /**
     * Simulates the logic in CancelledEntrantsActivity.processDocument()
     * so we can test it without Firebase or Android context.
     */
    private void buildLists(List<String> selected,
                            List<String> enrolled,
                            List<String> cancelled) {
        allList       = new ArrayList<>();
        declinedList  = new ArrayList<>();
        cancelledList = new ArrayList<>();

        // Declined = cancelledEntrants ∩ (selectedEntrants - enrolledEntrants)
        List<String> selectedMinusEnrolled = new ArrayList<>(selected);
        selectedMinusEnrolled.removeAll(enrolled);

        List<String> declinedIds = new ArrayList<>(cancelled);
        declinedIds.retainAll(selectedMinusEnrolled);

        for (String deviceId : cancelled) {
            String status = declinedIds.contains(deviceId) ? "Declined" : "Cancelled";
            CancelledEntrantsActivity.CancelledEntrant entrant =
                    new CancelledEntrantsActivity.CancelledEntrant(deviceId, status, null);
            allList.add(entrant);
            if ("Declined".equals(status)) declinedList.add(entrant);
            else                           cancelledList.add(entrant);
        }
    }

    // ─── Test 1: All tab shows all cancelled entrants ─────────────────────────

    @Test
    public void testAllTab_showsAllCancelledEntrants() {
        List<String> selected  = Arrays.asList("user1", "user2", "user3");
        List<String> enrolled  = Arrays.asList("user3");
        List<String> cancelled = Arrays.asList("user1", "user2", "user4");

        buildLists(selected, enrolled, cancelled);

        assertEquals("All tab should show all 3 cancelled entrants", 3, allList.size());
    }

    // ─── Test 2: Declined correctly identified ────────────────────────────────

    @Test
    public void testDeclinedTab_correctlyIdentifiesDeclined() {
        // user1 was selected, not enrolled, and is cancelled → Declined
        // user2 was selected, enrolled → NOT declined even if in cancelled
        // user4 was never selected → Cancelled (not declined)
        List<String> selected  = Arrays.asList("user1", "user2");
        List<String> enrolled  = Arrays.asList("user2");
        List<String> cancelled = Arrays.asList("user1", "user4");

        buildLists(selected, enrolled, cancelled);

        assertEquals("Only user1 should be declined", 1, declinedList.size());
        assertEquals("user1", declinedList.get(0).deviceId);
        assertEquals("Declined", declinedList.get(0).status);
    }

    // ─── Test 3: Cancelled tab = cancelled - declined ─────────────────────────

    @Test
    public void testCancelledTab_excludesDeclined() {
        List<String> selected  = Arrays.asList("user1");
        List<String> enrolled  = new ArrayList<>();
        List<String> cancelled = Arrays.asList("user1", "user4", "user5");

        buildLists(selected, enrolled, cancelled);

        // user1 = declined (selected, not enrolled, cancelled)
        // user4, user5 = cancelled (never selected)
        assertEquals("Cancelled tab should have 2", 2, cancelledList.size());
        for (CancelledEntrantsActivity.CancelledEntrant e : cancelledList) {
            assertNotEquals("Declined should not appear in cancelled tab", "Declined", e.status);
        }
    }

    // ─── Test 4: All lists empty when no cancelled entrants ──────────────────

    @Test
    public void testEmptyLists_noNullPointer() {
        buildLists(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        assertNotNull(allList);
        assertNotNull(declinedList);
        assertNotNull(cancelledList);
        assertEquals(0, allList.size());
        assertEquals(0, declinedList.size());
        assertEquals(0, cancelledList.size());
    }

    // ─── Test 5: Enrolled entrant NOT counted as declined ────────────────────

    @Test
    public void testEnrolledEntrant_notDeclined() {
        // user1 selected AND enrolled AND cancelled → should NOT be declined
        List<String> selected  = Arrays.asList("user1");
        List<String> enrolled  = Arrays.asList("user1");
        List<String> cancelled = Arrays.asList("user1");

        buildLists(selected, enrolled, cancelled);

        assertEquals("user1 is enrolled so not declined", 0, declinedList.size());
        assertEquals("user1 should be in cancelled tab", 1, cancelledList.size());
        assertEquals("Cancelled", cancelledList.get(0).status);
    }

    // ─── Test 6: All + declined + cancelled counts add up ────────────────────

    @Test
    public void testCounts_declinedPlusCancelledEqualsAll() {
        List<String> selected  = Arrays.asList("user1", "user2", "user3");
        List<String> enrolled  = Arrays.asList("user3");
        List<String> cancelled = Arrays.asList("user1", "user2", "user4", "user5");

        buildLists(selected, enrolled, cancelled);

        assertEquals("declined + cancelled should equal all",
                allList.size(),
                declinedList.size() + cancelledList.size());
    }

    // ─── Test 7: CancelledEntrant model fields set correctly ─────────────────

    @Test
    public void testCancelledEntrantModel_fieldsSetCorrectly() {
        Date now = new Date();
        CancelledEntrantsActivity.CancelledEntrant entrant =
                new CancelledEntrantsActivity.CancelledEntrant("device123", "Declined", now);

        assertEquals("device123", entrant.deviceId);
        assertEquals("Declined",  entrant.status);
        assertEquals(now,         entrant.cancelledAt);
    }

    // ─── Test 8: cancelledAt is null when not provided ───────────────────────

    @Test
    public void testCancelledEntrantModel_cancelledAtNullable() {
        CancelledEntrantsActivity.CancelledEntrant entrant =
                new CancelledEntrantsActivity.CancelledEntrant("device456", "Cancelled", null);

        assertNull("cancelledAt should be null when not provided", entrant.cancelledAt);
    }

    // ─── Test 9: Multiple declined entrants ──────────────────────────────────

    @Test
    public void testMultipleDeclined() {
        List<String> selected  = Arrays.asList("user1", "user2", "user3", "user4");
        List<String> enrolled  = Arrays.asList("user4");
        List<String> cancelled = Arrays.asList("user1", "user2", "user3");

        buildLists(selected, enrolled, cancelled);

        assertEquals("All 3 should be declined", 3, declinedList.size());
        assertEquals("None should be in cancelled tab", 0, cancelledList.size());
    }

    // ─── Test 10: Entrant in cancelled but not in selected ───────────────────

    @Test
    public void testNeverSelected_alwaysCancelled() {
        List<String> selected  = Arrays.asList("user1");
        List<String> enrolled  = new ArrayList<>();
        List<String> cancelled = Arrays.asList("user99"); // never selected

        buildLists(selected, enrolled, cancelled);

        assertEquals("Never selected → always Cancelled", 1, cancelledList.size());
        assertEquals("Cancelled", cancelledList.get(0).status);
        assertEquals(0, declinedList.size());
    }
}
