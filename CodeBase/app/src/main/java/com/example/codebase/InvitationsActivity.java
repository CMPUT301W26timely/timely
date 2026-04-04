package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the list of lottery invitations for the current device's entrant.
 *
 * <p>Queries Firestore for all events whose {@code selectedEntrants} array contains the
 * device ID, then filters out any events the entrant has already enrolled in. The
 * remaining events are presented in a {@link RecyclerView} via {@link ExploreEventAdapter},
 * which exposes accept, decline, and card-tap actions for each invitation.
 *
 * <p>Invitation visibility rules:
 * <ul>
 *   <li>Shown if the entrant is in {@code selectedEntrants}, even if previously in
 *       {@code cancelledEntrants} (re-selected entrants must be able to respond again).</li>
 *   <li>Hidden if the entrant is already in {@code enrolledEntrants} (already accepted).</li>
 * </ul>
 */
public class InvitationsActivity extends AppCompatActivity {

    /** Scrollable list that displays pending invitation cards. */
    private RecyclerView rvInvitations;

    /** Empty-state label shown when there are no pending invitations. */
    private TextView tvNoInvitations;

    /** Loading indicator displayed during Firestore fetch and update operations. */
    private View progressBar;

    /** Adapter backing {@link #rvInvitations} with accept/decline/tap action callbacks. */
    private ExploreEventAdapter adapter;

    /** Live dataset of {@link Event} objects currently displayed in the list. */
    private List<Event> invitationList = new ArrayList<>();

    /** Unique device identifier used to query and update Firestore entrant arrays. */
    private String deviceId;

    /**
     * Initialises the activity, binds views, configures the {@link RecyclerView} and its
     * adapter, and triggers the initial invitation load from Firestore.
     *
     * @param savedInstanceState If the activity is being re-created from a previous state,
     *                           this bundle contains the most recent data; otherwise {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        rvInvitations   = findViewById(R.id.rvInvitationsList);
        tvNoInvitations = findViewById(R.id.tvNoInvitationsList);
        progressBar     = findViewById(R.id.invitationsProgressBar);

        findViewById(R.id.btnBackInvitations).setOnClickListener(v -> finish());

        rvInvitations.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ExploreEventAdapter(invitationList, new ExploreEventAdapter.OnInvitationActionListener() {
            @Override
            public void onAcceptClick(Event event) {
                processInvitationResponse(event, true);
            }

            @Override
            public void onDeclineClick(Event event) {
                processInvitationResponse(event, false);
            }

            @Override
            public void onCardClick(Event event) {
                Intent intent = new Intent(InvitationsActivity.this, EntrantEventDetailActivity.class);
                intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, event.getId());
                startActivity(intent);
            }
        });
        rvInvitations.setAdapter(adapter);

        loadInvitations();
    }

    /**
     * Fetches events from Firestore where {@code selectedEntrants} contains the current
     * device ID and delegates result handling to {@link #populateList(QuerySnapshot)}.
     *
     * <p>Shows {@link #progressBar} while the query is in flight. Displays a {@link Toast}
     * and hides the progress bar if the query fails.
     */
    private void loadInvitations() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("events")
                .whereArrayContains("selectedEntrants", deviceId)
                .get()
                .addOnSuccessListener(this::populateList)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading invitations", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Populates {@link #invitationList} from a Firestore {@link QuerySnapshot} and
     * refreshes the UI.
     *
     * <p>Each {@link DocumentSnapshot} is converted to an {@link Event}. Events where the
     * device ID appears in {@code enrolledEntrants} are excluded (the entrant has already
     * accepted). Events where the device ID appears in {@code cancelledEntrants} are
     * intentionally included, allowing re-selected entrants to respond again.
     *
     * <p>After the list is rebuilt, the adapter is notified and the empty-state view and
     * {@link RecyclerView} visibility are toggled accordingly.
     *
     * @param snapshot The {@link QuerySnapshot} returned by the Firestore events query.
     */
    private void populateList(QuerySnapshot snapshot) {
        progressBar.setVisibility(View.GONE);
        invitationList.clear();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Event event = doc.toObject(Event.class);
            if (event != null) {
                event.setId(doc.getId());

                // Always show if in selectedEntrants, regardless of previous rejections
                // (cancelledEntrants). Only exclude if already accepted (enrolledEntrants).
                boolean isEnrolled = event.getEnrolledEntrants() != null
                        && event.getEnrolledEntrants().contains(deviceId);

                if (!isEnrolled) {
                    invitationList.add(event);
                }
            }
        }

        adapter.notifyDataSetChanged();
        tvNoInvitations.setVisibility(invitationList.isEmpty() ? View.VISIBLE : View.GONE);
        rvInvitations.setVisibility(invitationList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * Applies the entrant's accept or decline response to the given {@link Event} and
     * persists the updated entrant arrays to Firestore.
     *
     * <p>Delegates the in-memory state mutation to {@link Invitations#respondToInvitation},
     * then writes the updated {@code selectedEntrants}, {@code enrolledEntrants}, and
     * {@code cancelledEntrants} arrays back to the event's Firestore document.
     *
     * <p>On success, a confirmation {@link Toast} is shown and {@link #loadInvitations()}
     * is called to refresh the list. On failure, a {@link Toast} error is shown and
     * {@link #progressBar} is hidden.
     *
     * @param event      The {@link Event} the entrant is responding to.
     * @param isAccepted {@code true} if the entrant accepted the invitation;
     *                   {@code false} if they declined.
     */
    private void processInvitationResponse(Event event, boolean isAccepted) {
        progressBar.setVisibility(View.VISIBLE);

        Invitations invitationManager = new Invitations();
        invitationManager.respondToInvitation(event, deviceId, isAccepted);

        FirebaseFirestore.getInstance().collection("events").document(event.getId())
                .update(
                        "selectedEntrants",   event.getSelectedEntrants(),
                        "enrolledEntrants",   event.getEnrolledEntrants(),
                        "cancelledEntrants",  event.getCancelledEntrants()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isAccepted ? "Invitation Accepted!" : "Invitation Declined.", Toast.LENGTH_SHORT).show();
                    loadInvitations();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to update response. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }
}