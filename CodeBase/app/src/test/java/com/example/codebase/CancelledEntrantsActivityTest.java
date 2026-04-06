package com.example.codebase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for organizer-facing cancelled entrant classification.
 */
public class CancelledEntrantsActivityTest {

    @Test
    public void allCancelledEntrants_returnEntireCancelledList() {
        Event event = baseEvent();
        event.setCancelledEntrants(new ArrayList<>(Arrays.asList("u1", "u2", "u3")));

        assertEquals(Arrays.asList("u1", "u2", "u3"),
                EventRosterStatusHelper.cancelledEntrants(event));
    }

    @Test
    public void organizerCancelledEntrants_excludeExplicitDeclines() {
        Event event = baseEvent();
        event.setCancelledEntrants(new ArrayList<>(Arrays.asList("u1", "u2", "u3")));
        event.setDeclinedEntrants(new ArrayList<>(Arrays.asList("u2")));

        assertEquals(Arrays.asList("u1", "u3"),
                EventRosterStatusHelper.organizerCancelledEntrants(event));
    }

    @Test
    public void declinedEntrants_useExplicitDeclineListWhenPresent() {
        Event event = baseEvent();
        event.setCancelledEntrants(new ArrayList<>(Arrays.asList("u1", "u2")));
        event.setDeclinedEntrants(new ArrayList<>(Arrays.asList("u2")));

        assertEquals(Arrays.asList("u2"),
                EventRosterStatusHelper.declinedEntrants(event));
    }

    @Test
    public void declinedFallback_excludesAcceptedEntrantsForLegacyData() {
        Event event = baseEvent();
        event.setCancelledEntrants(new ArrayList<>(Arrays.asList("u1", "u2")));
        event.setEnrolledEntrants(new ArrayList<>(Arrays.asList("u1")));

        assertEquals(Arrays.asList("u2"),
                EventRosterStatusHelper.declinedEntrants(event));
    }

    private Event baseEvent() {
        Event event = new Event();
        event.setCancelledEntrants(new ArrayList<>());
        event.setDeclinedEntrants(new ArrayList<>());
        event.setEnrolledEntrants(new ArrayList<>());
        return event;
    }
}
