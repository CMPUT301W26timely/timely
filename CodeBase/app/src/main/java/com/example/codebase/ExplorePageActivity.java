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
 * ExplorePageActivity — Displays all available events and a summary card for invitations.
 */
public class ExplorePageActivity extends AppCompatActivity {

    private RecyclerView rvAllEvents;
    private TextView tvNoEvents;
    private View progressBar;
    private View layoutInvitationCard;
    private TextView tvInvitationCountSummary;

    private OrganizerEventAdapter adapter; // Reusing OrganizerEventAdapter for event list
    private List<Event> allEventsList = new ArrayList<>();
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_page);

        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        rvAllEvents = findViewById(R.id.rvAllEvents);
        tvNoEvents = findViewById(R.id.tvNoEventsExplore);
        progressBar = findViewById(R.id.exploreProgressBar);
        layoutInvitationCard = findViewById(R.id.layoutInvitationCard);
        tvInvitationCountSummary = findViewById(R.id.tvInvitationCountSummary);

        findViewById(R.id.btnBackExplore).setOnClickListener(v -> finish());

        // Setup Invitation Card Click
        layoutInvitationCard.setOnClickListener(v -> {
            startActivity(new Intent(ExplorePageActivity.this, InvitationsActivity.class));
        });

        // Setup All Events RecyclerView
        rvAllEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrganizerEventAdapter(allEventsList, event -> {
            // Check if user is the organizer
            if (deviceId.equals(event.getOrganizerDeviceId())) {
                // Redirect to Organizer Detail page (with editing privileges)
                Intent intent = new Intent(ExplorePageActivity.this, EventDetailActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
                startActivity(intent);
            } else {
                // Entrants see a read-only detail page with Join/Leave Waiting List buttons
                Intent intent = new Intent(ExplorePageActivity.this, EntrantEventDetailActivity.class);
                intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, event.getId());
                startActivity(intent);
            }
        });
        rvAllEvents.setAdapter(adapter);

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        loadInvitationsCount();
        loadAllEvents();
    }

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
                            boolean isEnrolled = event.getEnrolledEntrants() != null && event.getEnrolledEntrants().contains(deviceId);
                            boolean isCancelled = event.getCancelledEntrants() != null && event.getCancelledEntrants().contains(deviceId);

                            if (!isEnrolled && !isCancelled) {
                                count++;
                            }
                        }
                    }

                    if (count > 0) {
                        layoutInvitationCard.setVisibility(View.VISIBLE);
                        tvInvitationCountSummary.setText("You have " + count + " new invitation" + (count > 1 ? "s" : "") + "!");
                    } else {
                        layoutInvitationCard.setVisibility(View.GONE);
                    }
                });
    }

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
