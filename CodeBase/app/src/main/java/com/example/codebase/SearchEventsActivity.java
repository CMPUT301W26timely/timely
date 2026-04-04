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
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for searching events by keyword, availability, and capacity.
 */
public class SearchEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewSearchResults;
    private TextView textViewNoResults;
    private TextInputEditText editTextSearch;
    private ImageButton btnShowFilters;
    private ChipGroup chipGroupActiveFilters;
    
    private EventAdapter adapter;
    private List<Event> allActiveEvents = new ArrayList<>();
    private List<Event> filteredEvents = new ArrayList<>();
    private String deviceId;

    private Calendar availableFrom = null;
    private Calendar availableUntil = null;
    private String selectedCapacityFilter = "All";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final String[] capacityOptions = {"All", "<= 20", "<= 50", "<= 100", "<= 250", "<= 500", "<= 1000", "> 1000"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_events);

        recyclerViewSearchResults = findViewById(R.id.recyclerViewSearchResults);
        textViewNoResults = findViewById(R.id.textViewNoResults);
        editTextSearch = findViewById(R.id.editTextSearch);
        btnShowFilters = findViewById(R.id.btnShowFilters);
        chipGroupActiveFilters = findViewById(R.id.chipGroupActiveFilters);
        
        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(filteredEvents, event -> {
            Intent intent;
            if (deviceId.equals(event.getOrganizerDeviceId())) {
                intent = new Intent(SearchEventsActivity.this, EventDetailActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getEventId());
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
            } else {
                intent = new Intent(SearchEventsActivity.this, EntrantEventDetailActivity.class);
                intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, event.getEventId());
            }
            startActivity(intent);
        });
        recyclerViewSearchResults.setAdapter(adapter);

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnShowFilters.setOnClickListener(v -> showFilterDialog());

        setupBottomNavigation();
        loadAllEvents();
    }

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

        // Pre-fill existing values
        if (availableFrom != null) etFrom.setText(dateFormat.format(availableFrom.getTime()));
        if (availableUntil != null) etTo.setText(dateFormat.format(availableUntil.getTime()));
        
        ArrayAdapter<String> capAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, capacityOptions);
        actvCap.setAdapter(capAdapter);
        actvCap.setText(selectedCapacityFilter, false);

        etFrom.setOnClickListener(v -> {
            Calendar c = availableFrom != null ? availableFrom : Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                availableFrom = Calendar.getInstance();
                availableFrom.set(year, month, day, 0, 0, 0);
                etFrom.setText(dateFormat.format(availableFrom.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        etTo.setOnClickListener(v -> {
            Calendar c = availableUntil != null ? availableUntil : Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                availableUntil = Calendar.getInstance();
                availableUntil.set(year, month, day, 23, 59, 59);
                etTo.setText(dateFormat.format(availableUntil.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnReset.setOnClickListener(v -> {
            availableFrom = null;
            availableUntil = null;
            selectedCapacityFilter = "All";
            dialog.dismiss();
            applyFilters();
        });

        btnApply.setOnClickListener(v -> {
            selectedCapacityFilter = actvCap.getText().toString();
            dialog.dismiss();
            applyFilters();
        });

        dialog.show();
    }

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

        if (!selectedCapacityFilter.equals("All")) {
            addFilterChip("Cap: " + selectedCapacityFilter, v -> {
                selectedCapacityFilter = "All";
                applyFilters();
            });
            hasFilters = true;
        }

        chipGroupActiveFilters.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
    }

    private void addFilterChip(String text, View.OnClickListener onRemove) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(onRemove);
        chipGroupActiveFilters.addView(chip);
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navExplore).setOnClickListener(v -> {
            startActivity(new Intent(this, BrowseEventsActivity.class));
            finish();
        });
        findViewById(R.id.navSearch).setOnClickListener(v -> {});
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
    }

    private void loadAllEvents() {
        EventRepository.loadActiveEvents(new EventRepository.EventsCallback() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                allActiveEvents.clear();
                allActiveEvents.addAll(events);
                applyFilters();
            }
            @Override
            public void onError(Exception e) {
                Log.e("SearchEventsActivity", "Failed to load events", e);
                Toast.makeText(SearchEventsActivity.this, "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        updateActiveFilterChips();
        String query = editTextSearch.getText().toString();
        filteredEvents.clear();

        String[] keywordsArray = query.split(",");
        List<String> validKeywords = new ArrayList<>();
        for (String kw : keywordsArray) {
            String trimmed = kw.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                validKeywords.add(trimmed);
            }
        }

        for (Event event : allActiveEvents) {
            boolean keywordMatch = true;
            if (!validKeywords.isEmpty()) {
                String title = event.getTitle() != null ? event.getTitle().toLowerCase() : "";
                String description = event.getDescription() != null ? event.getDescription().toLowerCase() : "";
                for (String keyword : validKeywords) {
                    if (!title.contains(keyword) && !description.contains(keyword)) {
                        keywordMatch = false;
                        break;
                    }
                }
            }

            boolean dateMatch = true;
            if (event.getStartDate() != null) {
                long eventTime = event.getStartDate().getTime();
                if (availableFrom != null && eventTime < availableFrom.getTimeInMillis()) {
                    dateMatch = false;
                }
                if (availableUntil != null && eventTime > availableUntil.getTimeInMillis()) {
                    dateMatch = false;
                }
            }

            boolean capacityMatch = true;
            if (!selectedCapacityFilter.equals("All")) {
                Long cap = event.getMaxCapacity();
                if (cap == null) cap = 0L;
                
                switch (selectedCapacityFilter) {
                    case "<= 20": capacityMatch = cap <= 20; break;
                    case "<= 50": capacityMatch = cap <= 50; break;
                    case "<= 100": capacityMatch = cap <= 100; break;
                    case "<= 250": capacityMatch = cap <= 250; break;
                    case "<= 500": capacityMatch = cap <= 500; break;
                    case "<= 1000": capacityMatch = cap <= 1000; break;
                    case "> 1000": capacityMatch = cap > 1000; break;
                }
            }

            if (keywordMatch && dateMatch && capacityMatch) {
                filteredEvents.add(event);
            }
        }

        adapter.notifyDataSetChanged();
        
        boolean hasQuery = !query.trim().isEmpty() || availableFrom != null || availableUntil != null || !selectedCapacityFilter.equals("All");
        if (filteredEvents.isEmpty() && hasQuery) {
            textViewNoResults.setVisibility(View.VISIBLE);
            recyclerViewSearchResults.setVisibility(View.GONE);
        } else {
            textViewNoResults.setVisibility(View.GONE);
            recyclerViewSearchResults.setVisibility(View.VISIBLE);
        }
    }
}
