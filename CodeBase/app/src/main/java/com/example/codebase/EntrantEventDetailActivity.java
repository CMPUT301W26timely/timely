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

/**
 * Displays the full details of an event from an entrant's perspective and
 * provides actions to join/leave the waiting list or drop out of an accepted
 * invitation.
 *
 * <p>Button visibility and enabled state reflect the entrant's current
 * relationship to the event:
 * <ul>
 *   <li><b>Enrolled</b> — "Drop Out" button is shown; Join/Leave layout is hidden.</li>
 *   <li><b>On waiting list</b> — "Join" is disabled; "Leave" is enabled.</li>
 *   <li><b>Not on waiting list, registration open</b> — "Join" is enabled;
 *       "Leave" is disabled.</li>
 *   <li><b>Not on waiting list, registration closed</b> — both Join and Leave
 *       are disabled.</li>
 * </ul>
 *
 * <p>All Firestore mutations use {@link FieldValue#arrayUnion} and
 * {@link FieldValue#arrayRemove} to avoid race conditions on the entrant lists.
 * Each mutation is preceded by a confirmation {@link AlertDialog} and followed
 * by a full reload of the event document to keep the UI in sync.
 *
 * @see DeviceIdManager
 * @see EventPoster
 */
public class EntrantEventDetailActivity extends AppCompatActivity {

    /** Intent extra key for the Firestore event document ID. */
    public static final String EXTRA_EVENT_ID = "event_id";

    /** Firestore document ID of the event being displayed. */
    private String eventId;

    /** Device ID of the current user, used to check list membership. */
    private String deviceId;

    /** The most recently loaded {@link Event} object; {@code null} until the first load completes. */
    private Event event;

    // ── Views ─────────────────────────────────────────────────────────────────

    private TextView tvTitle, tvDate, tvLocation, tvDescription;
    private ImageView ivPoster;

    /** Adds the current device to the event's {@code waitingList}. */
    private Button btnJoin;

    /** Removes the current device from the event's {@code waitingList}. */
    private Button btnLeave;

    /** Removes the current device from {@code enrolledEntrants} and adds it to {@code cancelledEntrants}. */
    private Button btnDropOut;

    /** Container holding {@link #btnJoin} and {@link #btnLeave}; hidden when the user is enrolled. */
    private View layoutJoinLeave;

    /** Shown below the Join button when the entrant is a co-organizer for this event. */
    private TextView tvCoOrgWarning;

    private View progressBar;

    /** Date formatter used to display the event start date as {@code "MMM dd, yyyy · HH:mm"}. */
    private final SimpleDateFormat displayFormat =
            new SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault());

    /**
     * Initialises the activity, resolves the event ID and device ID, binds views,
     * wires button listeners, and triggers the initial Firestore load.
     *
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_event_detail);

        eventId  = getIntent().getStringExtra(EXTRA_EVENT_ID);
        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        tvTitle       = findViewById(R.id.tvEntrantEventTitle);
        tvDate        = findViewById(R.id.tvEntrantEventDate);
        tvLocation    = findViewById(R.id.tvEntrantEventLocation);
        tvDescription = findViewById(R.id.tvEntrantEventDescription);
        ivPoster      = findViewById(R.id.ivEntrantHeroPoster);
        btnJoin       = findViewById(R.id.btnJoinWaitingList);
        btnLeave      = findViewById(R.id.btnLeaveWaitingList);
        btnDropOut    = findViewById(R.id.btnDropOut);
        layoutJoinLeave = findViewById(R.id.layoutJoinLeave);
        progressBar   = findViewById(R.id.entrantDetailProgressBar);
        tvCoOrgWarning = findViewById(R.id.tvCoOrgWarning);

        findViewById(R.id.btnEntrantBack).setOnClickListener(v -> finish());

        btnJoin.setOnClickListener(v    -> showJoinConfirmation());
        btnLeave.setOnClickListener(v   -> showLeaveConfirmation());
        btnDropOut.setOnClickListener(v -> showDropOutConfirmation());

        loadEventDetails();
    }

    /**
     * Fetches the event document from Firestore and delegates to
     * {@link #onEventLoaded(DocumentSnapshot)} on success.
     *
     * <p>Shows the progress bar while the request is in flight and hides it on
     * failure, displaying a toast error message.
     */
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

    /**
     * Populates all UI fields from the loaded {@link Event} and refreshes button
     * states via {@link #updateButtonStates()}.
     *
     * <p>If the document cannot be mapped to an {@link Event} object (returns
     * {@code null}), the method returns silently without updating the UI.
     *
     * @param doc The Firestore {@link DocumentSnapshot} for the event.
     */
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
            ivPoster.setImageBitmap(
                    EventPoster.decodeImage(event.getPoster().getPosterImageBase64()));
        }

        updateButtonStates();
    }

    /**
     * Updates the visibility and enabled state of the Join, Leave, and Drop Out
     * buttons based on the current device's membership in the event's entrant lists.
     *
     * <p>Decision logic:
     * <ol>
     *   <li>If {@link #deviceId} is in {@code enrolledEntrants}: show Drop Out,
     *       hide Join/Leave layout.</li>
     *   <li>If {@link #deviceId} is in {@code waitingList}: disable Join (dimmed),
     *       enable Leave.</li>
     *   <li>Otherwise: enable Join only if {@code registrationDeadline} is in the
     *       future (or absent); disable Leave.</li>
     * </ol>
     */
    private void updateButtonStates() {
        if (event == null) return;

        // US 02.09.01 — co-organizers cannot join the entrant pool at all
        ArrayList<String> coOrganizers = event.getCoOrganizers();
        boolean isCoOrganizer = coOrganizers != null && coOrganizers.contains(deviceId);

        if (isCoOrganizer) {
            btnDropOut.setVisibility(View.GONE);
            layoutJoinLeave.setVisibility(View.VISIBLE);
            btnJoin.setEnabled(false);
            btnJoin.setAlpha(0.5f);
            btnLeave.setEnabled(false);
            btnLeave.setAlpha(0.5f);
            tvCoOrgWarning.setVisibility(View.VISIBLE);
            return;
        }

        tvCoOrgWarning.setVisibility(View.GONE);

        ArrayList<String> enrolled = event.getEnrolledEntrants();
        boolean isEnrolled = enrolled != null && enrolled.contains(deviceId);

        if (isEnrolled) {
            btnDropOut.setVisibility(View.VISIBLE);
            layoutJoinLeave.setVisibility(View.GONE);
        } else {
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

    /**
     * Shows a confirmation dialog before adding the device to the waiting list.
     * Calls {@link #joinWaitingList()} if the user confirms.
     */
    private void showJoinConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Join Waiting List")
                .setMessage("Would you like to join the waiting list for this event?")
                .setPositiveButton("Confirm", (dialog, which) -> joinWaitingList())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a confirmation dialog before removing the device from the waiting list.
     * Calls {@link #leaveWaitingList()} if the user confirms.
     */
    private void showLeaveConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Waiting List")
                .setMessage("Are you sure you want to leave the waiting list?")
                .setPositiveButton("Confirm", (dialog, which) -> leaveWaitingList())
                .setNegativeButton("Stay in list", null)
                .show();
    }

    /**
     * Shows a confirmation dialog before dropping the device out of the event.
     * Calls {@link #dropOutOfEvent()} if the user confirms.
     */
    private void showDropOutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Drop Out of Event")
                .setMessage("Are you sure you want to drop out of this event?")
                .setPositiveButton("Confirm", (dialog, which) -> dropOutOfEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Adds {@link #deviceId} to the event's {@code waitingList} in Firestore using
     * {@link FieldValue#arrayUnion}.
     *
     * <p>Shows a progress bar during the operation. On success a toast is shown and
     * the event is reloaded to refresh button states. On failure the progress bar is
     * hidden and an error toast is shown.
     */
    private void joinWaitingList() {
        progressBar.setVisibility(View.VISIBLE);

        // US 02.09.01 — re-check server-side in case co-organizer status was
        // assigned after this screen was loaded, before allowing the join
        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Event latestEvent = EventSchema.normalizeLoadedEvent(snapshot);
                    if (latestEvent != null
                            && latestEvent.getCoOrganizers().contains(deviceId)) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this,
                                "You are a co-organizer for this event and cannot join the waiting list.",
                                Toast.LENGTH_LONG).show();
                        loadEventDetails();
                        return;
                    }

                    // Safe to join
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
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to join", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Removes {@link #deviceId} from the event's {@code waitingList} in Firestore
     * using {@link FieldValue#arrayRemove}.
     *
     * <p>Shows a progress bar during the operation. On success a toast is shown and
     * the event is reloaded. On failure the progress bar is hidden and an error toast
     * is shown.
     */
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

    /**
     * Drops the current device out of the event by atomically removing
     * {@link #deviceId} from {@code enrolledEntrants} and adding it to
     * {@code cancelledEntrants} in a single Firestore update.
     *
     * <p>Shows a progress bar during the operation. On success a toast is shown and
     * the event is reloaded. On failure the progress bar is hidden and an error toast
     * is shown.
     */
    private void dropOutOfEvent() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .update(
                        "enrolledEntrants",  FieldValue.arrayRemove(deviceId),
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