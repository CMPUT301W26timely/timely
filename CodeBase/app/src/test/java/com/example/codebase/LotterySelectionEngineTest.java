package com.example.codebase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for replacement-draw and lottery eligibility rules.
 */
public class LotterySelectionEngineTest {

    @Test
    public void eligibleEntrants_excludesSelectedEnrolledCancelledAndCoOrganizers() {
        Event event = makeEvent();
        event.setWaitingList(new ArrayList<>(Arrays.asList(
                "selected-user",
                "enrolled-user",
                "cancelled-user",
                "coorganizer-user",
                "eligible-user-1",
                "eligible-user-2"
        )));
        event.setSelectedEntrants(new ArrayList<>(Arrays.asList("selected-user")));
        event.setEnrolledEntrants(new ArrayList<>(Arrays.asList("enrolled-user")));
        event.setCancelledEntrants(new ArrayList<>(Arrays.asList("cancelled-user")));
        event.setCoOrganizers(new ArrayList<>(Arrays.asList("coorganizer-user")));

        List<String> eligible = LotterySelectionEngine.getEligibleEntrants(event);

        assertEquals(2, eligible.size());
        assertTrue(eligible.contains("eligible-user-1"));
        assertTrue(eligible.contains("eligible-user-2"));
    }

    @Test
    public void drawEntrants_capsRequestedCountByEligiblePoolAndCapacity() {
        Event event = makeEvent();
        event.setMaxCapacity(3L);
        event.setEnrolledEntrants(new ArrayList<>(Arrays.asList("enrolled-1")));
        event.setSelectedEntrants(new ArrayList<>(Arrays.asList("selected-1")));
        event.setWaitingList(new ArrayList<>(Arrays.asList(
                "selected-1",
                "eligible-1",
                "eligible-2",
                "eligible-3"
        )));

        LotterySelectionEngine.DrawResult result =
                LotterySelectionEngine.drawEntrants(event, 5, new Random(7));

        assertEquals("Only one spot should remain after selected + enrolled", 1,
                result.getCapacityRemaining());
        assertEquals("The draw should be capped to the remaining capacity", 1,
                result.getActualCount());
        assertEquals(3, result.getEligibleEntrants().size());
    }

    @Test
    public void drawEntrants_neverReSelectsCancelledEntrants() {
        Event event = makeEvent();
        event.setWaitingList(new ArrayList<>(Arrays.asList(
                "cancelled-user",
                "eligible-user-1",
                "eligible-user-2"
        )));
        event.setCancelledEntrants(new ArrayList<>(Arrays.asList("cancelled-user")));

        LotterySelectionEngine.DrawResult result =
                LotterySelectionEngine.drawEntrants(event, 2, new Random(3));

        assertEquals(2, result.getActualCount());
        assertFalse(result.getDrawnEntrants().contains("cancelled-user"));
    }

    @Test
    public void drawEntrants_returnsEmptyWhenRequestedCountIsInvalid() {
        Event event = makeEvent();
        event.setWaitingList(new ArrayList<>(Arrays.asList("eligible-user")));

        LotterySelectionEngine.DrawResult result =
                LotterySelectionEngine.drawEntrants(event, 0, new Random(1));

        assertEquals(0, result.getActualCount());
        assertTrue(result.getDrawnEntrants().isEmpty());
    }

    @Test
    public void drawEntrants_returnsEmptyWhenNoEligibleEntrantsRemain() {
        Event event = makeEvent();
        event.setWaitingList(new ArrayList<>(Arrays.asList("cancelled-user")));
        event.setCancelledEntrants(new ArrayList<>(Arrays.asList("cancelled-user")));

        LotterySelectionEngine.DrawResult result =
                LotterySelectionEngine.drawEntrants(event, 1, new Random(5));

        assertEquals(0, result.getActualCount());
        assertTrue(result.getEligibleEntrants().isEmpty());
    }

    private Event makeEvent() {
        Event event = new Event();
        event.setId("event-replacement");
        event.setWaitingList(new ArrayList<>());
        event.setSelectedEntrants(new ArrayList<>());
        event.setEnrolledEntrants(new ArrayList<>());
        event.setCancelledEntrants(new ArrayList<>());
        event.setCoOrganizers(new ArrayList<>());
        event.setMaxCapacity(10L);
        return event;
    }
}
