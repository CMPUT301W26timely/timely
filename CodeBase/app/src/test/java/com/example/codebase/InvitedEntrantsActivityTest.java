package com.example.codebase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for organizer-facing invited entrant status classification.
 */
public class InvitedEntrantsActivityTest {

    @Test
    public void invitedHistory_usesPersistentInvitedEntrantsList() {
        Event event = baseEvent();
        event.setInvitedEntrants(new ArrayList<>(Arrays.asList("u1", "u2", "u3")));
        event.setSelectedEntrants(new ArrayList<>(Arrays.asList("u3")));
        event.setEnrolledEntrants(new ArrayList<>(Arrays.asList("u1")));
        event.setDeclinedEntrants(new ArrayList<>(Arrays.asList("u2")));

        assertEquals(Arrays.asList("u1", "u2", "u3"),
                EventRosterStatusHelper.invitedEntrants(event));
    }

    @Test
    public void acceptedEntrants_areDrivenByEnrolledState() {
        Event event = baseEvent();
        event.setInvitedEntrants(new ArrayList<>(Arrays.asList("u1", "u2")));
        event.setEnrolledEntrants(new ArrayList<>(Arrays.asList("u2")));

        assertEquals(Arrays.asList("u2"),
                EventRosterStatusHelper.acceptedEntrants(event));
    }

    @Test
    public void declinedEntrants_areDrivenByExplicitDeclineState() {
        Event event = baseEvent();
        event.setInvitedEntrants(new ArrayList<>(Arrays.asList("u1", "u2", "u3")));
        event.setDeclinedEntrants(new ArrayList<>(Arrays.asList("u3")));
        event.setCancelledEntrants(new ArrayList<>(Arrays.asList("u2", "u3")));

        assertEquals(Arrays.asList("u3"),
                EventRosterStatusHelper.declinedEntrants(event));
    }

    @Test
    public void pendingEntrants_areCurrentSelectedEntrantsOnly() {
        Event event = baseEvent();
        event.setSelectedEntrants(new ArrayList<>(Arrays.asList("u4", "u5")));
        event.setInvitedEntrants(new ArrayList<>(Arrays.asList("u1", "u2", "u4", "u5")));

        assertEquals(Arrays.asList("u4", "u5"),
                EventRosterStatusHelper.pendingEntrants(event));
    }

    @Test
    public void legacyFallback_usesExistingArraysWhenInvitedHistoryMissing() {
        Event event = baseEvent();
        event.setSelectedEntrants(new ArrayList<>(Arrays.asList("u1")));
        event.setEnrolledEntrants(new ArrayList<>(Arrays.asList("u2")));
        event.setCancelledEntrants(new ArrayList<>(Arrays.asList("u3")));

        assertTrue(EventRosterStatusHelper.invitedEntrants(event).containsAll(
                Arrays.asList("u1", "u2", "u3")));
        assertEquals(3, EventRosterStatusHelper.invitedEntrants(event).size());
    }

    private Event baseEvent() {
        Event event = new Event();
        event.setInvitedEntrants(new ArrayList<>());
        event.setSelectedEntrants(new ArrayList<>());
        event.setEnrolledEntrants(new ArrayList<>());
        event.setCancelledEntrants(new ArrayList<>());
        event.setDeclinedEntrants(new ArrayList<>());
        return event;
    }
}
