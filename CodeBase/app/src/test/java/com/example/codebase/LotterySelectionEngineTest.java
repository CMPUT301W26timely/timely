package com.example.codebase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LotterySelectionEngineTest {

    @Test
    public void eligibleWaitingList_excludesAlreadyResolvedStates() {
        Event event = baseEvent();
        event.setWaitingList(new ArrayList<>(Arrays.asList("w1", "w2", "w3", "w4", "w5")));
        event.setSelectedEntrants(new ArrayList<>(Arrays.asList("w2")));
        event.setEnrolledEntrants(new ArrayList<>(Arrays.asList("w3")));
        event.setCancelledEntrants(new ArrayList<>(Arrays.asList("w4")));
        event.setCoOrganizers(new ArrayList<>(Arrays.asList("w5")));

        assertEquals(Arrays.asList("w1"), LotterySelectionEngine.eligibleWaitingList(event));
    }

    @Test
    public void draw_capsRequestedCountByCapacityAndEligiblePool() {
        Event event = baseEvent();
        event.setMaxCapacity(3L);
        event.setWaitingList(new ArrayList<>(Arrays.asList("w1", "w2", "w3", "w4")));
        event.setSelectedEntrants(new ArrayList<>(Arrays.asList("existing-selected")));
        event.setEnrolledEntrants(new ArrayList<>(Arrays.asList("existing-enrolled")));

        LotterySelectionEngine.SelectionResult result =
                LotterySelectionEngine.draw(event, 10, new Random(1));

        assertEquals(1, result.getAvailableSpots());
        assertEquals(1, result.getDrawnEntrants().size());
    }

    @Test
    public void draw_returnsRemainingEligibleEntrantsWithoutWinners() {
        Event event = baseEvent();
        event.setWaitingList(new ArrayList<>(Arrays.asList("w1", "w2", "w3")));

        LotterySelectionEngine.SelectionResult result =
                LotterySelectionEngine.draw(event, 2, new Random(7));

        assertEquals(2, result.getDrawnEntrants().size());
        assertEquals(1, result.getRemainingEligibleEntrants().size());
        assertFalse(result.getRemainingEligibleEntrants().containsAll(result.getDrawnEntrants()));
    }

    @Test
    public void availableSpots_defaultsToUnlimitedWhenCapacityMissing() {
        Event event = baseEvent();
        assertEquals(Integer.MAX_VALUE, LotterySelectionEngine.availableSpots(event));
    }

    @Test
    public void draw_withNoEligibleEntrantsReturnsEmptySelection() {
        Event event = baseEvent();
        event.setWaitingList(new ArrayList<>(Arrays.asList("w1")));
        event.setCancelledEntrants(new ArrayList<>(Arrays.asList("w1")));

        LotterySelectionEngine.SelectionResult result =
                LotterySelectionEngine.draw(event, 1, new Random(3));

        assertTrue(result.getDrawnEntrants().isEmpty());
    }

    private Event baseEvent() {
        Event event = new Event();
        event.setWaitingList(new ArrayList<>());
        event.setSelectedEntrants(new ArrayList<>());
        event.setEnrolledEntrants(new ArrayList<>());
        event.setCancelledEntrants(new ArrayList<>());
        event.setCoOrganizers(new ArrayList<>());
        return event;
    }
}
