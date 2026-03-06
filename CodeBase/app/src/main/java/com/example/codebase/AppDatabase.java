package com.example.codebase;

import com.google.firebase.Firebase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AppDatabase {

    private static AppDatabase instance;
    private final FirebaseFirestore db;

    public final CollectionReference usersRef;

    private AppDatabase() {
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");
    }

    public static synchronized AppDatabase getInstance() {
        if (instance == null) {
            instance = new AppDatabase();
        }
        return instance;
    }
}
