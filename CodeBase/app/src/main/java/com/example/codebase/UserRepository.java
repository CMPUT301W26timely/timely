package com.example.codebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * UserRepository handles Firestore user profile operations.
 */
public class UserRepository {

    private static final String TAG = "UserRepository";

    public interface UserCallback {
        void onUserLoaded(User user);
        void onError(Exception e);
    }

    public interface ErrorCallback {
        void onError(Exception e);
    }

    public static void syncRole(Context context) {
        String deviceId = DeviceIdManager.getOrCreateDeviceId(context);
        String role = WelcomeActivity.getSessionRole(context);

        Map<String, Object> update = new HashMap<>();
        update.put("deviceId", deviceId);
        update.put("role", role.toLowerCase());

        AppDatabase.getInstance()
                .usersRef
                .document(deviceId)
                .set(update, SetOptions.merge())
                .addOnSuccessListener(v -> Log.d(TAG, "Role synced"))
                .addOnFailureListener(e -> Log.e(TAG, "Role sync failed", e));
    }

    public static void saveUserProfile(Context context,
                                       String name,
                                       String email,
                                       String phoneNumber,
                                       Runnable onSuccess,
                                       ErrorCallback onFailure) {
        String deviceId = DeviceIdManager.getOrCreateDeviceId(context);
        String role = WelcomeActivity.getSessionRole(context);

        Map<String, Object> update = new HashMap<>();
        update.put("deviceId", deviceId);
        update.put("role", role.toLowerCase());
        update.put("name", name);
        update.put("email", email);
        update.put("phoneNumber", phoneNumber);

        AppDatabase.getInstance()
                .usersRef
                .document(deviceId)
                .set(update, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    User user = new User(deviceId);
                    user.setRole(role.toLowerCase());
                    user.setName(name);
                    user.setEmail(email);
                    user.setPhoneNumber(phoneNumber);

                    // Save in memory cache too
                    AppCache.getInstance().setCachedUser(user);

                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) onFailure.onError(e);
                });
    }

    public static void loadUserProfile(Context context, @NonNull UserCallback callback) {
        String deviceId = DeviceIdManager.getOrCreateDeviceId(context);

        AppDatabase.getInstance()
                .usersRef
                .document(deviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = mapDocumentToUser(documentSnapshot, deviceId);

                    // Save in cache
                    AppCache.getInstance().setCachedUser(user);

                    callback.onUserLoaded(user);
                })
                .addOnFailureListener(callback::onError);
    }

    private static User mapDocumentToUser(DocumentSnapshot documentSnapshot, String deviceId) {
        User user = new User(deviceId);

        if (documentSnapshot.exists()) {
            user.setDeviceId(deviceId);

            String role = documentSnapshot.getString("role");
            String name = documentSnapshot.getString("name");
            String email = documentSnapshot.getString("email");
            String phoneNumber = documentSnapshot.getString("phoneNumber");

            user.setRole(role != null ? role : "entrant");
            user.setName(name != null ? name : "");
            user.setEmail(email != null ? email : "");
            user.setPhoneNumber(phoneNumber != null ? phoneNumber : "");
        }

        return user;
    }
}