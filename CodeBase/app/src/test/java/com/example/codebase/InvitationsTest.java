package com.example.codebase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the in-memory invitation response controller.
 */
public class InvitationsTest {

    /**
     * Verifies that accepting an invitation moves the entrant from selected to enrolled.
     */
    @Test
    public void respondToInvitation_acceptMovesEntrantToEnrolled() {
        Event event = baseEvent();

        new Invitations().respondToInvitation(event, "device-1", true);

        assertFalse(event.getSelectedEntrants().contains("device-1"));
        assertTrue(event.getEnrolledEntrants().contains("device-1"));
    }

    /**
     * Verifies that declining an invitation moves the entrant from selected to cancelled.
     */
    @Test
    public void respondToInvitation_declineMovesEntrantToCancelled() {
        Event event = baseEvent();

        new Invitations().respondToInvitation(event, "device-1", false);

        assertFalse(event.getSelectedEntrants().contains("device-1"));
        assertTrue(event.getCancelledEntrants().contains("device-1"));
    }

    private Event baseEvent() {
        Event event = new Event();
        event.setSelectedEntrants(new ArrayList<>(Arrays.asList("device-1", "device-2")));
        event.setEnrolledEntrants(new ArrayList<>());
        event.setCancelledEntrants(new ArrayList<>());
        return event;
    }
}
