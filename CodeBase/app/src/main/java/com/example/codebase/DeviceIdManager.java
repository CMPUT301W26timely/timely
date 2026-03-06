package com.example.codebase;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class DeviceIdManager {
    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_KEY = "device_id";

    public static String getOrCreateDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String deviceId = prefs.getString(PREF_KEY, null);
        
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString(PREF_KEY, deviceId).apply();
        }
        
        return deviceId;
    }
}
