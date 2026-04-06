package com.example.codebase;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView adapter for the administrator profile browser.
 */
public class AdminProfileAdapter extends RecyclerView.Adapter<AdminProfileAdapter.ProfileViewHolder> {

    /** Listener interface for administrator profile actions. */
    public interface OnProfileActionListener {
        /**
         * Called when the administrator taps the delete button for a profile.
         *
         * @param user The profile to be deleted.
         */
        void onProfileDelete(User user);
    }

    private final List<User> profiles;
    private final OnProfileActionListener listener;

    public AdminProfileAdapter(List<User> profiles, OnProfileActionListener listener) {
        this.profiles = profiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_profile, parent, false);
        return new ProfileViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        holder.bind(profiles.get(position));
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {

        private final TextView textViewName;
        private final TextView textViewRole;
        private final TextView textViewStatus;
        private final TextView textViewEmail;
        private final TextView textViewPhone;
        private final TextView textViewDeviceId;
        private final ImageButton btnDelete;
        private final OnProfileActionListener listener;

        ProfileViewHolder(@NonNull View itemView, OnProfileActionListener listener) {
            super(itemView);
            this.listener = listener;
            textViewName = itemView.findViewById(R.id.textViewAdminProfileName);
            textViewRole = itemView.findViewById(R.id.textViewAdminProfileRole);
            textViewStatus = itemView.findViewById(R.id.textViewAdminProfileStatus);
            textViewEmail = itemView.findViewById(R.id.textViewAdminProfileEmail);
            textViewPhone = itemView.findViewById(R.id.textViewAdminProfilePhone);
            textViewDeviceId = itemView.findViewById(R.id.textViewAdminProfileDeviceId);
            btnDelete = itemView.findViewById(R.id.btnDeleteProfileAdmin);
        }

        /**
         * Binds the profile row with fallbacks so incomplete data still remains visible
         * to the administrator instead of disappearing from the list.
         */
        void bind(User user) {
            android.content.Context context = itemView.getContext();
            boolean isComplete = AdminBrowseHelper.isProfileComplete(user);
            boolean isAdmin = AdminBrowseHelper.isAdminProfile(user);

            textViewName.setText(
                    TextUtils.isEmpty(user.getName())
                            ? context.getString(R.string.admin_profiles_unnamed)
                            : user.getName()
            );
            textViewRole.setText(isAdmin ? R.string.role_admin : R.string.role_entrant);
            textViewRole.setBackgroundResource(isAdmin ? R.drawable.bg_pill_green : R.drawable.bg_pill_amber);

            textViewStatus.setText(isComplete
                    ? R.string.profile_status_complete
                    : R.string.profile_status_incomplete);
            textViewStatus.setBackgroundResource(isComplete
                    ? R.drawable.bg_pill_green
                    : R.drawable.bg_pill_amber);

            textViewEmail.setText(
                    TextUtils.isEmpty(user.getEmail())
                            ? context.getString(R.string.profile_placeholder_email)
                            : user.getEmail()
            );
            textViewPhone.setText(
                    TextUtils.isEmpty(user.getPhoneNumber())
                            ? context.getString(R.string.profile_placeholder_phone)
                            : user.getPhoneNumber()
            );
            textViewDeviceId.setText(context.getString(
                    R.string.admin_profiles_device_id_value,
                    DeviceIdManager.getShortenedId(user.getDeviceId())
            ));

            // Wire delete button for administrator use
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProfileDelete(user);
                }
            });
        }
    }
}
