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
 * A {@link RecyclerView.Adapter} for {@code ExplorePageActivity} that displays
 * a list of event invitations to an {@link Entrant} and surfaces accept, decline,
 * and detail-view actions to the hosting component.
 *
 * <p>Each row is inflated from {@code R.layout.item_explore_event} and bound to
 * an {@link Event} from the provided invitation list. User interactions are
 * forwarded to the hosting component via {@link OnInvitationActionListener}
 * rather than handled directly in the adapter, keeping business logic out of
 * the adapter layer.</p>
 *
 * <p>If an event's title is {@code null}, the row displays {@code "Untitled Event"}
 * as a fallback.</p>
 */
public class ExploreEventAdapter extends RecyclerView.Adapter<ExploreEventAdapter.ExploreViewHolder> {

    /**
     * Callback interface for delivering invitation interaction events to the
     * hosting {@link android.app.Activity} or {@link androidx.fragment.app.Fragment}.
     *
     * <p>Implement this interface to respond to user actions on invitation rows
     * without coupling the adapter to any specific business logic.</p>
     */
    public interface OnInvitationActionListener {

        /**
         * Called when the user taps the Accept button on an invitation row.
         *
         * @param event the {@link Event} whose invitation was accepted;
         *              never {@code null}
         */
        void onAcceptClick(Event event);

        /**
         * Called when the user taps the Decline button on an invitation row.
         *
         * @param event the {@link Event} whose invitation was declined;
         *              never {@code null}
         */
        void onDeclineClick(Event event);

        /**
         * Called when the user taps anywhere on an invitation row outside
         * the Accept and Decline buttons, typically to view event details.
         *
         * @param event the {@link Event} whose row was tapped; never {@code null}
         */
        void onCardClick(Event event);
    }

    /** The list of invited {@link Event} objects to display. */
    private final List<Event> invitationList;

    /** Receives interaction callbacks from individual invitation rows. */
    private final OnInvitationActionListener listener;

    /**
     * Constructs an {@code ExploreEventAdapter} with the given invitation list
     * and interaction listener.
     *
     * @param invitationList the list of {@link Event} invitations to display;
     *                       must not be {@code null}
     * @param listener       the {@link OnInvitationActionListener} that will receive
     *                       accept, decline, and card-tap callbacks; must not be {@code null}
     */
    public ExploreEventAdapter(List<Event> invitationList, OnInvitationActionListener listener) {
        this.invitationList = invitationList;
        this.listener = listener;
    }

    /**
     * Inflates a new {@code R.layout.item_explore_event} row view and wraps it
     * in an {@link ExploreViewHolder}.
     *
     * @param parent   the {@link ViewGroup} into which the new view will be added
     * @param viewType the view type of the new view (unused; only one view type exists)
     * @return a new {@link ExploreViewHolder} holding the inflated row view
     */
    @NonNull
    @Override
    public ExploreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_explore_event, parent, false);
        return new ExploreViewHolder(view);
    }

    /**
     * Binds the {@link Event} at the given position to the provided {@link ExploreViewHolder}.
     *
     * <p>Binding behaviour:</p>
     * <ul>
     *     <li><b>Title</b> — set to {@link Event#getTitle()} if non-null,
     *         or {@code "Untitled Event"} as a fallback.</li>
     *     <li><b>Accept button</b> — forwards to
     *         {@link OnInvitationActionListener#onAcceptClick(Event)}.</li>
     *     <li><b>Decline button</b> — forwards to
     *         {@link OnInvitationActionListener#onDeclineClick(Event)}.</li>
     *     <li><b>Row tap</b> — forwards to
     *         {@link OnInvitationActionListener#onCardClick(Event)}.</li>
     * </ul>
     *
     * @param holder   the {@link ExploreViewHolder} to bind data into
     * @param position the index of the {@link Event} in {@link #invitationList}
     */
    @Override
    public void onBindViewHolder(@NonNull ExploreViewHolder holder, int position) {
        Event event = invitationList.get(position);

        holder.tvEventTitle.setText(
                event.getTitle() != null ? event.getTitle() : "Untitled Event"
        );

        holder.btnAccept.setOnClickListener(v -> listener.onAcceptClick(event));
        holder.btnDecline.setOnClickListener(v -> listener.onDeclineClick(event));
        holder.itemView.setOnClickListener(v -> listener.onCardClick(event));
    }

    /**
     * Returns the total number of invitation rows in the list.
     *
     * @return the size of {@link #invitationList}
     */
    @Override
    public int getItemCount() {
        return invitationList.size();
    }

    /**
     * A {@link RecyclerView.ViewHolder} that caches references to the views
     * within a single {@code R.layout.item_explore_event} row.
     */
    static class ExploreViewHolder extends RecyclerView.ViewHolder {

        /** Displays the event title. */
        TextView tvEventTitle;

        /** Button for accepting the event invitation. */
        Button btnAccept;

        /** Button for declining the event invitation. */
        Button btnDecline;

        /**
         * Constructs an {@code ExploreViewHolder} and binds view references
         * from the given row {@link View}.
         *
         * @param itemView the inflated {@code R.layout.item_explore_event} row view;
         *                 must not be {@code null}
         */
        public ExploreViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvExploreEventTitle);
            btnAccept = itemView.findViewById(R.id.btnExploreAccept);
            btnDecline = itemView.findViewById(R.id.btnExploreDecline);
        }
    }
}