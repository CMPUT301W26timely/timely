package com.example.codebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * ExploreEventAdapter — RecyclerView adapter for the ExplorePageActivity.
 * Displays invitations to an Entrant and handles Accept/Decline actions.
 */
public class ExploreEventAdapter extends RecyclerView.Adapter<ExploreEventAdapter.ExploreViewHolder> {

    // Interface to pass button clicks back to the Activity
    public interface OnInvitationActionListener {
        void onAcceptClick(Event event);
        void onDeclineClick(Event event);
        void onCardClick(Event event);
    }

    private final List<Event> invitationList;
    private final OnInvitationActionListener listener;

    public ExploreEventAdapter(List<Event> invitationList, OnInvitationActionListener listener) {
        this.invitationList = invitationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExploreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_explore_event, parent, false);
        return new ExploreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExploreViewHolder holder, int position) {
        Event event = invitationList.get(position);

        // Bind data to the views (Assume Event has these getters based on EventDetailActivity)
        holder.tvEventTitle.setText(event.getTitle() != null ? event.getTitle() : "Untitled Event");

        // Handle Action Buttons
        holder.btnAccept.setOnClickListener(v -> listener.onAcceptClick(event));
        holder.btnDecline.setOnClickListener(v -> listener.onDeclineClick(event));

        // Handle clicking the whole card to see details
        holder.itemView.setOnClickListener(v -> listener.onCardClick(event));
    }

    @Override
    public int getItemCount() {
        return invitationList.size();
    }

    static class ExploreViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTitle;
        Button btnAccept;
        Button btnDecline;

        public ExploreViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvExploreEventTitle);
            btnAccept = itemView.findViewById(R.id.btnExploreAccept);
            btnDecline = itemView.findViewById(R.id.btnExploreDecline);
        }
    }
}
