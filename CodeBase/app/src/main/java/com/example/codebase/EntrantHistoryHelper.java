package com.example.codebase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Pure-Java helper for building the entrant's registration history view.
 *
 * <p>The history screen needs two pieces of business logic that are easier to
 * test outside Android UI code:</p>
 * <ul>
 *     <li>Determining whether an event belongs in the entrant's history.</li>
 *     <li>Deriving the most useful status label for that event.</li>
 * </ul>
 *
 * <p>This helper also bridges old and new data. New registrations are stored in
 * {@code registeredEntrants}, but we still treat the legacy live arrays as
 * history evidence so existing seeded/demo data appears on the screen too.</p>
 */
public final class EntrantHistoryHelper {

    /**
     * Possible history outcomes shown to entrants.
     */
    public enum HistoryStatus {
        REGISTERED("Registered"),
        SELECTED("Selected"),
        ENROLLED("Enrolled"),
        NOT_SELECTED("Not Selected"),
        CANCELLED("Cancelled");

        private final String label;

        HistoryStatus(String label) {
            this.label = label;
        }

        /**
         * Returns the user-facing status label.
         *
         * @return a short, non-null label for the UI badge
         */
        public String getLabel() {
            return label;
        }
    }

    /**
     * Immutable row model for the history RecyclerView.
     */
    public static final class HistoryEntry {
        private final Event event;
        private final HistoryStatus status;

        HistoryEntry(Event event, HistoryStatus status) {
            this.event = event;
            this.status = status;
        }

        /**
         * Returns the underlying event.
         *
         * @return the normalized event for this history row
         */
        public Event getEvent() {
            return event;
        }

        /**
         * Returns the derived entrant status for the event.
         *
         * @return the entrant's history status
         */
        public HistoryStatus getStatus() {
            return status;
        }
    }

    private EntrantHistoryHelper() {
    }

    /**
     * Builds a reverse-chronological history list for one entrant.
     *
     * <p>Entries are sorted with the most recent event dates first. If an event
     * has no useful date metadata, it falls back to the title so ordering remains
     * deterministic for tests.</p>
     *
     * @param events the candidate events to inspect
     * @param deviceId the entrant's device ID
     * @param now the current time used for draw/result calculations
     * @return a filtered, sorted list of history entries
     */
    public static List<HistoryEntry> buildHistory(List<Event> events, String deviceId, Date now) {
        List<HistoryEntry> historyEntries = new ArrayList<>();
        if (events == null || deviceId == null || deviceId.trim().isEmpty()) {
            return historyEntries;
        }

        Date effectiveNow = now != null ? now : new Date();
        for (Event event : events) {
            if (!isInHistory(event, deviceId)) {
                continue;
            }

            HistoryStatus status = resolveStatus(event, deviceId, effectiveNow);
            if (status != null) {
                historyEntries.add(new HistoryEntry(event, status));
            }
        }

        Collections.sort(historyEntries, Comparator
                .comparing((HistoryEntry entry) -> getSortDate(entry.getEvent()),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(entry -> safeText(entry.getEvent().getTitle())));
        return historyEntries;
    }

    /**
     * Returns whether the entrant should see the event in history.
     *
     * <p>The permanent {@code registeredEntrants} array is preferred, but the
     * current live-state arrays are also checked so pre-existing data still
     * surfaces after this feature is introduced.</p>
     *
     * @param event the event to inspect
     * @param deviceId the entrant device ID
     * @return {@code true} if the event belongs in history
     */
    public static boolean isInHistory(Event event, String deviceId) {
        if (event == null || deviceId == null || deviceId.trim().isEmpty()) {
            return false;
        }

        return contains(event.getRegisteredEntrants(), deviceId)
                || contains(event.getWaitingList(), deviceId)
                || contains(event.getSelectedEntrants(), deviceId)
                || contains(event.getEnrolledEntrants(), deviceId)
                || contains(event.getCancelledEntrants(), deviceId);
    }

    /**
     * Resolves the entrant's current/final history status for an event.
     *
     * <p>Status precedence matters. For example, accepted invitations are shown as
     * {@link HistoryStatus#ENROLLED} rather than merely {@link HistoryStatus#SELECTED}.</p>
     *
     * @param event the event to inspect
     * @param deviceId the entrant device ID
     * @param now the current time used to decide when a non-selected result is final
     * @return the best matching history status, or {@code null} if the event does not belong
     *         to the entrant
     */
    public static HistoryStatus resolveStatus(Event event, String deviceId, Date now) {
        if (!isInHistory(event, deviceId)) {
            return null;
        }

        if (contains(event.getEnrolledEntrants(), deviceId)) {
            return HistoryStatus.ENROLLED;
        }

        if (contains(event.getCancelledEntrants(), deviceId)) {
            return HistoryStatus.CANCELLED;
        }

        if (contains(event.getSelectedEntrants(), deviceId)) {
            return HistoryStatus.SELECTED;
        }

        Date effectiveNow = now != null ? now : new Date();
        if (event.getDrawDate() != null && !effectiveNow.before(event.getDrawDate())) {
            return HistoryStatus.NOT_SELECTED;
        }

        return HistoryStatus.REGISTERED;
    }

    private static boolean contains(List<String> deviceIds, String deviceId) {
        return deviceIds != null && deviceIds.contains(deviceId);
    }

    private static Date getSortDate(Event event) {
        if (event == null) {
            return null;
        }
        if (event.getStartDate() != null) {
            return event.getStartDate();
        }
        if (event.getDrawDate() != null) {
            return event.getDrawDate();
        }
        if (event.getRegistrationDeadline() != null) {
            return event.getRegistrationDeadline();
        }
        return event.getRegistrationOpen();
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }
}
