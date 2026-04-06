package com.example.codebase;

import static android.view.View.GONE;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Organizer screen for the initial lottery draw and organizer-triggered replacement draws.
 */
public class LotteryDrawActivity extends AppCompatActivity {

    private TextView waitListView;
    private TextView capacityView;
    private TextView titleTextView;
    private Button runLotteryBtn;
    private Button drawReplacementBtn;
    private EditText spotsEditText;
    private CardView replacementCard;
    private TextView actionText;
    private TextView drawResultsText;

    private Event event;
    private String eventId;
    private int replacementSlots;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lottery_draw);

        waitListView = findViewById(R.id.tvWaitingList);
        capacityView = findViewById(R.id.tvCapacity);
        titleTextView = findViewById(R.id.eventTitleText);
        runLotteryBtn = findViewById(R.id.runLotteryDrawButton);
        drawReplacementBtn = findViewById(R.id.drawReplacements);
        spotsEditText = findViewById(R.id.numberOfSpotsEditText);
        replacementCard = findViewById(R.id.replacementCard);
        actionText = findViewById(R.id.actionTextView);
        drawResultsText = findViewById(R.id.drawResultsTextView);

        findViewById(R.id.btnBackCancelled).setOnClickListener(v -> finish());

        event = (Event) getIntent().getSerializableExtra("EXTRA_EVENT");
        if (event == null || event.getId() == null) {
            Toast.makeText(this, "Event not loaded", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventId = event.getId();
        titleTextView.setText(event.getTitle());

        runLotteryBtn.setOnClickListener(v -> {
            Integer requested = parseRequestedSpots();
            if (requested == null) {
                Toast.makeText(this, "Enter how many entrants to draw", Toast.LENGTH_SHORT).show();
                return;
            }

            performDraw(requested, false);
        });

        drawReplacementBtn.setOnClickListener(v -> {
            Integer parsedRequested = parseRequestedSpots();
            int requested = parsedRequested != null ? parsedRequested : replacementSlots;
            if (requested <= 0) {
                Toast.makeText(this, "No replacement slots are currently open", Toast.LENGTH_SHORT).show();
                return;
            }

            performDraw(requested, true);
        });

        loadEventState();
    }

    private Integer parseRequestedSpots() {
        String raw = spotsEditText.getText() != null ? spotsEditText.getText().toString().trim() : "";
        if (TextUtils.isEmpty(raw)) {
            return null;
        }

        try {
            int requested = Integer.parseInt(raw);
            return requested > 0 ? requested : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void loadEventState() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::bindEventState)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load lottery state", Toast.LENGTH_SHORT).show());
    }

    private void bindEventState(DocumentSnapshot snapshot) {
        Event loadedEvent = EventSchema.normalizeLoadedEvent(snapshot);
        if (loadedEvent == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        event = loadedEvent;
        titleTextView.setText(event.getTitle());
        waitListView.setText(String.valueOf(event.getWaitingList().size()));

        int availableSpots = LotterySelectionEngine.availableSpots(event);
        ArrayList<String> eligibleWaiting = LotterySelectionEngine.eligibleWaitingList(event);
        int displayCapacity = availableSpots == Integer.MAX_VALUE ? eligibleWaiting.size() : availableSpots;
        capacityView.setText(String.valueOf(Math.max(displayCapacity, 0)));

        int cancelledCount = EventRosterStatusHelper.cancelledEntrants(event).size();
        replacementSlots = Math.min(cancelledCount, displayCapacity);
        replacementSlots = Math.min(replacementSlots, eligibleWaiting.size());

        if (replacementSlots > 0) {
            replacementCard.setVisibility(android.view.View.VISIBLE);
            actionText.setText(replacementSlots + " spot"
                    + (replacementSlots > 1 ? "s are" : " is")
                    + " open for a replacement draw");
            drawResultsText.setVisibility(android.view.View.VISIBLE);
            drawResultsText.setText("Eligible replacements available: " + eligibleWaiting.size());
        } else {
            replacementCard.setVisibility(GONE);
            drawResultsText.setVisibility(GONE);
        }
    }

    private void performDraw(int requestedCount, boolean isReplacement) {
        if (requestedCount <= 0) {
            Toast.makeText(this, "Invalid draw count", Toast.LENGTH_SHORT).show();
            return;
        }

        runLotteryBtn.setEnabled(false);
        drawReplacementBtn.setEnabled(false);

        DocumentReference eventRef = FirebaseFirestore.getInstance().collection("events").document(eventId);
        FirebaseFirestore.getInstance()
                .runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(eventRef);
                    Event currentEvent = EventSchema.normalizeLoadedEvent(snapshot);
                    if (currentEvent == null) {
                        throw new IllegalStateException("Event not found");
                    }

                    LotterySelectionEngine.SelectionResult result =
                            LotterySelectionEngine.draw(currentEvent, requestedCount, new Random());
                    if (result.getDrawnEntrants().isEmpty()) {
                        throw new IllegalStateException("No eligible entrants available for this draw");
                    }

                    boolean firstSelectionForEvent =
                            EventRosterStatusHelper.invitedEntrants(currentEvent).isEmpty();

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("selectedEntrants",
                            com.google.firebase.firestore.FieldValue.arrayUnion(result.getDrawnEntrants().toArray()));
                    updates.put("invitedEntrants",
                            com.google.firebase.firestore.FieldValue.arrayUnion(result.getDrawnEntrants().toArray()));
                    updates.put("waitingList",
                            com.google.firebase.firestore.FieldValue.arrayRemove(result.getDrawnEntrants().toArray()));
                    transaction.update(eventRef, updates);

                    return new DrawOutcome(
                            currentEvent.getTitle(),
                            result.getDrawnEntrants(),
                            result.getRemainingEligibleEntrants(),
                            isReplacement,
                            firstSelectionForEvent
                    );
                })
                .addOnSuccessListener(outcome -> {
                    sendNotifications(outcome);
                    spotsEditText.setText("");
                    Toast.makeText(
                            this,
                            "Selected " + outcome.drawnEntrants.size() + " entrant"
                                    + (outcome.drawnEntrants.size() > 1 ? "s" : ""),
                            Toast.LENGTH_SHORT
                    ).show();
                    runLotteryBtn.setEnabled(true);
                    drawReplacementBtn.setEnabled(true);
                    loadEventState();
                })
                .addOnFailureListener(e -> {
                    runLotteryBtn.setEnabled(true);
                    drawReplacementBtn.setEnabled(true);
                    Toast.makeText(this, e.getMessage() != null ? e.getMessage() : "Draw failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotifications(DrawOutcome outcome) {
        String senderDeviceId = DeviceIdManager.getOrCreateDeviceId(this);
        String selectedMessage = outcome.isReplacement
                ? "A spot opened up for " + outcome.eventTitle + " and you have been selected. Please respond as soon as possible."
                : "Congratulations! You have been selected for " + outcome.eventTitle + ". Please confirm your spot in the app.";
        NotificationDispatchHelper.sendEventNotifications(
                outcome.drawnEntrants,
                eventId,
                outcome.eventTitle,
                outcome.isReplacement ? "Replacement spot available" : "You were selected",
                selectedMessage,
                "Selected",
                "selectedEntrants",
                senderDeviceId,
                outcome.isReplacement ? "Replacement" : "Selected",
                null
        );

        if (!outcome.isReplacement
                && outcome.firstSelectionForEvent
                && !outcome.remainingEligibleEntrants.isEmpty()) {
            NotificationDispatchHelper.sendEventNotifications(
                    outcome.remainingEligibleEntrants,
                    eventId,
                    outcome.eventTitle,
                    "Not selected in this draw",
                    "You were not selected in the current draw for " + outcome.eventTitle
                            + ". You remain on the waiting list if a spot opens up.",
                    "Waiting List",
                    "waitingList",
                    senderDeviceId,
                    "Waiting List",
                    null
            );
        }
    }

    private static final class DrawOutcome {
        private final String eventTitle;
        private final ArrayList<String> drawnEntrants;
        private final ArrayList<String> remainingEligibleEntrants;
        private final boolean isReplacement;
        private final boolean firstSelectionForEvent;

        private DrawOutcome(String eventTitle,
                            ArrayList<String> drawnEntrants,
                            ArrayList<String> remainingEligibleEntrants,
                            boolean isReplacement,
                            boolean firstSelectionForEvent) {
            this.eventTitle = eventTitle;
            this.drawnEntrants = drawnEntrants;
            this.remainingEligibleEntrants = remainingEligibleEntrants;
            this.isReplacement = isReplacement;
            this.firstSelectionForEvent = firstSelectionForEvent;
        }
    }
}
