package com.example.codebase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Displays the full details of an event from an entrant's perspective and
 * provides actions to join/leave the waiting list, drop out of an accepted
 * invitation, post a comment, and view filtered comment lists.
 *
 * <p><b>Comment features (US 01.08.01 &amp; US 01.08.02):</b></p>
 * <ul>
 *   <li>An {@link EditText} + "Post" button lets the entrant submit a comment
 *       stored in {@code events/{eventId}/comments} with {@code isOrganizer=false}.</li>
 *   <li>"View Organizer Comments" opens {@link EntrantViewCommentsFragment} in
 *       {@link EntrantViewCommentsFragment#MODE_ORGANIZER}.</li>
 *   <li>"View All Entrant Comments" opens {@link EntrantViewCommentsFragment} in
 *       {@link EntrantViewCommentsFragment#MODE_ENTRANT}.</li>
 *   <li>No delete action is available — entrants may only view (US 01.08.02).</li>
 * </ul>
 *
 * <p>Button visibility and enabled state for waiting-list actions reflect the
 * entrant's current relationship to the event:</p>
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
 * {@link FieldValue#arrayRemove} to avoid race conditions on the entrant lists.</p>
 *
 * @see DeviceIdManager
 * @see EventPoster
 * @see EntrantViewCommentsFragment
 * @see EntrantCommentAdapter
 */
public class EntrantEventDetailActivity extends AppCompatActivity {

    /** Intent extra key for the Firestore event document ID. */
    public static final String EXTRA_EVENT_ID = "event_id";

    private static final String[] LOCATION_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    /** Firestore document ID of the event being displayed. */
    private String eventId;

    /** Device ID of the current user, used to check list membership and author comments. */
    private String deviceId;

    /**
     * Display name fetched from the entrant's Firestore profile.
     * Falls back to {@code "Entrant"} if the profile has no name.
     */
    private String entrantName = "Entrant";

    /** The most recently loaded {@link Event} object; {@code null} until the first load completes. */
    private Event event;

    // ── Views ─────────────────────────────────────────────────────────────────

    private TextView tvTitle, tvDate, tvLocation, tvWaitingListCount, tvDescription;
    private ImageView ivPoster;

    /** Adds the current device to the event's {@code waitingList}. */
    private Button btnJoin;

    /** Removes the current device from the event's {@code waitingList}. */
    private Button btnLeave;

    /** Removes the current device from {@code enrolledEntrants} and adds to {@code cancelledEntrants}. */
    private Button btnDropOut;

    /** Container holding {@link #btnJoin} and {@link #btnLeave}; hidden when the user is enrolled. */
    private View layoutJoinLeave;

    /** Shown below the Join button when the entrant is a co-organizer for this event. */
    private TextView tvCoOrgWarning;

    private View progressBar;

    // ── Comment views (US 01.08.01 & US 01.08.02) ────────────────────────────

    /** Input field where the entrant types a new comment. */
    private EditText etNewComment;

    /** Submits the text in {@link #etNewComment} as a new comment. */
    private Button btnPostComment;

    /**
     * Opens {@link EntrantViewCommentsFragment} filtered to organizer comments only.
     * US 01.08.02 — "View Organizer Comments".
     */
    private Button btnViewOrganizerComments;

    /**
     * Opens {@link EntrantViewCommentsFragment} filtered to entrant comments only.
     * US 01.08.02 — "View All Entrant Comments".
     */
    private Button btnViewEntrantComments;

    /**
     * Full list of all comments loaded from Firestore.
     * Passed to {@link EntrantViewCommentsFragment} for client-side filtering —
     * no second Firestore round-trip is needed when the dialogs open.
     */
    private final List<Comment> commentList = new ArrayList<>();

    /** Date formatter used to display the event start date as {@code "MMM dd, yyyy · HH:mm"}. */
    private final SimpleDateFormat displayFormat =
            new SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateOnlyFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean pendingGeoJoin;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted =
                            Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                                    || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                    if (pendingGeoJoin && granted) {
                        pendingGeoJoin = false;
                        joinWaitingList();
                    } else if (pendingGeoJoin) {
                        pendingGeoJoin = false;
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(
                                this,
                                getString(R.string.location_required_join_denied),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );

        // ── Event detail views ────────────────────────────────────────────────
        tvTitle         = findViewById(R.id.tvEntrantEventTitle);
        tvDate          = findViewById(R.id.tvEntrantEventDate);
        tvLocation      = findViewById(R.id.tvEntrantEventLocation);
        tvWaitingListCount = findViewById(R.id.tvEntrantWaitingListCount);
        tvDescription   = findViewById(R.id.tvEntrantEventDescription);
        ivPoster        = findViewById(R.id.ivEntrantHeroPoster);
        btnJoin         = findViewById(R.id.btnJoinWaitingList);
        btnLeave        = findViewById(R.id.btnLeaveWaitingList);
        btnDropOut      = findViewById(R.id.btnDropOut);
        layoutJoinLeave = findViewById(R.id.layoutJoinLeave);
        progressBar     = findViewById(R.id.entrantDetailProgressBar);
        tvCoOrgWarning  = findViewById(R.id.tvCoOrgWarning);

        // ── Comment views (US 01.08.01 & US 01.08.02) ────────────────────────
        etNewComment            = findViewById(R.id.etEntrantNewComment);
        btnPostComment          = findViewById(R.id.btnEntrantPostComment);
        btnViewOrganizerComments = findViewById(R.id.btnViewOrganizerComments);
        btnViewEntrantComments  = findViewById(R.id.btnViewAllEntrantComments);

        // ── Button listeners ─────────────────────────────────────────────────
        findViewById(R.id.btnEntrantBack).setOnClickListener(v -> finish());
        btnJoin.setOnClickListener(v    -> showJoinConfirmation());
        btnLeave.setOnClickListener(v   -> showLeaveConfirmation());
        btnDropOut.setOnClickListener(v -> showDropOutConfirmation());

        btnPostComment.setOnClickListener(v -> postComment());

        btnViewOrganizerComments.setOnClickListener(v ->
                openCommentDialog(EntrantViewCommentsFragment.MODE_ORGANIZER));

        btnViewEntrantComments.setOnClickListener(v ->
                openCommentDialog(EntrantViewCommentsFragment.MODE_ENTRANT));

        findViewById(R.id.btnViewLottoInfo).setOnClickListener(v -> {
            startActivity(new Intent(this, LotteryInfoActivity.class));
        });


        // Load the entrant's display name first, then event details
        loadEntrantName();
        loadEventDetails();
        loadComments();
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    /**
     * Fetches the entrant's display name from their Firestore profile document.
     * The name is used as {@link Comment#getAuthorName()} when posting a comment.
     * Falls back to {@code "Entrant"} if the document or field is missing.
     */
    private void loadEntrantName() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(deviceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            entrantName = name;
                        }
                    }
                });
    }

    /**
     * Fetches the event document from Firestore and delegates to
     * {@link #onEventLoaded(DocumentSnapshot)} on success.
     *
     * <p>Shows the progress bar while the request is in-flight and hides it on
     * failure, displaying a toast error message.</p>
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
     * Loads all comments for this event from Firestore in chronological order
     * (oldest first, via {@code orderBy("timestamp")}).
     *
     * <p>Results are stored in {@link #commentList}. The comment buttons are
     * always available regardless of whether comments have loaded yet — the list
     * is simply empty until this completes.</p>
     */
    private void loadComments() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    commentList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Comment c = doc.toObject(Comment.class);
                        if (c != null) {
                            c.setCommentId(doc.getId());
                            commentList.add(c);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load comments", Toast.LENGTH_SHORT).show());
    }

    /**
     * Populates all UI fields from the loaded {@link Event} and refreshes button
     * states via {@link #updateButtonStates()}.
     *
     * <p>If the document cannot be mapped to an {@link Event} object (returns
     * {@code null}), the method returns silently without updating the UI.</p>
     *
     * @param doc The Firestore {@link DocumentSnapshot} for the event.
     */
    private void onEventLoaded(DocumentSnapshot doc) {
        progressBar.setVisibility(View.GONE);
        event = EventSchema.normalizeLoadedEvent(doc);
        if (event == null) return;

        tvTitle.setText(resolveTitle(event));
        tvLocation.setText(resolveLocation(event));
        // Keep the current waiting-list total visible so entrants understand demand.
        tvWaitingListCount.setText(getString(
                R.string.entrant_waiting_list_count,
                event.getWaitingList().size()
        ));
        tvDescription.setText(resolveDescription(event));
        tvDate.setText(resolveDateRange(event));

        if (event.getPoster() != null && event.getPoster().getPosterImageBase64() != null) {
            ivPoster.setImageBitmap(
                    EventPoster.decodeImage(event.getPoster().getPosterImageBase64()));
        }

        updateButtonStates();
    }

    private String resolveTitle(Event currentEvent) {
        String title = currentEvent.getTitle();
        return title == null || title.trim().isEmpty()
                ? getString(R.string.untitled_event)
                : title.trim();
    }

    private String resolveLocation(Event currentEvent) {
        String location = currentEvent.getLocation();
        return location == null || location.trim().isEmpty()
                ? getString(R.string.history_location_not_set)
                : location.trim();
    }

    private String resolveDescription(Event currentEvent) {
        String description = currentEvent.getDescription();
        return description == null || description.trim().isEmpty()
                ? getString(R.string.entrant_event_description_fallback)
                : description.trim();
    }

    private String resolveDateRange(Event currentEvent) {
        Date startDate = currentEvent.getStartDate();
        Date endDate = currentEvent.getEndDate();

        if (startDate == null && endDate == null) {
            return getString(R.string.date_not_set);
        }
        if (startDate != null && endDate != null) {
            String start = dateOnlyFormat.format(startDate);
            String end = dateOnlyFormat.format(endDate);
            return start.equals(end) ? start : start + " - " + end;
        }
        return dateOnlyFormat.format(startDate != null ? startDate : endDate);
    }

    // ── Comment actions (US 01.08.01) ─────────────────────────────────────────

    /**
     * Reads the text from {@link #etNewComment} and writes a new {@link Comment}
     * document to {@code events/{eventId}/comments} in Firestore.
     *
     * <p>The comment is tagged with {@code isOrganizer=false} (entrant comment).
     * On success the input field is cleared and {@link #loadComments()} is called
     * to refresh the in-memory list so the view dialogs reflect the new comment.</p>
     */
    private void postComment() {
        String text = etNewComment.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // isOrganizer = false because this is posted from the entrant screen
        Comment comment = new Comment(deviceId, entrantName, text, false);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(ref -> {
                    etNewComment.setText("");
                    Toast.makeText(this, "Comment posted!", Toast.LENGTH_SHORT).show();
                    loadComments();   // refresh the cached list
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show());
    }

    // ── Comment view dialogs (US 01.08.02) ────────────────────────────────────

    /**
     * Opens a {@link EntrantViewCommentsFragment} dialog filtered by the given mode.
     *
     * @param mode {@link EntrantViewCommentsFragment#MODE_ORGANIZER} to show only
     *             organizer comments, or {@link EntrantViewCommentsFragment#MODE_ENTRANT}
     *             to show all entrant comments.
     */
    private void openCommentDialog(int mode) {
        EntrantViewCommentsFragment dialog =
                EntrantViewCommentsFragment.newInstance(commentList, mode);
        dialog.show(getSupportFragmentManager(), "entrant_comments");
    }

    // ── Waiting-list button state ─────────────────────────────────────────────

    /**
     * Updates the visibility and enabled state of the Join, Leave, and Drop Out
     * buttons based on the current device's membership in the event's entrant lists.
     *
     * <p>Decision logic:</p>
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
                boolean isRegOpen = isRegistrationOpenForJoin(event);
                boolean isPendingSelection = event.getSelectedEntrants() != null
                        && event.getSelectedEntrants().contains(deviceId);

                if (isRegOpen && !isPendingSelection) {
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

    // ── Confirmation dialogs ──────────────────────────────────────────────────

    /**
     * Shows a confirmation dialog before adding the device to the waiting list.
     * Calls {@link #joinWaitingList()} if the user confirms.
     */
    private void showJoinConfirmation() {
        boolean geoRequired = GeoJoinPolicy.requiresLocationCapture(event);
        new AlertDialog.Builder(this)
                .setTitle("Join Waiting List")
                .setMessage(geoRequired
                        ? getString(R.string.location_required_join_message)
                        : "Would you like to join the waiting list for this event?")
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

    // ── Firestore mutations ───────────────────────────────────────────────────

    /**
     * Adds {@link #deviceId} to the event's {@code waitingList} in Firestore using
     * {@link FieldValue#arrayUnion}.
     *
     * <p>Checks server-side whether the entrant has since become a co-organizer
     * before writing, to guard against a race condition.</p>
     */
    private void joinWaitingList() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Event latestEvent = EventSchema.normalizeLoadedEvent(snapshot);
                    if (latestEvent == null) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to join", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (latestEvent != null
                            && latestEvent.getCoOrganizers().contains(deviceId)) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this,
                                "You are a co-organizer for this event and cannot join the waiting list.",
                                Toast.LENGTH_LONG).show();
                        loadEventDetails();
                        return;
                    }

                    if (isAlreadyActiveParticipant(latestEvent)) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "You already have an active place in this event.", Toast.LENGTH_SHORT).show();
                        loadEventDetails();
                        return;
                    }

                    if (!isRegistrationOpenForJoin(latestEvent)) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Registration is not currently open for this event.", Toast.LENGTH_SHORT).show();
                        loadEventDetails();
                        return;
                    }

                    if (!hasWaitlistCapacity(latestEvent)) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "This waiting list is full.", Toast.LENGTH_SHORT).show();
                        loadEventDetails();
                        return;
                    }

                    verifyPrivateInviteEligibility(snapshot.getReference(), latestEvent, () -> {
                        if (GeoJoinPolicy.requiresLocationCapture(latestEvent)) {
                            ensureLocationAndJoin();
                            return;
                        }

                        commitJoinToFirestore(null);
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to join", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isRegistrationOpenForJoin(Event currentEvent) {
        if (currentEvent == null) {
            return false;
        }

        Date now = new Date();
        Date regOpen = currentEvent.getRegistrationOpen();
        Date regDeadline = currentEvent.getRegistrationDeadline();

        if (regOpen != null && now.before(regOpen)) {
            return false;
        }
        return regDeadline == null || !now.after(regDeadline);
    }

    private boolean isAlreadyActiveParticipant(Event currentEvent) {
        return (currentEvent.getWaitingList() != null && currentEvent.getWaitingList().contains(deviceId))
                || (currentEvent.getSelectedEntrants() != null && currentEvent.getSelectedEntrants().contains(deviceId))
                || (currentEvent.getEnrolledEntrants() != null && currentEvent.getEnrolledEntrants().contains(deviceId));
    }

    private boolean hasWaitlistCapacity(Event currentEvent) {
        int waitlistCap = currentEvent.getWaitlistCap();
        return waitlistCap <= 0
                || currentEvent.getWaitingList() == null
                || currentEvent.getWaitingList().size() < waitlistCap;
    }

    private void verifyPrivateInviteEligibility(DocumentReference eventRef,
                                                Event currentEvent,
                                                Runnable onEligible) {
        if (currentEvent == null || !currentEvent.isPrivate()) {
            onEligible.run();
            return;
        }

        eventRef.collection(PrivateEventInvite.SUBCOLLECTION)
                .document(deviceId)
                .get()
                .addOnSuccessListener(inviteSnapshot -> {
                    PrivateEventInvite invite = inviteSnapshot.toObject(PrivateEventInvite.class);
                    if (invite == null
                            || !PrivateEventInvite.STATUS_ACCEPTED.equals(invite.getStatus())) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(
                                this,
                                "Accept your private event invitation before joining the waiting list.",
                                Toast.LENGTH_LONG
                        ).show();
                        loadEventDetails();
                        return;
                    }
                    onEligible.run();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to verify your invitation", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Removes {@link #deviceId} from the event's {@code waitingList} in Firestore
     * using {@link FieldValue#arrayRemove}.
     */
    private void leaveWaitingList() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference eventRef = db.collection("events").document(eventId);
        WriteBatch batch = db.batch();
        batch.update(eventRef, "waitingList", FieldValue.arrayRemove(deviceId));
        batch.delete(eventRef.collection("entrantLocations").document(deviceId));
        batch.commit()
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

    private void ensureLocationAndJoin() {
        if (hasLocationPermission()) {
            captureLocationAndJoin();
            return;
        }

        progressBar.setVisibility(View.GONE);
        pendingGeoJoin = true;

        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.location_required_dialog_title)
                    .setMessage(R.string.location_required_dialog_message)
                    .setPositiveButton(R.string.location_required_dialog_continue,
                            (dialog, which) -> locationPermissionLauncher.launch(LOCATION_PERMISSIONS))
                    .setNegativeButton(R.string.location_required_dialog_cancel,
                            (dialog, which) -> pendingGeoJoin = false)
                    .show();
            return;
        }

        locationPermissionLauncher.launch(LOCATION_PERMISSIONS);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void captureLocationAndJoin() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        commitJoinToFirestore(buildEntrantLocation(location));
                        return;
                    }

                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(lastLocation -> {
                                if (lastLocation != null) {
                                    commitJoinToFirestore(buildEntrantLocation(lastLocation));
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(
                                            this,
                                            getString(R.string.location_required_join_unavailable),
                                            Toast.LENGTH_LONG
                                    ).show();
                                    loadEventDetails();
                                }
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(
                                        this,
                                        getString(R.string.location_required_join_unavailable),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(
                            this,
                            getString(R.string.location_required_join_unavailable),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private EntrantLocation buildEntrantLocation(Location location) {
        return new EntrantLocation(
                deviceId,
                entrantName,
                location.getLatitude(),
                location.getLongitude(),
                new Date()
        );
    }

    private void commitJoinToFirestore(EntrantLocation entrantLocation) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference entrantLocationRef =
                eventRef.collection("entrantLocations").document(deviceId);
        WriteBatch batch = db.batch();

        batch.update(
                eventRef,
                "waitingList", FieldValue.arrayUnion(deviceId),
                "registeredEntrants", FieldValue.arrayUnion(deviceId)
        );

        if (entrantLocation != null) {
            batch.set(entrantLocationRef, entrantLocation);
        } else {
            batch.delete(entrantLocationRef);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to join", Toast.LENGTH_SHORT).show();
                });
    }
}
