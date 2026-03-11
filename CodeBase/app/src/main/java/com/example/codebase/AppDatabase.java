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

    /**
     * Public collection reference for the 'users' collection.
     */
    public final CollectionReference usersRef;

    /**
     * Private constructor to initialize the Firestore instance and collection references.
     */
    private AppDatabase() {
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");
    }

    /**
     * Thread-safe singleton access to the AppDatabase instance.
     *
     * @return The single instance of AppDatabase.
     */
    public static synchronized AppDatabase getInstance() {
        if (instance == null) {
            instance = new AppDatabase();
        }
        return instance;
    }
}
