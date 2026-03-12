package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * EventDetailActivity — Event detail screen for Organizer role.
 *
 * Reads from Firestore:
 *   title, description, location
 *   eventStart, eventEnd, regClose (Timestamps); drawDate = regClose + 3 days (calculated)
 *   maxCapacity, winnersCount
 *   waitingList, selectedEntrants, enrolledEntrants, cancelledEntrants (arrays)
 *
 * Status badge is calculated in real time from dates + arrays.
 * Firestore status field is NOT used.
 *
 * Add to AndroidManifest.xml:
 *   <activity android:name=".EventDetailActivity" android:exported="false" />
 */
public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";

    private String eventId;
    private String eventTitle;

    private TextView tvDetailTitle;
    private TextView tvDetailDate;
    private TextView tvDetailLocation;
    private TextView tvDetailDescription;
    private TextView tvStatusBadge;
    private TextView tvWaitingCount;
    private TextView tvInvitedCount;
    private TextView tvEnrolledCount;
    private TextView tvCancelledCount;
    private TextView tvWaitingListRowCount;
    private TextView tvMaxCapacity;
    private TextView tvWinnersCount;
    private android.widget.ImageView ivHeroPoster;
    private View progressBar;

    private final SimpleDateFormat displayFormat =
            new SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        // ── Bind views ────────────────────────────────────────────────────────
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailDate = findViewById(R.id.tvDetailDate);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvStatusBadge = findViewById(R.id.tvDetailStatusBadge);
        tvWaitingCount = findViewById(R.id.tvWaitingCount);
        tvInvitedCount = findViewById(R.id.tvInvitedCount);
        tvEnrolledCount = findViewById(R.id.tvEnrolledCount);
        tvCancelledCount = findViewById(R.id.tvCancelledCount);
        tvWaitingListRowCount = findViewById(R.id.tvWaitingListRowCount);
        tvMaxCapacity = findViewById(R.id.tvMaxCapacity);
        tvWinnersCount = findViewById(R.id.tvWinnersCount);
        ivHeroPoster = findViewById(R.id.ivHeroPoster);
        progressBar = findViewById(R.id.detailProgressBar);

        // ── Back button ───────────────────────────────────────────────────────
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // ── Invited stat card → InvitedEntrantsActivity ───────────────────────
        findViewById(R.id.cardInvited).setOnClickListener(v -> {
            Intent intent = new Intent(this, InvitedEntrantsActivity.class);
            intent.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_ID, eventId);
            intent.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_TITLE, eventTitle);
            startActivity(intent);
        });

        // ── LOTTERY MANAGEMENT ────────────────────────────────────────────────
        findViewById(R.id.rowRunLottery).setOnClickListener(v ->
                Toast.makeText(this,
                        getString(R.string.lottery_draw_coming_soon),
                        Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.rowSendNotification).setOnClickListener(v ->
                Toast.makeText(this,
                        getString(R.string.send_notification_coming_soon),
                        Toast.LENGTH_SHORT).show()
        );

        // ── EVENT DATA ────────────────────────────────────────────────────────
        findViewById(R.id.rowViewWaitingList).setOnClickListener(v ->
                Toast.makeText(this,
                        getString(R.string.view_waiting_list_coming_soon),
                        Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.rowViewEntrantMap).setOnClickListener(v ->
                Toast.makeText(this,
                        getString(R.string.view_entrant_map_coming_soon),
                        Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.rowViewQrCode).setOnClickListener(v -> {
            Intent intent = new Intent(this, QrDisplayActivity.class);
            intent.putExtra("event_id", eventId);
            startActivity(intent);
        });

        // ── SETTINGS ─────────────────────────────────────────────────────────
        findViewById(R.id.rowEditEvent).setOnClickListener(v ->
                Toast.makeText(this,
                        getString(R.string.edit_event_coming_soon),
                        Toast.LENGTH_SHORT).show()
        );
        // Cancelled stat card
        findViewById(R.id.cardCancelled).setOnClickListener(v -> {
            Intent intent = new Intent(this, CancelledEntrantsActivity.class);
            intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_ID, eventId);
            intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_TITLE, eventTitle);
            startActivity(intent);
        });

        // View Cancelled Users row
        findViewById(R.id.rowCancelledUsers).setOnClickListener(v -> {
            Intent intent = new Intent(this, CancelledEntrantsActivity.class);
            intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_ID, eventId);
            intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_TITLE, eventTitle);
            startActivity(intent);
        });

        loadEventDetails();
    }

    // ─── Load event from Firestore ────────────────────────────────────────────

    private void loadEventDetails() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::onEventLoaded)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            getString(R.string.error_loading_event_detail),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void onEventLoaded(DocumentSnapshot doc) {
        progressBar.setVisibility(View.GONE);

        if (!doc.exists()) {
            Toast.makeText(this,
                    getString(R.string.error_event_not_found),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Event event = doc.toObject(Event.class);

        // ── Poster image — stored as Base64 string in Firestore ──────────────
        if (event.getPoster().getPosterImageBase64() != null && !event.getPoster().getPosterImageBase64().isEmpty()) {
            try {
                android.graphics.Bitmap bitmap = EventPoster.decodeImage(event.getPoster().getPosterImageBase64());
                if (bitmap != null) {
                    ivHeroPoster.setImageBitmap(bitmap);
                    ivHeroPoster.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                } else {
                    ivHeroPoster.setBackgroundResource(R.drawable.bg_hero_image);
                }
            } catch (Exception e) {
                ivHeroPoster.setBackgroundResource(R.drawable.bg_hero_image);
            }
        } else {
            ivHeroPoster.setBackgroundResource(R.drawable.bg_hero_image);
        }

        // ── Basic fields ──────────────────────────────────────────────────────
        eventTitle = event.getTitle() != null
                ? event.getTitle() : getString(R.string.untitled_event);
        String location = event.getLocation() != null ? event.getLocation() : "";
        String description = event.getDescription() != null ? event.getDescription() : "";

        tvDetailTitle.setText(eventTitle);
        tvDetailLocation.setText(location);
        tvDetailDescription.setText(description);

        // ── Timestamps ────────────────────────────────────────────────────────
        // ── Misc fields ───────────────────────────────────────────────────────
        Boolean geoEnabled = event.isGeoEnabled(); // reserved for future use

        // ── Timestamps ────────────────────────────────────────────────────────
        Date regOpen = event.getRegistrationOpen() != null ? event.getRegistrationOpen() : null;
        Date registrationDeadline = event.getRegistrationDeadline() != null ? event.getRegistrationDeadline() : null;
        // drawDate = regClose + 3 days (calculated, not stored in Firestore)
        Date drawDate = event.getDrawDate() != null ? event.getDrawDate() : null;
        Date startDate = event.getStartDate() != null ? event.getStartDate()  : null;
        Date endDate = event.getEndDate() != null ? event.getEndDate() : null;

        // Display startDate on hero area
        tvDetailDate.setText(startDate != null
                ? displayFormat.format(startDate) : getString(R.string.date_not_set));

        // ── Capacity fields ───────────────────────────────────────────────────
        Long maxCapacity = (long) event.getWaitlistCap();
        Long winnersCount = event.getMaxCapacity();

        if (tvMaxCapacity != null) {
            tvMaxCapacity.setText((maxCapacity != null && maxCapacity > 0)
                    ? String.valueOf(maxCapacity) : "—");
        }
        if (tvWinnersCount != null) {
            tvWinnersCount.setText(winnersCount != null
                    ? String.valueOf(winnersCount) : "—");
        }

        // ── Arrays ────────────────────────────────────────────────────────────
        ArrayList<?> waitingList = (ArrayList<?>) event.getWaitingList();
        ArrayList<?> selectedEntrants = (ArrayList<?>) event.getSelectedEntrants();
        ArrayList<?> enrolledEntrants = (ArrayList<?>) event.getEnrolledEntrants();
        ArrayList<?> cancelledEntrants = (ArrayList<?>) event.getCancelledEntrants();

        int waitingCount = waitingList != null ? waitingList.size() : 0;
        int invitedCount = selectedEntrants != null ? selectedEntrants.size() : 0;
        int enrolledCount = enrolledEntrants != null ? enrolledEntrants.size() : 0;
        int cancelledCount = cancelledEntrants != null ? cancelledEntrants.size() : 0;

        tvWaitingCount.setText(String.valueOf(waitingCount));
        tvInvitedCount.setText(String.valueOf(invitedCount));
        tvEnrolledCount.setText(String.valueOf(enrolledCount));
        tvCancelledCount.setText(String.valueOf(cancelledCount));
        tvWaitingListRowCount.setText(String.valueOf(waitingCount));

        // ── Status badge — calculated from dates + arrays ─────────────────────
        String status = calculateStatus(
                regOpen, registrationDeadline, drawDate, startDate, endDate,
                selectedEntrants, enrolledEntrants
        );
        applyStatusBadge(status);
    }

    // ─── Status calculation ───────────────────────────────────────────────────

    private String calculateStatus(
            Date regOpen,
            Date registrationDeadline,
            Date drawDate,
            Date startDate,
            Date endDate,
            List<?> selectedEntrants,
            List<?> enrolledEntrants) {

        // Any null date → Draft
        if (regOpen == null || registrationDeadline == null || drawDate == null
                || startDate == null || endDate == null) {
            return "Draft";
        }

        Date today = new Date();
        boolean selectedEmpty = selectedEntrants == null || selectedEntrants.isEmpty();
        boolean enrolledEmpty = enrolledEntrants == null || enrolledEntrants.isEmpty();

        if (today.before(regOpen)) {
            // today < regOpen
            return "Registration Opening Soon";

        } else if (!today.before(regOpen) && !today.after(registrationDeadline)) {
            // today >= regOpen AND today <= regClose
            return "Registration Open";

        } else if (today.after(registrationDeadline)
                && today.before(drawDate)
                && selectedEmpty) {
            // today > regClose AND today < drawDate AND no winners yet
            return "Registration Closed / Lottery Opening Soon";

        } else if (today.after(drawDate)
                && today.before(startDate)
                && !selectedEmpty) {
            // today > drawDate AND today < eventStart AND winners selected
            return "Lottery Closed & Event Scheduled";

        } else if (!today.before(startDate)
                && !today.after(endDate)
                && !enrolledEmpty) {
            // today >= eventStart AND today <= eventEnd AND people enrolled
            return "In Progress";

        } else if (today.after(endDate)) {
            return "Event Ended";

        } else {
            return "Draft";
        }
    }

    // ─── Apply status badge color + text ─────────────────────────────────────

    private void applyStatusBadge(String status) {
        tvStatusBadge.setText(status);
        tvStatusBadge.setVisibility(View.VISIBLE);

        switch (status) {
            case "Registration Opening Soon":
                tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_amber);
                tvStatusBadge.setTextColor(0xFF8B7A2A);
                break;
            case "Registration Open":
                tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_green);
                tvStatusBadge.setTextColor(0xFF4A7A4A);
                break;
            case "Registration Closed / Lottery Opening Soon":
                tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_amber);
                tvStatusBadge.setTextColor(0xFF8B7A2A);
                break;
            case "Lottery Closed & Event Scheduled":
                tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_amber);
                tvStatusBadge.setTextColor(0xFF8B7A2A);
                break;
            case "In Progress":
                tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_green);
                tvStatusBadge.setTextColor(0xFF4A7A4A);
                break;
            case "Event Ended":
                tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_red);
                tvStatusBadge.setTextColor(0xFF8B3A3A);
                break;
            default: // Draft
                tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_amber);
                tvStatusBadge.setTextColor(0xFF8B7A2A);
                break;
        }
    }
}