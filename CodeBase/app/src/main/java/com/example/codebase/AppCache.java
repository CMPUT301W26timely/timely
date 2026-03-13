package com.example.codebase;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple singleton memory cache for profile and event data.
 *
 * Avoids fetching from Firestore every time a screen opens by storing
 * the current user and event list in memory for the duration of the
 * app session. Cache is cleared on logout or explicit {@link #clear()} call.
 */
public class AppCache {

    /** Single shared instance of AppCache. */
    private static AppCache instance;

    /** The currently authenticated user stored in memory. */
    private User cachedUser;

    /** The list of events stored in memory. */
    private List<Event> cachedEvents;

    /**
     * Private constructor — initialises an empty event list.
     * Use {@link #getInstance()} to obtain the singleton instance.
     */
    private AppCache() {
        cachedEvents = new ArrayList<>();
    }

    /**
     * Returns the singleton instance of AppCache.
     * Creates a new instance if one does not already exist.
     * Thread-safe via synchronization.
     *
     * @return the shared {@link AppCache} instance
     */
    public static synchronized AppCache getInstance() {
        if (instance == null) {
            instance = new AppCache();
        }
        return instance;
    }

    /**
     * Returns the currently cached user.
     *
     * @return the cached {@link User}, or {@code null} if not set
     */
    public User getCachedUser() {
        return cachedUser;
    }

    /**
     * Stores a user in the cache.
     *
     * @param cachedUser the {@link User} to cache
     */
    public void setCachedUser(User cachedUser) {
        this.cachedUser = cachedUser;
    }

    /**
     * Returns the currently cached list of events.
     *
     * @return list of cached {@link Event} objects, never {@code null}
     */
    public List<Event> getCachedEvents() {
        return cachedEvents;
    }

    /**
     * Stores a list of events in the cache.
     * If {@code null} is passed, an empty list is stored instead.
     *
     * @param cachedEvents the list of {@link Event} objects to cache
     */
    public void setCachedEvents(List<Event> cachedEvents) {
        this.cachedEvents = cachedEvents != null ? cachedEvents : new ArrayList<>();
    }

    /**
     * Returns whether a user is currently stored in the cache.
     *
     * @return {@code true} if a cached user exists, {@code false} otherwise
     */
    public boolean hasCachedUser() {
        return cachedUser != null;
    }

    /**
     * Returns whether any events are currently stored in the cache.
     *
     * @return {@code true} if the cached event list is non-null and non-empty,
     *         {@code false} otherwise
     */
    public boolean hasCachedEvents() {
        return cachedEvents != null && !cachedEvents.isEmpty();
    }

    /**
     * Clears all cached data.
     * Sets the cached user to {@code null} and resets the event list to empty.
     * Should be called on user logout or session end.
     */
    public void clear() {
        cachedUser = null;
        cachedEvents = new ArrayList<>();
    }
}