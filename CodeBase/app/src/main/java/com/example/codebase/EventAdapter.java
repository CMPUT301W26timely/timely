package com.example.codebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * {@link RecyclerView.Adapter} for displaying a list of {@link Event} objects.
 *
 * <p>Each card shows the event title, location, and start date. Fallback strings
 * ({@code "Untitled Event"}, {@code "Not set"}, {@code "Date not set"}) are used
 * when the corresponding field is {@code null} or empty. Tapping a card invokes
 * {@link OnEventClickListener#onEventClick(Event)}.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    /**
     * Listener interface for item-tap events on the event list.
     */
    public interface OnEventClickListener {
        /**
         * Called when the user taps an event card.
         *
         * @param event The {@link Event} that was tapped.
         */
        void onEventClick(Event event);
    }

    /** The data set backing this adapter. */
    private final List<Event> events;

    /** Listener notified when an event card is tapped. */
    private final OnEventClickListener listener;

    /** Formats event start dates as {@code MMM dd, yyyy} (e.g. {@code Jan 01, 2025}). */
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /**
     * Constructs a new {@code EventAdapter}.
     *
     * @param events   The list of {@link Event} objects to display.
     * @param listener The {@link OnEventClickListener} to notify on item taps.
     */
    public EventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events   = events;
        this.listener = listener;
    }

    /**
     * Inflates the {@code item_event} layout and wraps it in an {@link EventViewHolder}.
     *
     * @param parent   The parent {@link ViewGroup}.
     * @param viewType Unused; only one view type is used.
     * @return A new {@link EventViewHolder}.
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds the {@link Event} at {@code position} to {@code holder}.
     *
     * <p>Fallback display values:
     * <ul>
     *   <li>Title — {@code "Untitled Event"} if {@code null} or empty.</li>
     *   <li>Location — {@code "Not set"} if {@code null} or empty.</li>
     *   <li>Start date — {@code "Date not set"} if {@code null}; otherwise formatted
     *       as {@code MMM dd, yyyy}.</li>
     * </ul>
     *
     * @param holder   The {@link EventViewHolder} to populate.
     * @param position Index into {@link #events}.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.textViewEventName.setText(
                event.getTitle() == null || event.getTitle().isEmpty()
                        ? "Untitled Event"
                        : event.getTitle()
        );

        holder.textViewEventLocation.setText(
                event.getLocation() == null || event.getLocation().isEmpty()
                        ? "Not set"
                        : event.getLocation()
        );

        if (event.getStartDate() != null) {
            holder.textViewEventDate.setText(dateFormat.format(event.getStartDate()));
        } else {
            holder.textViewEventDate.setText("Date not set");
        }

        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    /**
     * Returns the total number of events in the data set.
     *
     * @return Size of {@link #events}.
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Holds and binds the views for a single event card.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {

        /** Displays the event title. */
        TextView textViewEventName;

        /** Displays the event's start date. */
        TextView textViewEventDate;

        /** Displays the event's location. */
        TextView textViewEventLocation;

        /**
         * Constructs an {@link EventViewHolder} and finds all child views.
         *
         * @param itemView The inflated item view.
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewEventName     = itemView.findViewById(R.id.textViewEventName);
            textViewEventDate     = itemView.findViewById(R.id.textViewEventDate);
            textViewEventLocation = itemView.findViewById(R.id.textViewEventLocation);
        }
    }
}