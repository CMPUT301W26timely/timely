package com.example.codebase;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Unit tests for DeviceIdManager utility methods.
 */
public class SplashActivityTest {

    /**
     * Verifies that the getShortenedId method correctly returns the first 8
     * characters of a standard UUID string.
     */
    @Test
    public void testShortenedId_StandardUuid() {
        String fullUuid = "550e8400-e29b-41d4-a716-446655440000";
        String expectedShort = "550e8400";
        
        String actualShort = DeviceIdManager.getShortenedId(fullUuid);
        
        assertEquals("The shortened ID should be the first 8 characters", 
                     expectedShort, actualShort);
    }

    /**
     * Verifies that getShortenedId handles strings shorter than 8 characters.
     */
    @Test
    public void testShortenedId_ShortUuid() {
        String shortUuid = "abc-123";
        
        String actualShort = DeviceIdManager.getShortenedId(shortUuid);
        
        assertEquals("The shortened ID should return the original string if it is shorter than 8 characters", 
                     shortUuid, actualShort);
    }

    /**
     * Verifies that getShortenedId handles null input gracefully.
     */
    @Test
    public void testShortenedId_Null() {
        String actualShort = DeviceIdManager.getShortenedId(null);
        
        assertEquals("The shortened ID should return an empty string for null input", 
                     "", actualShort);
    }
}
