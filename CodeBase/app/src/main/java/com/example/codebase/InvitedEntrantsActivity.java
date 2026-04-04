package com.example.codebase;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays all selected entrants for an event, split across four tabs:
 * All, Pending, Accepted, and Declined.
 *
 * <p>Entrant status is derived from the three Firestore lists as follows:
 * <ul>
 *   <li><b>Accepted</b> — present in {@code enrolledEntrants}</li>
 *   <li><b>Declined</b> — present in {@code cancelledEntrants} ∩
 *       ({@code selectedEntrants} − {@code enrolledEntrants})</li>
 *   <li><b>Pending</b> — present in {@code selectedEntrants} but absent from
 *       both {@code enrolledEntrants} and the derived declined set</li>
 * </ul>
 *
 * <p>Implements US 02.06.04 (organizer cancel entrant): the Cancel action is
 * only enabled after the event's registration deadline has passed. On
 * confirmation the entrant is atomically removed from {@code selectedEntrants}
 * and added to {@code cancelledEntrants} in Firestore.
 *
 * @see InvitedEntrantsAdapter
 * @see InvitedEntrant
 */
public class InvitedEntrantsActivity extends AppCompatActivity
        implements InvitedEntrantsAdapter.CancelListener {

    /** Intent extra key for the Firestore event document ID. */
    public static final String EXTRA_EVENT_ID    = "event_id";

    /** Intent extra key for the event title displayed in the toolbar. */
    public static final String EXTRA_EVENT_TITLE = "event_title";

    // ── Tab index constants ───────────────────────────────────────────────────

    /** Tab index showing all selected entrants regardless of status. */
    private static final int TAB_ALL      = 0;

    /** Tab index showing entrants who have not yet responded. */
    private static final int TAB_PENDING  = 1;

    /** Tab index showing entrants who accepted their invitation. */
    private static final int TAB_ACCEPTED = 2;

    /** Tab index showing entrants who declined their invitation. */
    private static final int TAB_DECLINED = 3;

    /** The tab index currently selected; defaults to {@link #TAB_ALL}. */
    private int currentTab = TAB_ALL;

    // ── Views ─────────────────────────────────────────────────────────────────

    private TextView tvTitle;
    private TextView tvCount;
    private TextView tvTabAll;
    private TextView tvTabPending;
    private TextView tvTabAccepted;
    private TextView tvTabDeclined;
    private RecyclerView rvEntrants;
    private TextView tvNoEntrants;
    private View progressBar;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Firestore document ID of the event being viewed. */
    private String eventId;

    /** Display title of the event shown in the toolbar. */
    private String eventTitle;

    /**
     * Registration deadline loaded from Firestore. Passed to the adapter so it
     * can enable or disable the Cancel button based on whether the deadline has
     * passed. {@code null} until the Firestore document is successfully loaded.
     */
    private Date registrationDeadlineDate = null;

    // ── Entrant lists (populated in processDocument) ──────────────────────────

    /** All selected entrants regardless of status. */
    private List<InvitedEntrant> allList     = new ArrayList<>();

    /** Entrants who have not yet accepted or declined. */
    private List<InvitedEntrant> pendingList  = new ArrayList<>();

    /** Entrants who have accepted their invitation. */
    private List<InvitedEntrant> acceptedList = new ArrayList<>();

    /** Entrants who have declined their invitation. */
    private List<InvitedEntrant> declinedList = new ArrayList<>();

    private InvitedEntrantsAdapter adapter;

    /**
     * Initialises the activity, binds views, wires tab and back-button listeners,
     * sets up the {@link RecyclerView}, and triggers the initial Firestore load.
     *
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invited_entrants);

        eventId    = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        tvTitle       = findViewById(R.id.tvInvitedTitle);
        tvCount       = findViewById(R.id.tvInvitedCount);
        tvTabAll      = findViewById(R.id.tabAll);
        tvTabPending  = findViewById(R.id.tabPending);
        tvTabAccepted = findViewById(R.id.tabAccepted);
        tvTabDeclined = findViewById(R.id.tabDeclined);
        rvEntrants    = findViewById(R.id.rvInvitedEntrants);
        tvNoEntrants  = findViewById(R.id.tvNoInvitedEntrants);
        progressBar   = findViewById(R.id.invitedProgressBar);

        tvTitle.setText(eventTitle != null ? eventTitle : "Invited Entrants");

        findViewById(R.id.btnBackInvited).setOnClickListener(v -> finish());

        tvTabAll.setOnClickListener(v      -> switchTab(TAB_ALL));
        tvTabPending.setOnClickListener(v  -> switchTab(TAB_PENDING));
        tvTabAccepted.setOnClickListener(v -> switchTab(TAB_ACCEPTED));
        tvTabDeclined.setOnClickListener(v -> switchTab(TAB_DECLINED));

        adapter = new InvitedEntrantsAdapter(new ArrayList<>(), this);
        rvEntrants.setLayoutManager(new LinearLayoutManager(this));
        rvEntrants.setAdapter(adapter);

        switchTab(TAB_ALL);
        loadEntrants();
    }

    // ─── Firestore loading ────────────────────────────────────────────────────

    /**
     * Fetches the event document from Firestore and delegates parsing to
     * {@link #processDocument(DocumentSnapshot)}.
     *
     * <p>Shows a progress indicator while the request is in flight and hides it
     * on failure, displaying a toast error message.
     */
    private void loadEntrants() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::processDocument)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load entrants", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Parses the Firestore {@link DocumentSnapshot} into categorised entrant lists
     * and refreshes the UI.
     *
     * <p>Derives entrant status using the following set logic:
     * <ul>
     *   <li>{@code declined} = {@code cancelledEntrants} ∩
     *       ({@code selectedEntrants} − {@code enrolledEntrants})</li>
     *   <li>{@code pending} = {@code selectedEntrants} − {@code enrolledEntrants}
     *       − {@code declined}</li>
     *   <li>{@code accepted} = {@code enrolledEntrants} ∩ {@code selectedEntrants}</li>
     * </ul>
     *
     * <p>Finishes the activity with a toast if the document does not exist or cannot
     * be normalised.
     *
     * @param doc The Firestore document snapshot for the event.
     */
    private void processDocument(DocumentSnapshot doc) {
        progressBar.setVisibility(View.GONE);

        if (!doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Event event = EventSchema.normalizeLoadedEvent(doc);
        if (event == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        registrationDeadlineDate = event.getRegistrationDeadline();
        adapter.setRegistrationDeadlineDate(registrationDeadlineDate);

        List<String> selected  = event.getSelectedEntrants();
        List<String> enrolled  = event.getEnrolledEntrants();
        List<String> cancelled = event.getCancelledEntrants();

        List<String> selectedMinusEnrolled = new ArrayList<>(selected);
        selectedMinusEnrolled.removeAll(enrolled);

        List<String> declined = new ArrayList<>(cancelled);
        declined.retainAll(selectedMinusEnrolled);

        List<String> pending = new ArrayList<>(selected);
        pending.removeAll(enrolled);
        pending.removeAll(declined);

        allList.clear();
        pendingList.clear();
        acceptedList.clear();
        declinedList.clear();

        for (String deviceId : selected) {
            String status;
            if (enrolled.contains(deviceId))      status = "Accepted";
            else if (declined.contains(deviceId)) status = "Declined";
            else                                  status = "Pending";

            InvitedEntrant entrant = new InvitedEntrant(deviceId, status);
            allList.add(entrant);
            switch (status) {
                case "Accepted": acceptedList.add(entrant); break;
                case "Declined": declinedList.add(entrant); break;
                case "Pending":  pendingList.add(entrant);  break;
            }
        }

        updateTabCount();
        switchTab(currentTab);
    }

    // ─── US 02.06.04 — Cancel entrant ────────────────────────────────────────

    /**
     * Called by the adapter when the organizer presses Cancel on a Pending entrant.
     *
     * <p>Presents a confirmation {@link AlertDialog} before any Firestore write is
     * performed. On confirmation, delegates to
     * {@link #cancelEntrantInFirestore(String)}.
     *
     * @param entrant The {@link InvitedEntrant} the organizer wishes to cancel.
     */
    @Override
    public void onCancelRequested(InvitedEntrant entrant) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Entrant")
                .setMessage("Are you sure you want to cancel this entrant?\n\n"
                        + "They will be removed from the selected list.")
                .setPositiveButton("Yes, Cancel", (dialog, which) ->
                        cancelEntrantInFirestore(entrant.deviceId))
                .setNegativeButton("Go Back", null)
                .show();
    }

    /**
     * Atomically removes {@code deviceId} from {@code selectedEntrants} and adds it
     * to {@code cancelledEntrants} in the event's Firestore document.
     *
     * <p>Uses {@link FieldValue#arrayRemove(Object...)} and
     * {@link FieldValue#arrayUnion(Object...)} in a single {@code update} call to
     * avoid partial writes. On success the entrant list is reloaded via
     * {@link #loadEntrants()}. On failure the progress bar is hidden and an error
     * toast is shown.
     *
     * @param deviceId The device ID of the entrant to cancel.
     */
    private void cancelEntrantInFirestore(String deviceId) {
        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> updates = new HashMap<>();
        updates.put("selectedEntrants",  FieldValue.arrayRemove(deviceId));
        updates.put("cancelledEntrants", FieldValue.arrayUnion(deviceId));

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Entrant removed successfully",
                            Toast.LENGTH_SHORT).show();
                    loadEntrants();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to cancel entrant. Try again.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ─── Tab switching ────────────────────────────────────────────────────────

    /**
     * Switches the active tab, updates tab styling, and refreshes the
     * {@link RecyclerView} with the corresponding entrant list.
     *
     * <p>Also updates {@link #tvCount} with the size of the newly displayed list
     * and toggles the empty-state view accordingly.
     *
     * @param tab One of {@link #TAB_ALL}, {@link #TAB_PENDING},
     *            {@link #TAB_ACCEPTED}, or {@link #TAB_DECLINED}.
     */
    private void switchTab(int tab) {
        currentTab = tab;

        resetTab(tvTabAll);
        resetTab(tvTabPending);
        resetTab(tvTabAccepted);
        resetTab(tvTabDeclined);

        List<InvitedEntrant> list;
        switch (tab) {
            case TAB_PENDING:
                selectTab(tvTabPending);
                list = pendingList;
                break;
            case TAB_ACCEPTED:
                selectTab(tvTabAccepted);
                list = acceptedList;
                break;
            case TAB_DECLINED:
                selectTab(tvTabDeclined);
                list = declinedList;
                break;
            default:
                selectTab(tvTabAll);
                list = allList;
                break;
        }

        adapter.updateList(list);
        tvCount.setText(list.size() + " ENTRANTS");
        tvNoEntrants.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        rvEntrants.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * Applies the inactive visual style to the given tab {@link TextView}.
     *
     * @param tab The tab view to deactivate.
     */
    private void resetTab(TextView tab) {
        tab.setTextColor(0xFFAAAAAA);
        tab.setBackgroundResource(R.drawable.bg_tab_inactive);
    }

    /**
     * Applies the active visual style to the given tab {@link TextView}.
     *
     * @param tab The tab view to activate.
     */
    private void selectTab(TextView tab) {
        tab.setTextColor(0xFFFFFFFF);
        tab.setBackgroundResource(R.drawable.bg_tab_active);
    }

    /**
     * Updates {@link #tvCount} to reflect the total number of selected entrants
     * across all tabs.
     */
    private void updateTabCount() {
        tvCount.setText(allList.size() + " ENTRANTS");
    }

    // ─── Model ────────────────────────────────────────────────────────────────

    /**
     * Lightweight model representing a single selected entrant and their
     * current invitation status.
     */
    public static class InvitedEntrant {

        /** The device ID uniquely identifying this entrant. */
        public String deviceId;

        /**
         * The entrant's current status. One of:
         * <ul>
         *   <li>{@code "Accepted"} — the entrant enrolled after being selected</li>
         *   <li>{@code "Declined"} — the entrant was cancelled or did not accept</li>
         *   <li>{@code "Pending"} — the entrant has not yet responded</li>
         * </ul>
         */
        public String status;

        /**
         * Constructs an {@code InvitedEntrant} with the given device ID and status.
         *
         * @param deviceId The device ID of the entrant.
         * @param status   One of {@code "Accepted"}, {@code "Declined"}, or
         *                 {@code "Pending"}.
         */
        public InvitedEntrant(String deviceId, String status) {
            this.deviceId = deviceId;
            this.status   = status;
        }
    }
}