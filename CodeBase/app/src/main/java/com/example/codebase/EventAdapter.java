package com.example.codebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
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

        holder.textViewEventName.setText(resolveTitle(holder, event));
        holder.textViewEventLocation.setText(resolveLocation(holder, event));
        holder.textViewEventDate.setText(resolveDateRange(holder, event));
        bindPoster(holder, event);
        bindStatus(holder, event);
        bindPrivacy(holder, event);

        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    private String resolveTitle(EventViewHolder holder, Event event) {
        String title = event.getTitle();
        return title == null || title.trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.untitled_event)
                : title.trim();
    }

    private String resolveLocation(EventViewHolder holder, Event event) {
        String location = event.getLocation();
        return location == null || location.trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.history_location_not_set)
                : location.trim();
    }

    private String resolveDateRange(EventViewHolder holder, Event event) {
        Date startDate = event.getStartDate();
        Date endDate = event.getEndDate();

        if (startDate == null && endDate == null) {
            return holder.itemView.getContext().getString(R.string.date_not_set);
        }
        if (startDate != null && endDate != null) {
            String start = dateFormat.format(startDate);
            String end = dateFormat.format(endDate);
            return start.equals(end) ? start : start + " - " + end;
        }
        Date onlyDate = startDate != null ? startDate : endDate;
        return dateFormat.format(onlyDate);
    }

    private void bindPoster(EventViewHolder holder, Event event) {
        if (event.getPoster() != null
                && event.getPoster().getPosterImageBase64() != null
                && !event.getPoster().getPosterImageBase64().isEmpty()) {
            try {
                holder.imageViewThumbnail.setImageBitmap(
                        EventPoster.decodeImage(event.getPoster().getPosterImageBase64())
                );
            } catch (Exception e) {
                holder.imageViewThumbnail.setImageBitmap(null);
            }
        } else {
            holder.imageViewThumbnail.setImageBitmap(null);
        }
    }

    private void bindStatus(EventViewHolder holder, Event event) {
        String status = calculateStatus(event);
        holder.textViewEventStatus.setText(status);

        switch (status) {
            case "Registration Opening Soon":
            case "Registration Closed / Lottery Opening Soon":
            case "Lottery Closed & Event Scheduled":
            case "Draft":
                holder.textViewEventStatus.setBackgroundResource(R.drawable.bg_pill_amber);
                holder.textViewEventStatus.setTextColor(0xFF8B7A2A);
                break;
            case "Registration Open":
            case "In Progress":
                holder.textViewEventStatus.setBackgroundResource(R.drawable.bg_pill_green);
                holder.textViewEventStatus.setTextColor(0xFF4A7A4A);
                break;
            default:
                holder.textViewEventStatus.setBackgroundResource(R.drawable.bg_pill_red);
                holder.textViewEventStatus.setTextColor(0xFF8B3A3A);
                break;
        }
    }

    private void bindPrivacy(EventViewHolder holder, Event event) {
        if (event.isPrivate()) {
            holder.textViewEventPrivacy.setText("Private");
            holder.textViewEventPrivacy.setBackgroundResource(R.drawable.bg_pill_amber);
            holder.textViewEventPrivacy.setTextColor(0xFF8B7A2A);
        } else {
            holder.textViewEventPrivacy.setText("Public");
            holder.textViewEventPrivacy.setBackgroundResource(R.drawable.bg_pill_green);
            holder.textViewEventPrivacy.setTextColor(0xFF4A7A4A);
        }
    }

    private String calculateStatus(Event event) {
        if (event.getRegistrationOpen() == null || event.getRegistrationDeadline() == null
                || event.getDrawDate() == null
                || event.getStartDate() == null || event.getEndDate() == null) {
            return "Draft";
        }

        Date today = new Date();
        List<String> invitedEntrants = EventRosterStatusHelper.invitedEntrants(event);
        List<String> enrolledEntrants = event.getEnrolledEntrants();

        boolean invitedEmpty = invitedEntrants == null || invitedEntrants.isEmpty();
        boolean enrolledEmpty = enrolledEntrants == null || enrolledEntrants.isEmpty();

        if (today.before(event.getRegistrationOpen())) {
            return "Registration Opening Soon";
        } else if (!today.before(event.getRegistrationOpen())
                && !today.after(event.getRegistrationDeadline())) {
            return "Registration Open";
        } else if (today.after(event.getRegistrationDeadline())
                && today.before(event.getDrawDate())
                && invitedEmpty) {
            return "Registration Closed / Lottery Opening Soon";
        } else if (today.after(event.getDrawDate())
                && today.before(event.getStartDate())
                && (!invitedEmpty || !enrolledEmpty)) {
            return "Lottery Closed & Event Scheduled";
        } else if (!today.before(event.getStartDate())
                && !today.after(event.getEndDate())
                && !enrolledEmpty) {
            return "In Progress";
        } else if (today.after(event.getEndDate())) {
            return "Event Ended";
        }
        return "Draft";
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

        /** Displays the status badge. */
        TextView textViewEventStatus;

        /** Displays the privacy badge. */
        TextView textViewEventPrivacy;

        /** Displays the event thumbnail. */
        ImageView imageViewThumbnail;

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
            textViewEventStatus   = itemView.findViewById(R.id.tvEventStatus);
            textViewEventPrivacy  = itemView.findViewById(R.id.tvEventPrivacy);
            imageViewThumbnail    = itemView.findViewById(R.id.ivEventThumbnail);
        }
    }
}
