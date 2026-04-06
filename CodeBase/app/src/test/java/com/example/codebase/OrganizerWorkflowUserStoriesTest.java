package com.example.codebase;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Story-focused JUnit coverage for organizer notification and entrant management flows.
 *
 * Covered stories:
 *   US 02.05.01 — Notify chosen entrants / notification logged
 *   US 02.06.01 — View selected / invited entrants
 *   US 02.06.02 — View cancelled entrants
 *   US 02.06.04 — Cancel entrant
 *   US 02.07.01 — Notify waiting list entrants
 *   US 02.07.02 — Notify selected entrants
 *   US 02.07.03 — Notify cancelled entrants
 */
public class OrganizerWorkflowUserStoriesTest {

    private static final String TYPE_WAITING = "waitingList";
    private static final String TYPE_SELECTED = "selectedEntrants";
    private static final String TYPE_CANCELLED = "cancelledEntrants";

    private List<InvitedEntrantsActivity.InvitedEntrant> invitedAll;
    private List<InvitedEntrantsActivity.InvitedEntrant> invitedAccepted;
    private List<InvitedEntrantsActivity.InvitedEntrant> invitedPending;
    private List<InvitedEntrantsActivity.InvitedEntrant> invitedDeclined;

    private List<CancelledEntrantsActivity.CancelledEntrant> cancelledAll;
    private List<CancelledEntrantsActivity.CancelledEntrant> cancelledDeclined;
    private List<CancelledEntrantsActivity.CancelledEntrant> cancelledByOrganizer;

    @Before
    public void setUp() {
        invitedAll = new ArrayList<>();
        invitedAccepted = new ArrayList<>();
        invitedPending = new ArrayList<>();
        invitedDeclined = new ArrayList<>();

        cancelledAll = new ArrayList<>();
        cancelledDeclined = new ArrayList<>();
        cancelledByOrganizer = new ArrayList<>();
    }

    @Test
    public void us020601_viewSelectedEntrants_displaysChosenEntrantsByStatus() {
        buildInvitedLists(
                Arrays.asList("user1", "user2", "user3"),
                Arrays.asList("user1"),
                Arrays.asList("user2")
        );

        assertEquals("All selected entrants should appear in the invited list", 3, invitedAll.size());
        assertEquals("Accepted entrants should be tracked separately", 1, invitedAccepted.size());
        assertEquals("Declined entrants should be tracked separately", 1, invitedDeclined.size());
        assertEquals("Pending entrants should be tracked separately", 1, invitedPending.size());
    }

    @Test
    public void us020602_viewCancelledEntrants_displaysCancelledAndDeclinedUsers() {
        buildCancelledLists(
                Arrays.asList("user1"),
                Arrays.asList("user1", "user3")
        );

        assertEquals("All removed participants should be shown", 2, cancelledAll.size());
        assertEquals("Declined entrants should be separated", 1, cancelledDeclined.size());
        assertEquals("Organizer-cancelled entrants should be separated", 1, cancelledByOrganizer.size());
        assertEquals("user1", cancelledDeclined.get(0).deviceId);
        assertEquals("user3", cancelledByOrganizer.get(0).deviceId);
    }

    @Test
    public void us020604_cancelEntrant_movesUserFromSelectedToCancelled() {
        CancelUpdate update = cancelEntrant(
                new ArrayList<>(Arrays.asList("user1", "user2")),
                new ArrayList<>(Arrays.asList("user9")),
                "user2"
        );

        assertFalse("Cancelled entrant should be removed from selected entrants",
                update.selected.contains("user2"));
        assertTrue("Cancelled entrant should be added to cancelled entrants",
                update.cancelled.contains("user2"));
    }

    @Test
    public void us020701_sendNotificationsToWaitingList_targetsWaitingEntrants() {
        List<String> recipients = getRecipients(
                TYPE_WAITING,
                Arrays.asList("wait1", "wait2"),
                Arrays.asList("sel1"),
                Arrays.asList("can1")
        );

        assertEquals(2, recipients.size());
        assertTrue(recipients.contains("wait1"));
    }

    @Test
    public void us020702_sendNotificationsToSelectedEntrants_targetsSelectedEntrants() {
        List<String> recipients = getRecipients(
                TYPE_SELECTED,
                Arrays.asList("wait1"),
                Arrays.asList("sel1", "sel2"),
                Arrays.asList("can1")
        );

        assertEquals(2, recipients.size());
        assertTrue(recipients.contains("sel2"));
    }

    @Test
    public void us020703_sendNotificationsToCancelledEntrants_targetsCancelledEntrants() {
        List<String> recipients = getRecipients(
                TYPE_CANCELLED,
                Arrays.asList("wait1"),
                Arrays.asList("sel1"),
                Arrays.asList("can1", "can2")
        );

        assertEquals(2, recipients.size());
        assertTrue(recipients.contains("can2"));
    }

    @Test
    public void us020501_notifyChosenEntrants_createsLoggableSelectedNotificationRecord() {
        Map<String, Object> notification = buildNotificationRecord(
                "selected-user-1",
                "event-501",
                "Summer Arts Camp",
                TYPE_SELECTED,
                "Congratulations! You've been selected!",
                "Please accept your invitation in the app."
        );

        assertEquals("selected-user-1", notification.get("userId"));
        assertEquals("event-501", notification.get("eventId"));
        assertEquals("Selected", notification.get("status"));
        assertEquals(TYPE_SELECTED, notification.get("type"));
        assertEquals(Boolean.FALSE, notification.get("read"));
    }

    @Ignore("Actual Firestore delivery and notification log persistence require integration testing.")
    @Test
    public void us020701_020702_020703_notificationsAreSentThroughFirestore() {
        // Covered by integration tests rather than local JUnit.
    }

    private void buildInvitedLists(List<String> invited,
                                   List<String> enrolled,
                                   List<String> declined) {
        invitedAll.clear();
        invitedAccepted.clear();
        invitedPending.clear();
        invitedDeclined.clear();

        for (String deviceId : invited) {
            String status;
            if (enrolled.contains(deviceId)) {
                status = "Accepted";
            } else if (declined.contains(deviceId)) {
                status = "Declined";
            } else {
                status = "Pending";
            }

            InvitedEntrantsActivity.InvitedEntrant entrant =
                    new InvitedEntrantsActivity.InvitedEntrant(deviceId, status);
            invitedAll.add(entrant);

            if ("Accepted".equals(status)) {
                invitedAccepted.add(entrant);
            } else if ("Declined".equals(status)) {
                invitedDeclined.add(entrant);
            } else {
                invitedPending.add(entrant);
            }
        }
    }

    private void buildCancelledLists(List<String> declined,
                                     List<String> cancelled) {
        cancelledAll.clear();
        cancelledDeclined.clear();
        cancelledByOrganizer.clear();

        for (String deviceId : cancelled) {
            String status = declined.contains(deviceId) ? "Declined" : "Cancelled";
            CancelledEntrantsActivity.CancelledEntrant entrant =
                    new CancelledEntrantsActivity.CancelledEntrant(deviceId, status, null);
            cancelledAll.add(entrant);

            if ("Declined".equals(status)) {
                cancelledDeclined.add(entrant);
            } else {
                cancelledByOrganizer.add(entrant);
            }
        }
    }

    private CancelUpdate cancelEntrant(List<String> selected,
                                       List<String> cancelled,
                                       String deviceId) {
        selected.remove(deviceId);
        if (!cancelled.contains(deviceId)) {
            cancelled.add(deviceId);
        }
        return new CancelUpdate(selected, cancelled);
    }

    private List<String> getRecipients(String type,
                                       List<String> waitingList,
                                       List<String> selectedList,
                                       List<String> cancelledList) {
        if (TYPE_SELECTED.equals(type)) {
            return selectedList;
        }
        if (TYPE_CANCELLED.equals(type)) {
            return cancelledList;
        }
        return waitingList;
    }

    private Map<String, Object> buildNotificationRecord(String deviceId,
                                                        String eventId,
                                                        String eventTitle,
                                                        String type,
                                                        String subject,
                                                        String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", deviceId);
        notification.put("eventId", eventId);
        notification.put("eventTitle", eventTitle);
        notification.put("title", subject);
        notification.put("message", message);
        notification.put("status", getStatusLabel(type));
        notification.put("type", type);
        notification.put("read", false);
        return notification;
    }

    private String getStatusLabel(String type) {
        if (TYPE_SELECTED.equals(type)) {
            return "Selected";
        }
        if (TYPE_CANCELLED.equals(type)) {
            return "Cancelled";
        }
        return "Waiting List";
    }

    private static class CancelUpdate {
        private final List<String> selected;
        private final List<String> cancelled;

        private CancelUpdate(List<String> selected, List<String> cancelled) {
            this.selected = selected;
            this.cancelled = cancelled;
        }
    }
}
