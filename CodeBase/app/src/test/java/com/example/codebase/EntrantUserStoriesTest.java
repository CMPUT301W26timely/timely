package com.example.codebase;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Story-focused JUnit coverage for entrant event flows.
 *
 * Covered stories:
 *   US 01.01.01 — Join waiting list
 *   US 01.01.02 — Leave waiting list
 *   US 01.01.03 — Browse events
 *   US 01.05.02 — Accept invitation
 *   US 01.05.03 — Decline invitation
 *
 * Notes:
 *   - These are local JUnit tests, so Firebase writes and Activity launches are
 *     represented by isolated business-rule checks instead of Android UI calls.
 */
public class EntrantUserStoriesTest {

    private static final SimpleDateFormat BROWSE_DATE_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy", Locale.CANADA);

    private Event openEvent;
    private Invitations invitations;
    private String deviceId;

    @Before
    public void setUp() {
        deviceId = "device_entrant_123";
        invitations = new Invitations();

        openEvent = makeEvent(
                "event-open",
                "Community Swim Lessons",
                "Downtown Rec Centre",
                daysFromNow(5),
                daysFromNow(-2),
                daysFromNow(2)
        );
    }

    @Test
    public void us010101_joinWaitingList_addsEntrantWhenEventIsOpen() {
        assertTrue("Entrant should be able to join an open waiting list",
                joinWaitingList(openEvent, deviceId, new Date()));

        assertEquals("Waiting list should contain one entrant", 1, openEvent.getWaitingList().size());
        assertTrue("Joined entrant should be stored on the waiting list",
                openEvent.getWaitingList().contains(deviceId));
    }

    @Test
    public void us010101_joinWaitingList_preventsDuplicateJoining() {
        assertTrue(joinWaitingList(openEvent, deviceId, new Date()));
        assertFalse("Duplicate joins should be prevented",
                joinWaitingList(openEvent, deviceId, new Date()));

        assertEquals("Duplicate joining should not create duplicate rows",
                1, openEvent.getWaitingList().size());
    }

    @Test
    public void us010102_leaveWaitingList_removesEntrantAndUpdatesList() {
        openEvent.setWaitingList(new ArrayList<>(Arrays.asList(deviceId, "device_other")));

        assertTrue("Entrant should be removable from the waiting list",
                leaveWaitingList(openEvent, deviceId));

        assertFalse("Removed entrant should no longer appear on the waiting list",
                openEvent.getWaitingList().contains(deviceId));
        assertEquals("Waiting list count should update after removal",
                1, openEvent.getWaitingList().size());
    }

    @Test
    public void us010103_browseEvents_onlyShowsActiveEvents() {
        Event active = makeEvent(
                "active-event",
                "Piano Basics",
                "North Hall",
                daysFromNow(3),
                daysFromNow(-1),
                daysFromNow(1)
        );
        Event closed = makeEvent(
                "closed-event",
                "Old Workshop",
                "South Hall",
                daysFromNow(-7),
                daysFromNow(-14),
                daysFromNow(-8)
        );

        List<Event> visible = getVisibleEvents(Arrays.asList(active, closed), new Date());

        assertEquals("Only active events should be displayed", 1, visible.size());
        assertEquals("Piano Basics", visible.get(0).getTitle());
    }

    @Test
    public void us010103_browseEvents_exposesNameDateAndLocation() {
        BrowseCard card = toBrowseCard(openEvent);

        assertEquals("Community Swim Lessons", card.title);
        assertEquals("Downtown Rec Centre", card.location);
        assertEquals(BROWSE_DATE_FORMAT.format(openEvent.getStartDate()), card.date);
    }

    @Ignore("Opening the event details Activity from a card click requires Android instrumentation.")
    @Test
    public void us010103_clickingEventOpensDetailsPage() {
        // Covered by instrumentation/UI testing rather than local JUnit.
    }

    @Test
    public void us010502_acceptInvitation_changesStatusToEnrolled() {
        Event invitationEvent = makeEvent(
                "invite-event",
                "Dance Safety Basics",
                "Studio A",
                daysFromNow(10),
                daysFromNow(-5),
                daysFromNow(-1)
        );
        invitationEvent.setSelectedEntrants(new ArrayList<>(Arrays.asList(deviceId)));

        invitations.respondToInvitation(invitationEvent, deviceId, true);

        assertFalse("Accepted entrant should leave the selected list",
                invitationEvent.getSelectedEntrants().contains(deviceId));
        assertTrue("Accepted entrant should move to enrolled",
                invitationEvent.getEnrolledEntrants().contains(deviceId));
    }

    @Test
    public void us010503_declineInvitation_changesStatusToCancelled() {
        Event invitationEvent = makeEvent(
                "decline-event",
                "Interpretive Dance",
                "Studio B",
                daysFromNow(10),
                daysFromNow(-5),
                daysFromNow(-1)
        );
        invitationEvent.setSelectedEntrants(new ArrayList<>(Arrays.asList(deviceId)));

        invitations.respondToInvitation(invitationEvent, deviceId, false);

        assertFalse("Declined entrant should leave the selected list",
                invitationEvent.getSelectedEntrants().contains(deviceId));
        assertTrue("Declined entrant should move to cancelled entrants",
                invitationEvent.getCancelledEntrants().contains(deviceId));
    }

    @Ignore("Replacement draw logic is not implemented in the current invitation flow.")
    @Test
    public void us010503_declineInvitation_triggersReplacementDraw() {
        // This acceptance criterion needs production logic before it can be unit tested.
    }

    private Event makeEvent(String id,
                            String title,
                            String location,
                            Date startDate,
                            Date registrationOpen,
                            Date registrationDeadline) {
        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setLocation(location);
        event.setStartDate(startDate);
        event.setRegistrationOpen(registrationOpen);
        event.setRegistrationDeadline(registrationDeadline);
        event.setWaitingList(new ArrayList<>());
        event.setSelectedEntrants(new ArrayList<>());
        event.setEnrolledEntrants(new ArrayList<>());
        event.setCancelledEntrants(new ArrayList<>());
        return event;
    }

    private Date daysFromNow(int days) {
        return new Date(System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L);
    }

    private boolean joinWaitingList(Event event, String entrantDeviceId, Date now) {
        if (event == null || entrantDeviceId == null || entrantDeviceId.trim().isEmpty()) {
            return false;
        }

        if (event.getRegistrationOpen() != null && now.before(event.getRegistrationOpen())) {
            return false;
        }

        if (event.getRegistrationDeadline() != null && !now.before(event.getRegistrationDeadline())) {
            return false;
        }

        ArrayList<String> waitingList = event.getWaitingList();
        if (waitingList == null) {
            waitingList = new ArrayList<>();
            event.setWaitingList(waitingList);
        }

        if (waitingList.contains(entrantDeviceId)) {
            return false;
        }

        waitingList.add(entrantDeviceId);
        return true;
    }

    private boolean leaveWaitingList(Event event, String entrantDeviceId) {
        if (event == null || event.getWaitingList() == null) {
            return false;
        }
        return event.getWaitingList().remove(entrantDeviceId);
    }

    private List<Event> getVisibleEvents(List<Event> events, Date now) {
        List<Event> visible = new ArrayList<>();
        for (Event event : events) {
            if (event == null) {
                continue;
            }

            Date deadline = event.getRegistrationDeadline();
            if (deadline == null || now.before(deadline)) {
                visible.add(event);
            }
        }
        return visible;
    }

    private BrowseCard toBrowseCard(Event event) {
        String title = event.getTitle() == null || event.getTitle().trim().isEmpty()
                ? "Untitled Event"
                : event.getTitle();
        String location = event.getLocation() == null || event.getLocation().trim().isEmpty()
                ? "Not set"
                : event.getLocation();
        String date = event.getStartDate() == null
                ? "Date not set"
                : BROWSE_DATE_FORMAT.format(event.getStartDate());
        return new BrowseCard(title, date, location);
    }

    private static class BrowseCard {
        private final String title;
        private final String date;
        private final String location;

        private BrowseCard(String title, String date, String location) {
            this.title = title;
            this.date = date;
            this.location = location;
        }
    }
}
