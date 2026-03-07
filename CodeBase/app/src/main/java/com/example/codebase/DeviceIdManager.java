package com.example.codebase;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Manages the unique device identifier for the application.
 * Persists a generated UUID in SharedPreferences on first run and retrieves
 * it on subsequent launches.
 */
public class DeviceIdManager {
    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_KEY = "device_id";

    /**
     * Retrieves the stored device ID or generates a new one if none exists.
     *
     * @param context The application or activity context to access SharedPreferences.
     * @return The stored or newly generated unique device ID.
     */
    public static String getOrCreateDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String deviceId = prefs.getString(PREF_KEY, null);
        
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString(PREF_KEY, deviceId).apply();
        }
        
        return deviceId;
    }

    /**
     * Shortens a device ID to its first 8 characters for UI display.
     *
     * @param deviceId The full device ID string.
     * @return The first 8 characters of the ID, or the full ID if shorter.
     */
    public static String getShortenedId(String deviceId) {
        if (deviceId == null) return "";
        return deviceId.length() > 8 ? deviceId.substring(0, 8) : deviceId;
    }
}
