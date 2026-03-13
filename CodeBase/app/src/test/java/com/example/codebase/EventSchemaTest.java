package com.example.codebase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link EventSchema}.
 *
 * <p>These tests cover the schema-normalization rules that are shared across multiple
 * organizer and entrant flows.
 */
public class EventSchemaTest {

    /**
     * Verifies that missing event IDs, entrant lists, draw date, and winners count are
     * repaired during normalization.
     */
    @Test
    public void normalizeLoadedEvent_appliesExpectedFallbacks() {
        Event event = new Event();
        event.setMaxCapacity(25L);
        event.setRegistrationDeadline(daysFromNow(2));

        Event normalized = EventSchema.normalizeLoadedEvent(event, "event-doc-123");

        assertEquals("event-doc-123", normalized.getId());
        assertEquals("event-doc-123", normalized.getEventId());
        assertNotNull(normalized.getWaitingList());
        assertNotNull(normalized.getSelectedEntrants());
        assertNotNull(normalized.getEnrolledEntrants());
        assertNotNull(normalized.getCancelledEntrants());
        assertNotNull(normalized.getDrawDate());
        assertEquals(Long.valueOf(25), normalized.getWinnersCount());
    }

    /**
     * Verifies that the draw date is derived three days after the registration deadline.
     */
    @Test
    public void calculateDrawDate_offsetsDeadlineByThreeDays() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.MARCH, 13, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date deadline = calendar.getTime();

        Date drawDate = EventSchema.calculateDrawDate(deadline);

        calendar.add(Calendar.DAY_OF_YEAR, 3);
        assertEquals(calendar.getTime(), drawDate);
    }

    /**
     * Verifies that string and legacy map list entries are converted to device IDs.
     */
    @Test
    public void toDeviceIdList_convertsSupportedEntryShapes() {
        Map<String, Object> legacyEntry = new HashMap<>();
        legacyEntry.put("deviceId", "legacy-device");

        List<Object> raw = Arrays.asList("plain-device", legacyEntry, 42);

        ArrayList<String> deviceIds = EventSchema.toDeviceIdList(raw);

        assertEquals(2, deviceIds.size());
        assertTrue(deviceIds.contains("plain-device"));
        assertTrue(deviceIds.contains("legacy-device"));
    }

    private Date daysFromNow(int offsetDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, offsetDays);
        return calendar.getTime();
    }
}
