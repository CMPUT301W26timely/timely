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
 * Shows all cancelled entrants split into three tabs:
 *
 *   All       = full cancelledEntrants array
 *   Declined  = cancelledEntrants ∩ (selectedEntrants - enrolledEntrants)
 *               — entrants who actively declined the invite
 *   Cancelled = cancelledEntrants - declined
 *               — entrants cancelled by the organizer
 *
 * Add to AndroidManifest.xml:
 *   <activity android:name=".CancelledEntrantsActivity" android:exported="false" />
 *
 * Launch from EventDetailActivity rowCancelledUsers click listener:
 *   Intent intent = new Intent(this, CancelledEntrantsActivity.class);
 *   intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_ID, eventId);
 *   intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_TITLE, eventTitle);
 *   startActivity(intent);
 */
public class CancelledEntrantsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID    = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";

    // Tabs
    private static final int TAB_ALL       = 0;
    private static final int TAB_DECLINED  = 1;
    private static final int TAB_CANCELLED = 2;

    private int currentTab = TAB_ALL;

    private TextView tvTitle;
    private TextView tvCount;
    private TextView tvTabAll;
    private TextView tvTabDeclined;
    private TextView tvTabCancelled;
    private RecyclerView rvEntrants;
    private TextView tvNoEntrants;
    private View progressBar;

    private String eventId;
    private String eventTitle;

    // Lists
    private List<CancelledEntrant> allList       = new ArrayList<>();
    private List<CancelledEntrant> declinedList  = new ArrayList<>();
    private List<CancelledEntrant> cancelledList = new ArrayList<>();

    private CancelledEntrantsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancelled_entrants);

        eventId    = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        // ── Bind views ────────────────────────────────────────────────────────
        tvTitle        = findViewById(R.id.tvCancelledTitle);
        tvCount        = findViewById(R.id.tvCancelledCount);
        tvTabAll       = findViewById(R.id.tabCancelledAll);
        tvTabDeclined  = findViewById(R.id.tabCancelledDeclined);
        tvTabCancelled = findViewById(R.id.tabCancelledCancelled);
        rvEntrants     = findViewById(R.id.rvCancelledEntrants);
        tvNoEntrants   = findViewById(R.id.tvNoCancelledEntrants);
        progressBar    = findViewById(R.id.cancelledProgressBar);

        if (eventTitle != null) tvTitle.setText(eventTitle);

        // ── Back button ───────────────────────────────────────────────────────
        findViewById(R.id.btnBackCancelled).setOnClickListener(v -> finish());

        // ── Tabs ──────────────────────────────────────────────────────────────
        tvTabAll.setOnClickListener(v       -> switchTab(TAB_ALL));
        tvTabDeclined.setOnClickListener(v  -> switchTab(TAB_DECLINED));
        tvTabCancelled.setOnClickListener(v -> switchTab(TAB_CANCELLED));

        // ── RecyclerView ──────────────────────────────────────────────────────
        adapter = new CancelledEntrantsAdapter(new ArrayList<>());
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

        // ── Read arrays ───────────────────────────────────────────────────────
        List<String> selected  = toStringList(doc.get("selectedEntrants"));
        List<String> enrolled  = toStringList(doc.get("enrolledEntrants"));
        List<String> cancelled = toStringList(doc.get("cancelledEntrants"));

        // ── Calculate declined ────────────────────────────────────────────────
        // Declined = cancelledEntrants ∩ (selectedEntrants - enrolledEntrants)
        List<String> selectedMinusEnrolled = new ArrayList<>(selected);
        selectedMinusEnrolled.removeAll(enrolled);

        List<String> declinedIds = new ArrayList<>(cancelled);
        declinedIds.retainAll(selectedMinusEnrolled);

        // ── Build display lists ───────────────────────────────────────────────
        allList.clear();
        declinedList.clear();
        cancelledList.clear();

        // No cancelledAt timestamp available per-entrant yet — use null for now
        for (String deviceId : cancelled) {
            String status = declinedIds.contains(deviceId) ? "Declined" : "Cancelled";
            CancelledEntrant entrant = new CancelledEntrant(deviceId, status, null);
            allList.add(entrant);
            if ("Declined".equals(status)) declinedList.add(entrant);
            else                           cancelledList.add(entrant);
        }

        switchTab(currentTab);
    }

    // ─── Tab switching ────────────────────────────────────────────────────────

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
            default: // TAB_ALL
                selectTab(tvTabAll);
                list = allList;
                break;
        }

        adapter.updateList(list);
        tvCount.setText("Showing " + list.size() + " removed participants");
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

    // ─── Helper ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object obj) {
        if (obj instanceof List) {
            List<?> raw = (List<?>) obj;
            List<String> result = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof String) result.add((String) item);
            }
            return result;
        }
        return new ArrayList<>();
    }

    // ─── Model ────────────────────────────────────────────────────────────────

    public static class CancelledEntrant {
        public String deviceId;
        public String status;    // "Declined" or "Cancelled"
        public Date   cancelledAt; // null for now — no per-entrant timestamp yet

        public CancelledEntrant(String deviceId, String status, Date cancelledAt) {
            this.deviceId    = deviceId;
            this.status      = status;
            this.cancelledAt = cancelledAt;
        }
    }
}