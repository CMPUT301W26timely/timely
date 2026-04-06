package com.example.codebase;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for entrant join-location records.
 */
public class EntrantLocationTest {

    @Test
    public void hasValidCoordinates_acceptsRealWorldPoint() {
        EntrantLocation entrantLocation =
                new EntrantLocation("device-1", "Chetan", 53.5461, -113.4938, new Date());

        assertTrue(entrantLocation.hasValidCoordinates());
    }

    @Test
    public void hasValidCoordinates_rejectsLatitudeOutsideRange() {
        EntrantLocation entrantLocation =
                new EntrantLocation("device-1", "Chetan", 120.0, -113.4938, new Date());

        assertFalse(entrantLocation.hasValidCoordinates());
    }

    @Test
    public void hasValidCoordinates_rejectsLongitudeOutsideRange() {
        EntrantLocation entrantLocation =
                new EntrantLocation("device-1", "Chetan", 53.5461, -190.0, new Date());

        assertFalse(entrantLocation.hasValidCoordinates());
    }
}
