package com.example.codebase;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * EventDetailActivity — Refined event detail screen.
 */
public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID    = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";

    private String eventId;
    private String eventTitle;

    private TextView tvDetailTitle;
    private TextView tvDetailDate;
    private TextView tvDetailLocation;
    private TextView tvDetailDescription;
    private TextView tvStatusBadge;
    private TextView tvWaitingCount;
    private TextView tvSelectedCount;
    private android.widget.ImageView ivHeroPoster;
    private View     progressBar;

    private final SimpleDateFormat displayFormat =
            new SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        eventId    = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        // ── Bind views ────────────────────────────────────────────────────────
        tvDetailTitle       = findViewById(R.id.tvDetailTitle);
        tvDetailDate        = findViewById(R.id.tvDetailDate);
        tvDetailLocation    = findViewById(R.id.tvDetailLocation);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvStatusBadge       = findViewById(R.id.tvDetailStatusBadge);
        tvWaitingCount      = findViewById(R.id.tvWaitingCount);
        tvSelectedCount     = findViewById(R.id.tvSelectedCount);
        ivHeroPoster        = findViewById(R.id.ivHeroPoster);
        progressBar         = findViewById(R.id.detailProgressBar);

        // ── Toolbar / Back button ─────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // ── Actions ──────────────────────────────────────────────────────────
        findViewById(R.id.btnJoinWaitingList).setOnClickListener(v ->
                Toast.makeText(this, "Join list coming soon", Toast.LENGTH_SHORT).show()
        );

        loadEventDetails();
    }

    private void loadEventDetails() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::onEventLoaded)
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
                });
    }

    private void onEventLoaded(DocumentSnapshot doc) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);

        if (!doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ── Poster image ──────────────────────────────────────────────────────
        String posterUrl = doc.getString("posterUrl");
        Glide.with(this)
                .load(posterUrl)
                .placeholder(R.drawable.bg_hero_image)
                .error(R.drawable.bg_hero_image)
                .centerCrop()
                .into(ivHeroPoster);

        // ── Basic fields ──────────────────────────────────────────────────────
        eventTitle = doc.getString("title") != null ? doc.getString("title") : "Untitled Event";
        String location    = doc.getString("location")    != null ? doc.getString("location")    : "Location TBD";
        String description = doc.getString("description") != null ? doc.getString("description") : "No description provided.";

        tvDetailTitle.setText(eventTitle);
        tvDetailLocation.setText(location);
        tvDetailDescription.setText(description);

        // ── Timestamps ────────────────────────────────────────────────────────
        Timestamp startDateTs = doc.getTimestamp("startDate");
        Date startDate = startDateTs != null ? startDateTs.toDate() : null;

        tvDetailDate.setText(startDate != null ? displayFormat.format(startDate) : "Date TBD");

        // ── Arrays ────────────────────────────────────────────────────────────
        List<?> waitingList      = (List<?>) doc.get("waitingList");
        List<?> selectedEntrants = (List<?>) doc.get("selectedEntrants");

        int waitingCount  = waitingList      != null ? waitingList.size()      : 0;
        int selectedCount = selectedEntrants != null ? selectedEntrants.size() : 0;

        tvWaitingCount.setText(String.valueOf(waitingCount));
        tvSelectedCount.setText(String.valueOf(selectedCount));

        // ── Status badge ──────────────────────────────────────────────────────
        Timestamp regDeadlineTs = doc.getTimestamp("registrationDeadline");
        Date regDeadline = regDeadlineTs != null ? regDeadlineTs.toDate() : null;
        
        if (regDeadline != null && new Date().before(regDeadline)) {
            tvStatusBadge.setText("REGISTRATION OPEN");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_open);
            tvStatusBadge.setTextColor(getColor(R.color.primaryAccent));
        } else {
            tvStatusBadge.setText("REGISTRATION CLOSED");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_closed);
            tvStatusBadge.setTextColor(getColor(R.color.textSecondary));
        }
    }
}