package com.example.codebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;

/**
 * OrganizerEventAdapter — RecyclerView adapter for My Events screen.
 *
 * Status badge is calculated in real time from Firestore date fields and arrays.
 * Status field in Firestore is NOT used for display.
 *
 * Status logic:
 *   today <= registrationDeadline                              → Registration Open
 *   today > registrationDeadline && today <= drawDate
 *       && selectedEntrants empty                              → Lottery Pending
 *   today > drawDate && today < startDate
 *       && selectedEntrants not empty                          → Lottery Closed
 *   today >= startDate && today <= endDate
 *       && enrolledEntrants not empty                          → Scheduled
 *   today > endDate                                            → Event Ended
 *   any date null or unexpected state                          → Draft
 */
public class OrganizerEventAdapter extends
        RecyclerView.Adapter<OrganizerEventAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final List<Event> events;
    private final OnEventClickListener                   listener;

    public OrganizerEventAdapter(
            List<Event> events,
            OnEventClickListener listener) {
        this.events   = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(events.get(position), listener);
    }

    @Override
    public int getItemCount() { return events.size(); }

    // ─── Status Calculation ───────────────────────────────────────────────────

    private static String calculateStatus(Event event) {
        // If any date is null → Draft (event not fully set up)
        if (event.getRegistrationOpen() == null || event.getRegistrationDeadline() == null
                || event.getDrawDate() == null
                || event.getStartDate() == null || event.getEndDate() == null) {
            return "Draft";
        }

        Date today = new Date();
        List<?> selectedEntrants = event.getSelectedEntrants();
        List<?> enrolledEntrants = event.getEnrolledEntrants();

        boolean selectedEmpty = selectedEntrants == null || selectedEntrants.isEmpty();
        boolean enrolledEmpty = enrolledEntrants == null || enrolledEntrants.isEmpty();

        if (today.before(event.getRegistrationOpen())) {
            // today < regOpen
            return "Registration Opening Soon";

        } else if (!today.before(event.getRegistrationOpen()) && !today.after(event.getRegistrationDeadline())) {
            // today >= regOpen AND today <= regClose
            return "Registration Open";

        } else if (today.after(event.getRegistrationDeadline())
                && today.before(event.getDrawDate())
                && selectedEmpty) {
            // today > regClose AND today < drawDate AND no winners yet
            return "Registration Closed / Lottery Opening Soon";

        } else if (today.after(event.getDrawDate())
                && today.before(event.getStartDate())
                && !selectedEmpty) {
            // today > drawDate AND today < eventStart AND winners selected
            return "Lottery Closed & Event Scheduled";

        } else if (!today.before(event.getStartDate())
                && !today.after(event.getEndDate())
                && !enrolledEmpty) {
            // today >= eventStart AND today <= eventEnd AND people enrolled
            return "In Progress";

        } else if (today.after(event.getEndDate())) {
            return "Event Ended";

        } else {
            return "Draft";
        }
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    static class EventViewHolder extends RecyclerView.ViewHolder {

        private final TextView   tvTitle;
        private final TextView   tvDate;
        private final TextView   tvStatus;
        private final TextView   tvWaiting;
        private final TextView   tvSelected;
        private final android.widget.ImageView ivThumbnail;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle     = itemView.findViewById(R.id.tvEventItemTitle);
            tvDate      = itemView.findViewById(R.id.tvEventItemDate);
            tvStatus    = itemView.findViewById(R.id.tvEventStatus);
            tvWaiting   = itemView.findViewById(R.id.tvWaitingCount);
            tvSelected  = itemView.findViewById(R.id.tvSelectedCount);
            ivThumbnail = itemView.findViewById(R.id.ivEventThumbnail);
        }

        void bind(Event event, OnEventClickListener listener) {
            tvTitle.setText(event.getTitle());
            // Fix 1: guard null startDate
            tvDate.setText(event.getStartDate() != null ? event.getStartDate().toString() : "Date TBD");

            // ── Poster thumbnail ──────────────────────────────────────────────
            if (event.getPoster() != null && event.getPoster().getPosterImageBase64() != null && !event.getPoster().getPosterImageBase64().isEmpty()) {
                try {
                    android.graphics.Bitmap bmp = EventPoster.decodeImage(event.getPoster().getPosterImageBase64());
                    if (bmp != null) {
                        ivThumbnail.setImageBitmap(bmp);
                    } else {
                        ivThumbnail.setImageBitmap(null);
                    }
                } catch (Exception e) {
                    ivThumbnail.setImageBitmap(null);
                }
            } else {
                ivThumbnail.setImageBitmap(null); // shows bg_event_thumbnail placeholder
            }

            // Fix 2: guard null lists
            long waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
            long selectedCount = event.getSelectedEntrants() != null ? event.getSelectedEntrants().size() : 0;
            tvWaiting.setText(itemView.getContext()
                    .getString(R.string.waitlist_count, waitingCount));
            tvSelected.setText(itemView.getContext()
                    .getString(R.string.drawn_count, selectedCount));
            tvSelected.setVisibility(View.VISIBLE);

            // ── Calculate and apply status badge ──────────────────────────────
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