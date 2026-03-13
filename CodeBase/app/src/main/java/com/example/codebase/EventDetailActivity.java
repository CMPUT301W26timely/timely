package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Event detail screen for organizer role.
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
    private TextView tvEventCost;
    private android.widget.ImageView ivHeroPoster;
    private View progressBar;
    private Event event;

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final NumberFormat currencyFormat =
            NumberFormat.getCurrencyInstance(Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

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
        tvEventCost = findViewById(R.id.tvEventCost);
        ivHeroPoster = findViewById(R.id.ivHeroPoster);
        progressBar = findViewById(R.id.detailProgressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.cardInvited).setOnClickListener(v -> {
            Intent intent = new Intent(this, InvitedEntrantsActivity.class);
            intent.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_ID, eventId);
            intent.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_TITLE, eventTitle);
            startActivity(intent);
        });

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

        findViewById(R.id.rowEditEvent).setOnClickListener(v -> {
            if (event == null) {
                Toast.makeText(this, "Event not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            Intent intent = new Intent(this, CreateEventActivity.class);
            intent.putExtra("isEditMode", true);
            intent.putExtra("editingEventId", eventId);
            intent.putExtra("name", event.getTitle() != null ? event.getTitle() : "");
            intent.putExtra("description", event.getDescription() != null ? event.getDescription() : "");
            intent.putExtra("location", event.getLocation() != null ? event.getLocation() : "");
            intent.putExtra("price", (double) event.getPrice());
            intent.putExtra("geoRequired", event.isGeoEnabled());
            intent.putExtra("waitlistCap", event.getWaitlistCap());
            intent.putExtra("capacity", event.getMaxCapacity() != null ? event.getMaxCapacity().intValue() : 0);
            intent.putExtra("posterBase64", event.getPoster() != null ? event.getPoster().getPosterImageBase64() : "");

            if (event.getStartDate() != null) {
                intent.putExtra("startDate", sdf.format(event.getStartDate()));
            }
            if (event.getEndDate() != null) {
                intent.putExtra("endDate", sdf.format(event.getEndDate()));
            }
            if (event.getRegistrationOpen() != null) {
                intent.putExtra("registrationOpen", sdf.format(event.getRegistrationOpen()));
            }
            if (event.getRegistrationDeadline() != null) {
                intent.putExtra("registrationDeadline", sdf.format(event.getRegistrationDeadline()));
            }

            startActivity(intent);
        });

        findViewById(R.id.cardCancelled).setOnClickListener(v -> {
            Intent intent = new Intent(this, CancelledEntrantsActivity.class);
            intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_ID, eventId);
            intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_TITLE, eventTitle);
            startActivity(intent);
        });

        findViewById(R.id.rowCancelledUsers).setOnClickListener(v -> {
            Intent intent = new Intent(this, CancelledEntrantsActivity.class);
            intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_ID, eventId);
            intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_TITLE, eventTitle);
            startActivity(intent);
        });

        loadEventDetails();
    }

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

        event = EventSchema.normalizeLoadedEvent(doc);
        if (event == null) {
            Toast.makeText(this,
                    getString(R.string.error_event_not_found),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (event.getPoster() != null
                && event.getPoster().getPosterImageBase64() != null
                && !event.getPoster().getPosterImageBase64().isEmpty()) {
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

        eventTitle = event.getTitle() != null
                ? event.getTitle() : getString(R.string.untitled_event);
        String location = event.getLocation() != null ? event.getLocation() : "";
        String description = event.getDescription() != null ? event.getDescription() : "";

        tvDetailTitle.setText(eventTitle);
        tvDetailLocation.setText(location);
        tvDetailDescription.setText(description);

        Date regOpen = event.getRegistrationOpen();
        Date registrationDeadline = event.getRegistrationDeadline();
        Date drawDate = event.getDrawDate();
        Date startDate = event.getStartDate();
        Date endDate = event.getEndDate();

        tvDetailDate.setText(formatEventDateRange(startDate, endDate));

        Long maxCapacity = event.getMaxCapacity();
        int waitlistCap = event.getWaitlistCap();
        float price = event.getPrice();

        tvEventCost.setText(currencyFormat.format(price));
        tvMaxCapacity.setText((maxCapacity != null && maxCapacity > 0)
                ? String.valueOf(maxCapacity)
                : getString(R.string.not_applicable_short));
        tvWinnersCount.setText(waitlistCap > 0
                ? String.valueOf(waitlistCap)
                : getString(R.string.not_applicable_short));

        List<String> waitingList = event.getWaitingList();
        List<String> selectedEntrants = event.getSelectedEntrants();
        List<String> enrolledEntrants = event.getEnrolledEntrants();
        List<String> cancelledEntrants = event.getCancelledEntrants();

        int waitingCount = waitingList != null ? waitingList.size() : 0;
        int invitedCount = selectedEntrants != null ? selectedEntrants.size() : 0;
        int enrolledCount = enrolledEntrants != null ? enrolledEntrants.size() : 0;
        int cancelledCount = cancelledEntrants != null ? cancelledEntrants.size() : 0;

        tvWaitingCount.setText(String.valueOf(waitingCount));
        tvInvitedCount.setText(String.valueOf(invitedCount));
        tvEnrolledCount.setText(String.valueOf(enrolledCount));
        tvCancelledCount.setText(String.valueOf(cancelledCount));
        tvWaitingListRowCount.setText(String.valueOf(waitingCount));

        String status = calculateStatus(
                regOpen, registrationDeadline, drawDate, startDate, endDate,
                selectedEntrants, enrolledEntrants
        );
        applyStatusBadge(status);
    }

    private String calculateStatus(
            Date regOpen,
            Date registrationDeadline,
            Date drawDate,
            Date startDate,
            Date endDate,
            List<String> selectedEntrants,
            List<String> enrolledEntrants) {

        if (regOpen == null || registrationDeadline == null || drawDate == null
                || startDate == null || endDate == null) {
            return "Draft";
        }

        Date today = new Date();
        boolean selectedEmpty = selectedEntrants == null || selectedEntrants.isEmpty();
        boolean enrolledEmpty = enrolledEntrants == null || enrolledEntrants.isEmpty();

        if (today.before(regOpen)) {
            return "Registration Opening Soon";
        } else if (!today.before(regOpen) && !today.after(registrationDeadline)) {
            return "Registration Open";
        } else if (today.after(registrationDeadline)
                && today.before(drawDate)
                && selectedEmpty) {
            return "Registration Closed / Lottery Opening Soon";
        } else if (today.after(drawDate)
                && today.before(startDate)
                && !selectedEmpty) {
            return "Lottery Closed & Event Scheduled";
        } else if (!today.before(startDate)
                && !today.after(endDate)
                && !enrolledEmpty) {
            return "In Progress";
        } else if (today.after(endDate)) {
            return "Event Ended";
        } else {
            return "Draft";
        }
    }

    private String formatEventDateRange(Date startDate, Date endDate) {
        if (startDate == null) {
            return getString(R.string.date_not_set);
        }

        if (endDate == null) {
            return displayDateFormat.format(startDate);
        }

        String startText = displayDateFormat.format(startDate);
        String endText = displayDateFormat.format(endDate);
        if (startText.equals(endText)) {
            return startText;
        }
        return startText + " - " + endText;
    }

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
            default:
                tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_amber);
                tvStatusBadge.setTextColor(0xFF8B7A2A);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null) {
            loadEventDetails();
        }
    }
}
