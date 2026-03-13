package com.example.codebase;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

//User story 02.06.03
public class FinalEntrantListFragment extends Fragment {

    interface FinalEntrantListListener{
        Event getEvent();

    }

    private FinalEntrantListListener listener;

    TextView capacityText;
    TextView percentageCapacityText;
    ProgressBar progressBar;
    SearchView searchView;
    ListView listView;

    Event event;
    ArrayList<String> finalEntrants;
    Long maxCapacity;

    FinalEntrantListAdapter entrantAdapter;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof FinalEntrantListListener)
            listener = (FinalEntrantListListener) context;
        else
            throw new RuntimeException(context + "Must implement GetFinalEntrantListListener");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.final_entrant_list_fragment, container, false);

        capacityText = view.findViewById(R.id.capacityValueText);
        percentageCapacityText = view.findViewById(R.id.percentageCapacityText);
        progressBar = view.findViewById(R.id.capacityProgressBar);
        searchView = view.findViewById(R.id.entrantSearchView);
        listView = view.findViewById(R.id.entrantList);

        event = listener.getEvent();
        finalEntrants = event.getEnrolledEntrants();
        maxCapacity = event.getMaxCapacity();
        entrantAdapter = new FinalEntrantListAdapter(view.getContext(), finalEntrants);
        listView.setAdapter(entrantAdapter);

        int percentageCap = 0;
        if (maxCapacity != null && maxCapacity > 0) {
            percentageCap = (int) ((finalEntrants.size() * 100L) / maxCapacity);
        }
        String capacityString = finalEntrants.size()+"/"+maxCapacity;
        String percentageString = Integer.toString(percentageCap)+"%";

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
