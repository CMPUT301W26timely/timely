package com.example.codebase;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CreateScheduleFragment extends Fragment {
    private CreateEventViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_schedule, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(CreateEventViewModel.class);

        EditText etEventStart = view.findViewById(R.id.etEventStart);
        EditText etEventEnd = view.findViewById(R.id.etEventEnd);
        EditText etRegOpen = view.findViewById(R.id.etRegOpen);
        EditText etRegClose = view.findViewById(R.id.etRegClose);
        EditText etCapacity = view.findViewById(R.id.etCapacity);

        // Pre-fill fields on navigation back
        etEventStart.setText(viewModel.eventStart);
        etEventEnd.setText(viewModel.eventEnd);
        etRegOpen.setText(viewModel.regOpen);
        etRegClose.setText(viewModel.regClose);
        if (viewModel.capacity > 0) etCapacity.setText(String.valueOf(viewModel.capacity));

        // Use proper Calendar pickers
        setupDatePicker(etEventStart, "Select Event Start Date", date -> viewModel.eventStart = date);
        setupDatePicker(etEventEnd, "Select Event End Date", date -> viewModel.eventEnd = date);
        setupDatePicker(etRegOpen, "Select Registration Open Date", date -> viewModel.regOpen = date);
        setupDatePicker(etRegClose, "Select Registration Close Date", date -> viewModel.regClose = date);

        etCapacity.addTextChangedListener(new CreateEventFragment.SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try { viewModel.capacity = Integer.parseInt(s.toString()); } catch (NumberFormatException ignored) {}
            }
        });

        return view;
    }

    // A simple callback interface to update ViewModel
    interface DateSelectionCallback {
        void onDateSelected(String date);
    }

    private void setupDatePicker(EditText editText, String titleText, DateSelectionCallback callback) {
        editText.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(titleText)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                // Convert UTC milliseconds to standard YYYY-MM-DD
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String formatted = sdf.format(new Date(selection));
                
                editText.setText(formatted);
                callback.onDateSelected(formatted);
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });
    }
}