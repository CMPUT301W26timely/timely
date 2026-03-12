package com.example.codebase;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.materialswitch.MaterialSwitch;

public class CreateSettingsFragment extends Fragment {
    private CreateEventViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_settings, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(CreateEventViewModel.class);

        MaterialSwitch swLimitWaitlist = view.findViewById(R.id.swLimitWaitlist);
        MaterialSwitch swGeoRequired = view.findViewById(R.id.swGeoRequired);
        
        LinearLayout root = (LinearLayout) view;
        
        // Dynamic waitlist limit layout
        LinearLayout dynamicWaitlistLayout = new LinearLayout(getContext());
        dynamicWaitlistLayout.setOrientation(LinearLayout.VERTICAL);
        
        TextView label = new TextView(getContext());
        label.setText("Maximum waitlist entries");
        label.setTextSize(14);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setTextColor(getResources().getColor(R.color.textPrimary, null));
        label.setPadding(0, 0, 0, 8); // bottom margin
        
        EditText etWaitlistLimit = new EditText(getContext());
        etWaitlistLimit.setHint("e.g. 100");
        etWaitlistLimit.setInputType(InputType.TYPE_CLASS_NUMBER);
        etWaitlistLimit.setBackgroundResource(R.drawable.bg_input_field);
        etWaitlistLimit.setPadding(48, 48, 48, 48); // (16dp approx padding)
        
        dynamicWaitlistLayout.addView(label);
        dynamicWaitlistLayout.addView(etWaitlistLimit);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 48); // bottom margin
        dynamicWaitlistLayout.setLayoutParams(params);
        
        dynamicWaitlistLayout.setVisibility(viewModel.waitlistLimit > 0 ? View.VISIBLE : View.GONE);
        root.addView(dynamicWaitlistLayout, 2); // Insert after the first switch (index 2 because of header)

        swLimitWaitlist.setChecked(viewModel.waitlistLimit > 0);
        swGeoRequired.setChecked(viewModel.geoRequired);
        if (viewModel.waitlistLimit > 0) {
            etWaitlistLimit.setText(String.valueOf(viewModel.waitlistLimit));
        }

        swLimitWaitlist.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dynamicWaitlistLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                viewModel.waitlistLimit = -1;
                etWaitlistLimit.setText("");
            }
        });

        swGeoRequired.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.geoRequired = isChecked;
        });

        etWaitlistLimit.addTextChangedListener(new CreateEventFragment.SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    viewModel.waitlistLimit = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    viewModel.waitlistLimit = -1;
                }
            }
        });

        return view;
    }
}