package com.example.codebase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Activity for creating or updating a user's profile information.
 *
 * <p>Operates in two distinct modes controlled by {@link #EXTRA_FIRST_RUN}:
 * <ul>
 *   <li><b>First-run mode</b> — shown during initial onboarding. The back button is
 *       disabled and the user must complete the form before proceeding to
 *       {@link OrganizerActivity}. Clears the {@link WelcomeActivity#KEY_PROFILE_PENDING}
 *       flag on successful save.</li>
 *   <li><b>Edit mode</b> — shown for returning users updating an existing profile.
 *       Normal back-navigation is permitted.</li>
 * </ul>
 *
 * <p>On launch the activity attempts to pre-populate fields from {@link AppCache}
 * (synchronous) and then from {@link UserRepository} (asynchronous Firestore fetch).
 *
 * <p>Satisfies user stories <b>US 01.02.01</b> and <b>US 01.02.02</b>.
 */
public class ProfileSettingsActivity extends AppCompatActivity {

    /**
     * Boolean {@link Intent} extra that controls the activity's operating mode.
     * <ul>
     *   <li>{@code true} — first-run / onboarding mode.</li>
     *   <li>{@code false} (default) — profile-edit mode.</li>
     * </ul>
     */
    public static final String EXTRA_FIRST_RUN = "extra_first_run";

    /**
     * Whether the activity was launched during first-run onboarding.
     * Derived from {@link #EXTRA_FIRST_RUN}; drives {@link #applyMode()}.
     */
    private boolean isFirstRun;
    private boolean isPopulatingFields;
    private boolean hasUserEditedFields;
    private TextView textViewTitle;

    /** Displays the screen subtitle, which differs between first-run and edit modes. */
    private TextView textViewSubtitle;

    /** Displays the device's unique identifier obtained from {@link DeviceIdManager}. */
    private TextView textViewDeviceId;
    private TextView textViewModeBadge;
    private ImageButton buttonBack;
    private TextInputLayout inputLayoutName;
    private TextInputLayout inputLayoutEmail;
    private TextInputLayout inputLayoutPhone;
    private TextInputEditText editTextName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPhone;
    private TextView textViewNotificationsSummary;
    private SwitchCompat switchNotificationsEnabled;
    private MaterialCardView cardDeleteProfile;
    private Button buttonDeleteProfile;
    private Button buttonSaveChanges;

    /**
     * Initialises the activity, resolves the operating mode, populates UI views,
     * and triggers an asynchronous profile load from {@link UserRepository}.
     *
     * @param savedInstanceState If the activity is being re-created from a previous state,
     *                           this bundle contains the most recent data; otherwise {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        isFirstRun = getIntent().getBooleanExtra(EXTRA_FIRST_RUN, false);

        textViewTitle    = findViewById(R.id.textViewTitle);
        textViewSubtitle = findViewById(R.id.textViewSubtitle);
        textViewDeviceId = findViewById(R.id.textViewDeviceId);
        textViewModeBadge = findViewById(R.id.textViewModeBadge);
        buttonBack = findViewById(R.id.buttonBack);
        inputLayoutName = findViewById(R.id.inputLayoutName);
        inputLayoutEmail = findViewById(R.id.inputLayoutEmail);
        inputLayoutPhone = findViewById(R.id.inputLayoutPhone);
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        textViewNotificationsSummary = findViewById(R.id.textViewNotificationsSummary);
        switchNotificationsEnabled = findViewById(R.id.switchNotificationsEnabled);
        cardDeleteProfile = findViewById(R.id.cardDeleteProfile);
        buttonDeleteProfile = findViewById(R.id.buttonDeleteProfile);
        buttonSaveChanges = findViewById(R.id.buttonSaveChanges);

        applyMode();
        attachFieldWatchers();
        attachNotificationToggle();
        textViewDeviceId.setText(
                getString(
                        R.string.profile_settings_device_id_value,
                        DeviceIdManager.getOrCreateDeviceId(this)
                )
        );

        if (AppCache.getInstance().hasCachedUser()) {
            populateFieldsIfPristine(AppCache.getInstance().getCachedUser());
        }

        UserRepository.loadUserProfile(this, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                populateFieldsIfPristine(user);
            }

            @Override
            public void onError(Exception e) {
                // Leave fields empty if Firestore load fails.
            }
        });

        buttonSaveChanges.setOnClickListener(v -> saveProfile());
        buttonDeleteProfile.setOnClickListener(v -> confirmDeleteProfile());
    }

    /**
     * Applies mode-specific string resources to the title, subtitle, and save button
     * based on the value of {@link #isFirstRun}.
     *
     * <p>First-run mode uses onboarding copy and a "Continue" button label.
     * Edit mode uses standard profile-editing copy and a "Save" button label.
     */
    private void applyMode() {
        if (isFirstRun) {
            textViewTitle.setText(R.string.profile_settings_first_run_title);
            textViewSubtitle.setText(R.string.profile_settings_first_run_subtitle);
            buttonSaveChanges.setText(R.string.profile_settings_continue);
            textViewModeBadge.setText(R.string.profile_settings_mode_required);
            textViewModeBadge.setBackgroundResource(R.drawable.bg_pill_amber);
            buttonBack.setVisibility(View.INVISIBLE);
            cardDeleteProfile.setVisibility(View.GONE);
        } else {
            textViewTitle.setText(R.string.profile_settings_edit_title);
            textViewSubtitle.setText(R.string.profile_settings_edit_subtitle);
            buttonSaveChanges.setText(R.string.profile_settings_save);
            textViewModeBadge.setText(R.string.profile_settings_mode_editable);
            textViewModeBadge.setBackgroundResource(R.drawable.bg_pill_green);
            buttonBack.setOnClickListener(v -> finish());
            cardDeleteProfile.setVisibility(View.VISIBLE);
        }
    }

    private void attachFieldWatchers() {
        editTextName.addTextChangedListener(createWatcher(inputLayoutName));
        editTextEmail.addTextChangedListener(createWatcher(inputLayoutEmail));
        editTextPhone.addTextChangedListener(createWatcher(inputLayoutPhone));
    }

    private void attachNotificationToggle() {
        switchNotificationsEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isPopulatingFields) {
                return;
            }
            hasUserEditedFields = true;
            updateNotificationSummary(isChecked);
        });
        updateNotificationSummary(switchNotificationsEnabled.isChecked());
    }

    private TextWatcher createWatcher(TextInputLayout layout) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No-op.
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isPopulatingFields) {
                    return;
                }
                hasUserEditedFields = true;
                layout.setError(null);
                layout.setErrorEnabled(false);
            }
        };
    }

    private void populateFieldsIfPristine(User user) {
        if (user == null || hasUserEditedFields) {
            return;
        }
        populateFields(user);
    }

    private void populateFields(User user) {
        isPopulatingFields = true;
        setTextIfDifferent(editTextName, user.getName());
        setTextIfDifferent(editTextEmail, user.getEmail());
        setTextIfDifferent(editTextPhone, user.getPhoneNumber());
        switchNotificationsEnabled.setChecked(user.isNotificationsEnabled());
        updateNotificationSummary(user.isNotificationsEnabled());
        isPopulatingFields = false;
    }

    private void setTextIfDifferent(TextInputEditText editText, String value) {
        String safeValue = value == null ? "" : value;
        String currentValue = readValue(editText);
        if (!safeValue.equals(currentValue)) {
            editText.setText(safeValue);
        }
    }

    private void saveProfile() {
        String name = ProfileInputValidator.safeTrim(readValue(editTextName));
        String email = ProfileInputValidator.safeTrim(readValue(editTextEmail));
        String phone = ProfileInputValidator.safeTrim(readValue(editTextPhone));
        boolean notificationsEnabled = switchNotificationsEnabled.isChecked();

        clearErrors();
        ProfileInputValidator.ValidationResult result =
                ProfileInputValidator.validate(name, email, phone);

        if (!result.isValid()) {
            showValidationError(result);
            return;
        }

        setSavingState(true);

        UserRepository.saveUserProfile(
                this,
                name,
                email,
                phone,
                notificationsEnabled,
                () -> {
                    setSavingState(false);
                    hasUserEditedFields = false;

                    SharedPreferences prefs =
                            getSharedPreferences(WelcomeActivity.PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().putBoolean(WelcomeActivity.KEY_PROFILE_PENDING, false).apply();

                    Toast.makeText(
                            this,
                            getString(R.string.profile_settings_saved),
                            Toast.LENGTH_SHORT
                    ).show();
                    if (isFirstRun) {
                        startActivity(new Intent(this, OrganizerActivity.class));
                    }
                    finish();
                },
                e -> {
                    setSavingState(false);
                    Toast.makeText(
                            this,
                            R.string.profile_settings_save_failed,
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );
    }

    private void setSavingState(boolean isSaving) {
        buttonSaveChanges.setEnabled(!isSaving);
        buttonDeleteProfile.setEnabled(!isSaving);
        editTextName.setEnabled(!isSaving);
        editTextEmail.setEnabled(!isSaving);
        editTextPhone.setEnabled(!isSaving);
        switchNotificationsEnabled.setEnabled(!isSaving);
        buttonBack.setEnabled(!isSaving);
        buttonSaveChanges.setText(
                isSaving
                        ? R.string.profile_settings_saving
                        : isFirstRun
                        ? R.string.profile_settings_continue
                        : R.string.profile_settings_save
        );
    }

    private void setDeletingState(boolean isDeleting) {
        buttonSaveChanges.setEnabled(!isDeleting);
        buttonDeleteProfile.setEnabled(!isDeleting);
        editTextName.setEnabled(!isDeleting);
        editTextEmail.setEnabled(!isDeleting);
        editTextPhone.setEnabled(!isDeleting);
        switchNotificationsEnabled.setEnabled(!isDeleting);
        buttonBack.setEnabled(!isDeleting);
        buttonDeleteProfile.setText(
                isDeleting
                        ? R.string.profile_settings_deleting
                        : R.string.profile_settings_delete_button
        );
    }

    private void clearErrors() {
        inputLayoutName.setError(null);
        inputLayoutEmail.setError(null);
        inputLayoutPhone.setError(null);
        inputLayoutName.setErrorEnabled(false);
        inputLayoutEmail.setErrorEnabled(false);
        inputLayoutPhone.setErrorEnabled(false);
    }

    private void showValidationError(ProfileInputValidator.ValidationResult result) {
        if (result.getField() == ProfileInputValidator.Field.NAME) {
            inputLayoutName.setErrorEnabled(true);
            inputLayoutName.setError(getString(R.string.profile_settings_name_required));
            editTextName.requestFocus();
            return;
        }

        if (result.getField() == ProfileInputValidator.Field.EMAIL) {
            String currentEmail = ProfileInputValidator.safeTrim(readValue(editTextEmail));
            int messageRes = currentEmail.isEmpty()
                    ? R.string.profile_settings_email_required
                    : R.string.profile_settings_email_invalid;
            inputLayoutEmail.setErrorEnabled(true);
            inputLayoutEmail.setError(getString(messageRes));
            editTextEmail.requestFocus();
            return;
        }

        if (result.getField() == ProfileInputValidator.Field.PHONE) {
            inputLayoutPhone.setErrorEnabled(true);
            inputLayoutPhone.setError(getString(R.string.profile_settings_phone_invalid));
            editTextPhone.requestFocus();
        }
    }

    private String readValue(TextInputEditText editText) {
        Editable editable = editText.getText();
        return editable == null ? "" : editable.toString();
    }

    private void updateNotificationSummary(boolean notificationsEnabled) {
        textViewNotificationsSummary.setText(
                notificationsEnabled
                        ? R.string.profile_settings_notifications_enabled_summary
                        : R.string.profile_settings_notifications_disabled_summary
        );
    }

    private void confirmDeleteProfile() {
        if (isFirstRun) {
            return;
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_settings_delete_confirm_title)
                .setMessage(R.string.profile_settings_delete_confirm_message)
                .setNegativeButton(R.string.profile_settings_delete_cancel, null)
                .setPositiveButton(R.string.profile_settings_delete_confirm, (dialog, which) ->
                        deleteProfile())
                .show();
    }

    private void deleteProfile() {
        setDeletingState(true);

        UserRepository.deleteUserProfile(
                this,
                () -> {
                    setDeletingState(false);
                    getSharedPreferences(WelcomeActivity.PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply();

                    Toast.makeText(
                            this,
                            R.string.profile_settings_delete_success,
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent = new Intent(this, WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                },
                e -> {
                    setDeletingState(false);
                    Toast.makeText(
                            this,
                            R.string.profile_settings_delete_failed,
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );
    }

    @Override
    public void onBackPressed() {
        if (isFirstRun) {
            Toast.makeText(this, getString(R.string.profile_settings_required), Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}
