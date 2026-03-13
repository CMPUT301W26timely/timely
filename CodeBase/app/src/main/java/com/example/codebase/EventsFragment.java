package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * A {@link Fragment} that displays a browsable list of all active events available to entrants.
 *
 * <p>Satisfies user story <b>US 01.01.03</b>.</p>
 *
 * <p>This fragment implements a cache-first loading strategy:</p>
 * <ol>
 *     <li>If {@link AppCache} holds a previously fetched event list, it is displayed
 *         immediately to minimize perceived load time.</li>
 *     <li>A fresh fetch from Firestore via {@link EventRepository} is then triggered
 *         in the background regardless, ensuring the displayed list stays up to date.</li>
 *     <li>If no cache is available, only the Firestore fetch is performed.</li>
 * </ol>
 *
 * <p>Selecting an event navigates the user to {@link EventDetailsActivity},
 * passing the event's ID as an {@link Intent} extra under the key {@code "eventId"}.</p>
 */
public class EventsFragment extends Fragment {

    /** Displays the list of active events. Hidden when the event list is empty. */
    private RecyclerView recyclerViewEvents;

    /** Shown in place of the {@link RecyclerView} when there are no active events to display. */
    private TextView textViewEmptyState;

    /**
     * Inflates the fragment layout.
     *
     * @param inflater           the {@link LayoutInflater} used to inflate the fragment's view
     * @param container          the parent {@link ViewGroup} the fragment UI will be attached to,
     *                           or {@code null} if there is no parent
     * @param savedInstanceState a {@link Bundle} containing the fragment's previously saved state,
     *                           or {@code null} if this is a fresh creation
     * @return the inflated {@link View} for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    /**
     * Binds views and initiates the cache-first event loading strategy.
     *
     * <p>If {@link AppCache} contains a previously loaded event list, it is rendered
     * immediately via {@link #showEvents(List)} before a background refresh is triggered.
     * If the cache is empty, only {@link #refreshEvents()} is called.</p>
     *
     * @param view               the fully inflated {@link View} returned by
     *                           {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param savedInstanceState a {@link Bundle} containing the fragment's previously saved state,
     *                           or {@code null} if this is a fresh creation
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerViewEvents = view.findViewById(R.id.recyclerViewEvents);
        textViewEmptyState = view.findViewById(R.id.textViewEmptyState);

        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (AppCache.getInstance().hasCachedEvents()) {
            showEvents(AppCache.getInstance().getCachedEvents());
            refreshEvents();
        } else {
            refreshEvents();
        }
    }

    /**
     * Fetches the current list of active events from Firestore via {@link EventRepository}
     * and updates the UI on completion.
     *
     * <p>On success, the result is passed to {@link #showEvents(List)} to update
     * the {@link RecyclerView}. On failure, a {@link Toast} is shown with the error message.</p>
     */
    private void refreshEvents() {
        EventRepository.loadActiveEvents(new EventRepository.EventsCallback() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                showEvents(events);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(),
                        "Failed to load events: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Renders the provided list of events in the {@link RecyclerView}, or shows an
     * empty state view if the list is {@code null} or empty.
     *
     * <p>When events are present, an {@link EventAdapter} is created with an item click
     * listener that launches {@link EventDetailsActivity}, passing the selected event's
     * ID as an {@link Intent} extra under the key {@code "eventId"}.</p>
     *
     * @param events the list of {@link Event} objects to display; may be {@code null} or empty
     */
    private void showEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            textViewEmptyState.setVisibility(View.VISIBLE);
            recyclerViewEvents.setVisibility(View.GONE);
        } else {
            textViewEmptyState.setVisibility(View.GONE);
            recyclerViewEvents.setVisibility(View.VISIBLE);

            EventAdapter adapter = new EventAdapter(events, event -> {
                Intent intent = new Intent(requireContext(), EventDetailsActivity.class);
                intent.putExtra("eventId", event.getEventId());
                startActivity(intent);
            });

            recyclerViewEvents.setAdapter(adapter);
        }
    }
}