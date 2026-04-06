package com.example.codebase;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Singleton database management class for Firebase Firestore.
 *
 * Provides a central point of access for all Firestore collection references
 * used throughout the app. All activities and fragments should access
 * Firestore through this class rather than calling
 * {@link FirebaseFirestore#getInstance()} directly, ensuring consistent
 * collection naming and a single shared connection.
 */
public class AppDatabase {

    /** Single shared instance of AppDatabase. */
    private static AppDatabase instance;

    /** The underlying Firestore database connection. */
    private final FirebaseFirestore db;

    /**
     * Reference to the {@code users} Firestore collection.
     * Each document is keyed by the device ID of the user.
     */
    public final CollectionReference usersRef;

    /**
     * Reference to the {@code events} Firestore collection.
     * Each document represents a single event created by an organizer.
     */
    public final CollectionReference eventsRef;

    /**
     * Reference to the {@code notifications} Firestore collection.
     * Structured as {@code notifications/{deviceId}/messages/{autoId}}.
     * Each sub-document represents a notification sent to an entrant.
     */
    public final CollectionReference notificationsRef;

    /** Reference to admin-visible audit logs of organizer-sent notifications. */
    public final CollectionReference notificationLogsRef;

    /**
     * Private constructor — initialises the Firestore connection and
     * all collection references.
     * Use {@link #getInstance()} to obtain the singleton instance.
     */
    private AppDatabase() {
        db = FirebaseFirestore.getInstance();
        usersRef          = db.collection("users");
        eventsRef         = db.collection("events");
        notificationsRef  = db.collection("notifications");
        notificationLogsRef = db.collection("notificationLogs");
    }

    /**
     * Returns the singleton instance of AppDatabase.
     * Creates a new instance if one does not already exist.
     * Thread-safe via synchronization.
     *
     * @return the shared {@link AppDatabase} instance
     */
    public static synchronized AppDatabase getInstance() {
        if (instance == null) {
            instance = new AppDatabase();
        }
        return instance;
    }
}
