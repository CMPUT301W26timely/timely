package com.example.codebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * RecyclerView adapter for the Waiting List screen (US 02.02.01).
 *
 * <p>Each row shows the entrant's name and email address.
 * Both values are fetched lazily from the {@code users} Firestore collection
 * using the entrant's device ID as the document key, so the list renders
 * immediately while the names/emails load in the background.
 */
public class WaitingListAdapter extends
        RecyclerView.Adapter<WaitingListAdapter.WaitingEntrantViewHolder> {

    /** Device IDs of the entrants on the waiting list. */
    private List<String> deviceIds;

    /**
     * Constructs a new {@code WaitingListAdapter}.
     *
     * @param deviceIds The list of device IDs on the event's waiting list.
     */
    public WaitingListAdapter(List<String> deviceIds) {
        this.deviceIds = deviceIds;
    }

    /**
     * Replaces the current data set and refreshes the list.
     *
     * @param newDeviceIds The new list of device IDs.
     */
    public void updateList(List<String> newDeviceIds) {
        this.deviceIds = newDeviceIds;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WaitingEntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waiting_entrant, parent, false);
        return new WaitingEntrantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WaitingEntrantViewHolder holder, int position) {
        holder.bind(deviceIds.get(position));
    }

    @Override
    public int getItemCount() { return deviceIds.size(); }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    /**
     * Holds and binds the views for a single waiting-list entrant row.
     */
    static class WaitingEntrantViewHolder extends RecyclerView.ViewHolder {

        /** Displays the entrant's full name resolved from the {@code users} collection. */
        private final TextView tvName;

        /** Displays the entrant's email address resolved from the {@code users} collection. */
        private final TextView tvEmail;

        WaitingEntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName  = itemView.findViewById(R.id.tvWaitingEntrantName);
            tvEmail = itemView.findViewById(R.id.tvWaitingEntrantEmail);
        }

        /**
         * Binds a device ID to this row. Shows a loading placeholder immediately,
         * then resolves the name and email from Firestore asynchronously.
         *
         * @param deviceId The entrant's device ID used as the Firestore document key.
         */
        void bind(String deviceId) {
            // Show placeholder while loading
            tvName.setText("Loading...");
            tvEmail.setVisibility(View.GONE);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(deviceId)
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
        }
    }
}