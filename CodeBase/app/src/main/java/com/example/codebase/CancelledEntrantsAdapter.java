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
 * CancelledEntrantsAdapter — RecyclerView adapter for the Cancelled Entrants screen.
 *
 * <p>Each row displays:</p>
 * <ul>
 *   <li>Avatar circle with initials derived from the first 2 characters of the device ID</li>
 *   <li>Short form of the device ID — will be replaced with name and email once
 *       the user structure is available</li>
 *   <li>Email placeholder row (currently hidden)</li>
 *   <li>Status badge: {@code "Declined"} or {@code "Cancelled"}</li>
 *   <li>Cancellation date if available</li>
 * </ul>
 *
 * <p>Used by {@link CancelledEntrantsActivity}.</p>
 */
public class CancelledEntrantsAdapter extends
        RecyclerView.Adapter<CancelledEntrantsAdapter.ViewHolder> {

    /** The current list of cancelled entrants being displayed. */
    private List<CancelledEntrantsActivity.CancelledEntrant> list;

    /**
     * Constructs a new CancelledEntrantsAdapter with the given list.
     *
     * @param list the initial list of {@link CancelledEntrantsActivity.CancelledEntrant} to display
     */
    public CancelledEntrantsAdapter(List<CancelledEntrantsActivity.CancelledEntrant> list) {
        this.list = list;
    }

    /**
     * Replaces the current list with a new one and refreshes the RecyclerView.
     *
     * @param newList the updated list of {@link CancelledEntrantsActivity.CancelledEntrant} to display
     */
    public void updateList(List<CancelledEntrantsActivity.CancelledEntrant> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    /**
     * Inflates the item layout and creates a new {@link ViewHolder}.
     *
     * @param parent   the parent {@link ViewGroup} into which the new view will be added
     * @param viewType the view type of the new view (not used — single view type)
     * @return a new {@link ViewHolder} wrapping the inflated item view
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cancelled_entrant, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds data from the entrant at the given position to the {@link ViewHolder}.
     *
     * @param holder   the {@link ViewHolder} to bind data into
     * @param position the index of the item in the list
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    /**
     * Returns the total number of items in the list.
     *
     * @return the size of the entrant list
     */
    @Override
    public int getItemCount() { return list.size(); }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    /**
     * ViewHolder for a single cancelled entrant row.
     * Holds references to all views within {@code item_cancelled_entrant.xml}.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        /** Avatar circle displaying the entrant's initials. */
        private final TextView tvAvatar;

        /** Displays a shortened form of the entrant's device ID. */
        private final TextView tvDeviceId;

        /** Email placeholder — currently hidden until user structure is available. */
        private final TextView tvEmail;

        /** Status badge showing {@code "Declined"} or {@code "Cancelled"}. */
        private final TextView tvStatus;

        /** Displays the cancellation date if available. */
        private final TextView tvDate;

        /**
         * Constructs a ViewHolder and binds all child views.
         *
         * @param itemView the root view of the cancelled entrant list item
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar   = itemView.findViewById(R.id.tvAvatar);
            tvDeviceId = itemView.findViewById(R.id.tvCancelledDeviceId);
            tvEmail    = itemView.findViewById(R.id.tvCancelledEmail);
            tvStatus   = itemView.findViewById(R.id.tvCancelledStatus);
            tvDate     = itemView.findViewById(R.id.tvCancelledDate);
        }

        /**
         * Binds a {@link CancelledEntrantsActivity.CancelledEntrant} to this row.
         *
         * <p>Performs the following:</p>
         * <ul>
         *   <li>Derives initials from the first 2 characters of the device ID</li>
         *   <li>Assigns a deterministic avatar background colour based on the device ID hash</li>
         *   <li>Truncates the device ID for display if longer than 12 characters</li>
         *   <li>Hides the email row (placeholder until user data is available)</li>
         *   <li>Applies a red badge for {@code "Declined"} or grey for {@code "Cancelled"}</li>
         *   <li>Formats and displays the cancellation date if present</li>
         * </ul>
         *
         * @param entrant the {@link CancelledEntrantsActivity.CancelledEntrant} to display
         */
        void bind(CancelledEntrantsActivity.CancelledEntrant entrant) {
            String id = entrant.deviceId;

            // ── Avatar initials (first 2 chars of deviceId, uppercased) ──────
            String initials = id.length() >= 2
                    ? id.substring(0, 2).toUpperCase()
                    : id.toUpperCase();
            tvAvatar.setText(initials);

            // ── Deterministic avatar colour based on deviceId hash ────────────
            int[] avatarColors = {
                    0xFF2A4A6A, 0xFF3A2A5A, 0xFF1A4A3A,
                    0xFF4A2A2A, 0xFF2A3A5A, 0xFF4A3A1A
            };
            int colorIndex = Math.abs(id.hashCode()) % avatarColors.length;
            tvAvatar.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(avatarColors[colorIndex]));

            // ── Short deviceId as name placeholder ───────────────────────────
            String shortId = id.length() > 12
                    ? id.substring(0, 8) + "..." + id.substring(id.length() - 4)
                    : id;
            tvDeviceId.setText(shortId);

            // ── Email placeholder hidden until user structure is known ─────────
            tvEmail.setVisibility(android.view.View.GONE);

            // ── Status badge ──────────────────────────────────────────────────
            if ("Declined".equals(entrant.status)) {
                tvStatus.setText("× Declined");
                tvStatus.setBackgroundResource(R.drawable.bg_pill_red);
                tvStatus.setTextColor(0xFF8B3A3A);
            } else {
                tvStatus.setText("⦸ Cancelled");
                tvStatus.setBackgroundResource(R.drawable.bg_status_draft);
                tvStatus.setTextColor(0xFFAAAAAA);
            }

            // ── Cancellation date ─────────────────────────────────────────────
            if (entrant.cancelledAt != null) {
                String dateStr = new SimpleDateFormat("MMM dd", Locale.getDefault())
                        .format(entrant.cancelledAt);
                tvDate.setText(dateStr);
            } else {
                tvDate.setText("");
            }
        }
    }
}