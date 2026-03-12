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
 * EventsFragment shows all active events entrants can browse.
 * This satisfies US 01.01.03.
 */
public class EventsFragment extends Fragment {

    private RecyclerView recyclerViewEvents;
    private TextView textViewEmptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerViewEvents = view.findViewById(R.id.recyclerViewEvents);
        textViewEmptyState = view.findViewById(R.id.textViewEmptyState);

        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Use cache first for fast load
        if (AppCache.getInstance().hasCachedEvents()) {
            showEvents(AppCache.getInstance().getCachedEvents());
            refreshEvents();
        } else {
            refreshEvents();
        }
    }

    /**
     * Refresh active events from Firestore.
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
     * Display event list in the RecyclerView.
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