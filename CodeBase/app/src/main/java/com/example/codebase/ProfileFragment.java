package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * A {@link Fragment} that displays the current user's profile information
 * and provides a shortcut to edit it.
 *
 * <p>This fragment implements a cache-first loading strategy via {@link #loadProfile()}:</p>
 * <ol>
 *     <li>If {@link AppCache} holds a previously fetched {@link User}, it is displayed
 *         immediately to minimize perceived load time.</li>
 *     <li>A fresh fetch from Firestore via {@link UserRepository} is then triggered,
 *         updating the UI once the response arrives.</li>
 * </ol>
 *
 * <p>The profile is also reloaded in {@link #onResume()} so that any changes made
 * in {@link ProfileSettingsActivity} are reflected immediately on return.</p>
 *
 * <p>Tapping the edit link navigates to {@link ProfileSettingsActivity}.</p>
 */
public class ProfileFragment extends Fragment {

    /** Displays the user's name, or {@code "No name set"} if absent. */
    private TextView textViewProfileName;

    /** Displays the user's email address, or {@code "No email set"} if absent. */
    private TextView textViewProfileEmail;

    /** Displays the user's phone number, or {@code "No phone number set"} if absent. */
    private TextView textViewProfilePhone;

    /** Displays the device's unique ID as returned by {@link DeviceIdManager#getOrCreateDeviceId}. */
    private TextView textViewDeviceId;

    /** Tappable label that launches {@link ProfileSettingsActivity} when clicked. */
    private TextView textViewEditProfile;

    /**
     * Inflates the fragment layout.
     *
     * @param inflater           the {@link LayoutInflater} used to inflate the fragment's view
     * @param container          the parent {@link ViewGroup} the fragment UI will be attached to,
     *                           or {@code null} if there is no parent
     * @param savedInstanceState a {@link Bundle} containing the fragment's previously saved state,
     *                           or {@code null} if this is a fresh creation
     * @return the inflated {@link View} for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    /**
     * Binds views, populates the device ID, initiates the initial profile load,
     * and attaches the edit profile click listener.
     *
     * <p>The device ID is populated once here and does not need to be refreshed
     * on subsequent resumes, as it is immutable for the lifetime of the installation.</p>
     *
     * @param view               the fully inflated {@link View} returned by
     *                           {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param savedInstanceState a {@link Bundle} containing the fragment's previously saved state,
     *                           or {@code null} if this is a fresh creation
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        textViewProfileName = view.findViewById(R.id.textViewProfileName);
        textViewProfileEmail = view.findViewById(R.id.textViewProfileEmail);
        textViewProfilePhone = view.findViewById(R.id.textViewProfilePhone);
        textViewDeviceId = view.findViewById(R.id.textViewDeviceId);
        textViewEditProfile = view.findViewById(R.id.textViewEditProfile);

        textViewDeviceId.setText(DeviceIdManager.getOrCreateDeviceId(requireContext()));

        loadProfile();

        textViewEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ProfileSettingsActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Reloads the user profile each time the fragment becomes visible.
     *
     * <p>This ensures that edits made in {@link ProfileSettingsActivity} are
     * reflected immediately when the user navigates back to this fragment,
     * without requiring a manual refresh.</p>
     */
    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }

    /**
     * Loads the user's profile using a cache-first strategy.
     *
     * <p>If {@link AppCache} contains a previously fetched {@link User}, it is
     * passed to {@link #showUser(User)} immediately for a fast initial render.
     * A Firestore fetch via {@link UserRepository#loadUserProfile} is then
     * always performed to ensure the displayed data stays current.</p>
     *
     * <p>Firestore errors are silently ignored — the cached data, if any,
     * remains visible on screen.</p>
     */
    private void loadProfile() {
        if (AppCache.getInstance().hasCachedUser()) {
            showUser(AppCache.getInstance().getCachedUser());
        }

        UserRepository.loadUserProfile(requireContext(), new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                showUser(user);
            }

            @Override
            public void onError(Exception e) {
                // Silently retain cached data; no error UI shown to the user.
            }
        });
    }

    /**
     * Populates the profile {@link TextView}s with data from the given {@link User}.
     *
     * <p>Each field falls back to a human-readable placeholder string if the
     * corresponding {@link User} value is {@code null} or empty:</p>
     * <ul>
     *     <li>Name → {@code "No name set"}</li>
     *     <li>Email → {@code "No email set"}</li>
     *     <li>Phone → {@code "No phone number set"}</li>
     * </ul>
     *
     * @param user the {@link User} whose data should be displayed; must not be {@code null}
     */
    private void showUser(User user) {
        textViewProfileName.setText(
                TextUtils.isEmpty(user.getName()) ? "No name set" : user.getName()
        );
        textViewProfileEmail.setText(
                TextUtils.isEmpty(user.getEmail()) ? "No email set" : user.getEmail()
        );
        textViewProfilePhone.setText(
                TextUtils.isEmpty(user.getPhoneNumber()) ? "No phone number set" : user.getPhoneNumber()
        );
    }
}