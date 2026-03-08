package com.example.codebase;

import android.app.Application;

/**
 * Main application class for Timely.
 * Initializes the application-wide database instance.
 */
public class MyApp extends Application {
    /**
     * Called when the application is starting, before any activity, service,
     * or receiver objects (excluding content providers) have been created.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // Warm up the database singleton
    }
}