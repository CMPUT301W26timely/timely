package com.example.codebase;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class EntrantEventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";

    private String eventId;
    private String deviceId;
    private Event event;

    private TextView tvTitle, tvDate, tvLocation, tvDescription;
    private ImageView ivPoster;
    private Button btnJoin, btnLeave, btnDropOut;
    private View layoutJoinLeave;
    private View progressBar;

    private final SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_event_detail);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        tvTitle = findViewById(R.id.tvEntrantEventTitle);
        tvDate = findViewById(R.id.tvEntrantEventDate);
        tvLocation = findViewById(R.id.tvEntrantEventLocation);
        tvDescription = findViewById(R.id.tvEntrantEventDescription);
        ivPoster = findViewById(R.id.ivEntrantHeroPoster);
        btnJoin = findViewById(R.id.btnJoinWaitingList);
        btnLeave = findViewById(R.id.btnLeaveWaitingList);
        btnDropOut = findViewById(R.id.btnDropOut);
        layoutJoinLeave = findViewById(R.id.layoutJoinLeave);
        progressBar = findViewById(R.id.entrantDetailProgressBar);

        findViewById(R.id.btnEntrantBack).setOnClickListener(v -> finish());

        btnJoin.setOnClickListener(v -> showJoinConfirmation());
        btnLeave.setOnClickListener(v -> showLeaveConfirmation());
        btnDropOut.setOnClickListener(v -> showDropOutConfirmation());

        loadEventDetails();
    }

    private void loadEventDetails() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(this::onEventLoaded)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
                });
    }

    private void onEventLoaded(DocumentSnapshot doc) {
        progressBar.setVisibility(View.GONE);
        event = doc.toObject(Event.class);
        if (event == null) return;

        tvTitle.setText(event.getTitle());
        tvLocation.setText(event.getLocation());
        tvDescription.setText(event.getDescription());

        if (event.getStartDate() != null) {
            tvDate.setText(displayFormat.format(event.getStartDate()));
        }

        if (event.getPoster() != null && event.getPoster().getPosterImageBase64() != null) {
            ivPoster.setImageBitmap(EventPoster.decodeImage(event.getPoster().getPosterImageBase64()));
        }

        updateButtonStates();
    }

    private void updateButtonStates() {
        if (event == null) return;

        ArrayList<String> enrolled = event.getEnrolledEntrants();
        boolean isEnrolled = enrolled != null && enrolled.contains(deviceId);

        if (isEnrolled) {
            // User has accepted invitation
            btnDropOut.setVisibility(View.VISIBLE);
            layoutJoinLeave.setVisibility(View.GONE);
        } else {
            // User is not enrolled
            btnDropOut.setVisibility(View.GONE);
            layoutJoinLeave.setVisibility(View.VISIBLE);

            ArrayList<String> waitingList = event.getWaitingList();
            boolean isWaiting = waitingList != null && waitingList.contains(deviceId);

            if (isWaiting) {
                btnJoin.setEnabled(false);
                btnJoin.setAlpha(0.5f);
                btnLeave.setEnabled(true);
                btnLeave.setAlpha(1.0f);
            } else {
                // Check if registration is still open to allow joining
                boolean isRegOpen = true;
                if (event.getRegistrationDeadline() != null) {
                    isRegOpen = new Date().before(event.getRegistrationDeadline());
                }

                if (isRegOpen) {
                    btnJoin.setEnabled(true);
                    btnJoin.setAlpha(1.0f);
                } else {
                    btnJoin.setEnabled(false);
                    btnJoin.setAlpha(0.5f);
                }
                btnLeave.setEnabled(false);
                btnLeave.setAlpha(0.5f);
            }
        }
    }

    private void showJoinConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Join Waiting List")
                .setMessage("Would you like to join the waiting list for this event?")
                .setPositiveButton("Confirm", (dialog, which) -> joinWaitingList())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLeaveConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Waiting List")
                .setMessage("Are you sure you want to leave the waiting list?")
                .setPositiveButton("Confirm", (dialog, which) -> leaveWaitingList())
                .setNegativeButton("Stay in list", null)
                .show();
    }

    private void showDropOutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Drop Out of Event")
                .setMessage("Are you sure you want to drop out of this event?")
                .setPositiveButton("Confirm", (dialog, which) -> dropOutOfEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void joinWaitingList() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .update("waitingList", FieldValue.arrayUnion(deviceId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to join", Toast.LENGTH_SHORT).show();
                });
    }

    private void leaveWaitingList() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .update("waitingList", FieldValue.arrayRemove(deviceId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Left waiting list.", Toast.LENGTH_SHORT).show();
                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to leave", Toast.LENGTH_SHORT).show();
                });
    }

    private void dropOutOfEvent() {
        progressBar.setVisibility(View.VISIBLE);
        // Remove from enrolledEntrants and add to cancelledEntrants
        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .update(
                        "enrolledEntrants", FieldValue.arrayRemove(deviceId),
                        "cancelledEntrants", FieldValue.arrayUnion(deviceId)
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Dropped out of event.", Toast.LENGTH_SHORT).show();
                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to drop out", Toast.LENGTH_SHORT).show();
                });
    }
}
