package com.example.codebase;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragment responsible for displaying an event invitation to the Entrant.
 * Handles the UI for accepting or declining the invitation.
 */
public class InvitationFragment extends Fragment {

    // These would typically be passed in via a Bundle (getArguments) or a ViewModel
    private Event currentEvent;
    private Entrant currentEntrant;

    // The logic class we created earlier
    private Invitations invitationManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate your layout file (e.g., res/layout/fragment_invitations.xml)
        View view = inflater.inflate(R.layout.fragment_invitations, container, false);

        // 1. Initialize the logic controller
        invitationManager = new Invitations();

        // 2. Find your buttons from the XML layout
        Button btnAccept = view.findViewById(R.id.btnAccept);
        Button btnDecline = view.findViewById(R.id.btnDecline);

        // TODO: Retrieve the actual currentEvent and currentEntrant data here
        // (e.g., from an Intent, Bundle arguments, or shared ViewModel)

        // 3. Set up the Accept Button (Step 2 from earlier)
        btnAccept.setOnClickListener(v -> {
            if (currentEvent != null && currentEntrant != null) {
                // Pass true for accepted
                invitationManager.respondToInvitation(currentEvent, currentEntrant.getDeviceId(), true);

                Toast.makeText(getContext(), "Added to the Final Entrant List!", Toast.LENGTH_SHORT).show();

                // Optional: Disable buttons or close the fragment so they can't click it twice
                btnAccept.setEnabled(false);
                btnDecline.setEnabled(false);
            } else {
                Toast.makeText(getContext(), "Error: Event or Entrant data is missing.", Toast.LENGTH_SHORT).show();
            }
        });

        // 4. Set up the Decline Button
        btnDecline.setOnClickListener(v -> {
            if (currentEvent != null && currentEntrant != null) {
                // Pass false for declined
                invitationManager.respondToInvitation(currentEvent, currentEntrant.getDeviceId(), false);

                Toast.makeText(getContext(), "Invitation declined.", Toast.LENGTH_SHORT).show();

                // Optional: Disable buttons or close the fragment
                btnAccept.setEnabled(false);
                btnDecline.setEnabled(false);
            } else {
                Toast.makeText(getContext(), "Error: Event or Entrant data is missing.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    // Setter methods used to pass data into this fragment from your Activity
    public void setEventData(Event event, Entrant entrant) {
        this.currentEvent = event;
        this.currentEntrant = entrant;
    }
}
