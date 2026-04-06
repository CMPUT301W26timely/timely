package com.example.codebase;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for geo-required waitlist join rules.
 */
public class GeoJoinPolicyTest {

    @Test
    public void requiresLocationCapture_returnsTrueWhenEventGeoEnabled() {
        Event event = new Event();
        event.setGeoEnabled(true);

        assertTrue(GeoJoinPolicy.requiresLocationCapture(event));
    }

    @Test
    public void requiresLocationCapture_returnsFalseWhenEventGeoDisabled() {
        Event event = new Event();
        event.setGeoEnabled(false);

        assertFalse(GeoJoinPolicy.requiresLocationCapture(event));
    }

    @Test
    public void canCompleteJoin_allowsNonGeoEventsWithoutLocation() {
        assertTrue(GeoJoinPolicy.canCompleteJoin(false, false, null));
    }

    @Test
    public void canCompleteJoin_blocksGeoJoinWithoutPermission() {
        EntrantLocation entrantLocation =
                new EntrantLocation("device-1", "Chetan", 53.5461, -113.4938, new Date());

        assertFalse(GeoJoinPolicy.canCompleteJoin(true, false, entrantLocation));
    }

    @Test
    public void canCompleteJoin_blocksGeoJoinWithInvalidCoordinates() {
        EntrantLocation entrantLocation =
                new EntrantLocation("device-1", "Chetan", 153.5461, -113.4938, new Date());

        assertFalse(GeoJoinPolicy.canCompleteJoin(true, true, entrantLocation));
    }

    @Test
    public void canCompleteJoin_allowsGeoJoinWithPermissionAndValidLocation() {
        EntrantLocation entrantLocation =
                new EntrantLocation("device-1", "Chetan", 53.5461, -113.4938, new Date());

        assertTrue(GeoJoinPolicy.canCompleteJoin(true, true, entrantLocation));
    }
}
