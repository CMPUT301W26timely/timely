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
import java.util.Locale;

public class EntrantEventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";

    private String eventId;
    private String deviceId;
    private Event event;

    private TextView tvTitle, tvDate, tvLocation, tvDescription;
    private ImageView ivPoster;
    private Button btnJoin, btnLeave;
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
        progressBar = findViewById(R.id.entrantDetailProgressBar);

        findViewById(R.id.btnEntrantBack).setOnClickListener(v -> finish());

        btnJoin.setOnClickListener(v -> showJoinConfirmation());
        btnLeave.setOnClickListener(v -> showLeaveConfirmation());

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

        ArrayList<String> waitingList = event.getWaitingList();
        boolean isWaiting = waitingList != null && waitingList.contains(deviceId);

        // Always show both buttons side by side
        btnJoin.setVisibility(View.VISIBLE);
        btnLeave.setVisibility(View.VISIBLE);

        if (isWaiting) {
            // User is already in the list: disable Join, enable Leave
            btnJoin.setEnabled(false);
            btnJoin.setAlpha(0.5f); // Grayed out effect

            btnLeave.setEnabled(true);
            btnLeave.setAlpha(1.0f);
        } else {
            // User is not in the list: enable Join, disable Leave
            btnJoin.setEnabled(true);
            btnJoin.setAlpha(1.0f);

            btnLeave.setEnabled(false);
            btnLeave.setAlpha(0.5f); // Grayed out effect
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
}
