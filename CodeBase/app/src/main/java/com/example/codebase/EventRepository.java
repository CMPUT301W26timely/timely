package com.example.codebase;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Repository class responsible for loading {@link Event} data from Firestore.
 *
 * <p>All queries are executed against {@link AppDatabase#eventsRef}. Successfully loaded
 * event lists are cached in {@link AppCache} so that subsequent lookups by ID can be
 * served without a network round-trip.
 *
 * <p>Malformed Firestore documents are silently skipped to prevent a single bad record
 * from crashing the app.
 */
public class EventRepository {

    /**
     * Callback interface for operations that return a collection of {@link Event} objects.
     */
    public interface EventsCallback {
        /**
         * Called when the event list has been successfully loaded.
         *
         * @param events The list of loaded {@link Event} objects; never {@code null},
         *               but may be empty.
         */
        void onEventsLoaded(List<Event> events);

        /**
         * Called when the load operation fails.
         *
         * @param e The exception describing the failure.
         */
        void onError(Exception e);
    }

    /**
     * Loads every event in the system for administrator browse flows.
     *
     * <p>Unlike {@link #loadActiveEvents(EventsCallback)}, this method does not filter
     * out closed events. Administrators need visibility into the full event catalogue,
     * including past or inactive records.</p>
     *
     * @param callback The {@link EventsCallback} to receive the loaded events or an error.
     */
    public static void loadAllEvents(@NonNull EventsCallback callback) {
        AppDatabase.getInstance()
                .eventsRef
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Event event = EventSchema.normalizeLoadedEvent(doc);
                            if (event != null) {
                                events.add(event);
                            }
                        } catch (Exception ignored) {
                            // Skip malformed event docs instead of crashing the admin browser.
                        }
                    }

                    AppCache.getInstance().setCachedEvents(events);
                    callback.onEventsLoaded(events);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Callback interface for operations that return a single {@link Event}.
     */
    public interface EventDetailsCallback {
        /**
         * Called when the requested {@link Event} has been successfully loaded.
         *
         * @param event The loaded {@link Event}; may be {@code null} if
         *              {@link EventSchema#normalizeLoadedEvent} returns {@code null}.
         */
        void onEventLoaded(Event event);

        /**
         * Called when the load operation fails or the event document does not exist.
         *
         * @param e The exception describing the failure.
         */
        void onError(Exception e);
    }

    /**
     * Loads all active events from Firestore and delivers them via {@code callback}.
     *
     * <p>An event is considered active if its registration deadline is {@code null}
     * (no deadline set) or has not yet passed at the time of the call. Each Firestore
     * document is normalised via {@link EventSchema#normalizeLoadedEvent}; documents
     * that produce a {@code null} result or throw an exception are skipped.
     *
     * <p>The resulting list is stored in {@link AppCache} before being delivered to
     * {@link EventsCallback#onEventsLoaded(List)}.
     *
     * @param callback The {@link EventsCallback} to receive the loaded events or an error.
     */
    public static void loadActiveEvents(@NonNull EventsCallback callback) {
        AppDatabase.getInstance()
                .eventsRef
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Event event = EventSchema.normalizeLoadedEvent(doc);

                            if (event == null) continue;

                            // Private events are not visible on the public event listing (US 02.01.02)
                            if (event.isPrivate()) continue;

                            if (isEventActive(event)) {
                                events.add(event);
                            }

                        } catch (Exception ignored) {
                            // Skip malformed event docs instead of crashing the app.
                        }
                    }

                    AppCache.getInstance().setCachedEvents(events);
                    callback.onEventsLoaded(events);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Determines whether an event is currently active based on its registration deadline.
     *
     * @param event The {@link Event} to evaluate.
     * @return {@code true} if the registration deadline is {@code null} or is in the
     *         future relative to the current time; {@code false} otherwise.
     */
    private static boolean isEventActive(Event event) {
        Date deadline = event.getRegistrationDeadline();
        if (deadline == null) return true;
        return new Date().before(deadline);
    }

    /**
     * Loads a single {@link Event} by its Firestore document ID.
     *
     * <p>The {@link AppCache} is checked first; if a matching event is found it is
     * returned immediately without a network call. Otherwise, the event document is
     * fetched from Firestore and normalised via {@link EventSchema#normalizeLoadedEvent}.
     *
     * <p>Calls {@link EventDetailsCallback#onError(Exception)} if:
     * <ul>
     *   <li>The document does not exist in Firestore.</li>
     *   <li>Normalisation throws an exception.</li>
     *   <li>The Firestore query itself fails.</li>
     * </ul>
     *
     * @param eventId  The Firestore document ID of the event to load.
     * @param callback The {@link EventDetailsCallback} to receive the loaded event or an error.
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
                            Event event = EventSchema.normalizeLoadedEvent(documentSnapshot);
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