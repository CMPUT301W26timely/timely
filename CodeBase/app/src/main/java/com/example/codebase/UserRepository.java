package com.example.codebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * UserRepository — Handles syncing user role to Firestore.
 *
 * Uses set() with merge option instead of update() so it works
 * whether the document exists or not:
 *   - Document exists → updates role field only
 *   - Document does not exist → creates it with role field
 *
 * Called from MainActivity.onCreate() on every launch.
 *
 * Usage:
 *   UserRepository.syncRole(this);
 */
public class UserRepository {

    private static final String TAG = "UserRepository";

    /**
     * Syncs the locally saved role to Firestore using set+merge.
     * Safe to call whether the document exists or not.
     *
     * @param context Any context (Activity, Application, etc.)
     */
    public static void syncRole(Context context) {
        String deviceId = DeviceIdManager.getOrCreateDeviceId(context);
        String role     = WelcomeActivity.getSessionRole(context);

        // Use a Map so we only update the role field — not overwrite the whole document
        Map<String, Object> update = new HashMap<>();
        update.put("deviceId", deviceId);
        update.put("role", role.toLowerCase());

        AppDatabase.getInstance()
                .usersRef
                .document(deviceId)
                .set(update, SetOptions.merge())  // merge = update if exists, create if not
                .addOnSuccessListener(v ->
                        Log.d(TAG, "Firestore role synced: " + role))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to sync role to Firestore", e));
    }
}