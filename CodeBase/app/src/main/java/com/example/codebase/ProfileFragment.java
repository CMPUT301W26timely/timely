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
 * ProfileFragment shows the user's current profile info.
 * Clicking Edit opens ProfileSettingsActivity.
 */
public class ProfileFragment extends Fragment {

    private TextView textViewProfileName;
    private TextView textViewProfileEmail;
    private TextView textViewProfilePhone;
    private TextView textViewDeviceId;
    private TextView textViewEditProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        textViewProfileName = view.findViewById(R.id.textViewProfileName);
        textViewProfileEmail = view.findViewById(R.id.textViewProfileEmail);
        textViewProfilePhone = view.findViewById(R.id.textViewProfilePhone);
        textViewDeviceId = view.findViewById(R.id.textViewDeviceId);
        textViewEditProfile = view.findViewById(R.id.textViewEditProfile);

        // Show current device ID
        textViewDeviceId.setText(DeviceIdManager.getOrCreateDeviceId(requireContext()));

        loadProfile();

        // Open profile settings screen
        textViewEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ProfileSettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload profile after returning from settings page
        loadProfile();
    }

    private void loadProfile() {
        // Show cached profile first
        if (AppCache.getInstance().hasCachedUser()) {
            showUser(AppCache.getInstance().getCachedUser());
        }

        // Refresh from Firestore
        UserRepository.loadUserProfile(requireContext(), new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                showUser(user);
            }

            @Override
            public void onError(Exception e) {
                // Keep silent for now
            }
        });
    }

    /**
     * Update the UI with the current user's data.
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