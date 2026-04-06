package com.example.codebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView adapter for the Notifications screen.
 *
 * <p>Each row shows a status pill and message body. For notifications with
 * {@code type == "privateInvite"} (US 01.05.07), an inline Accept / Decline
 * button row is shown so the entrant can respond to a private waiting-list
 * invitation without leaving the notifications screen.
 *
 * <p>Replaces the previous {@link android.widget.SimpleAdapter}-based
 * implementation which could not support per-row interactive buttons.
 */
public class NotificationListAdapter extends
        RecyclerView.Adapter<NotificationListAdapter.NotificationViewHolder> {

    // ── Callback interfaces ───────────────────────────────────────────────────

    /**
     * Called when the entrant taps a notification row to view event details.
     */
    public interface OnItemClickListener {
        void onItemClick(NotificationItem item);
    }

    /**
     * Called when the entrant taps Accept or Decline on a {@code privateInvite} row.
     * US 01.05.07.
     */
    public interface OnPrivateInviteActionListener {
        void onAccept(NotificationItem item);
        void onDecline(NotificationItem item);
    }

    // ── Data model ────────────────────────────────────────────────────────────

    /**
     * Represents a single notification row.
     */
    public static class NotificationItem {
        /** Status label shown in the pill (e.g. "Selected", "Private Invite"). */
        public final String status;
        /** Message body text. */
        public final String message;
        /** Firestore event document ID. */
        public final String eventId;
        /**
         * Notification type. {@code "privateInvite"} shows Accept / Decline buttons.
         * All other values show a standard chevron row.
         */
        public final String type;

        public NotificationItem(String status, String message, String eventId, String type) {
            this.status  = status;
            this.message = message;
            this.eventId = eventId;
            this.type    = type;
        }
    }

    // ── Adapter fields ────────────────────────────────────────────────────────

    private List<NotificationItem> items;
    private final OnItemClickListener itemClickListener;
    private final OnPrivateInviteActionListener privateInviteListener;

    /**
     * Constructs a new adapter.
     *
     * @param items                 Initial notification rows.
     * @param itemClickListener     Row tap callback (navigation to event detail).
     * @param privateInviteListener Accept / Decline callback for privateInvite rows.
     */
    public NotificationListAdapter(
            List<NotificationItem> items,
            OnItemClickListener itemClickListener,
            OnPrivateInviteActionListener privateInviteListener) {
        this.items                  = items;
        this.itemClickListener      = itemClickListener;
        this.privateInviteListener  = privateInviteListener;
    }

    /** Replaces the data set and refreshes the list. */
    public void updateItems(List<NotificationItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    // ── RecyclerView.Adapter ──────────────────────────────────────────────────

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(items.get(position), itemClickListener, privateInviteListener);
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        private final TextView   tvStatus;
        private final TextView   tvMessage;
        private final ImageView  ivChevron;
        private final View       layoutPrivateActions;
        private final Button     btnAccept;
        private final Button     btnDecline;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus             = itemView.findViewById(R.id.tvNotificationStatus);
            tvMessage            = itemView.findViewById(R.id.tvNotificationMessage);
            ivChevron            = itemView.findViewById(R.id.ivNotifChevron);
            layoutPrivateActions = itemView.findViewById(R.id.layoutNotifPrivateActions);
            btnAccept            = itemView.findViewById(R.id.btnNotifAccept);
            btnDecline           = itemView.findViewById(R.id.btnNotifDecline);
        }

        /**
         * Binds a {@link NotificationItem} to this row.
         *
         * <p>For {@code privateInvite} type: shows Accept/Decline buttons, hides chevron.
         * For all other types: shows chevron, hides buttons.
         */
        void bind(
                NotificationItem item,
                OnItemClickListener itemClickListener,
                OnPrivateInviteActionListener privateInviteListener) {

            tvStatus.setText(item.status);
            tvMessage.setText(item.message);

            if ("privateInvite".equals(item.type)) {
                // US 01.05.07 — show inline Accept / Decline
                layoutPrivateActions.setVisibility(View.VISIBLE);
                ivChevron.setVisibility(View.GONE);

                if (privateInviteListener != null) {
                    btnAccept.setOnClickListener(v  -> privateInviteListener.onAccept(item));
                    btnDecline.setOnClickListener(v -> privateInviteListener.onDecline(item));
                }
            } else {
                layoutPrivateActions.setVisibility(View.GONE);
                ivChevron.setVisibility(View.VISIBLE);
            }

            // Row tap always navigates to event detail
            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) itemClickListener.onItemClick(item);
            });
        }
    }
}