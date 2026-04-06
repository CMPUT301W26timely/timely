package com.example.codebase;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Entrant browse events screen.
 *
 * <p>Displays all active public events and lets the user search or filter them directly
 * from the Explore screen. Tapping an event navigates to either
 * {@link EventDetailActivity} (if the current user is the organizer) or
 * {@link EntrantEventDetailActivity} (if the current user is an entrant).</p>
 */
public class BrowseEventsActivity extends AppCompatActivity {

    /** RecyclerView that displays the visible events. */
    private RecyclerView recyclerViewEvents;

    /** Empty state label shown when no events are available or filters match nothing. */
    private TextView textViewEmptyState;

    /** Search input embedded directly in Explore. */
    private TextInputEditText editTextSearch;

    /** Opens the filter dialog for date and capacity filters. */
    private ImageButton btnShowFilters;

    /** Displays removable chips for active non-keyword filters. */
    private ChipGroup chipGroupActiveFilters;

    /** Adapter backing the filtered Explore list. */
    private EventAdapter adapter;

    /** All currently active public events loaded from Firestore. */
    private final List<Event> allActiveEvents = new ArrayList<>();

    /** Visible events after keyword and filter processing. */
    private final List<Event> filteredEvents = new ArrayList<>();

    /** Device ID of the current user, used to determine organizer vs entrant role. */
    private String deviceId;

    /** Lower bound for the event start-date filter. */
    private Calendar availableFrom;

    /** Upper bound for the event start-date filter. */
    private Calendar availableUntil;

    /** Selected capacity bucket label. */
    private String selectedCapacityFilter = "All";

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final String[] capacityOptions =
            {"All", "<= 20", "<= 50", "<= 100", "<= 250", "<= 500", "<= 1000", "> 1000"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_events);

        recyclerViewEvents = findViewById(R.id.recyclerViewEvents);
        textViewEmptyState = findViewById(R.id.textViewEmptyState);
        editTextSearch = findViewById(R.id.editTextSearch);
        btnShowFilters = findViewById(R.id.btnShowFilters);
        chipGroupActiveFilters = findViewById(R.id.chipGroupActiveFilters);
        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(filteredEvents, this::openEventDetails);
        recyclerViewEvents.setAdapter(adapter);

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op.
            }
        });

        btnShowFilters.setOnClickListener(v -> showFilterDialog());

        setupBottomNavigation();
        loadEvents();
    }

    /**
     * Wires up the bottom navigation bar click listeners.
     */
    private void setupBottomNavigation() {
        findViewById(R.id.navExplore).setOnClickListener(v -> {
            // Already on Explore.
        });

        findViewById(R.id.navHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        });

        findViewById(R.id.navMyEvents).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerActivity.class));
            finish();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            finish();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });

        findViewById(R.id.fabCamera).setOnClickListener(v ->
                startActivity(new Intent(this, QRScannerActivity.class)));
    }

    /**
     * Loads all active events from Firestore.
     */
    private void loadEvents() {
        EventRepository.loadActiveEvents(new EventRepository.EventsCallback() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                allActiveEvents.clear();
                if (events != null) {
                    allActiveEvents.addAll(events);
                }
                applyFilters();
            }

            @Override
            public void onError(Exception e) {
                Log.e("BrowseEventsActivity", "Failed to load events", e);
                Toast.makeText(BrowseEventsActivity.this,
                        getString(R.string.error_loading_events),
                        Toast.LENGTH_LONG).show();
                allActiveEvents.clear();
                filteredEvents.clear();
                adapter.notifyDataSetChanged();
                showEmptyState(R.string.browse_events_empty);
            }
        });
    }

    /**
     * Opens the event detail screen that matches the current user's relation to the event.
     */
    private void openEventDetails(Event event) {
        Intent intent;
        if (deviceId.equals(event.getOrganizerDeviceId())) {
            intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getEventId());
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
        } else {
            intent = new Intent(this, EntrantEventDetailActivity.class);
            intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, event.getEventId());
        }
        startActivity(intent);
    }

    /**
     * Shows the date/capacity filter dialog and applies the selected values.
     */
    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filter_events, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        TextInputEditText etFrom = dialogView.findViewById(R.id.etAvailableFrom);
        TextInputEditText etTo = dialogView.findViewById(R.id.etAvailableTo);
        AutoCompleteTextView actvCap = dialogView.findViewById(R.id.actvCapacity);
        Button btnReset = dialogView.findViewById(R.id.btnResetFilters);
        Button btnApply = dialogView.findViewById(R.id.btnApplyFilters);

        if (availableFrom != null) {
            etFrom.setText(dateFormat.format(availableFrom.getTime()));
        }
        if (availableUntil != null) {
            etTo.setText(dateFormat.format(availableUntil.getTime()));
        }

        ArrayAdapter<String> capAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, capacityOptions);
        actvCap.setAdapter(capAdapter);
        actvCap.setText(selectedCapacityFilter, false);

        etFrom.setOnClickListener(v -> {
            Calendar calendar = availableFrom != null ? availableFrom : Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                availableFrom = Calendar.getInstance();
                availableFrom.set(year, month, day, 0, 0, 0);
                etFrom.setText(dateFormat.format(availableFrom.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        etTo.setOnClickListener(v -> {
            Calendar calendar = availableUntil != null ? availableUntil : Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                availableUntil = Calendar.getInstance();
                availableUntil.set(year, month, day, 23, 59, 59);
                etTo.setText(dateFormat.format(availableUntil.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnReset.setOnClickListener(v -> {
            availableFrom = null;
            availableUntil = null;
            selectedCapacityFilter = "All";
            dialog.dismiss();
            applyFilters();
        });

        btnApply.setOnClickListener(v -> {
            selectedCapacityFilter = actvCap.getText() == null
                    ? "All"
                    : actvCap.getText().toString();
            dialog.dismiss();
            applyFilters();
        });

        dialog.show();
    }

    /**
     * Recomputes the Explore list using the current keyword and filter state.
     */
    private void applyFilters() {
        updateActiveFilterChips();
        String query = editTextSearch.getText() == null ? "" : editTextSearch.getText().toString();
        filteredEvents.clear();
        filteredEvents.addAll(EventSearchFilter.filterEvents(
                allActiveEvents,
                query,
                availableFrom,
                availableUntil,
                selectedCapacityFilter
        ));

        adapter.notifyDataSetChanged();
        updateContentState(query);
    }

    /**
     * Updates the filter-chip row based on the current date/capacity filters.
     */
    private void updateActiveFilterChips() {
        chipGroupActiveFilters.removeAllViews();
        boolean hasFilters = false;

        if (availableFrom != null) {
            addFilterChip("From: " + dateFormat.format(availableFrom.getTime()), v -> {
                availableFrom = null;
                applyFilters();
            });
            hasFilters = true;
        }

        if (availableUntil != null) {
            addFilterChip("Until: " + dateFormat.format(availableUntil.getTime()), v -> {
                availableUntil = null;
                applyFilters();
            });
            hasFilters = true;
        }

        if (!"All".equals(selectedCapacityFilter)) {
            addFilterChip("Cap: " + selectedCapacityFilter, v -> {
                selectedCapacityFilter = "All";
                applyFilters();
            });
            hasFilters = true;
        }

        chipGroupActiveFilters.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
    }

    /**
     * Adds one removable active-filter chip to the row.
     */
    private void addFilterChip(String text, View.OnClickListener onRemove) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(onRemove);
        chipGroupActiveFilters.addView(chip);
    }

    /**
     * Toggles the empty state between "no events" and "no results" messaging.
     */
    private void updateContentState(String query) {
        boolean hasSearchOrFilters = !query.trim().isEmpty()
                || availableFrom != null
                || availableUntil != null
                || !"All".equals(selectedCapacityFilter);

        if (filteredEvents.isEmpty()) {
            if (allActiveEvents.isEmpty()) {
                showEmptyState(R.string.browse_events_empty);
            } else if (hasSearchOrFilters) {
                showEmptyState(R.string.browse_events_no_results);
            } else {
                showEmptyState(R.string.browse_events_empty);
            }
            return;
        }

        textViewEmptyState.setVisibility(View.GONE);
        recyclerViewEvents.setVisibility(View.VISIBLE);
    }

    private void showEmptyState(int messageResId) {
        textViewEmptyState.setText(messageResId);
        textViewEmptyState.setVisibility(View.VISIBLE);
        recyclerViewEvents.setVisibility(View.GONE);
    }
}
