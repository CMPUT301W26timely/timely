package com.example.codebase;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Singleton database management class for Firebase Firestore.
 * Provides a central point of access for Firestore collection references.
 */
public class AppDatabase {

    private static AppDatabase instance;
    private final FirebaseFirestore db;

    public final CollectionReference usersRef;
    public final CollectionReference eventsRef;
    public final CollectionReference notificationsRef;

    private AppDatabase() {
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");
        eventsRef = db.collection("events");
        notificationsRef = db.collection("notifications");
    }

    public static synchronized AppDatabase getInstance() {
        if (instance == null) {
            instance = new AppDatabase();
        }
        return instance;
    }
}