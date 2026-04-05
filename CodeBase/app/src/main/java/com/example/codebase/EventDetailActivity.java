package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.Serializable;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Event detail screen for the organizer role.
 *
 * <p>Displays full event metadata (title, date range, location, description, poster,
 * status badge, and entrant counts) loaded from Firestore, and provides navigation
 * to related organizer actions:
 * <ul>
 *   <li>View invited entrants → {@link InvitedEntrantsActivity}</li>
 *   <li>View cancelled entrants → {@link CancelledEntrantsActivity}</li>
 *   <li>View QR code → {@link QrDisplayActivity} (hidden for private events)</li>
 *   <li>Edit event → {@link CreateEventActivity} in edit mode</li>
 *   <li>Send notification → {@link SendNotificationFragment}</li>
 *   <li>Run lottery, view waiting list, view entrant map (stubs)</li>
 * </ul>
 *
 * <p>The event document is reloaded every time the activity resumes (via
 * {@link #onResume()}) so that counts and status remain fresh after returning
 * from a sub-screen.
 *
 * @see EventSchema
 * @see EventPoster
 */
public class EventDetailActivity extends AppCompatActivity {

    /** Intent extra key for the Firestore event document ID. */
    public static final String EXTRA_EVENT_ID    = "event_id";

    /** Intent extra key for the event title (used for toolbar display). */
    public static final String EXTRA_EVENT_TITLE = "event_title";

    /** Firestore document ID of the event being displayed. */
    private String eventId;

    /** Display title of the event; updated from Firestore on load. */
    private String eventTitle;

    // ── Views ─────────────────────────────────────────────────────────────────

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

    /** Co-organizer display views (US 02.09.01). */
    private View     layoutCoOrganizerDisplay;
    private TextView tvCoOrganizerName;

    /** Most recently loaded event; {@code null} until the first Firestore load completes. */
    private Event event;

    // ── Comment fields ─────────────────────────────────────────────────────────

    /** Device ID of the current organizer, used as the comment author ID. */
    private String deviceId;

    /** Display name of the organizer fetched from their Firestore profile. */
    private String organizerName = "Organizer";

    /** Input field where the organizer types a new comment. */
    private EditText etNewComment;

    /**
     * Full list of all comments loaded from Firestore.
     * Passed to {@link ViewCommentsFragment} for client-side filtering —
     * no second network round-trip is needed when the dialogs open.
     */
    private final List<Comment> commentList = new ArrayList<>();

    /** Formats dates as {@code "MMM dd, yyyy"} for the date range display. */
    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /** Formats the event price as a locale-appropriate currency string. */
    private final NumberFormat currencyFormat =
            NumberFormat.getCurrencyInstance(Locale.getDefault());

    /**
     * Initialises the activity, binds all views, wires click listeners for every
     * action row and card, and triggers the initial Firestore load.
     *
     * <p>The Edit Event row builds an intent with all current event field values
     * pre-populated as extras so that {@link CreateEventActivity} can enter edit
     * mode without a separate Firestore fetch.
     *
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        eventId    = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);
        deviceId   = DeviceIdManager.getOrCreateDeviceId(this);

        tvDetailTitle         = findViewById(R.id.tvDetailTitle);
        tvDetailDate          = findViewById(R.id.tvDetailDate);
        tvDetailLocation      = findViewById(R.id.tvDetailLocation);
        tvDetailDescription   = findViewById(R.id.tvDetailDescription);
        tvStatusBadge         = findViewById(R.id.tvDetailStatusBadge);
        tvWaitingCount        = findViewById(R.id.tvWaitingCount);
        tvInvitedCount        = findViewById(R.id.tvInvitedCount);
        tvEnrolledCount       = findViewById(R.id.tvEnrolledCount);
        tvCancelledCount      = findViewById(R.id.tvCancelledCount);
        tvWaitingListRowCount = findViewById(R.id.tvWaitingListRowCount);
        tvMaxCapacity         = findViewById(R.id.tvMaxCapacity);
        tvWinnersCount        = findViewById(R.id.tvWinnersCount);
        tvEventCost           = findViewById(R.id.tvEventCost);
        ivHeroPoster          = findViewById(R.id.ivHeroPoster);
        progressBar           = findViewById(R.id.detailProgressBar);
        layoutCoOrganizerDisplay = findViewById(R.id.layoutCoOrganizerDisplay);
        tvCoOrganizerName        = findViewById(R.id.tvCoOrganizerName);

        // ── Bind comment views (US 02.08.02) ──────────────────────────────────
        etNewComment = findViewById(R.id.etNewComment);
        etNewComment.setHintTextColor(android.graphics.Color.parseColor("#AAAAAA"));

        // "View Entrant Comments" opens a dialog showing only entrant-posted comments
        findViewById(R.id.btnViewEntrantComments).setOnClickListener(v -> {
            ViewCommentsFragment f = ViewCommentsFragment.newInstance(
                    eventId, commentList, ViewCommentsFragment.MODE_ENTRANT);
            f.show(getSupportFragmentManager(), "entrantComments");
        });

        // "View My Comments" opens a dialog showing only organizer-posted comments
        findViewById(R.id.btnViewMyComments).setOnClickListener(v -> {
            ViewCommentsFragment f = ViewCommentsFragment.newInstance(
                    eventId, commentList, ViewCommentsFragment.MODE_ORGANIZER);
            f.show(getSupportFragmentManager(), "myComments");
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.cardInvited).setOnClickListener(v -> {
            Intent intent = new Intent(this, InvitedEntrantsActivity.class);
            intent.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_ID, eventId);
            intent.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_TITLE, eventTitle);
            startActivity(intent);
        });

        findViewById(R.id.cardEnrolled).setOnClickListener(v -> {
            if (event == null) {
                Toast.makeText(this, "Event not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, FinalEntrantListActivity.class);
            intent.putExtra("EXTRA_EVENT", (Serializable) event);
            startActivity(intent);
        });

        findViewById(R.id.rowRunLottery).setOnClickListener(v -> {
            if (event == null) {
                Toast.makeText(this, "Event not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, LotteryDrawActivity.class);
            intent.putExtra("EXTRA_EVENT", (Serializable) event);
            startActivity(intent);
        });

        findViewById(R.id.rowSendNotification).setOnClickListener(v -> {
            if (event == null) {
                Toast.makeText(this, "Event not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }
            SendNotificationFragment sheet =
                    SendNotificationFragment.newInstance(eventId, event.getTitle());
            sheet.show(getSupportFragmentManager(), "sendNotification");
        });

        // US 02.02.01 — View waiting list (stat card at top)
        findViewById(R.id.cardWaitingList).setOnClickListener(v -> {
            Intent intent = new Intent(this, WaitingListActivity.class);
            intent.putExtra(WaitingListActivity.EXTRA_EVENT_ID, eventId);
            intent.putExtra(WaitingListActivity.EXTRA_EVENT_TITLE, eventTitle);
            startActivity(intent);
        });

        // US 02.02.01 — View waiting list
        findViewById(R.id.rowViewWaitingList).setOnClickListener(v -> {
            Intent intent = new Intent(this, WaitingListActivity.class);
            intent.putExtra(WaitingListActivity.EXTRA_EVENT_ID, eventId);
            intent.putExtra(WaitingListActivity.EXTRA_EVENT_TITLE, eventTitle);
            startActivity(intent);
        });

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
            intent.putExtra("name",        event.getTitle()       != null ? event.getTitle()       : "");
            intent.putExtra("description", event.getDescription() != null ? event.getDescription() : "");
            intent.putExtra("location",    event.getLocation()    != null ? event.getLocation()    : "");
            intent.putExtra("price",       (double) event.getPrice());
            intent.putExtra("geoRequired", event.isGeoEnabled());
            intent.putExtra("isPrivate",   event.isPrivate());
            intent.putExtra("waitlistCap", event.getWaitlistCap());
            intent.putExtra("capacity",    event.getMaxCapacity() != null
                    ? event.getMaxCapacity().intValue() : 0);
            intent.putExtra("posterBase64", event.getPoster() != null
                    ? event.getPoster().getPosterImageBase64() : "");

            if (event.getStartDate()            != null) intent.putExtra("startDate",            sdf.format(event.getStartDate()));
            if (event.getEndDate()              != null) intent.putExtra("endDate",               sdf.format(event.getEndDate()));
            if (event.getRegistrationOpen()     != null) intent.putExtra("registrationOpen",      sdf.format(event.getRegistrationOpen()));
            if (event.getRegistrationDeadline() != null) intent.putExtra("registrationDeadline",  sdf.format(event.getRegistrationDeadline()));

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

        // US 02.09.01 — Assign Co-Organizer
        findViewById(R.id.rowAssignCoOrganizer).setOnClickListener(v -> {
            AssignCoOrganizerFragment fragment = AssignCoOrganizerFragment.newInstance(eventId, eventTitle);
            // Reload the event when the dialog closes so co-organizer displays immediately
            fragment.setDismissListener(() -> {
                if (eventId != null) loadEventDetails();
            });
            fragment.show(getSupportFragmentManager(), "AssignCoOrganizer");
        });

        loadEventDetails();

        // ── Wire comment post button and load comments ──────────────────────────
        findViewById(R.id.btnPostComment).setOnClickListener(v -> postComment());
        loadOrganizerName();
        loadComments();
    }

    /**
     * Fetches the event document from Firestore and delegates to
     * {@link #onEventLoaded(DocumentSnapshot)} on success.
     *
     * <p>Shows the progress bar while the request is in flight and hides it on
     * failure, displaying a localised error toast.
     */
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

    /**
     * Populates all UI fields from the loaded {@link Event}.
     *
     * <p>Handles poster display with three fallback tiers:
     * <ol>
     *   <li>Decode Base64 poster and set as bitmap.</li>
     *   <li>If decoding returns {@code null}, fall back to
     *       {@code bg_hero_image} drawable.</li>
     *   <li>If decoding throws, fall back to {@code bg_hero_image} drawable.</li>
     * </ol>
     *
     * <p>Computes entrant counts from the four list fields and delegates status
     * calculation to {@link #calculateStatus} and badge styling to
     * {@link #applyStatusBadge(String)}.
     *
     * <p>Finishes the activity with a toast if the document does not exist or
     * cannot be normalised by {@link EventSchema#normalizeLoadedEvent(DocumentSnapshot)}.
     *
     * @param doc The Firestore {@link DocumentSnapshot} for the event.
     */
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
                android.graphics.Bitmap bitmap =
                        EventPoster.decodeImage(event.getPoster().getPosterImageBase64());
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

        eventTitle  = event.getTitle()       != null ? event.getTitle()       : getString(R.string.untitled_event);
        String location    = event.getLocation()    != null ? event.getLocation()    : "";
        String description = event.getDescription() != null ? event.getDescription() : "";

        tvDetailTitle.setText(eventTitle);
        tvDetailLocation.setText(location);
        tvDetailDescription.setText(description);

        Date regOpen              = event.getRegistrationOpen();
        Date registrationDeadline = event.getRegistrationDeadline();
        Date drawDate             = event.getDrawDate();
        Date startDate            = event.getStartDate();
        Date endDate              = event.getEndDate();

        tvDetailDate.setText(formatEventDateRange(startDate, endDate));

        Long  maxCapacity = event.getMaxCapacity();
        int   waitlistCap = event.getWaitlistCap();
        float price       = event.getPrice();

        tvEventCost.setText(currencyFormat.format(price));
        tvMaxCapacity.setText((maxCapacity != null && maxCapacity > 0)
                ? String.valueOf(maxCapacity)
                : getString(R.string.not_applicable_short));
        tvWinnersCount.setText(waitlistCap > 0
                ? String.valueOf(waitlistCap)
                : getString(R.string.not_applicable_short));

        List<String> waitingList       = event.getWaitingList();
        List<String> selectedEntrants  = event.getSelectedEntrants();
        List<String> enrolledEntrants  = event.getEnrolledEntrants();
        List<String> cancelledEntrants = event.getCancelledEntrants();

        int waitingCount   = waitingList       != null ? waitingList.size()       : 0;
        int invitedCount   = selectedEntrants  != null ? selectedEntrants.size()  : 0;
        int enrolledCount  = enrolledEntrants  != null ? enrolledEntrants.size()  : 0;
        int cancelledCount = cancelledEntrants != null ? cancelledEntrants.size() : 0;

        tvWaitingCount.setText(String.valueOf(waitingCount));
        tvInvitedCount.setText(String.valueOf(invitedCount));
        tvEnrolledCount.setText(String.valueOf(enrolledCount));
        tvCancelledCount.setText(String.valueOf(cancelledCount));
        tvWaitingListRowCount.setText(String.valueOf(waitingCount));

        // US 02.09.01 — show the assigned co-organizer name if one exists
        ArrayList<String> coOrganizers = event.getCoOrganizers();
        if (coOrganizers != null && !coOrganizers.isEmpty()) {
            String coOrgDeviceId = coOrganizers.get(0);
            layoutCoOrganizerDisplay.setVisibility(android.view.View.VISIBLE);
            tvCoOrganizerName.setText("Loading...");
            AppDatabase.getInstance().usersRef.document(coOrgDeviceId)
                    .get()
                    .addOnSuccessListener(userSnap -> {
                        String name = userSnap.exists() ? userSnap.getString("name") : null;
                        tvCoOrganizerName.setText(
                                (name != null && !name.isEmpty()) ? name : coOrgDeviceId);
                    })
                    .addOnFailureListener(e -> tvCoOrganizerName.setText(coOrgDeviceId));
        } else {
            layoutCoOrganizerDisplay.setVisibility(android.view.View.GONE);
        }

        String status = calculateStatus(
                regOpen, registrationDeadline, drawDate, startDate, endDate,
                selectedEntrants, enrolledEntrants
        );
        applyStatusBadge(status);

        // US 02.01.02: Hide "View Event QR Code" row for private events
        // Private events do not have a promotional QR code
        View rowViewQrCode = findViewById(R.id.rowViewQrCode);
        if (rowViewQrCode != null) {
            rowViewQrCode.setVisibility(event.isPrivate() ? View.GONE : View.VISIBLE);
        }

        // US 02.01.03: Show "Invite Entrants" button only for private events
        View rowInviteEntrants = findViewById(R.id.rowInviteEntrants);
        if (rowInviteEntrants != null) {
            if (event.isPrivate()) {
                rowInviteEntrants.setVisibility(View.VISIBLE);
                rowInviteEntrants.setOnClickListener(v -> {
                    Intent intent = new Intent(this, InviteEntrantsActivity.class);
                    intent.putExtra(InviteEntrantsActivity.EXTRA_EVENT_ID, eventId);
                    intent.putExtra(InviteEntrantsActivity.EXTRA_EVENT_TITLE, eventTitle);
                    startActivity(intent);
                });
            } else {
                rowInviteEntrants.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Derives a human-readable event lifecycle status string from the event's key dates
     * and entrant lists.
     *
     * <p>Status values and their conditions (evaluated in order):
     * <ol>
     *   <li><b>"Draft"</b> — any required date ({@code regOpen}, {@code registrationDeadline},
     *       {@code drawDate}, {@code startDate}, {@code endDate}) is {@code null}.</li>
     *   <li><b>"Registration Opening Soon"</b> — today is before {@code regOpen}.</li>
     *   <li><b>"Registration Open"</b> — today is between {@code regOpen} and
     *       {@code registrationDeadline} (inclusive).</li>
     *   <li><b>"Registration Closed / Lottery Opening Soon"</b> — today is after
     *       {@code registrationDeadline} but before {@code drawDate}, and no entrants
     *       have been selected yet.</li>
     *   <li><b>"Lottery Closed & Event Scheduled"</b> — today is after {@code drawDate}
     *       and before {@code startDate}, and selected entrants exist.</li>
     *   <li><b>"In Progress"</b> — today is between {@code startDate} and {@code endDate}
     *       (inclusive), and enrolled entrants exist.</li>
     *   <li><b>"Event Ended"</b> — today is after {@code endDate}.</li>
     *   <li><b>"Draft"</b> — fallback for any other condition.</li>
     * </ol>
     *
     * @param regOpen              Date registration opens; {@code null} triggers "Draft".
     * @param registrationDeadline Date registration closes; {@code null} triggers "Draft".
     * @param drawDate             Date of the lottery draw; {@code null} triggers "Draft".
     * @param startDate            Event start date; {@code null} triggers "Draft".
     * @param endDate              Event end date; {@code null} triggers "Draft".
     * @param selectedEntrants     List of selected entrant device IDs; may be {@code null}.
     * @param enrolledEntrants     List of enrolled entrant device IDs; may be {@code null}.
     * @return A non-null status string describing the event's current lifecycle phase.
     */
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
        } else if (today.after(registrationDeadline) && today.before(drawDate) && selectedEmpty) {
            return "Registration Closed / Lottery Opening Soon";
        } else if (today.after(drawDate) && today.before(startDate) && !selectedEmpty) {
            return "Lottery Closed & Event Scheduled";
        } else if (!today.before(startDate) && !today.after(endDate) && !enrolledEmpty) {
            return "In Progress";
        } else if (today.after(endDate)) {
            return "Event Ended";
        } else {
            return "Draft";
        }
    }

    /**
     * Formats a start and end date into a display string.
     *
     * <ul>
     *   <li>If {@code startDate} is {@code null}, returns the localised "date not set" string.</li>
     *   <li>If {@code endDate} is {@code null}, returns the formatted start date alone.</li>
     *   <li>If both dates format to the same string (same-day event), returns that string once.</li>
     *   <li>Otherwise returns {@code "<startDate> - <endDate>"}.</li>
     * </ul>
     *
     * @param startDate The event start date, or {@code null}.
     * @param endDate   The event end date, or {@code null}.
     * @return A formatted date range string for display in {@code tvDetailDate}.
     */
    private String formatEventDateRange(Date startDate, Date endDate) {
        if (startDate == null) {
            return getString(R.string.date_not_set);
        }
        if (endDate == null) {
            return displayDateFormat.format(startDate);
        }
        String startText = displayDateFormat.format(startDate);
        String endText   = displayDateFormat.format(endDate);
        if (startText.equals(endText)) {
            return startText;
        }
        return startText + " - " + endText;
    }

    /**
     * Sets the text, background drawable, and text colour of {@link #tvStatusBadge}
     * to match the given status string.
     *
     * <p>Colour scheme:
     * <ul>
     *   <li>Green pill — {@code "Registration Open"}, {@code "In Progress"}</li>
     *   <li>Amber pill — {@code "Registration Opening Soon"},
     *       {@code "Registration Closed / Lottery Opening Soon"},
     *       {@code "Lottery Closed & Event Scheduled"}, and the default fallback</li>
     *   <li>Red pill — {@code "Event Ended"}</li>
     * </ul>
     *
     * @param status The status string produced by {@link #calculateStatus}.
     */
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

    /**
     * Reloads the event details from Firestore each time the activity resumes,
     * ensuring entrant counts and status badge stay current after returning from
     * sub-screens such as {@link InvitedEntrantsActivity} or
     * {@link CancelledEntrantsActivity}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null) {
            loadEventDetails();
        }
    }

    // ── Comments (US 02.08.02) ─────────────────────────────────────────────────

    /**
     * Fetches the organizer's display name from their Firestore user document
     * ({@code users/{deviceId}}) so that it can be attached to new comments.
     * Falls back to {@code "Organizer"} if the name field is blank.
     */
    private void loadOrganizerName() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(deviceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.trim().isEmpty()) {
                            organizerName = name.trim();
                        }
                    }
                });
    }

    /**
     * Loads all comments from {@code events/{eventId}/comments} ordered by
     * timestamp ascending into {@link #commentList}.
     * The list is used by {@link ViewCommentsFragment} when the view buttons are tapped.
     */
    private void loadComments() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    commentList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment != null) {
                            comment.setCommentId(doc.getId());
                            commentList.add(comment);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load comments", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Posts a new comment from the organizer to Firestore.
     * Validates the input, writes a {@link Comment} doc to
     * {@code events/{eventId}/comments}, then reloads the list on success.
     */
    private void postComment() {
        String text = etNewComment.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            etNewComment.setError("Comment cannot be empty");
            return;
        }

        // Append "(Organizer)" tag so the comment is visually distinguished.
        // isOrganizer=true lets ViewCommentsFragment filter it into "My Comments".
        String displayName = organizerName + " (Organizer)";
        Comment comment = new Comment(deviceId, displayName, text, true);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(docRef -> {
                    etNewComment.setText("");
                    loadComments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show()
                );
    }
}