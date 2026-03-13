package com.example.codebase;

import java.util.ArrayList;

/**
 * Controller class to manage Entrant invitations locally.
 * This class modifies the Event object's lists in memory based on the user's choice.
 */
public class Invitations {
    /**
     * Processes an entrant's response to an event invitation.
     *
     * @param event          The event the entrant was invited to.
     * @param deviceId       The device ID of the entrant responding.
     * @param isAccepted     True if the entrant pressed "Accept", false if "Decline".
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
        }
    }
}
