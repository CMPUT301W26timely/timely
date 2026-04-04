package com.example.codebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.List;

/**
 * RecyclerView adapter for the Invited Entrants screen.
 *
 * <p>Each row shows the entrant's name, email, a colour-coded status badge,
 * and — for Pending entrants only — a Cancel button (US 02.06.04).
 * Name and email are fetched from the {@code users} collection using the device ID.
 */
public class InvitedEntrantsAdapter extends
        RecyclerView.Adapter<InvitedEntrantsAdapter.EntrantViewHolder> {

    // ─── Cancel callback ──────────────────────────────────────────────────────

    public interface CancelListener {
        void onCancelRequested(InvitedEntrantsActivity.InvitedEntrant entrant);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private List<InvitedEntrantsActivity.InvitedEntrant> list;
    private final CancelListener cancelListener;
    private Date registrationDeadlineDate = null;

    public InvitedEntrantsAdapter(
            List<InvitedEntrantsActivity.InvitedEntrant> list,
            CancelListener cancelListener) {
        this.list           = list;
        this.cancelListener = cancelListener;
    }

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

        private final TextView tvName;
        private final TextView tvEmail;
        private final TextView tvStatus;
        private final Button   btnCancel;

        EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName    = itemView.findViewById(R.id.tvEntrantDeviceId); // kept original ID — now shows name
            tvEmail   = itemView.findViewById(R.id.tvEntrantEmail);
            tvStatus  = itemView.findViewById(R.id.tvEntrantStatus);
            btnCancel = itemView.findViewById(R.id.btnCancelEntrant);
        }

        void bind(
                InvitedEntrantsActivity.InvitedEntrant entrant,
                Date registrationDeadlineDate,
                CancelListener cancelListener) {

            String id = entrant.deviceId;

            // Show placeholder while loading
            tvName.setText("Loading...");
            tvEmail.setVisibility(View.GONE);

            // Fetch name and email from users collection
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(id)
                    .get()
                    .addOnSuccessListener(userSnap -> {
                        String name  = userSnap.exists() ? userSnap.getString("name")  : null;
                        String email = userSnap.exists() ? userSnap.getString("email") : null;

                        tvName.setText((name != null && !name.trim().isEmpty())
                                ? name.trim() : "Unknown");

                        if (email != null && !email.trim().isEmpty()) {
                            tvEmail.setText(email.trim());
                            tvEmail.setVisibility(View.VISIBLE);
                        } else {
                            tvEmail.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(e -> tvName.setText("Unknown"));

            // Status badge
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

            // Cancel button — only for Pending
            if (!entrant.status.equals("Pending")) {
                btnCancel.setVisibility(View.GONE);
                return;
            }

            btnCancel.setVisibility(View.VISIBLE);

            Date today = new Date();
            boolean canCancel = registrationDeadlineDate != null
                    && today.after(registrationDeadlineDate);

            if (canCancel) {
                btnCancel.setEnabled(true);
                btnCancel.setAlpha(1.0f);
                btnCancel.setText("Cancel");
                btnCancel.setOnClickListener(v -> cancelListener.onCancelRequested(entrant));
            } else {
                btnCancel.setEnabled(false);
                btnCancel.setAlpha(0.4f);
                btnCancel.setText("Waiting...");
                btnCancel.setOnClickListener(null);
            }
        }
    }
}