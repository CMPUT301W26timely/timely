package com.example.codebase;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

/**
 * Organizer screen for initial lottery draws and replacement draws.
 *
 * <p>The screen always reloads the latest event state from Firestore before drawing so the
 * organizer sees current waiting-list, selected, enrolled, cancelled, and replacement-eligible
 * counts. Entrants are selected from the waiting list using
 * {@link LotterySelectionEngine}, which excludes already selected, enrolled, cancelled,
 * and co-organizer device IDs.</p>
 */
public class LotteryDrawActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT = "EXTRA_EVENT";
    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";
    public static final String EXTRA_EVENT_TITLE = "EXTRA_EVENT_TITLE";

    private TextView waitListView;
    private TextView capacityView;
    private TextView titleTextView;
    private TextView selectedCountView;
    private TextView cancelledCountView;
    private TextView eligibleCountView;
    private TextView drawSummaryView;
    private Button runLotteryBtn;
    private EditText spotsEditText;

    private Event event;
    private String eventId;
    private String eventTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lottery_draw);

        waitListView = findViewById(R.id.tvWaitingList);
        capacityView = findViewById(R.id.tvCapacity);
        titleTextView = findViewById(R.id.eventTitleText);
        selectedCountView = findViewById(R.id.tvSelectedCount);
        cancelledCountView = findViewById(R.id.tvCancelledCount);
        eligibleCountView = findViewById(R.id.tvEligibleCount);
        drawSummaryView = findViewById(R.id.tvDrawSummary);
        runLotteryBtn = findViewById(R.id.runLotteryDrawButton);
        spotsEditText = findViewById(R.id.numberOfSpotsEditText);

        findViewById(R.id.btnBackCancelled).setOnClickListener(v -> finish());

        event = (Event) getIntent().getSerializableExtra(EXTRA_EVENT);
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        if (TextUtils.isEmpty(eventId) && event != null) {
            eventId = event.getId();
        }
        if (TextUtils.isEmpty(eventTitle) && event != null) {
            eventTitle = event.getTitle();
        }

        titleTextView.setText(TextUtils.isEmpty(eventTitle)
                ? getString(R.string.untitled_event)
                : eventTitle);

        runLotteryBtn.setOnClickListener(v -> runDraw());
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.error_event_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadLatestEvent();
    }

    private void loadLatestEvent() {
        setLoading(true);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::onEventLoaded)
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.error_loading_event_detail, Toast.LENGTH_SHORT)
                            .show();
                    finish();
                });
    }

    private void onEventLoaded(DocumentSnapshot documentSnapshot) {
        setLoading(false);

        Event loadedEvent = EventSchema.normalizeLoadedEvent(documentSnapshot);
        if (loadedEvent == null) {
            Toast.makeText(this, R.string.error_event_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        event = loadedEvent;
        eventId = loadedEvent.getId();
        eventTitle = loadedEvent.getTitle();
        titleTextView.setText(TextUtils.isEmpty(eventTitle)
                ? getString(R.string.untitled_event)
                : eventTitle);
        renderEventState();
    }

    private void renderEventState() {
        int waitingListSize = safeSize(event.getWaitingList());
        int selectedCount = safeSize(event.getSelectedEntrants());
        int cancelledCount = safeSize(event.getCancelledEntrants());
        int eligibleCount = LotterySelectionEngine.getEligibleEntrants(event).size();
        int availableSpots = LotterySelectionEngine.getAvailableSpots(event);

        waitListView.setText(String.valueOf(waitingListSize));
        capacityView.setText(formatSpotCount(availableSpots));
        selectedCountView.setText(String.valueOf(selectedCount));
        cancelledCountView.setText(String.valueOf(cancelledCount));
        eligibleCountView.setText(String.valueOf(eligibleCount));

        boolean replacementMode = selectedCount > 0 || cancelledCount > 0;
        drawSummaryView.setText(buildSummaryText(replacementMode, eligibleCount, availableSpots));
        runLotteryBtn.setText(replacementMode
                ? R.string.lottery_draw_button_replacement
                : R.string.lottery_draw_button_initial);
        runLotteryBtn.setEnabled(eligibleCount > 0 && availableSpots > 0);
    }

    private void runDraw() {
        if (event == null) {
            Toast.makeText(this, R.string.error_loading_event_detail, Toast.LENGTH_SHORT).show();
            return;
        }

        String rawValue = spotsEditText.getText() == null
                ? ""
                : spotsEditText.getText().toString().trim();
        if (rawValue.isEmpty()) {
            Toast.makeText(this, R.string.lottery_draw_invalid_count, Toast.LENGTH_SHORT).show();
            return;
        }

        int requestedCount;
        try {
            requestedCount = Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.lottery_draw_invalid_count, Toast.LENGTH_SHORT).show();
            return;
        }

        LotterySelectionEngine.DrawResult result =
                LotterySelectionEngine.drawEntrants(event, requestedCount);

        if (requestedCount <= 0) {
            Toast.makeText(this, R.string.lottery_draw_invalid_count, Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.getCapacityRemaining() <= 0) {
            Toast.makeText(this, R.string.lottery_draw_no_capacity, Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.getEligibleEntrants().isEmpty()) {
            Toast.makeText(this, R.string.lottery_draw_no_eligible, Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.getDrawnEntrants().isEmpty()) {
            Toast.makeText(this, R.string.lottery_draw_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        AppDatabase.getInstance()
                .addSelectedEntrants(event, result.getDrawnEntrants())
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    spotsEditText.setText("");
                    Toast.makeText(
                            this,
                            getString(R.string.lottery_draw_success,
                                    result.getActualCount(),
                                    result.getRequestedCount()),
                            Toast.LENGTH_SHORT
                    ).show();
                    loadLatestEvent();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.lottery_draw_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private String buildSummaryText(boolean replacementMode, int eligibleCount, int availableSpots) {
        if (availableSpots <= 0) {
            return getString(R.string.lottery_draw_summary_full);
        }
        if (eligibleCount <= 0) {
            return getString(R.string.lottery_draw_summary_empty);
        }
        if (replacementMode) {
            return getString(R.string.lottery_draw_summary_replacement, eligibleCount);
        }
        return getString(R.string.lottery_draw_summary_initial, eligibleCount);
    }

    private String formatSpotCount(int availableSpots) {
        if (availableSpots == Integer.MAX_VALUE) {
            return getString(R.string.not_applicable_short);
        }
        return String.format(Locale.getDefault(), "%d", availableSpots);
    }

    private int safeSize(java.util.List<String> list) {
        return list == null ? 0 : list.size();
    }

    private void setLoading(boolean isLoading) {
        runLotteryBtn.setEnabled(!isLoading);
        spotsEditText.setEnabled(!isLoading);
    }
}
