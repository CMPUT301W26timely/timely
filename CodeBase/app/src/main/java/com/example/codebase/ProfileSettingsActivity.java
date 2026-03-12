package com.example.codebase;

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

    private TextView textViewDeviceId;
    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPhone;
    private Button buttonSaveChanges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        textViewDeviceId = findViewById(R.id.textViewDeviceId);
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        buttonSaveChanges = findViewById(R.id.buttonSaveChanges);

        // Show current device ID
        textViewDeviceId.setText(DeviceIdManager.getOrCreateDeviceId(this));

        // Load cached data first if available
        if (AppCache.getInstance().hasCachedUser()) {
            populateFields(AppCache.getInstance().getCachedUser());
        }

        // Refresh from Firestore
        UserRepository.loadUserProfile(this, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                populateFields(user);
            }

            @Override
            public void onError(Exception e) {
                // Silent for now
            }
        });

        buttonSaveChanges.setOnClickListener(v -> saveProfile());
    }

    /**
     * Fill text fields with existing user data.
     */
    private void populateFields(User user) {
        editTextName.setText(user.getName());
        editTextEmail.setText(user.getEmail());
        editTextPhone.setText(user.getPhoneNumber());
    }

    /**
     * Validate and save profile.
     */
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
                    Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
                    finish();
                },
                e -> Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show()
        );
    }
}