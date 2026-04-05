package com.example.codebase;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Displays the entrant's history of event registrations.
 *
 * <p>The history is built from events the current device has ever registered
 * for. The screen shows both active and completed registrations, along with a
 * derived status such as "Registered", "Selected", or "Not Selected".</p>
 */
public class HistoryFragment extends Fragment {

    /** Scrollable list of history rows. */
    private RecyclerView recyclerViewHistory;

    /** Empty-state card shown when the entrant has no registrations yet. */
    private MaterialCardView emptyStateCard;

    /** Loading indicator shown while Firestore data is in flight. */
    private View progressBar;

    /** Adapter backing {@link #recyclerViewHistory}. */
    private EntrantHistoryAdapter historyAdapter;

    /** Mutable list reused across refreshes to avoid recreating the adapter. */
    private final List<EntrantHistoryHelper.HistoryEntry> historyEntries = new ArrayList<>();

    /** Current entrant device ID used to filter the event collection. */
    private String deviceId;

    /**
     * Inflates the fragment layout.
     *
     * @param inflater           the {@link LayoutInflater} used to inflate the fragment's view
     * @param container          the parent {@link ViewGroup} the fragment UI will be attached to,
     *                           or {@code null} if there is no parent
     * @param savedInstanceState a {@link Bundle} containing the fragment's previously saved state,
     *                           or {@code null} if this is a fresh creation
     * @return the inflated {@link View} for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    /**
     * Binds views, configures the list, and loads the entrant's registration history.
     *
     * @param view the inflated fragment view
     * @param savedInstanceState previously saved fragment state, or {@code null}
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        deviceId = DeviceIdManager.getOrCreateDeviceId(requireContext());
        recyclerViewHistory = view.findViewById(R.id.recyclerViewHistory);
        emptyStateCard = view.findViewById(R.id.cardHistoryEmptyState);
        progressBar = view.findViewById(R.id.historyProgressBar);

        historyAdapter = new EntrantHistoryAdapter(historyEntries, entry -> {
            if (entry.getEvent().getId() == null) {
                return;
            }

            android.content.Intent intent =
                    new android.content.Intent(requireContext(), EntrantEventDetailActivity.class);
            intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, entry.getEvent().getId());
            startActivity(intent);
        });

        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewHistory.setAdapter(historyAdapter);

        loadHistory();
    }

    /**
     * Refreshes history each time the fragment becomes visible again so new joins,
     * responses, or cancellations appear immediately after returning from detail screens.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            loadHistory();
        }
    }

    /**
     * Loads all events, normalizes them, filters them down to the current
     * entrant's registrations, and renders the resulting history list.
     *
     * <p>A full collection read is used here because existing demo data may not
     * yet contain the new {@code registeredEntrants} history field. Local
     * filtering keeps the feature backward-compatible while the new field starts
     * being populated on future registrations.</p>
     */
    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewHistory.setVisibility(View.GONE);
        emptyStateCard.setVisibility(View.GONE);

        AppDatabase.getInstance()
                .eventsRef
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Event event = EventSchema.normalizeLoadedEvent(doc);
                        if (event != null) {
                            events.add(event);
                        }
                    }

                    List<EntrantHistoryHelper.HistoryEntry> history =
                            EntrantHistoryHelper.buildHistory(events, deviceId, new Date());
                    renderHistory(history);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    recyclerViewHistory.setVisibility(View.GONE);
                    emptyStateCard.setVisibility(View.VISIBLE);
                });
    }

    /**
     * Applies the latest history data to the RecyclerView and toggles empty-state UI.
     *
     * @param latestHistory the latest filtered history rows for the current entrant
     */
    private void renderHistory(List<EntrantHistoryHelper.HistoryEntry> latestHistory) {
        progressBar.setVisibility(View.GONE);
        historyEntries.clear();
        historyEntries.addAll(latestHistory);
        historyAdapter.notifyDataSetChanged();

        boolean hasHistory = !historyEntries.isEmpty();
        recyclerViewHistory.setVisibility(hasHistory ? View.VISIBLE : View.GONE);
        emptyStateCard.setVisibility(hasHistory ? View.GONE : View.VISIBLE);
    }
}
