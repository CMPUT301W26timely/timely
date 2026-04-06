package com.example.codebase;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * CancelledEntrantsActivity — US 02.06.02
 *
 * Displays all cancelled entrants for a given event, split across three tabs:
 * <ul>
 *   <li><b>All</b> — the full {@code cancelledEntrants} array from Firestore</li>
 *   <li><b>Declined</b> — entrants present in {@code declinedEntrants}</li>
 *   <li><b>Cancelled</b> — entrants in {@code cancelledEntrants - declinedEntrants}</li>
 * </ul>
 *
 * <p>Launched from {@code EventDetailActivity} via the cancelled users row.</p>
 *
 * Add to AndroidManifest.xml:
 * <pre>
 *   &lt;activity android:name=".CancelledEntrantsActivity" android:exported="false" /&gt;
 * </pre>
 */
public class CancelledEntrantsActivity extends AppCompatActivity {

    /** Intent extra key for the Firestore event document ID. */
    public static final String EXTRA_EVENT_ID    = "event_id";

    /** Intent extra key for the event display title. */
    public static final String EXTRA_EVENT_TITLE = "event_title";

    /** Tab index for the All tab — shows every cancelled entrant. */
    private static final int TAB_ALL       = 0;

    /** Tab index for the Declined tab — shows entrants who declined their invitation. */
    private static final int TAB_DECLINED  = 1;

    /** Tab index for the Cancelled tab — shows entrants cancelled by the organizer. */
    private static final int TAB_CANCELLED = 2;

    /** The currently selected tab index. Defaults to {@link #TAB_ALL}. */
    private int currentTab = TAB_ALL;

    /** Displays the event title at the top of the screen. */
    private TextView tvTitle;

    /** Displays the count of entrants shown in the current tab. */
    private TextView tvCount;

    /** Tab button for the All tab. */
    private TextView tvTabAll;

    /** Tab button for the Declined tab. */
    private TextView tvTabDeclined;

    /** Tab button for the Cancelled tab. */
    private TextView tvTabCancelled;

    /** RecyclerView displaying the list of cancelled entrants. */
    private RecyclerView rvEntrants;

    /** Empty state label shown when the current tab has no entrants. */
    private TextView tvNoEntrants;

    /** Progress bar shown while Firestore data is loading. */
    private View progressBar;

    /** Firestore document ID of the event being viewed. */
    private String eventId;

    /** Display title of the event being viewed. */
    private String eventTitle;

    /** Full list of all cancelled entrants shown in the All tab. */
    private List<CancelledEntrant> allList       = new ArrayList<>();

    /** List of entrants who actively declined their invitation. */
    private List<CancelledEntrant> declinedList  = new ArrayList<>();

    /** List of entrants who were cancelled by the organizer. */
    private List<CancelledEntrant> cancelledList = new ArrayList<>();

    /** Adapter for the cancelled entrants RecyclerView. */
    private CancelledEntrantsAdapter adapter;

    /**
     * Called when the activity is created.
     * Binds views, sets up tab listeners, initialises the RecyclerView,
     * and triggers loading of entrant data from Firestore.
     *
     * @param savedInstanceState saved state bundle from a previous instance, or {@code null}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancelled_entrants);

        eventId    = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        tvTitle        = findViewById(R.id.tvCancelledTitle);
        tvCount        = findViewById(R.id.tvCancelledCount);
        tvTabAll       = findViewById(R.id.tabCancelledAll);
        tvTabDeclined  = findViewById(R.id.tabCancelledDeclined);
        tvTabCancelled = findViewById(R.id.tabCancelledCancelled);
        rvEntrants     = findViewById(R.id.rvCancelledEntrants);
        tvNoEntrants   = findViewById(R.id.tvNoCancelledEntrants);
        progressBar    = findViewById(R.id.cancelledProgressBar);

        if (eventTitle != null) tvTitle.setText(eventTitle);

        findViewById(R.id.btnBackCancelled).setOnClickListener(v -> finish());

        tvTabAll.setOnClickListener(v       -> switchTab(TAB_ALL));
        tvTabDeclined.setOnClickListener(v  -> switchTab(TAB_DECLINED));
        tvTabCancelled.setOnClickListener(v -> switchTab(TAB_CANCELLED));

        adapter = new CancelledEntrantsAdapter(new ArrayList<>());
        rvEntrants.setLayoutManager(new LinearLayoutManager(this));
        rvEntrants.setAdapter(adapter);

        switchTab(TAB_ALL);
        loadEntrants();
    }

    /**
     * Loads the event document from Firestore and passes it to
     * {@link #processDocument(DocumentSnapshot)} on success.
     * Shows a toast and hides the progress bar on failure.
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
     * Processes the loaded Firestore document and builds the three entrant lists.
     *
     * @param doc the Firestore {@link DocumentSnapshot} for the event
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

        List<String> cancelled = EventRosterStatusHelper.cancelledEntrants(event);
        List<String> declinedIds = EventRosterStatusHelper.declinedEntrants(event);

        allList.clear();
        declinedList.clear();
        cancelledList.clear();

        for (String deviceId : cancelled) {
            String status = declinedIds.contains(deviceId) ? "Declined" : "Cancelled";
            CancelledEntrant entrant = new CancelledEntrant(deviceId, status, null);
            allList.add(entrant);
            if ("Declined".equals(status)) declinedList.add(entrant);
            else                           cancelledList.add(entrant);
        }

        switchTab(currentTab);
    }

    /**
     * Switches the active tab and updates the RecyclerView with the
     * corresponding entrant list.
     *
     * <p>Resets all tab styles, highlights the selected tab, updates the
     * adapter, count label, and empty state visibility.</p>
     *
     * @param tab the tab index to switch to — one of
     *            {@link #TAB_ALL}, {@link #TAB_DECLINED}, or {@link #TAB_CANCELLED}
     */
    private void switchTab(int tab) {
        currentTab = tab;

        resetTab(tvTabAll);
        resetTab(tvTabDeclined);
        resetTab(tvTabCancelled);

        List<CancelledEntrant> list;
        switch (tab) {
            case TAB_DECLINED:
                selectTab(tvTabDeclined);
                list = declinedList;
                break;
            case TAB_CANCELLED:
                selectTab(tvTabCancelled);
                list = cancelledList;
                break;
            default:
                selectTab(tvTabAll);
                list = allList;
                break;
        }

        adapter.updateList(list);
        tvCount.setText("Showing " + list.size() + " removed participants");
        tvNoEntrants.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        rvEntrants.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * Resets a tab button to its inactive visual style.
     * Sets grey text and the inactive background drawable.
     *
     * @param tab the {@link TextView} tab button to reset
     */
    private void resetTab(TextView tab) {
        tab.setTextColor(0xFFAAAAAA);
        tab.setBackgroundResource(R.drawable.bg_tab_inactive);
    }

    /**
     * Applies the active visual style to a tab button.
     * Sets white text and the active background drawable.
     *
     * @param tab the {@link TextView} tab button to highlight
     */
    private void selectTab(TextView tab) {
        tab.setTextColor(0xFFFFFFFF);
        tab.setBackgroundResource(R.drawable.bg_tab_active);
    }

    /**
     * Lightweight model representing a single cancelled entrant row.
     *
     * <p>Used internally by {@link CancelledEntrantsActivity} and
     * {@link CancelledEntrantsAdapter} to hold display data.</p>
     */
    public static class CancelledEntrant {

        /** Device ID of the cancelled entrant. */
        public String deviceId;

        /**
         * Reason for cancellation.
         * Expected values: {@code "Declined"} or {@code "Cancelled"}.
         */
        public String status;

        /**
         * Timestamp when the entrant was cancelled.
         * Currently {@code null} — no per-entrant timestamp is stored yet.
         */
        public Date cancelledAt;

        /**
         * Constructs a new CancelledEntrant with the given fields.
         *
         * @param deviceId    the device ID of the cancelled entrant
         * @param status      the cancellation reason — {@code "Declined"} or {@code "Cancelled"}
         * @param cancelledAt the timestamp of cancellation, or {@code null} if unavailable
         */
        public CancelledEntrant(String deviceId, String status, Date cancelledAt) {
            this.deviceId    = deviceId;
            this.status      = status;
            this.cancelledAt = cancelledAt;
        }
    }
}
