package com.example.codebase;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple singleton memory cache for profile and event data.
 * Avoids fetching from Firestore every time a screen opens.
 */
public class AppCache {

    private static AppCache instance;

    // Cached current user
    private User cachedUser;

    // Cached event list
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