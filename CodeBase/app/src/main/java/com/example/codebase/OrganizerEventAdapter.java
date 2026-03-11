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
        void onEventClick(OrganizerActivity.OrganizerEvent event);
    }

    private final List<OrganizerActivity.OrganizerEvent> events;
    private final OnEventClickListener                   listener;

    public OrganizerEventAdapter(
            List<OrganizerActivity.OrganizerEvent> events,
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

    private static String calculateStatus(OrganizerActivity.OrganizerEvent event) {
        // If any date is null → Draft (event not fully set up)
        if (event.registrationDeadline == null || event.drawDate == null
                || event.startDate == null || event.endDate == null) {
            return "Draft";
        }

        Date today             = new Date();
        List<?> selectedEntrants = event.selectedEntrants;
        List<?> enrolledEntrants = event.enrolledEntrants;

        boolean selectedEmpty  = selectedEntrants == null || selectedEntrants.isEmpty();
        boolean enrolledEmpty  = enrolledEntrants == null || enrolledEntrants.isEmpty();

        if (!today.after(event.registrationDeadline)) {
            // today <= registrationDeadline
            return "Registration Open";

        } else if (today.after(event.registrationDeadline)
                && !today.after(event.drawDate)
                && selectedEmpty) {
            // today > registrationDeadline AND today <= drawDate AND no winners yet
            return "Lottery Pending";

        } else if (today.after(event.drawDate)
                && today.before(event.startDate)
                && !selectedEmpty) {
            // today > drawDate AND today < startDate AND winners selected
            return "Lottery Closed & Event Scheduled";

        } else if (!today.before(event.startDate)
                && !today.after(event.endDate)
                && !enrolledEmpty) {
            // today >= startDate AND today <= endDate AND people enrolled
            return "In Progress";

        } else if (today.after(event.endDate)) {
            return "Event Ended";

        } else {
            return "Draft";
        }
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    static class EventViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvDate;
        private final TextView tvStatus;
        private final TextView tvWaiting;
        private final TextView tvSelected;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tvEventItemTitle);
            tvDate     = itemView.findViewById(R.id.tvEventItemDate);
            tvStatus   = itemView.findViewById(R.id.tvEventStatus);
            tvWaiting  = itemView.findViewById(R.id.tvWaitingCount);
            tvSelected = itemView.findViewById(R.id.tvSelectedCount);
        }

        void bind(OrganizerActivity.OrganizerEvent event, OnEventClickListener listener) {
            tvTitle.setText(event.title);
            tvDate.setText(event.displayDate);

            tvWaiting.setText(itemView.getContext()
                    .getString(R.string.waitlist_count, event.waitingCount));
            tvSelected.setText(itemView.getContext()
                    .getString(R.string.drawn_count, event.selectedCount));
            tvSelected.setVisibility(View.VISIBLE);

            // ── Calculate and apply status badge ──────────────────────────────
            String status = calculateStatus(event);
            tvStatus.setText(status);

            switch (status) {
                case "Registration Open":
                    tvStatus.setBackgroundResource(R.drawable.bg_status_open);
                    tvStatus.setTextColor(0xFF1DB954);
                    break;
                case "Lottery Pending":
                    tvStatus.setBackgroundResource(R.drawable.bg_status_draft);
                    tvStatus.setTextColor(0xFFFFD700);
                    break;
                case "Lottery Closed & Event Scheduled":
                    tvStatus.setBackgroundResource(R.drawable.bg_status_closed);
                    tvStatus.setTextColor(0xFFFF8C00);
                    break;
                case "In Progress":
                    tvStatus.setBackgroundResource(R.drawable.bg_status_open);
                    tvStatus.setTextColor(0xFF1DB954);
                    break;
                case "Event Ended":
                    tvStatus.setBackgroundResource(R.drawable.bg_status_closed);
                    tvStatus.setTextColor(0xFFAAAAAA);
                    break;
                default: // Draft
                    tvStatus.setBackgroundResource(R.drawable.bg_status_draft);
                    tvStatus.setTextColor(0xFF8899AA);
                    break;
            }

            itemView.setOnClickListener(v -> listener.onEventClick(event));
        }
    }
}