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

/**
 * A {@link Fragment} that provides the UI for configuring event and registration schedule details
 * during the event creation flow.
 *
 * <p>This fragment allows organizers to select the following dates using a
 * {@link MaterialDatePicker}:</p>
 * <ul>
 *     <li>Event start and end dates</li>
 *     <li>Registration open and close dates</li>
 * </ul>
 *
 * <p>It also accepts an optional attendee capacity input. All values are persisted
 * to the shared {@link CreateEventViewModel} so they survive navigation between fragments.</p>
 */
public class CreateScheduleFragment extends Fragment {

    /** Shared ViewModel used to persist schedule data across the event creation flow. */
    private CreateEventViewModel viewModel;

    /**
     * Inflates the fragment layout, retrieves the shared {@link CreateEventViewModel},
     * pre-fills all input fields with any previously entered values, and attaches
     * date pickers and a text watcher to the appropriate fields.
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_schedule, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(CreateEventViewModel.class);

        EditText etEventStart = view.findViewById(R.id.etEventStart);
        EditText etEventEnd = view.findViewById(R.id.etEventEnd);
        EditText etRegOpen = view.findViewById(R.id.etRegOpen);
        EditText etRegClose = view.findViewById(R.id.etRegClose);
        EditText etCapacity = view.findViewById(R.id.etCapacity);

        // Pre-fill fields on navigation back
        etEventStart.setText(viewModel.startDate);
        etEventEnd.setText(viewModel.endDate);
        etRegOpen.setText(viewModel.registrationOpen);
        etRegClose.setText(viewModel.registrationDeadline);
        if (viewModel.capacity > 0) etCapacity.setText(String.valueOf(viewModel.capacity));

        // Use proper Calendar pickers
        setupDatePicker(etEventStart, "Select Event Start Date", date -> viewModel.startDate = date);
        setupDatePicker(etEventEnd, "Select Event End Date", date -> viewModel.endDate = date);
        setupDatePicker(etRegOpen, "Select Registration Open Date", date -> viewModel.registrationOpen = date);
        setupDatePicker(etRegClose, "Select Registration Close Date", date -> viewModel.registrationDeadline = date);

        etCapacity.addTextChangedListener(new CreateEventFragment.SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try { viewModel.capacity = Integer.parseInt(s.toString()); } catch (NumberFormatException ignored) {}
            }
        });

        return view;
    }

    /**
     * Callback interface used to return a formatted date string after the user
     * confirms a selection in the {@link MaterialDatePicker}.
     */
    interface DateSelectionCallback {
        /**
         * Called when the user successfully selects a date.
         *
         * @param date the selected date formatted as {@code yyyy-MM-dd}
         */
        void onDateSelected(String date);
    }

    /**
     * Attaches a {@link MaterialDatePicker} to the given {@link EditText} field.
     *
     * <p>When the field is clicked, a date picker dialog is shown with the provided title.
     * Upon confirmation, the selected date is formatted as {@code yyyy-MM-dd} in UTC
     * and written to the field. The supplied {@link DateSelectionCallback} is then
     * invoked to propagate the value to the {@link CreateEventViewModel}.</p>
     *
     * @param editText  the {@link EditText} that triggers and displays the selected date
     * @param titleText the title displayed at the top of the date picker dialog
     * @param callback  the {@link DateSelectionCallback} invoked with the formatted date string
     *                  once the user confirms their selection
     */
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