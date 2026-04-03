package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
 *   <li>View QR code → {@link QrDisplayActivity}</li>
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
public class EventDetailActivity extends AppCompatActivity
        implements OrganizerCommentAdapter.OnDeleteClickListener {

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

    /** Most recently loaded event; {@code null} until the first Firestore load completes. */
    private Event event;

    // ── Comment fields ─────────────────────────────────────────────────────────

    /** Device ID of the current organizer, used as the comment author ID. */
    private String deviceId;

    /** Display name of the organizer fetched from their Firestore profile. */
    private String organizerName = "Organizer";

    /** RecyclerView that lists all comments on this event. */
    private RecyclerView rvComments;

    /** Input field where the organizer types a new comment. */
    private EditText etNewComment;

    /** Shows a spinner while comments are being loaded from Firestore. */
    private View commentsProgressBar;

    /** Shown when there are no comments yet. */
    private TextView tvNoComments;

    /** Live list of {@link Comment} objects backing {@link #commentAdapter}. */
    private final List<Comment> commentList = new ArrayList<>();

    /** Adapter binding {@link #commentList} to {@link #rvComments}. */
    private OrganizerCommentAdapter commentAdapter;

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

        // ── Bind comment views ─────────────────────────────────────────────────
        rvComments          = findViewById(R.id.rvComments);
        etNewComment        = findViewById(R.id.etNewComment);
        etNewComment.setHintTextColor(android.graphics.Color.parseColor("#AAAAAA"));
        commentsProgressBar = findViewById(R.id.commentsProgressBar);
        tvNoComments        = findViewById(R.id.tvNoComments);

        // ── Set up comments RecyclerView ───────────────────────────────────────
        commentAdapter = new OrganizerCommentAdapter(commentList, this);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
        rvComments.setNestedScrollingEnabled(false);

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

        findViewById(R.id.rowRunLottery).setOnClickListener(v ->
                Toast.makeText(this,
                        getString(R.string.lottery_draw_coming_soon),
                        Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.rowSendNotification).setOnClickListener(v -> {
            if (event == null) {
                Toast.makeText(this, "Event not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }
            SendNotificationFragment sheet =
                    SendNotificationFragment.newInstance(eventId, event.getTitle());
            sheet.show(getSupportFragmentManager(), "sendNotification");
        });

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
            intent.putExtra("name",        event.getTitle()       != null ? event.getTitle()       : "");
            intent.putExtra("description", event.getDescription() != null ? event.getDescription() : "");
            intent.putExtra("location",    event.getLocation()    != null ? event.getLocation()    : "");
            intent.putExtra("price",       (double) event.getPrice());
            intent.putExtra("geoRequired", event.isGeoEnabled());
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

        String status = calculateStatus(
                regOpen, registrationDeadline, drawDate, startDate, endDate,
                selectedEntrants, enrolledEntrants
        );
        applyStatusBadge(status);
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
     * Loads all comments for this event from the Firestore sub-collection
     * {@code events/{eventId}/comments}, ordered by timestamp ascending.
     * Clears and repopulates {@link #commentList}, then notifies the adapter.
     * Shows {@link #tvNoComments} when the list is empty.
     */
    private void loadComments() {
        commentsProgressBar.setVisibility(View.VISIBLE);
        tvNoComments.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    commentsProgressBar.setVisibility(View.GONE);
                    commentList.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment != null) {
                            comment.setCommentId(doc.getId());
                            commentList.add(comment);
                        }
                    }

                    commentAdapter.notifyDataSetChanged();
                    tvNoComments.setVisibility(commentList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    commentsProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load comments", Toast.LENGTH_SHORT).show();
                });
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

        Comment comment = new Comment(deviceId, organizerName, text);

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

    /**
     * Handles the delete button tap on a comment row (US 02.08.01).
     * Shows a confirmation dialog, then deletes the Firestore document and
     * removes the item from the list with a smooth adapter animation.
     *
     * @param comment  The {@link Comment} to delete.
     * @param position The adapter position of the item being deleted.
     */
    @Override
    public void onDeleteClick(Comment comment, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseFirestore.getInstance()
                            .collection("events")
                            .document(eventId)
                            .collection("comments")
                            .document(comment.getCommentId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                if (position < commentList.size()) {
                                    commentList.remove(position);
                                    commentAdapter.notifyItemRemoved(position);
                                    commentAdapter.notifyItemRangeChanged(
                                            position, commentList.size());
                                }
                                tvNoComments.setVisibility(
                                        commentList.isEmpty() ? View.VISIBLE : View.GONE);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete comment",
                                            Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

}