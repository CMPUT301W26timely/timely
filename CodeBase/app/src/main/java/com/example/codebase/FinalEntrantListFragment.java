package com.example.codebase;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
public class FinalEntrantListFragment extends Fragment {

    /**
     * Callback interface that the hosting {@link android.app.Activity} must implement
     * to provide the {@link Event} whose final entrant list this fragment displays.
     *
     * <p>This interface is resolved in {@link #onAttach(Context)} and a
     * {@link RuntimeException} is thrown if the host does not implement it.</p>
     */
    interface FinalEntrantListListener {

        /**
         * Returns the {@link Event} whose enrolled entrant list should be displayed.
         *
         * @return the current {@link Event}; must not be {@code null}
         */
        Event getEvent();
    }

    /** The hosting {@link android.app.Activity} cast to {@link FinalEntrantListListener}. */
    private FinalEntrantListListener listener;

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
     * Verifies that the hosting {@link android.app.Activity} implements
     * {@link FinalEntrantListListener} and retains a reference to it.
     *
     * @param context the hosting {@link Context}; must implement {@link FinalEntrantListListener}
     * @throws RuntimeException if {@code context} does not implement
     *                          {@link FinalEntrantListListener}
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof FinalEntrantListListener)
            listener = (FinalEntrantListListener) context;
        else
            throw new RuntimeException(context + " Must implement FinalEntrantListListener");
    }

    /**
     * Inflates the fragment layout, retrieves event data from the host via
     * {@link FinalEntrantListListener#getEvent()}, and initializes all views.
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
     * @param inflater           the {@link LayoutInflater} used to inflate the fragment's view
     * @param container          the parent {@link ViewGroup} the fragment UI will be attached to,
     *                           or {@code null} if there is no parent
     * @param savedInstanceState a {@link Bundle} containing the fragment's previously saved state,
     *                           or {@code null} if this is a fresh creation
     * @return the fully inflated and configured {@link View} for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.final_entrant_list_fragment, container, false);

        capacityText = view.findViewById(R.id.capacityValueText);
        percentageCapacityText = view.findViewById(R.id.percentageCapacityText);
        progressBar = view.findViewById(R.id.capacityProgressBar);
        searchView = view.findViewById(R.id.entrantSearchView);
        listView = view.findViewById(R.id.entrantList);

        event = listener.getEvent();
        finalEntrants = event.getEnrolledEntrants();
        if (finalEntrants == null) finalEntrants = new ArrayList<>();

        maxCapacity = event.getMaxCapacity();
        if (maxCapacity == null || maxCapacity == 0) maxCapacity = 1L; // Avoid division by zero

        entrantAdapter = new FinalEntrantListAdapter(view.getContext(), finalEntrants);
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

        return view;
    }
}