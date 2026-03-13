package com.example.codebase;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/**
 * {@link ArrayAdapter} that displays a list of entrants identified by device ID,
 * resolving each device ID to a name and email via an asynchronous Firestore lookup.
 *
 * <p>Each item in the list is a device ID string. While the Firestore fetch is in
 * flight, the name field shows "Loading…" and the email field shows the raw device ID
 * as a placeholder. Once the user document is retrieved, both fields are updated with
 * the stored {@code name} and {@code email} values, falling back to {@code "Unknown User"}
 * or the device ID respectively if those fields are absent.
 *
 * <p>View recycling via {@code convertView} is supported; stale "Loading…" text may
 * briefly appear on recycled views before the new Firestore response arrives.
 */
public class FinalEntrantListAdapter extends ArrayAdapter<String> {

    /**
     * Constructs a new {@code FinalEntrantListAdapter}.
     *
     * @param context  The current {@link Context}.
     * @param entrants The list of device ID strings to display.
     */
    public FinalEntrantListAdapter(Context context, ArrayList<String> entrants) {
        super(context, 0, entrants);
    }

    /**
     * Returns a view for the entrant at the given position, inflating
     * {@code final_entrant_list_item} if no recycled view is available.
     *
     * <p>The view is initially populated with placeholder text, then updated
     * asynchronously once the Firestore user document for the device ID is fetched.
     * If the device ID is {@code null} or empty the Firestore fetch is skipped and
     * the placeholders remain.
     *
     * @param position    The index of the item within the adapter's data set.
     * @param convertView A recycled view to reuse, or {@code null} if none is available.
     * @param parent      The parent {@link ViewGroup} that this view will be attached to.
     * @return The populated list item {@link View}.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null)
            view = LayoutInflater.from(getContext()).inflate(R.layout.final_entrant_list_item, parent, false);
        else
            view = convertView;

        String deviceId = getItem(position);

        TextView entrantName  = view.findViewById(R.id.entrantNameTextView);
        TextView entrantEmail = view.findViewById(R.id.entrantEmailTextView);

        // Show placeholders while the Firestore fetch is in flight.
        entrantName.setText("Loading...");
        entrantEmail.setText(deviceId != null ? deviceId : "");

        if (deviceId != null && !deviceId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(deviceId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name  = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            entrantName.setText(name  != null ? name  : "Unknown User");
                            entrantEmail.setText(email != null ? email : deviceId);
                        }
                    });
        }

        return view;
    }
}