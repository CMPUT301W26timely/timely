package com.example.codebase;

import java.util.ArrayList;

/**
 * Controller responsible for applying an entrant's accept/decline response to an event.
 *
 * <p>This class mutates the in-memory {@link Event} entrant lists only. Persistence is
 * handled by the caller after the updated {@link Event} is returned to the UI layer.
 */
public class Invitations {
    /**
     * Processes an entrant's response to an event invitation.
     *
     * <p>If the entrant accepts, they are removed from {@code selectedEntrants} and added
     * to {@code enrolledEntrants}. If the entrant declines, they are removed from
     * {@code selectedEntrants} and added to both {@code cancelledEntrants} and
     * {@code declinedEntrants}. Every response keeps the device ID in
     * {@code invitedEntrants} as historical organizer-visible state.
     *
     * @param event the event the entrant was invited to
     * @param deviceId the device ID of the entrant responding
     * @param isAccepted {@code true} if the entrant accepted, {@code false} if they declined
     */
    public void respondToInvitation(Event event, String deviceId, boolean isAccepted) {
        if (event == null || deviceId == null) {
            return; // Invalid input
        }

        ArrayList<String> selected = event.getSelectedEntrants();
        if (selected == null) {
            return;
        }

        // Remove from the pending invitations list (selectedEntrants)
        selected.remove(deviceId);

        ArrayList<String> invited = event.getInvitedEntrants();
        if (invited == null) {
            invited = new ArrayList<>();
            event.setInvitedEntrants(invited);
        }
        if (!invited.contains(deviceId)) {
            invited.add(deviceId);
        }

        if (isAccepted) {
            // Entrant pressed "Accept" -> Add to enrolledEntrants
            ArrayList<String> enrolled = event.getEnrolledEntrants();
            if (enrolled == null) {
                enrolled = new ArrayList<>();
                event.setEnrolledEntrants(enrolled);
            }
            
            if (!enrolled.contains(deviceId)) {
                enrolled.add(deviceId);
            }

            ArrayList<String> cancelled = event.getCancelledEntrants();
            if (cancelled != null) {
                cancelled.remove(deviceId);
            }
            ArrayList<String> declined = event.getDeclinedEntrants();
            if (declined != null) {
                declined.remove(deviceId);
            }
        } else {
            // Entrant pressed "Decline" -> Move to cancelledEntrants
            ArrayList<String> cancelled = event.getCancelledEntrants();
            if (cancelled == null) {
                cancelled = new ArrayList<>();
                event.setCancelledEntrants(cancelled);
            }
            
            if (!cancelled.contains(deviceId)) {
                cancelled.add(deviceId);
            }

            ArrayList<String> declined = event.getDeclinedEntrants();
            if (declined == null) {
                declined = new ArrayList<>();
                event.setDeclinedEntrants(declined);
            }

            if (!declined.contains(deviceId)) {
                declined.add(deviceId);
            }
        }
    }
}
