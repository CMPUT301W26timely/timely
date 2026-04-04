package com.example.codebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * CancelledEntrantsAdapter — RecyclerView adapter for the Cancelled Entrants screen.
 *
 * <p>Each row displays the entrant's name, email, status badge, and cancellation date.
 * Name and email are fetched from the {@code users} collection using the device ID.</p>
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
        private final TextView tvName;
        private final TextView tvEmail;
        private final TextView tvStatus;
        private final TextView tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvName   = itemView.findViewById(R.id.tvCancelledDeviceId);  // reused view — now shows name
            tvEmail  = itemView.findViewById(R.id.tvCancelledEmail);
            tvStatus = itemView.findViewById(R.id.tvCancelledStatus);
            tvDate   = itemView.findViewById(R.id.tvCancelledDate);
        }

        void bind(CancelledEntrantsActivity.CancelledEntrant entrant) {
            String id = entrant.deviceId;

            // Show placeholder while loading
            tvName.setText("Loading...");
            tvEmail.setVisibility(View.GONE);

            // Avatar initials — will update once name loads
            tvAvatar.setText("?");
            int[] avatarColors = {
                    0xFF2A4A6A, 0xFF3A2A5A, 0xFF1A4A3A,
                    0xFF4A2A2A, 0xFF2A3A5A, 0xFF4A3A1A
            };
            int colorIndex = Math.abs(id.hashCode()) % avatarColors.length;
            tvAvatar.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(avatarColors[colorIndex]));

            // Fetch name and email from users collection
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(id)
                    .get()
                    .addOnSuccessListener(userSnap -> {
                        String name  = userSnap.exists() ? userSnap.getString("name")  : null;
                        String email = userSnap.exists() ? userSnap.getString("email") : null;

                        // Name
                        String displayName = (name != null && !name.trim().isEmpty())
                                ? name.trim() : "Unknown";
                        tvName.setText(displayName);

                        // Avatar initials from name
                        String initials = displayName.length() >= 2
                                ? displayName.substring(0, 2).toUpperCase()
                                : displayName.toUpperCase();
                        tvAvatar.setText(initials);

                        // Email
                        if (email != null && !email.trim().isEmpty()) {
                            tvEmail.setText(email.trim());
                            tvEmail.setVisibility(View.VISIBLE);
                        } else {
                            tvEmail.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        tvName.setText("Unknown");
                        tvAvatar.setText("?");
                    });

            // Status badge
            if ("Declined".equals(entrant.status)) {
                tvStatus.setText("× Declined");
                tvStatus.setBackgroundResource(R.drawable.bg_pill_red);
                tvStatus.setTextColor(0xFF8B3A3A);
            } else {
                tvStatus.setText("⦸ Cancelled");
                tvStatus.setBackgroundResource(R.drawable.bg_status_draft);
                tvStatus.setTextColor(0xFFAAAAAA);
            }

            // Cancellation date
            if (entrant.cancelledAt != null) {
                tvDate.setText(new SimpleDateFormat("MMM dd", Locale.getDefault())
                        .format(entrant.cancelledAt));
            } else {
                tvDate.setText("");
            }
        }
    }
}