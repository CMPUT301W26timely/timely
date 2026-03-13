package com.example.codebase;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class FinalEntrantListAdapter extends ArrayAdapter<String> {
    public FinalEntrantListAdapter(Context context, ArrayList<String> entrants){
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

        String deviceId = getItem(position);

        TextView entrantName = view.findViewById(R.id.entrantNameTextView);
        TextView entrantEmail = view.findViewById(R.id.entrantEmailTextView);

        entrantName.setText("Loading...");
        entrantEmail.setText(deviceId != null ? deviceId : "");

        if (deviceId != null && !deviceId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(deviceId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            entrantName.setText(name != null ? name : "Unknown User");
                            entrantEmail.setText(email != null ? email : deviceId);
                        }
                    });
        }

        return view;
    }
}
