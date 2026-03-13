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
 * InvitedEntrantsAdapter — RecyclerView adapter for Invited Entrants screen.
 *
 * Cancel button rules (US 02.06.04):
 *   - Only shown for Pending entrants
 *   - GREYED OUT (disabled) if today < registration deadline  → entrant still has time
 *   - ENABLED (red)         if today >= registration deadline → time is up
 *
 * On cancel press → calls CancelListener.onCancelRequested() back to Activity
 * which shows confirmation dialog and updates Firestore.
 */
public class InvitedEntrantsAdapter extends
        RecyclerView.Adapter<InvitedEntrantsAdapter.EntrantViewHolder> {

    // ─── Cancel callback interface ────────────────────────────────────────────

    public interface CancelListener {
        void onCancelRequested(InvitedEntrantsActivity.InvitedEntrant entrant);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private List<InvitedEntrantsActivity.InvitedEntrant> list;
    private final CancelListener cancelListener;

    // Set by Activity after loading Firestore — determines button enabled state
    private Date registrationDeadlineDate = null;

    public InvitedEntrantsAdapter(
            List<InvitedEntrantsActivity.InvitedEntrant> list,
            CancelListener cancelListener) {
        this.list           = list;
        this.cancelListener = cancelListener;
    }

    /** Called by Activity once registration deadline is loaded from Firestore */
    public void setRegistrationDeadlineDate(Date registrationDeadlineDate) {
        this.registrationDeadlineDate = registrationDeadlineDate;
        notifyDataSetChanged();
    }

    public void updateList(List<InvitedEntrantsActivity.InvitedEntrant> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invited_entrant, parent, false);
        return new EntrantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        holder.bind(list.get(position), registrationDeadlineDate, cancelListener);
    }

    @Override
    public int getItemCount() { return list.size(); }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    static class EntrantViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvDeviceId;
        private final TextView tvStatus;
        private final Button   btnCancel;

        EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceId = itemView.findViewById(R.id.tvEntrantDeviceId);
            tvStatus   = itemView.findViewById(R.id.tvEntrantStatus);
            btnCancel  = itemView.findViewById(R.id.btnCancelEntrant);
        }

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
                // ── Enabled: event has started, time is up ────────────────────
                btnCancel.setEnabled(true);
                btnCancel.setAlpha(1.0f);
                btnCancel.setText("Cancel");
                btnCancel.setOnClickListener(v ->
                        cancelListener.onCancelRequested(entrant));
            } else {
                // ── Disabled: entrant still has time to respond ───────────────
                btnCancel.setEnabled(false);
                btnCancel.setAlpha(0.4f);
                btnCancel.setText("Waiting...");
                btnCancel.setOnClickListener(null);
            }
        }
    }
}
