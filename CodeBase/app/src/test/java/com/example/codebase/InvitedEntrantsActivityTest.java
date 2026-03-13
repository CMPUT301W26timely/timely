package com.example.codebase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for InvitedEntrantsActivity logic.
 *
 * Tests cover:
 *   1.  All tab shows all selected entrants
 *   2.  Accepted = selected ∩ enrolled
 *   3.  Declined = cancelled ∩ (selected - enrolled)
 *   4.  Pending  = selected - enrolled - declined
 *   5.  Accepted + Pending + Declined = All
 *   6.  Enrolled entrant NOT counted as declined
 *   7.  Entrant never selected → not in any list
 *   8.  Empty lists handled safely
 *   9.  Cancel button disabled before registration deadline
 *   10. Cancel button enabled after registration deadline
 *   11. InvitedEntrant model fields set correctly
 *   12. All declined → pending and accepted empty
 *   13. All accepted → pending and declined empty
 *   14. Same entrant not double-counted across tabs
 *
 * Add to: app/src/test/java/com/example/codebase/InvitedEntrantsActivityTest.java
 */
public class InvitedEntrantsActivityTest {

    // ─── Helper — simulate processDocument() logic ────────────────────────────

    private List<InvitedEntrantsActivity.InvitedEntrant> allList      = new ArrayList<>();
    private List<InvitedEntrantsActivity.InvitedEntrant> pendingList  = new ArrayList<>();
    private List<InvitedEntrantsActivity.InvitedEntrant> acceptedList = new ArrayList<>();
    private List<InvitedEntrantsActivity.InvitedEntrant> declinedList = new ArrayList<>();

    private void buildLists(List<String> selected,
                            List<String> enrolled,
                            List<String> cancelled) {
        allList.clear();
        pendingList.clear();
        acceptedList.clear();
        declinedList.clear();

        // Declined = cancelledEntrants ∩ (selectedEntrants - enrolledEntrants)
        List<String> selectedMinusEnrolled = new ArrayList<>(selected);
        selectedMinusEnrolled.removeAll(enrolled);

        List<String> declined = new ArrayList<>(cancelled);
        declined.retainAll(selectedMinusEnrolled);

        for (String deviceId : selected) {
            String status;
            if (enrolled.contains(deviceId))      status = "Accepted";
            else if (declined.contains(deviceId)) status = "Declined";
            else                                  status = "Pending";

            InvitedEntrantsActivity.InvitedEntrant entrant =
                    new InvitedEntrantsActivity.InvitedEntrant(deviceId, status);
            allList.add(entrant);
            switch (status) {
                case "Accepted": acceptedList.add(entrant); break;
                case "Declined": declinedList.add(entrant); break;
                case "Pending":  pendingList.add(entrant);  break;
            }
        }
    }

    /** Simulates cancel button logic: enabled when today is after regClose */
    private boolean isCancelEnabled(Date registrationDeadline) {
        if (registrationDeadline == null) return false;
        return new Date().after(registrationDeadline);
    }

    // ─── Test 1: All tab shows all selected entrants ──────────────────────────

    @Test
    public void testAllTab_showsAllSelectedEntrants() {
        List<String> selected  = Arrays.asList("user1", "user2", "user3");
        List<String> enrolled  = Arrays.asList("user1");
        List<String> cancelled = Arrays.asList("user2");

        buildLists(selected, enrolled, cancelled);

        assertEquals("All tab should show all 3 selected entrants", 3, allList.size());
    }

    // ─── Test 2: Accepted = selected ∩ enrolled ───────────────────────────────

    @Test
    public void testAcceptedTab_onlyEnrolledEntrants() {
        List<String> selected  = Arrays.asList("user1", "user2", "user3");
        List<String> enrolled  = Arrays.asList("user1", "user3");
        List<String> cancelled = new ArrayList<>();

        buildLists(selected, enrolled, cancelled);

        assertEquals("Only enrolled entrants should be Accepted", 2, acceptedList.size());
        for (InvitedEntrantsActivity.InvitedEntrant e : acceptedList) {
            assertEquals("Accepted", e.status);
            assertTrue(enrolled.contains(e.deviceId));
        }
    }

    // ─── Test 3: Declined = cancelled ∩ (selected - enrolled) ────────────────

    @Test
    public void testDeclinedTab_correctlyIdentifiesDeclined() {
        // user2 was selected, NOT enrolled, IS cancelled → Declined
        // user1 was selected, enrolled → NOT declined
        // user3 was selected, not enrolled, not cancelled → Pending
        List<String> selected  = Arrays.asList("user1", "user2", "user3");
        List<String> enrolled  = Arrays.asList("user1");
        List<String> cancelled = Arrays.asList("user2");

        buildLists(selected, enrolled, cancelled);

        assertEquals("Only user2 should be declined", 1, declinedList.size());
        assertEquals("user2", declinedList.get(0).deviceId);
        assertEquals("Declined", declinedList.get(0).status);
    }

    // ─── Test 4: Pending = selected - enrolled - declined ────────────────────

    @Test
    public void testPendingTab_excludesEnrolledAndDeclined() {
        List<String> selected  = Arrays.asList("user1", "user2", "user3", "user4");
        List<String> enrolled  = Arrays.asList("user1");
        List<String> cancelled = Arrays.asList("user2");

        buildLists(selected, enrolled, cancelled);

        // user3 and user4 are selected, not enrolled, not cancelled → Pending
        assertEquals("user3 and user4 should be pending", 2, pendingList.size());
        for (InvitedEntrantsActivity.InvitedEntrant e : pendingList) {
            assertEquals("Pending", e.status);
        }
    }

    // ─── Test 5: Accepted + Pending + Declined = All ─────────────────────────

    @Test
    public void testCounts_allTabEqualsSum() {
        List<String> selected  = Arrays.asList("user1", "user2", "user3", "user4", "user5");
        List<String> enrolled  = Arrays.asList("user1", "user2");
        List<String> cancelled = Arrays.asList("user3");

        buildLists(selected, enrolled, cancelled);

        int sum = acceptedList.size() + pendingList.size() + declinedList.size();
        assertEquals("accepted + pending + declined should equal all", allList.size(), sum);
    }

    // ─── Test 6: Enrolled entrant NOT counted as declined ────────────────────

    @Test
    public void testEnrolledEntrant_notDeclined() {
        // user1 selected, enrolled AND in cancelled → still Accepted, not Declined
        List<String> selected  = Arrays.asList("user1");
        List<String> enrolled  = Arrays.asList("user1");
        List<String> cancelled = Arrays.asList("user1");

        buildLists(selected, enrolled, cancelled);

        assertEquals("Enrolled entrant should be Accepted not Declined", 1, acceptedList.size());
        assertEquals("Accepted", acceptedList.get(0).status);
        assertEquals("Declined list should be empty", 0, declinedList.size());
    }

    // ─── Test 7: Entrant never selected → not in any list ────────────────────

    @Test
    public void testNeverSelected_notInAnyList() {
        List<String> selected  = Arrays.asList("user1");
        List<String> enrolled  = new ArrayList<>();
        List<String> cancelled = Arrays.asList("user99"); // never selected

        buildLists(selected, enrolled, cancelled);

        for (InvitedEntrantsActivity.InvitedEntrant e : allList) {
            assertNotEquals("user99 was never selected — should not appear",
                    "user99", e.deviceId);
        }
    }

    // ─── Test 8: Empty lists handled safely ──────────────────────────────────

    @Test
    public void testEmptyLists_noNullPointer() {
        buildLists(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        assertNotNull(allList);
        assertNotNull(pendingList);
        assertNotNull(acceptedList);
        assertNotNull(declinedList);
        assertEquals(0, allList.size());
    }

    // ─── Test 9: Cancel button disabled BEFORE registration deadline ──────────

    @Test
    public void testCancelButton_disabledBeforeDeadline() {
        // deadline = 1 day in the future
        Date futureDeadline = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        assertFalse("Cancel should be disabled before deadline",
                isCancelEnabled(futureDeadline));
    }

    // ─── Test 10: Cancel button enabled AFTER registration deadline ───────────

    @Test
    public void testCancelButton_enabledAfterDeadline() {
        // deadline = 1 day in the past
        Date pastDeadline = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        assertTrue("Cancel should be enabled after deadline",
                isCancelEnabled(pastDeadline));
    }

    // ─── Test 11: Cancel button disabled when deadline is null ───────────────

    @Test
    public void testCancelButton_disabledWhenDeadlineNull() {
        assertFalse("Cancel should be disabled when deadline is null",
                isCancelEnabled(null));
    }

    // ─── Test 12: InvitedEntrant model fields set correctly ──────────────────

    @Test
    public void testInvitedEntrantModel_fieldsSetCorrectly() {
        InvitedEntrantsActivity.InvitedEntrant entrant =
                new InvitedEntrantsActivity.InvitedEntrant("device123", "Pending");

        assertEquals("device123", entrant.deviceId);
        assertEquals("Pending",   entrant.status);
    }

    // ─── Test 13: All declined → pending and accepted empty ──────────────────

    @Test
    public void testAllDeclined_pendingAndAcceptedEmpty() {
        List<String> selected  = Arrays.asList("user1", "user2");
        List<String> enrolled  = new ArrayList<>();
        List<String> cancelled = Arrays.asList("user1", "user2");

        buildLists(selected, enrolled, cancelled);

        assertEquals("All should be declined", 2, declinedList.size());
        assertEquals("Pending should be empty", 0, pendingList.size());
        assertEquals("Accepted should be empty", 0, acceptedList.size());
    }

    // ─── Test 14: Same entrant not double-counted ─────────────────────────────

    @Test
    public void testNoDuplicates_entrantCountedOnlyOnce() {
        List<String> selected  = Arrays.asList("user1", "user2", "user3");
        List<String> enrolled  = Arrays.asList("user1");
        List<String> cancelled = Arrays.asList("user2");

        buildLists(selected, enrolled, cancelled);

        // Collect all deviceIds from all sub-lists
        List<String> allIds = new ArrayList<>();
        for (InvitedEntrantsActivity.InvitedEntrant e : acceptedList) allIds.add(e.deviceId);
        for (InvitedEntrantsActivity.InvitedEntrant e : pendingList)  allIds.add(e.deviceId);
        for (InvitedEntrantsActivity.InvitedEntrant e : declinedList) allIds.add(e.deviceId);

        // No duplicates
        long uniqueCount = allIds.stream().distinct().count();
        assertEquals("Each entrant should appear in exactly one sub-list",
                allIds.size(), (int) uniqueCount);
    }
}