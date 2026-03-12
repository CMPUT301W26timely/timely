package com.example.codebase;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * EventRepository loads event data from Firestore.
 */
public class EventRepository {

    public interface EventsCallback {
        void onEventsLoaded(List<Event> events);
        void onError(Exception e);
    }

    public interface EventDetailsCallback {
        void onEventLoaded(Event event);
        void onError(Exception e);
    }

    /**
     * Load all active events from Firestore.
     */
    public static void loadActiveEvents(@NonNull EventsCallback callback) {
        AppDatabase.getInstance()
                .eventsRef
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event.getEventId() == null || event.getEventId().isEmpty()) {
                            event.setEventId(doc.getId());
                        }
                        events.add(event);
                    }

                    // Save events into memory cache
                    AppCache.getInstance().setCachedEvents(events);

                    callback.onEventsLoaded(events);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Load one event by ID.
     */
    public static void loadEventById(String eventId, @NonNull EventDetailsCallback callback) {
        // Check cache first
        for (Event event : AppCache.getInstance().getCachedEvents()) {
            if (eventId.equals(event.getEventId())) {
                callback.onEventLoaded(event);
                return;
            }
        }

        // Fallback to Firestore
        AppDatabase.getInstance()
                .eventsRef
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null && (event.getEventId() == null || event.getEventId().isEmpty())) {
                            event.setEventId(documentSnapshot.getId());
                        }
                        callback.onEventLoaded(event);
                    } else {
                        callback.onError(new Exception("Event not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}