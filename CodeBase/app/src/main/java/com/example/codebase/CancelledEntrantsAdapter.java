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
 * CancelledEntrantsAdapter — RecyclerView adapter for Cancelled Entrants screen.
 *
 * Each row shows:
 *   - Avatar circle with initials (first 2 chars of deviceId)
 *   - DeviceId (short form) — will be replaced with name+email later
 *   - Secondary line (placeholder for email)
 *   - Status badge: Declined / Cancelled
 *   - Date
 */
public class CancelledEntrantsAdapter extends
        RecyclerView.Adapter<CancelledEntrantsAdapter.ViewHolder> {

    private List<CancelledEntrantsActivity.CancelledEntrant> list;

    public CancelledEntrantsAdapter(List<CancelledEntrantsActivity.CancelledEntrant> list) {
        this.list = list;
    }

    public void updateList(List<CancelledEntrantsActivity.CancelledEntrant> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cancelled_entrant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() { return list.size(); }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvAvatar;
        private final TextView tvDeviceId;
        private final TextView tvEmail;
        private final TextView tvStatus;
        private final TextView tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar   = itemView.findViewById(R.id.tvAvatar);
            tvDeviceId = itemView.findViewById(R.id.tvCancelledDeviceId);
            tvEmail    = itemView.findViewById(R.id.tvCancelledEmail);
            tvStatus   = itemView.findViewById(R.id.tvCancelledStatus);
            tvDate     = itemView.findViewById(R.id.tvCancelledDate);
        }

        void bind(CancelledEntrantsActivity.CancelledEntrant entrant) {
            String id = entrant.deviceId;

            // ── Avatar initials (first 2 chars of deviceId, uppercased) ──────
            String initials = id.length() >= 2
                    ? id.substring(0, 2).toUpperCase()
                    : id.toUpperCase();
            tvAvatar.setText(initials);

            // ── Colour avatar based on first char — deterministic colour ─────
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

            // ── Email placeholder — hidden until user structure is known ────────
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

            // ── Date ──────────────────────────────────────────────────────────
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