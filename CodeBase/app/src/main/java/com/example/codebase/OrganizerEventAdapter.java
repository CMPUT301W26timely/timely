package com.example.codebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * {@link RecyclerView.Adapter} for the organizer's "My Events" screen.
 *
 * <p>Each item displays the event title, start date, a poster thumbnail, waiting-list
 * and selected-entrant counts, and a colour-coded status badge. The status is derived
 * entirely from the event's date fields and entrant arrays at bind time; the Firestore
 * {@code status} field is intentionally ignored for display purposes.
 *
 * <p>Status badge derivation (evaluated in order):
 * <ol>
 *   <li>Any required date is {@code null} → <b>Draft</b></li>
 *   <li>today &lt; {@code registrationOpen} → <b>Registration Opening Soon</b></li>
 *   <li>today ≥ {@code registrationOpen} AND today ≤ {@code registrationDeadline}
 *       → <b>Registration Open</b></li>
 *   <li>today &gt; {@code registrationDeadline} AND today &lt; {@code drawDate}
 *       AND {@code selectedEntrants} is empty → <b>Registration Closed / Lottery Opening Soon</b></li>
 *   <li>today &gt; {@code drawDate} AND today &lt; {@code startDate}
 *       AND {@code selectedEntrants} not empty → <b>Lottery Closed &amp; Event Scheduled</b></li>
 *   <li>today ≥ {@code startDate} AND today ≤ {@code endDate}
 *       AND {@code enrolledEntrants} not empty → <b>In Progress</b></li>
 *   <li>today &gt; {@code endDate} → <b>Event Ended</b></li>
 *   <li>Any other state → <b>Draft</b></li>
 * </ol>
 */
public class OrganizerEventAdapter extends
        RecyclerView.Adapter<OrganizerEventAdapter.EventViewHolder> {

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

    /**
     * Constructs a new {@code OrganizerEventAdapter}.
     *
     * @param events   The list of {@link Event} objects to display.
     * @param listener The {@link OnEventClickListener} to notify on item taps.
     */
    public OrganizerEventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events   = events;
        this.listener = listener;
    }

    /**
     * Inflates the {@code item_organizer_event} layout and wraps it in an
     * {@link EventViewHolder}.
     *
     * @param parent   The parent {@link ViewGroup}.
     * @param viewType Unused; only one view type is used.
     * @return A new {@link EventViewHolder}.
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_event, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds the {@link Event} at {@code position} to {@code holder}.
     *
     * @param holder   The {@link EventViewHolder} to populate.
     * @param position Index into {@link #events}.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(events.get(position), listener);
    }

    /**
     * Returns the total number of events in the data set.
     *
     * @return Size of {@link #events}.
     */
    @Override
    public int getItemCount() { return events.size(); }

    // ─── Status Calculation ───────────────────────────────────────────────────

    /**
     * Derives the human-readable status label for an event based on its date fields
     * and entrant arrays at the current time.
     *
     * <p>See the class-level Javadoc for the full decision table.
     *
     * @param event The {@link Event} to evaluate.
     * @return A non-null status string such as {@code "Registration Open"},
     *         {@code "In Progress"}, {@code "Event Ended"}, or {@code "Draft"}.
     */
    private static String calculateStatus(Event event) {
        // If any required date is null the event is not fully configured → Draft.
        if (event.getRegistrationOpen() == null || event.getRegistrationDeadline() == null
                || event.getDrawDate() == null
                || event.getStartDate() == null || event.getEndDate() == null) {
            return "Draft";
        }

        Date today = new Date();
        List<String> selectedEntrants = event.getSelectedEntrants();
        List<String> enrolledEntrants = event.getEnrolledEntrants();

        boolean selectedEmpty = selectedEntrants == null || selectedEntrants.isEmpty();
        boolean enrolledEmpty = enrolledEntrants == null || enrolledEntrants.isEmpty();

        if (today.before(event.getRegistrationOpen())) {
            return "Registration Opening Soon";

        } else if (!today.before(event.getRegistrationOpen())
                && !today.after(event.getRegistrationDeadline())) {
            return "Registration Open";

        } else if (today.after(event.getRegistrationDeadline())
                && today.before(event.getDrawDate())
                && selectedEmpty) {
            return "Registration Closed / Lottery Opening Soon";

        } else if (today.after(event.getDrawDate())
                && today.before(event.getStartDate())
                && !selectedEmpty) {
            return "Lottery Closed & Event Scheduled";

        } else if (!today.before(event.getStartDate())
                && !today.after(event.getEndDate())
                && !enrolledEmpty) {
            return "In Progress";

        } else if (today.after(event.getEndDate())) {
            return "Event Ended";

        } else {
            return "Draft";
        }
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    /**
     * Holds and binds the views for a single event card in the organizer list.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {

        /** Date formatter used to display the event's start date as {@code yyyy-MM-dd}. */
        private static final SimpleDateFormat DATE_FORMAT =
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        /** Displays the event title. */
        private final TextView tvTitle;

        /** Displays the event's start date. */
        private final TextView tvDate;

        /** Displays the computed status badge. */
        private final TextView tvStatus;

        /** Displays the number of entrants on the waiting list. */
        private final TextView tvWaiting;

        /** Displays the number of selected (lottery-drawn) entrants. */
        private final TextView tvSelected;

        /** Displays the event poster thumbnail decoded from Base64. */
        private final android.widget.ImageView ivThumbnail;

        /**
         * Constructs an {@link EventViewHolder} and finds all child views.
         *
         * @param itemView The inflated item view.
         */
        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle     = itemView.findViewById(R.id.tvEventItemTitle);
            tvDate      = itemView.findViewById(R.id.tvEventItemDate);
            tvStatus    = itemView.findViewById(R.id.tvEventStatus);
            tvWaiting   = itemView.findViewById(R.id.tvWaitingCount);
            tvSelected  = itemView.findViewById(R.id.tvSelectedCount);
            ivThumbnail = itemView.findViewById(R.id.ivEventThumbnail);
        }

        /**
         * Populates the card views with data from {@code event} and wires the tap listener.
         *
         * <p>Poster thumbnail: decoded from the event's Base64 string via
         * {@link EventPoster#decodeImage(String)}. Falls back to the placeholder drawable
         * ({@code bg_event_thumbnail}) if the poster is absent, blank, or decoding fails.
         *
         * <p>Status badge: computed by {@link #calculateStatus(Event)} and mapped to a
         * background drawable and text colour:
         * <ul>
         *   <li>Green pill — {@code "Registration Open"}, {@code "In Progress"}</li>
         *   <li>Amber pill — {@code "Registration Opening Soon"},
         *       {@code "Registration Closed / Lottery Opening Soon"},
         *       {@code "Lottery Closed & Event Scheduled"}, {@code "Draft"}</li>
         *   <li>Red pill — {@code "Event Ended"}</li>
         * </ul>
         *
         * @param event    The {@link Event} whose data is displayed.
         * @param listener The {@link OnEventClickListener} invoked when the card is tapped.
         */
        void bind(Event event, OnEventClickListener listener) {
            tvTitle.setText(event.getTitle());
            tvDate.setText(event.getStartDate() != null
                    ? DATE_FORMAT.format(event.getStartDate())
                    : "Date: Not set");

            // ── Poster thumbnail ──────────────────────────────────────────────
            if (event.getPoster() != null
                    && event.getPoster().getPosterImageBase64() != null
                    && !event.getPoster().getPosterImageBase64().isEmpty()) {
                try {
                    android.graphics.Bitmap bmp =
                            EventPoster.decodeImage(event.getPoster().getPosterImageBase64());
                    ivThumbnail.setImageBitmap(bmp); // null shows placeholder
                } catch (Exception e) {
                    ivThumbnail.setImageBitmap(null);
                }
            } else {
                ivThumbnail.setImageBitmap(null); // shows bg_event_thumbnail placeholder
            }

            // ── Entrant counts ────────────────────────────────────────────────
            int waitlistCount = event.getWaitingList() != null
                    ? event.getWaitingList().size() : 0;
            int selectedCount = event.getSelectedEntrants() != null
                    ? event.getSelectedEntrants().size() : 0;
            tvWaiting.setText(itemView.getContext()
                    .getString(R.string.waitlist_count, waitlistCount));
            tvSelected.setText(itemView.getContext()
                    .getString(R.string.drawn_count, selectedCount));
            tvSelected.setVisibility(View.VISIBLE);

            // ── Status badge ──────────────────────────────────────────────────
            String status = calculateStatus(event);
            tvStatus.setText(status);

            switch (status) {
                case "Registration Opening Soon":
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_amber);
                    tvStatus.setTextColor(0xFF8B7A2A);
                    break;
                case "Registration Open":
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_green);
                    tvStatus.setTextColor(0xFF4A7A4A);
                    break;
                case "Registration Closed / Lottery Opening Soon":
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_amber);
                    tvStatus.setTextColor(0xFF8B7A2A);
                    break;
                case "Lottery Closed & Event Scheduled":
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_amber);
                    tvStatus.setTextColor(0xFF8B7A2A);
                    break;
                case "In Progress":
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_green);
                    tvStatus.setTextColor(0xFF4A7A4A);
                    break;
                case "Event Ended":
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_red);
                    tvStatus.setTextColor(0xFF8B3A3A);
                    break;
                default: // Draft
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_amber);
                    tvStatus.setTextColor(0xFF8B7A2A);
                    break;
            }

            itemView.setOnClickListener(v -> listener.onEventClick(event));
        }
    }
}