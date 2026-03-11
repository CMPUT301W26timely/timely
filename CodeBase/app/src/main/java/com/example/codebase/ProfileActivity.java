package com.example.codebase;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPhone;
    private Button buttonSaveProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        buttonSaveProfile = findViewById(R.id.buttonSaveProfile);

        if (AppCache.getInstance().hasCachedUser()) {
            populateFields(AppCache.getInstance().getCachedUser());
            refreshProfileInBackground();
        } else {
            loadProfile();
        }

        buttonSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void populateFields(User user) {
        editTextName.setText(user.getName());
        editTextEmail.setText(user.getEmail());
        editTextPhone.setText(user.getPhoneNumber());
    }

    private void loadProfile() {
        UserRepository.loadUserProfile(this, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                populateFields(user);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ProfileActivity.this,
                        "Failed to load profile",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshProfileInBackground() {
        UserRepository.loadUserProfile(this, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                populateFields(user);
            }

            @Override
            public void onError(Exception e) {
                // silent background refresh failure
            }
        });
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
                () -> Toast.makeText(ProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(ProfileActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show()
        );
    }
}