package com.example.codebase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Normalizes organizer-facing entrant status views onto one consistent event state model.
 */
public final class EventRosterStatusHelper {

    private EventRosterStatusHelper() {
    }

    public static ArrayList<String> invitedEntrants(Event event) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (event == null) {
            return new ArrayList<>();
        }

        ordered.addAll(nullSafe(event.getInvitedEntrants()));
        if (ordered.isEmpty()) {
            ordered.addAll(nullSafe(event.getSelectedEntrants()));
            ordered.addAll(nullSafe(event.getEnrolledEntrants()));
            ordered.addAll(nullSafe(event.getCancelledEntrants()));
        }
        return new ArrayList<>(ordered);
    }

    public static ArrayList<String> pendingEntrants(Event event) {
        return distinct(event != null ? event.getSelectedEntrants() : null);
    }

    public static ArrayList<String> acceptedEntrants(Event event) {
        LinkedHashSet<String> invited = new LinkedHashSet<>(invitedEntrants(event));
        invited.retainAll(nullSafe(event != null ? event.getEnrolledEntrants() : null));
        return new ArrayList<>(invited);
    }

    public static ArrayList<String> declinedEntrants(Event event) {
        ArrayList<String> explicitDeclines = distinct(event != null ? event.getDeclinedEntrants() : null);
        if (!explicitDeclines.isEmpty()) {
            return explicitDeclines;
        }

        ArrayList<String> legacyDeclines = distinct(event != null ? event.getCancelledEntrants() : null);
        legacyDeclines.removeAll(nullSafe(event != null ? event.getEnrolledEntrants() : null));
        return legacyDeclines;
    }

    public static ArrayList<String> cancelledEntrants(Event event) {
        return distinct(event != null ? event.getCancelledEntrants() : null);
    }

    public static ArrayList<String> organizerCancelledEntrants(Event event) {
        ArrayList<String> organizerCancelled = cancelledEntrants(event);
        organizerCancelled.removeAll(declinedEntrants(event));
        return organizerCancelled;
    }

    private static ArrayList<String> distinct(List<String> ids) {
        return new ArrayList<>(new LinkedHashSet<>(nullSafe(ids)));
    }

    private static List<String> nullSafe(List<String> ids) {
        return ids != null ? ids : new ArrayList<>();
    }
}
