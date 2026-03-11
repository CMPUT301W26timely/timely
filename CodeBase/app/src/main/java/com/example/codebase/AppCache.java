package com.example.codebase;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple in-memory cache for frequently used app data.
 * This avoids waiting on Firestore every time a screen opens.
 */
public class AppCache {

    private static AppCache instance;

    private User cachedUser;
    private List<Event> cachedEvents;

    private AppCache() {
        cachedEvents = new ArrayList<>();
    }

    public static synchronized AppCache getInstance() {
        if (instance == null) {
            instance = new AppCache();
        }
        return instance;
    }

    public User getCachedUser() {
        return cachedUser;
    }

    public void setCachedUser(User cachedUser) {
        this.cachedUser = cachedUser;
    }

    public List<Event> getCachedEvents() {
        return cachedEvents;
    }

    public void setCachedEvents(List<Event> cachedEvents) {
        this.cachedEvents = cachedEvents != null ? cachedEvents : new ArrayList<>();
    }

    public boolean hasCachedUser() {
        return cachedUser != null;
    }

    public boolean hasCachedEvents() {
        return cachedEvents != null && !cachedEvents.isEmpty();
    }

    public void clear() {
        cachedUser = null;
        cachedEvents = new ArrayList<>();
    }
}