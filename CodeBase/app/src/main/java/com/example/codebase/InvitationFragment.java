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
 * A {@link Fragment} that presents a lottery invitation to an {@link Entrant},
 * allowing them to accept or decline their selected spot in an {@link Event}.
 *
 * <p>This fragment expects {@link Event} and {@link Entrant} data to be injected
 * before it is displayed, either via {@link #setEventData(Event, Entrant)} from the
 * hosting {@link android.app.Activity}, or ideally via a shared ViewModel or
 * {@link androidx.fragment.app.Fragment#getArguments() Bundle arguments}
 * (see the TODO in {@link #onCreateView}).</p>
 *
 * <p>Invitation responses are delegated to {@link Invitations}, which handles
 * the underlying Firestore update. Once the entrant responds, both buttons are
 * disabled to prevent duplicate submissions.</p>
 */
public class InvitationFragment extends Fragment {

    /**
     * The event for which the invitation was issued.
     * Must be set via {@link #setEventData(Event, Entrant)} before the fragment is shown.
     */
    private Event currentEvent;

    /**
     * The entrant who received the invitation.
     * Must be set via {@link #setEventData(Event, Entrant)} before the fragment is shown.
     */
    private Entrant currentEntrant;

    /**
     * Handles the business logic for responding to a lottery invitation,
     * including the Firestore update.
     */
    private Invitations invitationManager;

    /**
     * Inflates the fragment layout and wires up the accept and decline invitation buttons.
     *
     * <p>Button click behaviour:</p>
     * <ul>
     *     <li><b>Accept</b> — calls
     *         {@link Invitations#respondToInvitation(Event, String, boolean)} with {@code true},
     *         shows a confirmation {@link Toast}, and disables both buttons.</li>
     *     <li><b>Decline</b> — calls
     *         {@link Invitations#respondToInvitation(Event, String, boolean)} with {@code false},
     *         shows a confirmation {@link Toast}, and disables both buttons.</li>
     * </ul>
     *
     * <p>If either {@link #currentEvent} or {@link #currentEntrant} is {@code null} when
     * a button is pressed, an error {@link Toast} is shown and no Firestore update is made.
     * This guard exists because data injection via {@link #setEventData(Event, Entrant)}
     * is not enforced at compile time.</p>
     *
     * @param inflater           the {@link LayoutInflater} used to inflate the fragment's view
     * @param container          the parent {@link ViewGroup} the fragment UI will be attached to,
     *                           or {@code null} if there is no parent
     * @param savedInstanceState a {@link Bundle} containing the fragment's previously saved state,
     *                           or {@code null} if this is a fresh creation
     * @return the fully inflated and configured {@link View} for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invitations, container, false);

        // 1. Initialize the logic controller
        invitationManager = new Invitations();

        // 2. Find your buttons from the XML layout
        Button btnAccept = view.findViewById(R.id.btnAccept);
        Button btnDecline = view.findViewById(R.id.btnDecline);

        // TODO: Retrieve the actual currentEvent and currentEntrant data here
        // (e.g., from an Intent, Bundle arguments, or shared ViewModel)

        // 3. Accept: record response and disable both buttons to prevent duplicate submissions
        btnAccept.setOnClickListener(v -> {
            if (currentEvent != null && currentEntrant != null) {
                invitationManager.respondToInvitation(currentEvent, currentEntrant.getDeviceId(), true);
                Toast.makeText(getContext(), "Added to the Final Entrant List!", Toast.LENGTH_SHORT).show();
                btnAccept.setEnabled(false);
                btnDecline.setEnabled(false);
            } else {
                Toast.makeText(getContext(), "Error: Event or Entrant data is missing.", Toast.LENGTH_SHORT).show();
            }
        });

        // 4. Decline: record response and disable both buttons to prevent duplicate submissions
        btnDecline.setOnClickListener(v -> {
            if (currentEvent != null && currentEntrant != null) {
                invitationManager.respondToInvitation(currentEvent, currentEntrant.getDeviceId(), false);
                Toast.makeText(getContext(), "Invitation declined.", Toast.LENGTH_SHORT).show();
                btnAccept.setEnabled(false);
                btnDecline.setEnabled(false);
            } else {
                Toast.makeText(getContext(), "Error: Event or Entrant data is missing.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    /**
     * Injects the {@link Event} and {@link Entrant} data required for this fragment to function.
     *
     * <p>This method must be called by the hosting {@link android.app.Activity} before the
     * fragment is shown. If it is not called, button presses will show an error
     * {@link Toast} and no invitation response will be recorded.</p>
     *
     * <p><b>Note:</b> consider migrating this data injection to a shared ViewModel
     * or {@link androidx.fragment.app.Fragment#getArguments() Bundle arguments}
     * to better align with Android's recommended fragment communication patterns.</p>
     *
     * @param event   the {@link Event} for which the invitation was issued;
     *                must not be {@code null}
     * @param entrant the {@link Entrant} who received the invitation;
     *                must not be {@code null}
     */
    public void setEventData(Event event, Entrant entrant) {
        this.currentEvent = event;
        this.currentEntrant = entrant;
    }
}