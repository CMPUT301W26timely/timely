package com.example.codebase;

import android.app.Application;

/**
 * The application-level class for the Timely app, serving as the entry point
 * for global initialization logic.
 *
 * <p>Declared in {@code AndroidManifest.xml} via the {@code android:name} attribute
 * on the {@code <application>} tag. Android instantiates this class before any
 * {@link android.app.Activity}, {@link android.app.Service}, or
 * {@link android.content.BroadcastReceiver} in the app process.</p>
 *
 * <p>Currently responsible for eagerly initializing the {@link AppDatabase} singleton
 * so that the first database access on the main thread does not incur cold-start
 * latency.</p>
 */
public class MyApp extends Application {

    /**
     * Called when the application process is starting, before any app components
     * (excluding content providers) have been created.
     *
     * <p>Warms up the {@link AppDatabase} singleton by calling
     * {@link AppDatabase#getInstance()} eagerly. Without this call, the first
     * component to access the database would pay the initialization cost
     * synchronously, which could cause jank if it occurs on the main thread.</p>
     */
    @Override
    public void onCreate() {
        super.onCreate();
        AppDatabase.getInstance();
    }
}