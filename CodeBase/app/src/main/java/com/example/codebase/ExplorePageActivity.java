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
 * Displays all available events and a summary card for pending lottery invitations.
 *
 * <p>The screen is composed of two sections:
 * <ul>
 *   <li><b>Invitation summary card</b> — shown only when the current device has one or
 *       more pending invitations (selected but neither enrolled nor cancelled). Tapping
 *       the card launches {@link InvitationsActivity}.</li>
 *   <li><b>All-events list</b> — a {@link RecyclerView} backed by
 *       {@link OrganizerEventAdapter} showing every event in Firestore. Tapping an event
 *       routes to {@link EventDetailActivity} (organizer view) if the device owns the
 *       event, or to {@link EntrantEventDetailActivity} (read-only view) otherwise.</li>
 * </ul>
 *
 * <p>Both data sources are refreshed on every {@link #onResume()} to reflect changes
 * made in other activities.
 */
public class ExplorePageActivity extends AppCompatActivity {

    /** Scrollable list displaying all events fetched from Firestore. */
    private RecyclerView rvAllEvents;

    /** Empty-state label shown when no events exist in Firestore. */
    private TextView tvNoEvents;

    /** Loading indicator shown while the all-events Firestore query is in flight. */
    private View progressBar;

    /**
     * Banner card that summarises pending invitations. Visible only when
     * {@link #loadInvitationsCount()} finds at least one actionable invitation.
     */
    private View layoutInvitationCard;

    /** Text inside {@link #layoutInvitationCard} describing the number of pending invitations. */
    private TextView tvInvitationCountSummary;

    /**
     * Adapter backing {@link #rvAllEvents}.
     * {@link OrganizerEventAdapter} is reused for the flat event list display.
     */
    private OrganizerEventAdapter adapter;

    /** Live dataset of {@link Event} objects displayed in {@link #rvAllEvents}. */
    private List<Event> allEventsList = new ArrayList<>();

    /** Unique device identifier used to determine organizer ownership and invitation status. */
    private String deviceId;

    /**
     * Initialises the activity, binds views, configures the invitation card and
     * {@link RecyclerView} with their respective click listeners, and triggers the
     * initial data load.
     *
     * @param savedInstanceState If the activity is being re-created from a previous state,
     *                           this bundle contains the most recent data; otherwise {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_page);

        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        rvAllEvents            = findViewById(R.id.rvAllEvents);
        tvNoEvents             = findViewById(R.id.tvNoEventsExplore);
        progressBar            = findViewById(R.id.exploreProgressBar);
        layoutInvitationCard   = findViewById(R.id.layoutInvitationCard);
        tvInvitationCountSummary = findViewById(R.id.tvInvitationCountSummary);

        findViewById(R.id.btnBackExplore).setOnClickListener(v -> finish());

        layoutInvitationCard.setOnClickListener(v ->
                startActivity(new Intent(ExplorePageActivity.this, InvitationsActivity.class)));

        rvAllEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrganizerEventAdapter(allEventsList, event -> {
            if (deviceId.equals(event.getOrganizerDeviceId())) {
                // Device owns this event — open organizer detail with editing privileges.
                Intent intent = new Intent(ExplorePageActivity.this, EventDetailActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
                startActivity(intent);
            } else {
                // Entrant view — read-only detail with Join/Leave Waiting List actions.
                Intent intent = new Intent(ExplorePageActivity.this, EntrantEventDetailActivity.class);
                intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, event.getId());
                startActivity(intent);
            }
        });
        rvAllEvents.setAdapter(adapter);

        loadData();
    }

    /**
     * Refreshes both the invitation count and the full events list each time the
     * activity returns to the foreground, ensuring stale data from other screens
     * (e.g. after accepting an invitation) is not displayed.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    /**
     * Convenience method that triggers both data-fetch operations in parallel.
     *
     * @see #loadInvitationsCount()
     * @see #loadAllEvents()
     */
    private void loadData() {
        loadInvitationsCount();
        loadAllEvents();
    }

    /**
     * Queries Firestore for events where the device ID is in {@code selectedEntrants}
     * and counts those that are still pending (neither enrolled nor cancelled).
     *
     * <p>If the count is greater than zero, {@link #layoutInvitationCard} is made visible
     * and {@link #tvInvitationCountSummary} is updated with a human-readable summary
     * (e.g. "You have 2 new invitations!"). If there are no pending invitations the card
     * is hidden.
     */
    private void loadInvitationsCount() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .whereArrayContains("selectedEntrants", deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            boolean isEnrolled  = event.getEnrolledEntrants()  != null && event.getEnrolledEntrants().contains(deviceId);
                            boolean isCancelled = event.getCancelledEntrants() != null && event.getCancelledEntrants().contains(deviceId);
                            if (!isEnrolled && !isCancelled) {
                                count++;
                            }
                        }
                    }

                    if (count > 0) {
                        layoutInvitationCard.setVisibility(View.VISIBLE);
                        tvInvitationCountSummary.setText(
                                "You have " + count + " new invitation" + (count > 1 ? "s" : "") + "!");
                    } else {
                        layoutInvitationCard.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * Fetches all documents from the Firestore {@code events} collection and populates
     * {@link #allEventsList}.
     *
     * <p>Shows {@link #progressBar} while the query is in flight. On success, clears and
     * rebuilds the list, notifies the adapter, and toggles {@link #tvNoEvents} based on
     * whether any events were returned. On failure, hides the progress bar and shows a
     * {@link Toast} error message.
     */
    private void loadAllEvents() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance()
                .collection("events")
                .get()
                .addOnSuccessListener(snapshot -> {
                    progressBar.setVisibility(View.GONE);
                    allEventsList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setId(doc.getId());
                            allEventsList.add(event);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    tvNoEvents.setVisibility(allEventsList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
                });
    }
}