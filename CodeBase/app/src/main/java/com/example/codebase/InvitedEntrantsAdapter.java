package com.example.codebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} for the Invited Entrants screen.
 *
 * <p>Renders one row per {@link InvitedEntrantsActivity.InvitedEntrant}, showing a
 * shortened device ID, a colour-coded status badge, and — for Pending entrants only —
 * a Cancel button whose enabled state depends on the event's registration deadline
 * (US 02.06.04):
 * <ul>
 *   <li><b>Greyed out / disabled</b> — today is before the registration deadline;
 *       the entrant still has time to respond.</li>
 *   <li><b>Red / enabled</b> — today is on or after the registration deadline;
 *       the organizer may cancel the entrant.</li>
 * </ul>
 *
 * <p>When the Cancel button is tapped the adapter delegates to
 * {@link CancelListener#onCancelRequested(InvitedEntrantsActivity.InvitedEntrant)},
 * allowing the host {@link InvitedEntrantsActivity} to show a confirmation dialog
 * and perform the Firestore update.
 *
 * @see InvitedEntrantsActivity
 * @see InvitedEntrantsActivity.InvitedEntrant
 */
public class InvitedEntrantsAdapter extends
        RecyclerView.Adapter<InvitedEntrantsAdapter.EntrantViewHolder> {

    // ─── Cancel callback interface ────────────────────────────────────────────

    /**
     * Callback interface implemented by the host activity to handle cancel
     * requests initiated from this adapter.
     */
    public interface CancelListener {
        /**
         * Called when the organizer taps the Cancel button for a Pending entrant.
         *
         * @param entrant The {@link InvitedEntrantsActivity.InvitedEntrant} the
         *                organizer wishes to cancel.
         */
        void onCancelRequested(InvitedEntrantsActivity.InvitedEntrant entrant);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** The current list of entrants being displayed. Replaced on tab switches. */
    private List<InvitedEntrantsActivity.InvitedEntrant> list;

    /** Host activity listener notified when a cancel action is requested. */
    private final CancelListener cancelListener;

    /**
     * Registration deadline for the event. Used to determine whether the Cancel
     * button should be enabled. {@code null} until set by the host activity after
     * the Firestore document is loaded.
     */
    private Date registrationDeadlineDate = null;

    /**
     * Constructs the adapter with an initial entrant list and a cancel listener.
     *
     * @param list           Initial list of entrants to display; may be empty.
     * @param cancelListener Host activity that handles cancel confirmations and
     *                       Firestore writes.
     */
    public InvitedEntrantsAdapter(
            List<InvitedEntrantsActivity.InvitedEntrant> list,
            CancelListener cancelListener) {
        this.list           = list;
        this.cancelListener = cancelListener;
    }

    /**
     * Sets the event's registration deadline and triggers a full rebind so that
     * all Cancel button states are updated immediately.
     *
     * <p>Called by the host activity once the Firestore document has been loaded
     * and the deadline date is available.
     *
     * @param registrationDeadlineDate The registration deadline {@link Date}, or
     *                                 {@code null} if the event has no deadline.
     */
    public void setRegistrationDeadlineDate(Date registrationDeadlineDate) {
        this.registrationDeadlineDate = registrationDeadlineDate;
        notifyDataSetChanged();
    }

    /**
     * Replaces the current entrant list and triggers a full rebind.
     *
     * <p>Called by the host activity when the user switches tabs.
     *
     * @param newList The replacement list of entrants to display.
     */
    public void updateList(List<InvitedEntrantsActivity.InvitedEntrant> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    /**
     * Inflates the {@code item_invited_entrant} layout and wraps it in a new
     * {@link EntrantViewHolder}.
     *
     * @param parent   The parent {@link ViewGroup} into which the new view will be added.
     * @param viewType Unused; this adapter has a single view type.
     * @return A new {@link EntrantViewHolder} for the inflated item view.
     */
    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invited_entrant, parent, false);
        return new EntrantViewHolder(view);
    }

    /**
     * Binds the entrant at {@code position} to {@code holder}.
     *
     * @param holder   The {@link EntrantViewHolder} to update.
     * @param position The index of the entrant in {@link #list}.
     */
    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        holder.bind(list.get(position), registrationDeadlineDate, cancelListener);
    }

    /**
     * Returns the number of entrants in the current list.
     *
     * @return Size of {@link #list}.
     */
    @Override
    public int getItemCount() { return list.size(); }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    /**
     * Holds references to the views within a single entrant row and binds data
     * to them.
     */
    static class EntrantViewHolder extends RecyclerView.ViewHolder {

        /** Displays a shortened version of the entrant's device ID. */
        private final TextView tvDeviceId;

        /** Colour-coded badge showing the entrant's status. */
        private final TextView tvStatus;

        /** Cancel button; visible only for Pending entrants. */
        private final Button   btnCancel;

        /**
         * Constructs the ViewHolder and looks up child views by ID.
         *
         * @param itemView The inflated item view for this row.
         */
        EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceId = itemView.findViewById(R.id.tvEntrantDeviceId);
            tvStatus   = itemView.findViewById(R.id.tvEntrantStatus);
            btnCancel  = itemView.findViewById(R.id.btnCancelEntrant);
        }

        /**
         * Binds an {@link InvitedEntrantsActivity.InvitedEntrant} to this row's views.
         *
         * <p>Binding behaviour:
         * <ul>
         *   <li><b>Device ID</b> — truncated to {@code <first 8 chars>...<last 4 chars>}
         *       when longer than 12 characters.</li>
         *   <li><b>Status badge</b> — background and text colour set per status:
         *     <ul>
         *       <li>{@code "Accepted"} → green pill</li>
         *       <li>{@code "Declined"} → red pill</li>
         *       <li>{@code "Pending"}  → amber pill</li>
         *     </ul>
         *   </li>
         *   <li><b>Cancel button</b> — hidden for non-Pending entrants. For Pending:
         *     <ul>
         *       <li>Enabled (full opacity, labelled "Cancel") when today is on or after
         *           {@code registrationDeadlineDate}.</li>
         *       <li>Disabled (40% opacity, labelled "Waiting...") when today is before
         *           the deadline or the deadline is {@code null}.</li>
         *     </ul>
         *   </li>
         * </ul>
         *
         * @param entrant                  The entrant data to display.
         * @param registrationDeadlineDate The event's registration deadline; may be
         *                                 {@code null}.
         * @param cancelListener           Callback invoked when the enabled Cancel
         *                                 button is tapped.
         */
        void bind(
                InvitedEntrantsActivity.InvitedEntrant entrant,
                Date registrationDeadlineDate,
                CancelListener cancelListener) {

            // ── Shortened deviceId ────────────────────────────────────────────
            String id = entrant.deviceId;
            String shortId = id.length() > 12
                    ? id.substring(0, 8) + "..." + id.substring(id.length() - 4)
                    : id;
            tvDeviceId.setText(shortId);

            // ── Status badge ──────────────────────────────────────────────────
            tvStatus.setText(entrant.status);
            switch (entrant.status) {
                case "Accepted":
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_green);
                    tvStatus.setTextColor(0xFF4A7A4A);
                    break;
                case "Declined":
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_red);
                    tvStatus.setTextColor(0xFF8B3A3A);
                    break;
                case "Pending":
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_amber);
                    tvStatus.setTextColor(0xFF8B7A2A);
                    break;
            }

            // ── Cancel button — only for Pending ─────────────────────────────
            if (!entrant.status.equals("Pending")) {
                btnCancel.setVisibility(View.GONE);
                return;
            }

            btnCancel.setVisibility(View.VISIBLE);

            Date today = new Date();
            boolean canCancel = registrationDeadlineDate != null && today.after(registrationDeadlineDate);

            if (canCancel) {
                btnCancel.setEnabled(true);
                btnCancel.setAlpha(1.0f);
                btnCancel.setText("Cancel");
                btnCancel.setOnClickListener(v ->
                        cancelListener.onCancelRequested(entrant));
            } else {
                btnCancel.setEnabled(false);
                btnCancel.setAlpha(0.4f);
                btnCancel.setText("Waiting...");
                btnCancel.setOnClickListener(null);
            }
        }
    }
}