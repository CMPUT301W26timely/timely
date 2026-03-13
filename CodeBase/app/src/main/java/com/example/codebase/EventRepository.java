package com.example.codebase;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
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
     * Load active events from Firestore.
     * For checkpoint, an event is treated as active if registration deadline
     * has not passed, or if no registration deadline is set.
     */
    public static void loadActiveEvents(@NonNull EventsCallback callback) {
        AppDatabase.getInstance()
                .eventsRef
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Event event = doc.toObject(Event.class);

                            if (event == null) continue;

                            if (event.getEventId() == null || event.getEventId().isEmpty()) {
                                event.setEventId(doc.getId());
                            }

                            if (event.getId() == null || event.getId().isEmpty()) {
                                event.setId(doc.getId());
                            }

                            if (isEventActive(event)) {
                                events.add(event);
                            }

                        } catch (Exception ignored) {
                            // Skip malformed event docs instead of crashing the app
                        }
                    }

                    AppCache.getInstance().setCachedEvents(events);
                    callback.onEventsLoaded(events);
                })
                .addOnFailureListener(callback::onError);
    }

    private static boolean isEventActive(Event event) {
        Date deadline = event.getRegistrationDeadline();
        if (deadline == null) return true;
        return new Date().before(deadline);
    }

    /**
     * Load one event by ID.
     */
    public static void loadEventById(String eventId, @NonNull EventDetailsCallback callback) {
        for (Event event : AppCache.getInstance().getCachedEvents()) {
            if (eventId.equals(event.getEventId())) {
                callback.onEventLoaded(event);
                return;
            }
        }

        AppDatabase.getInstance()
                .eventsRef
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            Event event = documentSnapshot.toObject(Event.class);
                            if (event != null) {
                                if (event.getEventId() == null || event.getEventId().isEmpty()) {
                                    event.setEventId(documentSnapshot.getId());
                                }
                                if (event.getId() == null || event.getId().isEmpty()) {
                                    event.setId(documentSnapshot.getId());
                                }
                            }
                            callback.onEventLoaded(event);
                        } catch (Exception e) {
                            callback.onError(e);
                        }
                    } else {
                        callback.onError(new Exception("Event not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}