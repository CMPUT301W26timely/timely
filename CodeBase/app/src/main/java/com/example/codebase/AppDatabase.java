package com.example.codebase;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

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

    public synchronized Task<Void> addSelectedEntrants(Event event, List<String> entrants){
        if (event == null || event.getId() == null || entrants == null || entrants.isEmpty()) {
            return Tasks.forResult((Void) null);
        }

        Log.d("AppDatabase", "Adding selected entrants: " + entrants);

        return db.collection("events")
                .document(event.getId())
                .update("selectedEntrants", FieldValue.arrayUnion(entrants.toArray()));
    }

    public synchronized void deleteWaitingEntrants(Event event, List<String> entrants){

        db.collection("events")
                .document(event.getId())
                .update("waitingList", FieldValue.arrayRemove(entrants.toArray()));
    }

}
