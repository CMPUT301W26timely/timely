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

/**
 * A {@link Fragment} that provides the UI for configuring optional event settings
 * during the event creation flow.
 *
 * <p>This fragment manages three toggleable settings via {@link MaterialSwitch} controls:</p>
 * <ul>
 *     <li><b>Waitlist limit</b> — optionally caps the number of waitlist entries.
 *         When enabled, a dynamically inserted {@link EditText} allows the organizer
 *         to specify the maximum number of entries. When disabled, the limit is
 *         reset to {@code -1} to indicate no cap.</li>
 *     <li><b>Geolocation requirement</b> — flags whether entrants must provide
 *         their location when joining the event waitlist.</li>
 *     <li><b>Private event</b> (US 02.01.02) — when enabled, the event is hidden from
 *         the public browse listing and no promotional QR code is generated. Entrants
 *         can only join via direct organizer invitation (US 02.01.03).</li>
 * </ul>
 *
 * <p>All settings are persisted to the shared {@link CreateEventViewModel} so they
 * survive navigation between fragments in the event creation flow.</p>
 */
public class CreateSettingsFragment extends Fragment {

    /** Shared ViewModel used to persist event settings across the event creation flow. */
    private CreateEventViewModel viewModel;

    /**
     * Inflates the fragment layout, retrieves the shared {@link CreateEventViewModel},
     * and sets up the settings controls.
     *
     * <p>This method dynamically constructs and inserts a waitlist limit input layout
     * into the view hierarchy at index 2 (after the section header and the waitlist
     * limit switch). The layout is only visible when the waitlist limit switch is enabled.</p>
     *
     * <p>The following UI behaviours are configured here:</p>
     * <ul>
     *     <li>Pre-fills all controls with values from the {@link CreateEventViewModel}
     *         to restore state on back-navigation.</li>
     *     <li>Toggles visibility of the waitlist limit input when
     *         {@code swLimitWaitlist} is changed, and resets {@code viewModel.waitlistLimit}
     *         to {@code -1} when the switch is turned off.</li>
     *     <li>Updates {@code viewModel.geoRequired} whenever {@code swGeoRequired} is toggled.</li>
     *     <li>Parses and writes the waitlist limit integer to the ViewModel on text change,
     *         or resets it to {@code -1} on invalid input.</li>
     * </ul>
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
        View view = inflater.inflate(R.layout.fragment_create_settings, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(CreateEventViewModel.class);

        MaterialSwitch swLimitWaitlist = view.findViewById(R.id.swLimitWaitlist);
        MaterialSwitch swGeoRequired = view.findViewById(R.id.swGeoRequired);
        MaterialSwitch swPrivateEvent = view.findViewById(R.id.swPrivateEvent);

        LinearLayout root = (LinearLayout) view;

        // Dynamic waitlist limit layout
        LinearLayout dynamicWaitlistLayout = new LinearLayout(getContext());
        dynamicWaitlistLayout.setOrientation(LinearLayout.VERTICAL);

        TextView label = new TextView(getContext());
        label.setText("Maximum waitlist entries");
        label.setTextSize(14);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setTextColor(getResources().getColor(R.color.textPrimary, null));
        label.setPadding(0, 0, 0, 8);

        EditText etWaitlistLimit = new EditText(getContext());
        etWaitlistLimit.setHint("e.g. 100");
        etWaitlistLimit.setInputType(InputType.TYPE_CLASS_NUMBER);
        etWaitlistLimit.setBackgroundResource(R.drawable.bg_input_field);
        etWaitlistLimit.setPadding(48, 48, 48, 48);

        dynamicWaitlistLayout.addView(label);
        dynamicWaitlistLayout.addView(etWaitlistLimit);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 48);
        dynamicWaitlistLayout.setLayoutParams(params);

        // Show the waitlist input only if a limit was previously set
        dynamicWaitlistLayout.setVisibility(viewModel.waitlistLimit > 0 ? View.VISIBLE : View.GONE);
        root.addView(dynamicWaitlistLayout, 2); // Insert after the first switch (index 2 because of header)

        swLimitWaitlist.setChecked(viewModel.waitlistLimit > 0);
        swGeoRequired.setChecked(viewModel.geoRequired);
        swPrivateEvent.setChecked(viewModel.isPrivate);
        if (viewModel.waitlistLimit > 0) {
            etWaitlistLimit.setText(String.valueOf(viewModel.waitlistLimit));
        }

        // Toggle waitlist limit input visibility; reset limit when switch is turned off
        swLimitWaitlist.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dynamicWaitlistLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                viewModel.waitlistLimit = -1;
                etWaitlistLimit.setText("");
            }
        });

        // Persist geolocation requirement toggle to ViewModel
        swGeoRequired.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.geoRequired = isChecked;
        });

        // Persist private event toggle to ViewModel (US 02.01.02)
        swPrivateEvent.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.isPrivate = isChecked;
        });

        // Parse waitlist limit from input; reset to -1 on invalid or empty input
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