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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * ProfileSettingsActivity allows the user to create or update profile info.
 * This satisfies US 01.02.01 and US 01.02.02.
 */
public class ProfileSettingsActivity extends AppCompatActivity {

    public static final String EXTRA_FIRST_RUN = "extra_first_run";

    private boolean isFirstRun;
    private boolean isPopulatingFields;
    private boolean hasUserEditedFields;
    private TextView textViewTitle;
    private TextView textViewSubtitle;
    private TextView textViewDeviceId;
    private TextView textViewModeBadge;
    private ImageButton buttonBack;
    private TextInputLayout inputLayoutName;
    private TextInputLayout inputLayoutEmail;
    private TextInputLayout inputLayoutPhone;
    private TextInputEditText editTextName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPhone;
    private Button buttonSaveChanges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        isFirstRun = getIntent().getBooleanExtra(EXTRA_FIRST_RUN, false);

        textViewTitle = findViewById(R.id.textViewTitle);
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
        buttonSaveChanges = findViewById(R.id.buttonSaveChanges);

        applyMode();
        attachFieldWatchers();
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
    }

    private void applyMode() {
        if (isFirstRun) {
            textViewTitle.setText(R.string.profile_settings_first_run_title);
            textViewSubtitle.setText(R.string.profile_settings_first_run_subtitle);
            buttonSaveChanges.setText(R.string.profile_settings_continue);
            textViewModeBadge.setText(R.string.profile_settings_mode_required);
            textViewModeBadge.setBackgroundResource(R.drawable.bg_pill_amber);
            buttonBack.setVisibility(View.INVISIBLE);
        } else {
            textViewTitle.setText(R.string.profile_settings_edit_title);
            textViewSubtitle.setText(R.string.profile_settings_edit_subtitle);
            buttonSaveChanges.setText(R.string.profile_settings_save);
            textViewModeBadge.setText(R.string.profile_settings_mode_editable);
            textViewModeBadge.setBackgroundResource(R.drawable.bg_pill_green);
            buttonBack.setOnClickListener(v -> finish());
        }
    }

    private void attachFieldWatchers() {
        editTextName.addTextChangedListener(createWatcher(inputLayoutName));
        editTextEmail.addTextChangedListener(createWatcher(inputLayoutEmail));
        editTextPhone.addTextChangedListener(createWatcher(inputLayoutPhone));
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
        editTextName.setEnabled(!isSaving);
        editTextEmail.setEnabled(!isSaving);
        editTextPhone.setEnabled(!isSaving);
        buttonSaveChanges.setText(
                isSaving
                        ? R.string.profile_settings_saving
                        : isFirstRun
                        ? R.string.profile_settings_continue
                        : R.string.profile_settings_save
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

    @Override
    public void onBackPressed() {
        if (isFirstRun) {
            Toast.makeText(this, getString(R.string.profile_settings_required), Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}
