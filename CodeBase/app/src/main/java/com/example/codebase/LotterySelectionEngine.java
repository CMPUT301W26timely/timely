package com.example.codebase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

/**
 * Pure helper for computing eligible lottery entrants and drawing winners.
 */
public final class LotterySelectionEngine {

    private LotterySelectionEngine() {
    }

    public static SelectionResult draw(Event event, int requestedCount, Random random) {
        ArrayList<String> eligible = eligibleWaitingList(event);
        int availableSpots = availableSpots(event);
        int actualCount = Math.max(0, Math.min(requestedCount, Math.min(eligible.size(), availableSpots)));

        ArrayList<String> shuffled = new ArrayList<>(eligible);
        Collections.shuffle(shuffled, random != null ? random : new Random());

        ArrayList<String> drawnEntrants = new ArrayList<>(shuffled.subList(0, actualCount));
        ArrayList<String> remainingEligible = new ArrayList<>(eligible);
        remainingEligible.removeAll(drawnEntrants);
        return new SelectionResult(eligible, drawnEntrants, remainingEligible, availableSpots);
    }

    public static ArrayList<String> eligibleWaitingList(Event event) {
        LinkedHashSet<String> eligible = new LinkedHashSet<>();
        if (event == null) {
            return new ArrayList<>();
        }

        eligible.addAll(nullSafe(event.getWaitingList()));
        eligible.removeAll(nullSafe(event.getSelectedEntrants()));
        eligible.removeAll(nullSafe(event.getEnrolledEntrants()));
        eligible.removeAll(nullSafe(event.getCancelledEntrants()));
        eligible.removeAll(nullSafe(event.getCoOrganizers()));
        return new ArrayList<>(eligible);
    }

    public static int availableSpots(Event event) {
        if (event == null) {
            return 0;
        }

        Long maxCapacity = event.getMaxCapacity();
        if (maxCapacity == null || maxCapacity <= 0) {
            return Integer.MAX_VALUE;
        }

        int occupied = nullSafe(event.getSelectedEntrants()).size()
                + nullSafe(event.getEnrolledEntrants()).size();
        return Math.max(0, (int) (long) maxCapacity - occupied);
    }

    private static List<String> nullSafe(List<String> ids) {
        return ids != null ? ids : new ArrayList<>();
    }

    public static final class SelectionResult {
        private final ArrayList<String> eligibleEntrants;
        private final ArrayList<String> drawnEntrants;
        private final ArrayList<String> remainingEligibleEntrants;
        private final int availableSpots;

        SelectionResult(ArrayList<String> eligibleEntrants,
                        ArrayList<String> drawnEntrants,
                        ArrayList<String> remainingEligibleEntrants,
                        int availableSpots) {
            this.eligibleEntrants = eligibleEntrants;
            this.drawnEntrants = drawnEntrants;
            this.remainingEligibleEntrants = remainingEligibleEntrants;
            this.availableSpots = availableSpots;
        }

        public ArrayList<String> getEligibleEntrants() {
            return eligibleEntrants;
        }

        public ArrayList<String> getDrawnEntrants() {
            return drawnEntrants;
        }

        public ArrayList<String> getRemainingEligibleEntrants() {
            return remainingEligibleEntrants;
        }

        public int getAvailableSpots() {
            return availableSpots;
        }
    }
}
