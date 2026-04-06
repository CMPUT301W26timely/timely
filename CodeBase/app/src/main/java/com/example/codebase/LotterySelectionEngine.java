package com.example.codebase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure helper for lottery and replacement-draw selection rules.
 *
 * <p>The waiting list remains the persistent pool of interested entrants. Eligibility for a
 * new draw is derived by excluding anyone who is already selected, enrolled, cancelled, or
 * assigned as a co-organizer for the same event.</p>
 */
public final class LotterySelectionEngine {

    private LotterySelectionEngine() {
    }

    /**
     * Returns the remaining number of selectable spots for the event.
     *
     * <p>Pending selected entrants and already enrolled entrants both occupy capacity. When the
     * event has no maximum capacity configured, this method returns {@link Integer#MAX_VALUE} so
     * callers can cap draws using only the eligible-pool size.</p>
     */
    public static int getAvailableSpots(Event event) {
        if (event == null) {
            return 0;
        }

        Long maxCapacity = event.getMaxCapacity();
        if (maxCapacity == null || maxCapacity <= 0) {
            return Integer.MAX_VALUE;
        }

        long remaining = maxCapacity
                - safeSize(event.getSelectedEntrants())
                - safeSize(event.getEnrolledEntrants());
        if (remaining <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    /**
     * Computes the current eligible draw pool from the waiting list.
     */
    public static List<String> getEligibleEntrants(Event event) {
        List<String> eligible = new ArrayList<>();
        if (event == null || event.getWaitingList() == null) {
            return eligible;
        }

        Set<String> excluded = new LinkedHashSet<>();
        addAll(excluded, event.getSelectedEntrants());
        addAll(excluded, event.getEnrolledEntrants());
        addAll(excluded, event.getCancelledEntrants());
        addAll(excluded, event.getCoOrganizers());

        for (String deviceId : event.getWaitingList()) {
            if (deviceId == null || deviceId.trim().isEmpty()) {
                continue;
            }
            if (excluded.contains(deviceId) || eligible.contains(deviceId)) {
                continue;
            }
            eligible.add(deviceId);
        }

        return eligible;
    }

    /**
     * Draws a random subset of eligible entrants for the event.
     */
    public static DrawResult drawEntrants(Event event, int requestedCount) {
        return drawEntrants(event, requestedCount, new Random());
    }

    /**
     * Draws a random subset of eligible entrants for the event using the provided source of
     * randomness, which makes unit tests deterministic.
     */
    public static DrawResult drawEntrants(Event event, int requestedCount, Random random) {
        List<String> eligible = getEligibleEntrants(event);
        int capacityRemaining = getAvailableSpots(event);

        if (requestedCount <= 0 || eligible.isEmpty() || capacityRemaining <= 0) {
            return new DrawResult(eligible, new ArrayList<>(), requestedCount, 0, capacityRemaining);
        }

        int actualCount = Math.min(requestedCount, Math.min(eligible.size(), capacityRemaining));
        List<String> shuffled = new ArrayList<>(eligible);
        Collections.shuffle(shuffled, random);
        List<String> drawn = new ArrayList<>(shuffled.subList(0, actualCount));
        return new DrawResult(eligible, drawn, requestedCount, actualCount, capacityRemaining);
    }

    private static int safeSize(List<String> list) {
        return list == null ? 0 : list.size();
    }

    private static void addAll(Set<String> target, List<String> source) {
        if (source == null) {
            return;
        }
        for (String value : source) {
            if (value != null && !value.trim().isEmpty()) {
                target.add(value);
            }
        }
    }

    /**
     * Immutable summary of a draw attempt.
     */
    public static final class DrawResult {
        private final List<String> eligibleEntrants;
        private final List<String> drawnEntrants;
        private final int requestedCount;
        private final int actualCount;
        private final int capacityRemaining;

        DrawResult(List<String> eligibleEntrants,
                   List<String> drawnEntrants,
                   int requestedCount,
                   int actualCount,
                   int capacityRemaining) {
            this.eligibleEntrants = new ArrayList<>(eligibleEntrants);
            this.drawnEntrants = new ArrayList<>(drawnEntrants);
            this.requestedCount = requestedCount;
            this.actualCount = actualCount;
            this.capacityRemaining = capacityRemaining;
        }

        public List<String> getEligibleEntrants() {
            return new ArrayList<>(eligibleEntrants);
        }

        public List<String> getDrawnEntrants() {
            return new ArrayList<>(drawnEntrants);
        }

        public int getRequestedCount() {
            return requestedCount;
        }

        public int getActualCount() {
            return actualCount;
        }

        public int getCapacityRemaining() {
            return capacityRemaining;
        }
    }
}
