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

public class InvitationsActivity extends AppCompatActivity {

    private RecyclerView rvInvitations;
    private TextView tvNoInvitations;
    private View progressBar;

    private ExploreEventAdapter adapter;
    private List<Event> invitationList = new ArrayList<>();
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        rvInvitations = findViewById(R.id.rvInvitationsList);
        tvNoInvitations = findViewById(R.id.tvNoInvitationsList);
        progressBar = findViewById(R.id.invitationsProgressBar);

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
                Intent intent = new Intent(InvitationsActivity.this, EventDetailActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
                startActivity(intent);
            }
        });
        rvInvitations.setAdapter(adapter);

        loadInvitations();
    }

    private void loadInvitations() {
        progressBar.setVisibility(View.VISIBLE);

        // Firestore query already filters for where user is in selectedEntrants
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

    private void populateList(QuerySnapshot snapshot) {
        progressBar.setVisibility(View.GONE);
        invitationList.clear();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Event event = doc.toObject(Event.class);
            if (event != null) {
                event.setId(doc.getId());
                
                // US Change: Always show if in selectedEntrants, regardless of previous rejections (cancelledEntrants).
                // Only exclude if they have already accepted (enrolledEntrants).
                boolean isEnrolled = event.getEnrolledEntrants() != null && event.getEnrolledEntrants().contains(deviceId);
                
                if (!isEnrolled) {
                    invitationList.add(event);
                }
            }
        }

        adapter.notifyDataSetChanged();
        tvNoInvitations.setVisibility(invitationList.isEmpty() ? View.VISIBLE : View.GONE);
        rvInvitations.setVisibility(invitationList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void processInvitationResponse(Event event, boolean isAccepted) {
        progressBar.setVisibility(View.VISIBLE);

        Invitations invitationManager = new Invitations();
        invitationManager.respondToInvitation(event, deviceId, isAccepted);

        FirebaseFirestore.getInstance().collection("events").document(event.getId())
                .update(
                        "selectedEntrants", event.getSelectedEntrants(),
                        "enrolledEntrants", event.getEnrolledEntrants(),
                        "cancelledEntrants", event.getCancelledEntrants()
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
