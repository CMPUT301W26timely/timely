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
import java.util.List;

/**
 * Displays the waiting list of entrants for an organizer's event.
 *
 * <p>Implements US 02.02.01: as an organizer, view the list of entrants who
 * joined the event's waiting list, showing each entrant's name and email address.
 *
 * <p>The waiting list device IDs are loaded from the {@code waitingList} field
 * of the Firestore event document. Each device ID is then resolved to a name
 * and email address by querying the {@code users} collection — this is done
 * lazily in the adapter to avoid N blocking calls before showing the list.
 */
public class WaitingListActivity extends AppCompatActivity {

    /** Intent extra key for the Firestore event document ID. */
    public static final String EXTRA_EVENT_ID    = "event_id";

    /** Intent extra key for the event title shown in the toolbar. */
    public static final String EXTRA_EVENT_TITLE = "event_title";

    // ── Views ─────────────────────────────────────────────────────────────────

    private TextView     tvTitle;
    private TextView     tvCount;
    private RecyclerView rvWaitingList;
    private TextView     tvEmpty;
    private View         progressBar;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Firestore document ID of the event being viewed. */
    private String eventId;

    /** Display title of the event shown in the toolbar. */
    private String eventTitle;

    private WaitingListAdapter adapter;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initialises the activity, binds views, wires the back button, sets up the
     * {@link RecyclerView}, and triggers the Firestore load.
     *
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_list);

        eventId    = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        tvTitle       = findViewById(R.id.tvWaitingListTitle);
        tvCount       = findViewById(R.id.tvWaitingListCount);
        rvWaitingList = findViewById(R.id.rvWaitingList);
        tvEmpty       = findViewById(R.id.tvWaitingListEmpty);
        progressBar   = findViewById(R.id.waitingListProgressBar);

        tvTitle.setText(eventTitle != null ? eventTitle : "Waiting List");

        findViewById(R.id.btnBackWaitingList).setOnClickListener(v -> finish());

        adapter = new WaitingListAdapter(new ArrayList<>());
        rvWaitingList.setLayoutManager(new LinearLayoutManager(this));
        rvWaitingList.setAdapter(adapter);

        loadWaitingList();
    }

    // ─── Firestore loading ────────────────────────────────────────────────────

    /**
     * Fetches the event document from Firestore and extracts the {@code waitingList}
     * device-ID array. Passes the list to the adapter for display.
     *
     * <p>Shows a progress indicator while the request is in-flight.
     */
    private void loadWaitingList() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::processDocument)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load waiting list", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Parses the Firestore {@link DocumentSnapshot}, extracts the
     * {@code waitingList} device-ID list, updates the adapter and count badge.
     *
     * @param doc The Firestore event document snapshot.
     */
    @SuppressWarnings("unchecked")
    private void processDocument(DocumentSnapshot doc) {
        progressBar.setVisibility(View.GONE);

        if (!doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        List<String> waitingList = (List<String>) doc.get("waitingList");
        if (waitingList == null) {
            waitingList = new ArrayList<>();
        }

        int count = waitingList.size();
        tvCount.setText(count + (count == 1 ? " entrant" : " entrants"));

        if (count == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvWaitingList.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvWaitingList.setVisibility(View.VISIBLE);
            adapter.updateList(waitingList);
        }
    }
}