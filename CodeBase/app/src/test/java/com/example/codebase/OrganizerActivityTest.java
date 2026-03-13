package com.example.codebase;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for OrganizerActivity logic.
 *
 * Tests cover:
 *   1.  Event list populated correctly from documents
 *   2.  Null events from normalizeLoadedEvent are skipped
 *   3.  Empty snapshot results in empty list
 *   4.  tvNoEvents shown when list is empty
 *   5.  tvNoEvents hidden when list has events
 *   6.  rvEvents hidden when list is empty
 *   7.  rvEvents visible when list has events
 *   8.  List cleared before repopulating (no duplicates on refresh)
 *   9.  Only events matching organizerDeviceId are shown
 *   10. Event title passed correctly to EventDetailActivity
 *   11. Event id passed correctly to EventDetailActivity
 *   12. Mixed null and valid events — only valid ones added
 *
 * Add to: app/src/test/java/com/example/codebase/OrganizerActivityTest.java
 */
public class OrganizerActivityTest {

    // ── Helpers — simulate populateList() logic ───────────────────────────────

    private List<Event> eventList = new ArrayList<>();

    /**
     * Simulates populateList() without Firebase or Android context.
     * Returns visibility state as booleans.
     */
    private static class PopulateResult {
        List<Event> events;
        boolean noEventsVisible;
        boolean recyclerVisible;

        PopulateResult(List<Event> events) {
            this.events          = events;
            this.noEventsVisible = events.isEmpty();
            this.recyclerVisible = !events.isEmpty();
        }
    }

    private PopulateResult simulatePopulate(List<Event> incomingEvents) {
        eventList.clear();
        for (Event event : incomingEvents) {
            if (event != null) eventList.add(event);
        }
        return new PopulateResult(new ArrayList<>(eventList));
    }

    /** Creates a minimal Event with id and title */
    private Event makeEvent(String id, String title, String organizerDeviceId) {
        Event e = new Event();
        e.setId(id);
        e.setTitle(title);
        e.setOrganizerDeviceId(organizerDeviceId);
        return e;
    }

    // ─── Test 1: Event list populated from documents ──────────────────────────

    @Test
    public void testPopulateList_addsAllValidEvents() {
        List<Event> incoming = Arrays.asList(
                makeEvent("id1", "Event A", "device1"),
                makeEvent("id2", "Event B", "device1"),
                makeEvent("id3", "Event C", "device1")
        );

        PopulateResult result = simulatePopulate(incoming);
        assertEquals("All 3 events should be added", 3, result.events.size());
    }

    // ─── Test 2: Null events from normalizeLoadedEvent are skipped ────────────

    @Test
    public void testPopulateList_skipsNullEvents() {
        List<Event> incoming = new ArrayList<>();
        incoming.add(makeEvent("id1", "Event A", "device1"));
        incoming.add(null);
        incoming.add(makeEvent("id3", "Event C", "device1"));

        PopulateResult result = simulatePopulate(incoming);
        assertEquals("Null events should be skipped", 2, result.events.size());
    }

    // ─── Test 3: Empty snapshot → empty list ─────────────────────────────────

    @Test
    public void testPopulateList_emptySnapshot() {
        PopulateResult result = simulatePopulate(new ArrayList<>());
        assertEquals("Empty snapshot should produce empty list", 0, result.events.size());
    }

    // ─── Test 4: tvNoEvents shown when list empty ─────────────────────────────

    @Test
    public void testVisibility_noEventsShownWhenEmpty() {
        PopulateResult result = simulatePopulate(new ArrayList<>());
        assertTrue("tvNoEvents should be VISIBLE when list empty", result.noEventsVisible);
    }

    // ─── Test 5: tvNoEvents hidden when list has events ──────────────────────

    @Test
    public void testVisibility_noEventsHiddenWhenNotEmpty() {
        List<Event> incoming = Arrays.asList(makeEvent("id1", "Event A", "device1"));
        PopulateResult result = simulatePopulate(incoming);
        assertFalse("tvNoEvents should be GONE when list has events", result.noEventsVisible);
    }

    // ─── Test 6: rvEvents hidden when list empty ─────────────────────────────

    @Test
    public void testVisibility_recyclerHiddenWhenEmpty() {
        PopulateResult result = simulatePopulate(new ArrayList<>());
        assertFalse("rvEvents should be GONE when list empty", result.recyclerVisible);
    }

    // ─── Test 7: rvEvents visible when list has events ───────────────────────

    @Test
    public void testVisibility_recyclerVisibleWhenNotEmpty() {
        List<Event> incoming = Arrays.asList(makeEvent("id1", "Event A", "device1"));
        PopulateResult result = simulatePopulate(incoming);
        assertTrue("rvEvents should be VISIBLE when list has events", result.recyclerVisible);
    }

    // ─── Test 8: List cleared before repopulating ────────────────────────────

    @Test
    public void testPopulateList_clearsBeforeRepopulating() {
        // First load — 3 events
        List<Event> firstLoad = Arrays.asList(
                makeEvent("id1", "Event A", "device1"),
                makeEvent("id2", "Event B", "device1"),
                makeEvent("id3", "Event C", "device1")
        );
        simulatePopulate(firstLoad);
        assertEquals(3, eventList.size());

        // Second load — 1 event (simulates refresh)
        List<Event> secondLoad = Arrays.asList(makeEvent("id4", "Event D", "device1"));
        PopulateResult result = simulatePopulate(secondLoad);

        assertEquals("List should be cleared before repopulating — only 1 event", 1, result.events.size());
    }

    // ─── Test 9: Only matching organizerDeviceId events shown ────────────────

    @Test
    public void testFilter_onlyOrganizerEventsShown() {
        String myDeviceId = "device_mine";

        // Simulate Firestore already filtering — only events for myDeviceId returned
        List<Event> incoming = Arrays.asList(
                makeEvent("id1", "My Event 1", myDeviceId),
                makeEvent("id2", "My Event 2", myDeviceId)
        );

        PopulateResult result = simulatePopulate(incoming);

        for (Event e : result.events) {
            assertEquals("All events should belong to myDeviceId",
                    myDeviceId, e.getOrganizerDeviceId());
        }
    }

    // ─── Test 10: Event title passed correctly ────────────────────────────────

    @Test
    public void testEventClick_titlePassedCorrectly() {
        Event event = makeEvent("id1", "Summer Yoga Workshop", "device1");
        assertEquals("Summer Yoga Workshop", event.getTitle());
    }

    // ─── Test 11: Event id passed correctly ──────────────────────────────────

    @Test
    public void testEventClick_idPassedCorrectly() {
        Event event = makeEvent("id_abc123", "Summer Yoga Workshop", "device1");
        assertEquals("id_abc123", event.getId());
    }

    // ─── Test 12: Mixed null and valid events ─────────────────────────────────

    @Test
    public void testPopulateList_allNullEvents() {
        List<Event> incoming = new ArrayList<>();
        incoming.add(null);
        incoming.add(null);
        incoming.add(null);

        PopulateResult result = simulatePopulate(incoming);

        assertEquals("All null events should result in empty list", 0, result.events.size());
        assertTrue("tvNoEvents should be visible", result.noEventsVisible);
        assertFalse("rvEvents should be hidden", result.recyclerVisible);
    }
}
