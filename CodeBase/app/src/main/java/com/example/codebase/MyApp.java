package com.example.codebase;

import android.app.Application;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Warm up the database singleton
        AppDatabase.getInstance();
    }
}
