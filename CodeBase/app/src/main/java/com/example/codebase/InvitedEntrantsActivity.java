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
 * InvitedEntrantsActivity — shows all selected entrants split into:
 *   All / Pending / Accepted / Declined
 *
 * US 02.06.04 — Cancel entrant (organizer action):
 *   Cancel button is only enabled once the registration deadline has passed.
 *   On confirm → remove from selectedEntrants, add to cancelledEntrants.
 *
 * Logic:
 *   Declined = cancelledEntrants ∩ (selectedEntrants - enrolledEntrants)
 *   Pending  = selectedEntrants - enrolledEntrants - declined
 *   Accepted = enrolledEntrants (who are in selectedEntrants)
 */
public class InvitedEntrantsActivity extends AppCompatActivity
        implements InvitedEntrantsAdapter.CancelListener {

    public static final String EXTRA_EVENT_ID    = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";

    // Tabs
    private static final int TAB_ALL      = 0;
    private static final int TAB_PENDING  = 1;
    private static final int TAB_ACCEPTED = 2;
    private static final int TAB_DECLINED = 3;

    private int currentTab = TAB_ALL;

    private TextView tvTitle;
    private TextView tvCount;
    private TextView tvTabAll;
    private TextView tvTabPending;
    private TextView tvTabAccepted;
    private TextView tvTabDeclined;
    private RecyclerView rvEntrants;
    private TextView tvNoEntrants;
    private View progressBar;

    private String eventId;
    private String eventTitle;

    // registration deadline loaded from Firestore — used to enable/disable cancel button
    private Date registrationDeadlineDate = null;

    // Full lists
    private List<InvitedEntrant> allList     = new ArrayList<>();
    private List<InvitedEntrant> pendingList  = new ArrayList<>();
    private List<InvitedEntrant> acceptedList = new ArrayList<>();
    private List<InvitedEntrant> declinedList = new ArrayList<>();

    private InvitedEntrantsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invited_entrants);

        eventId    = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        // ── Bind views ────────────────────────────────────────────────────────
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

        // ── Back button ───────────────────────────────────────────────────────
        findViewById(R.id.btnBackInvited).setOnClickListener(v -> finish());

        // ── Tabs ──────────────────────────────────────────────────────────────
        tvTabAll.setOnClickListener(v      -> switchTab(TAB_ALL));
        tvTabPending.setOnClickListener(v  -> switchTab(TAB_PENDING));
        tvTabAccepted.setOnClickListener(v -> switchTab(TAB_ACCEPTED));
        tvTabDeclined.setOnClickListener(v -> switchTab(TAB_DECLINED));

        // ── RecyclerView ──────────────────────────────────────────────────────
        // Pass 'this' as CancelListener so adapter can call back for cancellation
        adapter = new InvitedEntrantsAdapter(new ArrayList<>(), this);
        rvEntrants.setLayoutManager(new LinearLayoutManager(this));
        rvEntrants.setAdapter(adapter);

        switchTab(TAB_ALL);
        loadEntrants();
    }

    // ─── Load from Firestore ──────────────────────────────────────────────────

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

        List<String> selected = event.getSelectedEntrants();
        List<String> enrolled = event.getEnrolledEntrants();
        List<String> cancelled = event.getCancelledEntrants();

        // ── Calculate groups ──────────────────────────────────────────────────

        // Declined = cancelledEntrants ∩ (selectedEntrants - enrolledEntrants)
        List<String> selectedMinusEnrolled = new ArrayList<>(selected);
        selectedMinusEnrolled.removeAll(enrolled);

        List<String> declined = new ArrayList<>(cancelled);
        declined.retainAll(selectedMinusEnrolled);

        // Pending = selected - enrolled - declined
        List<String> pending = new ArrayList<>(selected);
        pending.removeAll(enrolled);
        pending.removeAll(declined);

        // ── Build display lists ───────────────────────────────────────────────
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
     * Called by adapter when organizer presses Cancel on a Pending entrant.
     * Shows a confirmation dialog before proceeding.
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
     * Atomically removes deviceId from selectedEntrants
     * and adds it to cancelledEntrants in Firestore.
     * Reloads the screen on success.
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
                    loadEntrants(); // refresh — entrant removed from Pending, not shown anywhere
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to cancel entrant. Try again.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ─── Tab switching ────────────────────────────────────────────────────────

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
            default: // TAB_ALL
                selectTab(tvTabAll);
                list = allList;
                break;
        }

        adapter.updateList(list);
        tvCount.setText(list.size() + " ENTRANTS");
        tvNoEntrants.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        rvEntrants.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void resetTab(TextView tab) {
        tab.setTextColor(0xFFAAAAAA);
        tab.setBackgroundResource(R.drawable.bg_tab_inactive);
    }

    private void selectTab(TextView tab) {
        tab.setTextColor(0xFFFFFFFF);
        tab.setBackgroundResource(R.drawable.bg_tab_active);
    }

    private void updateTabCount() {
        tvCount.setText(allList.size() + " ENTRANTS");
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    // ─── Model ────────────────────────────────────────────────────────────────

    public static class InvitedEntrant {
        public String deviceId;
        public String status; // "Accepted", "Declined", "Pending"

        public InvitedEntrant(String deviceId, String status) {
            this.deviceId = deviceId;
            this.status   = status;
        }
    }
}
