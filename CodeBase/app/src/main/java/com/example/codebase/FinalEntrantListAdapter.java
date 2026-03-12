package com.example.codebase;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class FinalEntrantListAdapter extends ArrayAdapter<Entrant> {
    public FinalEntrantListAdapter(Context context, ArrayList<Entrant> entrants){
        super(context, 0, entrants);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view;
        if(convertView == null)
            view = LayoutInflater.from(getContext()).inflate(R.layout.final_entrant_list_item, parent, false);
        else
            view = convertView;

        Entrant entrant = getItem(position);

        TextView entrantName = view.findViewById(R.id.entrantNameTextView);
        TextView entrantEmail = view.findViewById(R.id.entrantEmailTextView);

        entrantName.setText(entrant.getName());
        entrantName.setText(entrant.getEmail());

        return view;

    }
}
