package com.example.codebase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Pure filtering logic for Explore event search.
 */
public final class EventSearchFilter {

    private EventSearchFilter() {
    }

    /**
     * Returns the subset of events that match the provided keyword, date, and capacity filters.
     */
    public static List<Event> filterEvents(List<Event> events,
                                           String query,
                                           Calendar availableFrom,
                                           Calendar availableUntil,
                                           String selectedCapacityFilter) {
        List<Event> filteredEvents = new ArrayList<>();
        if (events == null || events.isEmpty()) {
            return filteredEvents;
        }

        List<String> validKeywords = parseKeywords(query);
        String normalizedCapacity = normalizeCapacityFilter(selectedCapacityFilter);

        for (Event event : events) {
            if (event == null) {
                continue;
            }

            if (matchesKeywords(event, validKeywords)
                    && matchesDates(event, availableFrom, availableUntil)
                    && matchesCapacity(event, normalizedCapacity)) {
                filteredEvents.add(event);
            }
        }

        return filteredEvents;
    }

    private static List<String> parseKeywords(String query) {
        List<String> validKeywords = new ArrayList<>();
        if (query == null) {
            return validKeywords;
        }

        for (String keyword : query.split(",")) {
            String trimmed = keyword.trim().toLowerCase(Locale.getDefault());
            if (!trimmed.isEmpty()) {
                validKeywords.add(trimmed);
            }
        }
        return validKeywords;
    }

    private static boolean matchesKeywords(Event event, List<String> keywords) {
        if (keywords.isEmpty()) {
            return true;
        }

        String title = event.getTitle() != null
                ? event.getTitle().toLowerCase(Locale.getDefault()) : "";
        String description = event.getDescription() != null
                ? event.getDescription().toLowerCase(Locale.getDefault()) : "";

        for (String keyword : keywords) {
            if (!title.contains(keyword) && !description.contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesDates(Event event, Calendar availableFrom, Calendar availableUntil) {
        if (event.getStartDate() == null) {
            return true;
        }

        long eventTime = event.getStartDate().getTime();
        if (availableFrom != null && eventTime < availableFrom.getTimeInMillis()) {
            return false;
        }
        if (availableUntil != null && eventTime > availableUntil.getTimeInMillis()) {
            return false;
        }
        return true;
    }

    private static boolean matchesCapacity(Event event, String selectedCapacityFilter) {
        if ("All".equals(selectedCapacityFilter)) {
            return true;
        }

        long capacity = event.getMaxCapacity() == null ? 0L : event.getMaxCapacity();
        switch (selectedCapacityFilter) {
            case "<= 20":
                return capacity <= 20;
            case "<= 50":
                return capacity <= 50;
            case "<= 100":
                return capacity <= 100;
            case "<= 250":
                return capacity <= 250;
            case "<= 500":
                return capacity <= 500;
            case "<= 1000":
                return capacity <= 1000;
            case "> 1000":
                return capacity > 1000;
            default:
                return true;
        }
    }

    private static String normalizeCapacityFilter(String selectedCapacityFilter) {
        if (selectedCapacityFilter == null || selectedCapacityFilter.trim().isEmpty()) {
            return "All";
        }
        return selectedCapacityFilter;
    }
}
