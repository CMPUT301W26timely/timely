package com.example.codebase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * ProfileSettingsActivity allows the user to create or update profile info.
 * This satisfies US 01.02.01 and US 01.02.02.
 */
public class ProfileSettingsActivity extends AppCompatActivity {

    public static final String EXTRA_FIRST_RUN = "extra_first_run";

    private boolean isFirstRun;
    private TextView textViewTitle;
    private TextView textViewSubtitle;
    private TextView textViewDeviceId;
    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPhone;
    private Button buttonSaveChanges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        isFirstRun = getIntent().getBooleanExtra(EXTRA_FIRST_RUN, false);

        textViewTitle = findViewById(R.id.textViewTitle);
        textViewSubtitle = findViewById(R.id.textViewSubtitle);
        textViewDeviceId = findViewById(R.id.textViewDeviceId);
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        buttonSaveChanges = findViewById(R.id.buttonSaveChanges);

        applyMode();
        textViewDeviceId.setText(
                getString(
                        R.string.profile_settings_device_id_value,
                        DeviceIdManager.getOrCreateDeviceId(this)
                )
        );

        if (AppCache.getInstance().hasCachedUser()) {
            populateFields(AppCache.getInstance().getCachedUser());
        }

        UserRepository.loadUserProfile(this, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                populateFields(user);
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
        } else {
            textViewTitle.setText(R.string.profile_settings_edit_title);
            textViewSubtitle.setText(R.string.profile_settings_edit_subtitle);
            buttonSaveChanges.setText(R.string.profile_settings_save);
        }
    }

    private void populateFields(User user) {
        editTextName.setText(user.getName());
        editTextEmail.setText(user.getEmail());
        editTextPhone.setText(user.getPhoneNumber());
    }

    private void saveProfile() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Enter a valid email");
            return;
        }

        UserRepository.saveUserProfile(
                this,
                name,
                email,
                phone,
                () -> {
                    SharedPreferences prefs =
                            getSharedPreferences(WelcomeActivity.PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().putBoolean(WelcomeActivity.KEY_PROFILE_PENDING, false).apply();

                    Toast.makeText(this, getString(R.string.profile_settings_saved), Toast.LENGTH_SHORT).show();
                    if (isFirstRun) {
                        startActivity(new Intent(this, OrganizerActivity.class));
                    }
                    finish();
                },
                e -> Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show()
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
