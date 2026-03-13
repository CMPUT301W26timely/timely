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

    /** Displays the screen title, which differs between first-run and edit modes. */
    private TextView textViewTitle;

    /** Displays the screen subtitle, which differs between first-run and edit modes. */
    private TextView textViewSubtitle;

    /** Displays the device's unique identifier obtained from {@link DeviceIdManager}. */
    private TextView textViewDeviceId;

    /** Input field for the user's display name. Required; must not be empty. */
    private EditText editTextName;

    /** Input field for the user's email address. Required; validated against {@link Patterns#EMAIL_ADDRESS}. */
    private EditText editTextEmail;

    /** Input field for the user's phone number. Optional. */
    private EditText editTextPhone;

    /** Primary action button; labelled "Continue" in first-run mode or "Save" in edit mode. */
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
        editTextName     = findViewById(R.id.editTextName);
        editTextEmail    = findViewById(R.id.editTextEmail);
        editTextPhone    = findViewById(R.id.editTextPhone);
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
        } else {
            textViewTitle.setText(R.string.profile_settings_edit_title);
            textViewSubtitle.setText(R.string.profile_settings_edit_subtitle);
            buttonSaveChanges.setText(R.string.profile_settings_save);
        }
    }

    /**
     * Pre-populates the name, email, and phone input fields from the given {@link User}.
     *
     * <p>Called once synchronously from the {@link AppCache} (if a cached user exists)
     * and again asynchronously once {@link UserRepository} returns the Firestore record.
     *
     * @param user The {@link User} whose data should be displayed in the form fields.
     */
    private void populateFields(User user) {
        editTextName.setText(user.getName());
        editTextEmail.setText(user.getEmail());
        editTextPhone.setText(user.getPhoneNumber());
    }

    /**
     * Validates form input and persists the profile via {@link UserRepository}.
     *
     * <p>Validation rules (evaluated in order):
     * <ol>
     *   <li>Name must not be empty.</li>
     *   <li>Email must not be empty.</li>
     *   <li>Email must match {@link Patterns#EMAIL_ADDRESS}.</li>
     * </ol>
     *
     * <p>On successful save:
     * <ul>
     *   <li>{@link WelcomeActivity#KEY_PROFILE_PENDING} is set to {@code false} in
     *       {@link SharedPreferences}.</li>
     *   <li>A confirmation {@link Toast} is shown.</li>
     *   <li>In first-run mode, {@link OrganizerActivity} is started before finishing.</li>
     *   <li>In edit mode, the activity simply finishes.</li>
     * </ul>
     *
     * <p>On failure, a {@link Toast} error message is displayed.
     */
    private void saveProfile() {
        String name  = editTextName.getText().toString().trim();
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

    /**
     * Intercepts the back button to enforce mandatory profile completion in first-run mode.
     *
     * <p>If {@link #isFirstRun} is {@code true}, a {@link Toast} reminds the user that
     * profile setup is required and back navigation is suppressed. Otherwise, the default
     * back behaviour is preserved.
     */
    @Override
    public void onBackPressed() {
        if (isFirstRun) {
            Toast.makeText(this, getString(R.string.profile_settings_required), Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}