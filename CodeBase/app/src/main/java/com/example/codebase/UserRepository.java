package com.example.codebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository class responsible for Firestore user profile operations.
 *
 * <p>All writes use {@link SetOptions#merge()} so that only the specified fields are
 * updated without overwriting unrelated document data. Successfully loaded or saved
 * profiles are stored in {@link AppCache} for synchronous access elsewhere in the app.
 *
 * <p>All methods are static; this class is not intended to be instantiated.
 */
public class UserRepository {

    private static final String TAG = "UserRepository";

    /**
     * Callback interface for operations that return a single {@link User}.
     */
    public interface UserCallback {
        /**
         * Called when the user profile has been successfully loaded.
         *
         * @param user The loaded {@link User}; never {@code null}.
         */
        void onUserLoaded(User user);

        /**
         * Called when the load operation fails.
         *
         * @param e The exception describing the failure.
         */
        void onError(Exception e);
    }

    /**
     * Callback interface for operations that return multiple {@link User} profiles.
     */
    public interface ProfilesCallback {
        /**
         * Called when user profiles have been successfully loaded.
         *
         * @param users loaded profiles; never {@code null}, but may be empty
         */
        void onProfilesLoaded(java.util.List<User> users);

        /**
         * Called when the profile browse operation fails.
         *
         * @param e the exception describing the failure
         */
        void onError(Exception e);
    }

    /**
     * Callback interface for operations that only need to report failure.
     */
    public interface ErrorCallback {
        /**
         * Called when an operation fails.
         *
         * @param e The exception describing the failure.
         */
        void onError(Exception e);
    }

    /**
     * Synchronises the current device ID and session role to the user's Firestore document.
     *
     * <p>Only the {@code deviceId} and {@code role} fields are written; all other profile
     * fields are left unchanged. Outcomes are logged at DEBUG (success) or ERROR (failure)
     * level using {@link #TAG}.
     *
     * @param context The {@link Context} used to resolve the device ID and session role.
     */
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

    /**
     * Persists the user's full profile to Firestore and updates the in-memory
     * {@link AppCache} on success.
     *
     * <p>The following fields are written: {@code deviceId}, {@code role}, {@code name},
     * {@code email}, and {@code phoneNumber}. The role is derived from the current
     * session via {@link WelcomeActivity#getSessionRole(Context)} and stored in
     * lower-case.
     *
     * <p>{@code onSuccess} is invoked on the main thread after both the Firestore write
     * and the cache update complete. {@code onFailure} is invoked if the Firestore write
     * fails; either callback may be {@code null} to ignore the respective outcome.
     *
     * @param context      The {@link Context} used to resolve the device ID and session role.
     * @param name         The display name to save.
     * @param email        The email address to save.
     * @param phoneNumber  The phone number to save (may be empty).
     * @param notificationsEnabled Whether organizer/admin notifications are enabled.
     * @param onSuccess    {@link Runnable} invoked after a successful save, or {@code null}.
     * @param onFailure    {@link ErrorCallback} invoked on failure, or {@code null}.
     */
    public static void saveUserProfile(Context context,
                                       String name,
                                       String email,
                                       String phoneNumber,
                                       boolean notificationsEnabled,
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
        update.put(NotificationPreferenceHelper.FIELD_NOTIFICATIONS_ENABLED, notificationsEnabled);

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
                    user.setNotificationsEnabled(notificationsEnabled);

                    // Update the in-memory cache so callers get fresh data immediately.
                    AppCache.getInstance().setCachedUser(user);

                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) onFailure.onError(e);
                });
    }

    /**
     * Loads the current device's user profile from Firestore and updates
     * {@link AppCache} with the result.
     *
     * <p>The Firestore document is mapped to a {@link User} via
     * {@link #mapDocumentToUser(DocumentSnapshot, String)}. If the document does not
     * exist, a default {@link User} (role {@code "entrant"}, all other fields empty) is
     * returned and cached.
     *
     * @param context  The {@link Context} used to resolve the device ID.
     * @param callback The {@link UserCallback} to receive the loaded {@link User} or an error.
     */
    public static void loadUserProfile(Context context, @NonNull UserCallback callback) {
        String deviceId = DeviceIdManager.getOrCreateDeviceId(context);

        AppDatabase.getInstance()
                .usersRef
                .document(deviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = mapDocumentToUser(documentSnapshot, deviceId);

                    // Cache the result for synchronous access.
                    AppCache.getInstance().setCachedUser(user);

                    callback.onUserLoaded(user);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Loads every saved profile from Firestore for administrator browsing screens.
     *
     * <p>Each document is mapped through the same normalization path used for the
     * current-device profile so missing optional fields fall back to safe defaults.</p>
     *
     * @param callback receives the loaded profiles or an error
     */
    public static void loadAllProfiles(@NonNull ProfilesCallback callback) {
        AppDatabase.getInstance()
                .usersRef
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.List<User> users = new java.util.ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                        users.add(mapDocumentToUser(documentSnapshot, documentSnapshot.getId()));
                    }
                    callback.onProfilesLoaded(users);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Maps a Firestore {@link DocumentSnapshot} to a {@link User} object.
     *
     * <p>If the document exists, each profile field is read and applied to the
     * {@link User}; {@code null} values fall back to safe defaults ({@code "entrant"}
     * for role, empty strings for all other fields). If the document does not exist,
     * the {@link User} constructed by {@link User#User(String)} is returned unchanged.
     *
     * @param documentSnapshot The Firestore document to map.
     * @param deviceId         The device ID to assign to the {@link User}.
     * @return A fully initialised {@link User}; never {@code null}.
     */
    private static User mapDocumentToUser(DocumentSnapshot documentSnapshot, String deviceId) {
        User user = new User(deviceId);

        if (documentSnapshot.exists()) {
            user.setDeviceId(deviceId);

            String role        = documentSnapshot.getString("role");
            String name        = documentSnapshot.getString("name");
            String email       = documentSnapshot.getString("email");
            String phoneNumber = documentSnapshot.getString("phoneNumber");
            Boolean notificationsEnabled =
                    documentSnapshot.getBoolean(NotificationPreferenceHelper.FIELD_NOTIFICATIONS_ENABLED);

            user.setRole(role != null ? role : "entrant");
            user.setName(name != null ? name : "");
            user.setEmail(email != null ? email : "");
            user.setPhoneNumber(phoneNumber != null ? phoneNumber : "");
            user.setNotificationsEnabled(
                    NotificationPreferenceHelper.isNotificationsEnabled(notificationsEnabled));
        }

        return user;
    }

    /**
     * Deletes the current user's profile and removes the device from any related
     * event/notification records that would otherwise keep the account active.
     *
     * <p>The deletion batch performs three cleanup tasks in one commit:</p>
     * <ul>
     *     <li>Delete every stored notification under
     *         {@code notifications/{deviceId}/messages}.</li>
     *     <li>Remove the device ID from all event entrant/co-organizer arrays.</li>
     *     <li>Delete the user document itself.</li>
     * </ul>
     *
     * <p>This keeps event rosters and notification collections consistent after an
     * entrant chooses to leave the app.</p>
     *
     * @param context   The {@link Context} used to resolve the current device ID.
     * @param onSuccess {@link Runnable} invoked after a successful delete, or {@code null}.
     * @param onFailure {@link ErrorCallback} invoked on failure, or {@code null}.
     */
    public static void deleteUserProfile(Context context,
                                         Runnable onSuccess,
                                         ErrorCallback onFailure) {
        String deviceId = DeviceIdManager.getOrCreateDeviceId(context);

        com.google.android.gms.tasks.Task<QuerySnapshot> eventsTask =
                AppDatabase.getInstance().eventsRef.get();
        com.google.android.gms.tasks.Task<QuerySnapshot> notificationsTask =
                AppDatabase.getInstance()
                        .notificationsRef
                        .document(deviceId)
                        .collection("messages")
                        .get();

        Tasks.whenAllSuccess(eventsTask, notificationsTask)
                .addOnSuccessListener(results -> {
                    QuerySnapshot eventsSnapshot = (QuerySnapshot) results.get(0);
                    QuerySnapshot notificationsSnapshot = (QuerySnapshot) results.get(1);

                    WriteBatch batch = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch();

                    for (DocumentSnapshot doc : eventsSnapshot.getDocuments()) {
                        Event event = EventSchema.normalizeLoadedEvent(doc);
                        if (event == null || !containsDevice(event, deviceId)) {
                            continue;
                        }

                        // Remove the device from every entrant-facing array so deleting the
                        // profile also withdraws the entrant from future event flows.
                        batch.update(doc.getReference(),
                                "waitingList", FieldValue.arrayRemove(deviceId),
                                "selectedEntrants", FieldValue.arrayRemove(deviceId),
                                "enrolledEntrants", FieldValue.arrayRemove(deviceId),
                                "cancelledEntrants", FieldValue.arrayRemove(deviceId),
                                "registeredEntrants", FieldValue.arrayRemove(deviceId),
                                "coOrganizers", FieldValue.arrayRemove(deviceId));
                    }

                    for (DocumentSnapshot doc : notificationsSnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.delete(AppDatabase.getInstance().usersRef.document(deviceId));

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                AppCache.getInstance().clear();
                                if (onSuccess != null) {
                                    onSuccess.run();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (onFailure != null) {
                                    onFailure.onError(e);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) {
                        onFailure.onError(e);
                    }
                });
    }

    private static boolean containsDevice(Event event, String deviceId) {
        return event.getWaitingList().contains(deviceId)
                || event.getSelectedEntrants().contains(deviceId)
                || event.getEnrolledEntrants().contains(deviceId)
                || event.getCancelledEntrants().contains(deviceId)
                || event.getRegisteredEntrants().contains(deviceId)
                || event.getCoOrganizers().contains(deviceId);
    }
}
