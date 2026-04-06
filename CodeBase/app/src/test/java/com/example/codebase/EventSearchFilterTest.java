package com.example.codebase;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Explore search and filter behavior.
 */
public class EventSearchFilterTest {

    @Test
    public void filterEvents_noFilters_returnsAllEvents() {
        List<Event> events = Arrays.asList(
                event("Swimming Lessons", "Beginner swim class", daysFromNow(2), 20L),
                event("Dance Basics", "Movement and rhythm", daysFromNow(5), 60L)
        );

        List<Event> filtered = EventSearchFilter.filterEvents(events, "", null, null, "All");

        assertEquals(2, filtered.size());
    }

    @Test
    public void filterEvents_keywordMatchesTitleOrDescription() {
        List<Event> events = Arrays.asList(
                event("Swimming Lessons", "Beginner swim class", daysFromNow(2), 20L),
                event("Dance Basics", "Movement and rhythm", daysFromNow(5), 60L)
        );

        List<Event> filtered = EventSearchFilter.filterEvents(events, "swim", null, null, "All");

        assertEquals(1, filtered.size());
        assertEquals("Swimming Lessons", filtered.get(0).getTitle());
    }

    @Test
    public void filterEvents_commaSeparatedKeywords_requireAllKeywords() {
        List<Event> events = Arrays.asList(
                event("Beginner Piano Lessons", "Piano basics for new learners", daysFromNow(3), 15L),
                event("Beginner Dance Lessons", "Movement basics for new learners", daysFromNow(3), 15L),
                event("Advanced Piano", "For experienced players", daysFromNow(3), 15L)
        );

        List<Event> filtered = EventSearchFilter.filterEvents(
                events,
                "beginner, piano",
                null,
                null,
                "All"
        );

        assertEquals(1, filtered.size());
        assertEquals("Beginner Piano Lessons", filtered.get(0).getTitle());
    }

    @Test
    public void filterEvents_dateWindow_filtersByStartDate() {
        List<Event> events = Arrays.asList(
                event("Soon Event", "Happens soon", daysFromNow(1), 25L),
                event("Later Event", "Happens later", daysFromNow(10), 25L)
        );

        Calendar from = Calendar.getInstance();
        from.setTime(daysFromNow(0));
        Calendar until = Calendar.getInstance();
        until.setTime(daysFromNow(3));

        List<Event> filtered = EventSearchFilter.filterEvents(events, "", from, until, "All");

        assertEquals(1, filtered.size());
        assertEquals("Soon Event", filtered.get(0).getTitle());
    }

    @Test
    public void filterEvents_capacityBucket_filtersByMaxCapacity() {
        List<Event> events = Arrays.asList(
                event("Small Event", "Small capacity", daysFromNow(2), 20L),
                event("Large Event", "Large capacity", daysFromNow(2), 120L)
        );

        List<Event> filtered = EventSearchFilter.filterEvents(events, "", null, null, "<= 50");

        assertEquals(1, filtered.size());
        assertEquals("Small Event", filtered.get(0).getTitle());
    }

    @Test
    public void filterEvents_nullInput_returnsEmptyList() {
        List<Event> filtered = EventSearchFilter.filterEvents(null, null, null, null, null);

        assertTrue(filtered.isEmpty());
    }

    private Event event(String title, String description, Date startDate, Long maxCapacity) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setStartDate(startDate);
        event.setMaxCapacity(maxCapacity);
        return event;
    }

    private Date daysFromNow(int offsetDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, offsetDays);
        return calendar.getTime();
    }
}
