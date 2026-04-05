package com.example.codebase;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * A {@link Fragment} that displays the final enrolled entrant list for an {@link Event},
 * along with a capacity progress indicator and a searchable list view.
 *
 * <p>Satisfies user story <b>US 02.06.03</b>.</p>
 *
 * <p>The hosting {@link android.app.Activity} must implement {@link FinalEntrantListListener}
 * to supply the {@link Event} whose enrolled entrants are displayed. Attachment fails with
 * a {@link RuntimeException} if this contract is not met.</p>
 *
 * <p>The capacity display shows:</p>
 * <ul>
 *     <li>An absolute count in the form {@code enrolled/maxCapacity}.</li>
 *     <li>A percentage of capacity filled, shown both as a {@link TextView} and
 *         as a {@link ProgressBar}.</li>
 * </ul>
 *
 * <p>If {@code maxCapacity} is {@code null} or {@code 0}, it is clamped to {@code 1}
 * to prevent division by zero during percentage calculation.</p>
 */
public class FinalEntrantListActivity extends AppCompatActivity {




    /** Displays the enrolled entrant count as {@code enrolled/maxCapacity}. */
    TextView capacityText;

    /** Displays the percentage of capacity filled (e.g. {@code "75%"}). */
    TextView percentageCapacityText;

    /** Visual indicator of capacity fill percentage, driven by the enrolled entrant count. */
    ProgressBar progressBar;

    /** Filters the entrant {@link ListView} by device ID as the user types. */
    SearchView searchView;

    /** Scrollable list of enrolled entrant device IDs, backed by {@link #entrantAdapter}. */
    ListView listView;

    /** The event whose enrolled entrants are being displayed. */
    Event event;

    /**
     * The list of enrolled entrant device ID strings sourced from
     * {@link Event#getEnrolledEntrants()}. Defaults to an empty list if {@code null}.
     */
    ArrayList<String> finalEntrants;

    /**
     * The maximum number of enrolled entrants allowed for the event.
     * Clamped to {@code 1L} if {@code null} or {@code 0} to avoid division by zero.
     */
    Long maxCapacity;

    /** Adapter that backs the {@link #listView} and supports filtering via {@link #searchView}. */
    FinalEntrantListAdapter entrantAdapter;

    /**
     *
     * <p>Initialization steps performed in order:</p>
     * <ol>
     *     <li>Binds all views from {@code R.layout.final_entrant_list_fragment}.</li>
     *     <li>Retrieves the enrolled entrant list from {@link Event#getEnrolledEntrants()},
     *         defaulting to an empty {@link ArrayList} if {@code null}.</li>
     *     <li>Clamps {@code maxCapacity} to {@code 1L} if it is {@code null} or {@code 0}
     *         to prevent division by zero.</li>
     *     <li>Creates and assigns a {@link FinalEntrantListAdapter} to {@link #listView}.</li>
     *     <li>Calculates the fill percentage and updates {@link #capacityText},
     *         {@link #percentageCapacityText}, and {@link #progressBar}.</li>
     *     <li>Attaches a {@link SearchView.OnQueryTextListener} that filters the
     *         entrant list on both text change and query submission.</li>
     * </ol>
     *
     * @param savedInstanceState a {@link Bundle} containing the fragment's previously saved state,
     *                           or {@code null} if this is a fresh creation
     * @return the fully inflated and configured {@link View} for this fragment
     */
    @Nullable
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.final_entrant_list_activity);

        capacityText = findViewById(R.id.capacityValueText);
        percentageCapacityText = findViewById(R.id.percentageCapacityText);
        progressBar = findViewById(R.id.capacityProgressBar);
        searchView = findViewById(R.id.entrantSearchView);
        listView = findViewById(R.id.entrantList);

        findViewById(R.id.btnBackCancelled).setOnClickListener(v -> finish());

        event = (Event) getIntent().getSerializableExtra("EXTRA_EVENT");
        finalEntrants = event.getEnrolledEntrants();
        if (finalEntrants == null) finalEntrants = new ArrayList<>();

        maxCapacity = event.getMaxCapacity();
        if (maxCapacity == null || maxCapacity == 0) maxCapacity = 1L; // Avoid division by zero

        entrantAdapter = new FinalEntrantListAdapter(this, finalEntrants);
        listView.setAdapter(entrantAdapter);

        int count = finalEntrants.size();
        int percentageCap = 0;
        if (maxCapacity != null && maxCapacity > 0) {
            percentageCap = (int) ((count * 100.0) / maxCapacity);
        }
        String capacityString = count + "/" + maxCapacity;
        String percentageString = percentageCap + "%";

        capacityText.setText(capacityString);
        percentageCapacityText.setText(percentageString);
        progressBar.setProgress(percentageCap);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                entrantAdapter.getFilter().filter(newText);
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                entrantAdapter.getFilter().filter(query);
                return false;
            }
        });

        findViewById(R.id.exportBtn).setOnClickListener(v -> exportToCSV());
    }

    /**
     * Exports the list of enrolled entrants to a CSV file and saves it to the device's
     * Downloads folder.
     *
     * <p>The CSV contains a single column header {@code "Entrant Device ID"} followed by
     * one device ID per row, sourced from {@link #finalEntrants}. The output file is named
     * {@code final_entrants_<eventTitle>.csv}.</p>
     *
     * <p>Uses {@link MediaStore} to write the file, which is the recommended approach for
     * API 29 and above and does not require the {@code WRITE_EXTERNAL_STORAGE} runtime
     * permission on those versions.</p>
     *
     * <p>A {@link Toast} is shown to the user on both success and failure:</p>
     * <ul>
     *     <li>On success: confirms the file path within Downloads.</li>
     *     <li>On failure: reports either that the file could not be created (if the
     *         {@link android.content.ContentResolver} returned a {@code null} {@link Uri})
     *         or the {@link IOException} message if writing failed.</li>
     * </ul>
     */
    private void exportToCSV(){
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Entrant Device ID\n");
        for (String entrant : finalEntrants) {
            csvBuilder.append(entrant).append("\n");
        }

        String fileName = "final_entrants_" + event.getTitle() + ".csv";
        String csvContent = csvBuilder.toString();

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                    outputStream.write(csvContent.getBytes());
                    outputStream.flush();
                }
                Toast.makeText(this, "Exported to Downloads/" + fileName, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Export failed: could not create file.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}