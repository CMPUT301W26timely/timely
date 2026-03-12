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
 * OrganizerEventAdapter — RecyclerView adapter for My Events screen.
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

    private static String calculateStatus(OrganizerActivity.OrganizerEvent event) {
        if ("draft".equalsIgnoreCase(event.status)) {
            return "DRAFT";
        }
        
        Date today = new Date();
        List<?> selectedEntrants = event.selectedEntrants;
        boolean selectedEmpty = selectedEntrants == null || selectedEntrants.isEmpty();

        if (event.registrationDeadline != null && today.before(event.registrationDeadline)) {
            return "OPEN";
        }
        if (event.registrationDeadline != null && today.after(event.registrationDeadline) && selectedEmpty) {
            return "PENDING"; // Waitlist closed, but lottery not yet drawn
        }
        if (event.endDate != null && today.after(event.endDate)) {
            return "ENDED";
        }
        
        return "ACTIVE";
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvDate;
        private final TextView tvStatus;
        private final TextView tvWaiting;
        private final TextView tvMonth;
        private final TextView tvDay;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle   = itemView.findViewById(R.id.tvEventItemTitle);
            tvDate    = itemView.findViewById(R.id.tvEventItemDate);
            tvStatus  = itemView.findViewById(R.id.tvEventStatus);
            tvWaiting = itemView.findViewById(R.id.tvWaitingCount);
            tvMonth   = itemView.findViewById(R.id.tvMonth);
            tvDay     = itemView.findViewById(R.id.tvDay);
        }

        void bind(OrganizerActivity.OrganizerEvent event, OnEventClickListener listener) {
            tvTitle.setText(event.title);
            tvDate.setText(event.location != null && !event.location.isEmpty() ? event.location : "Location TBD");

            if (event.startDate != null) {
                tvMonth.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(event.startDate).toUpperCase());
                tvDay.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(event.startDate));
            } else {
                tvMonth.setText("TBD");
                tvDay.setText("--");
            }

            tvWaiting.setText(event.waitingCount + " entries");

            String status = calculateStatus(event);
            tvStatus.setText(status);

            // Minimal styling for status
            if (status.equals("OPEN") || status.equals("ACTIVE")) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_badge_open);
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.primaryAccent));
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_status_badge_closed);
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.textSecondary));
            }

            itemView.setOnClickListener(v -> listener.onEventClick(event));
        }
    }
}