package com.example.codebase;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Shared organizer-access checks used by organizer-owned screens.
 */
public final class OrganizerPrivilegeHelper {

    /**
     * Callback for resolving the current user's organizer-access state.
     */
    public interface AccessCallback {
        void onAllowed(User user);
        void onRevoked(User user);
        void onError(Exception e);
    }

    private OrganizerPrivilegeHelper() {
    }

    public static boolean isRevoked(User user) {
        return user != null && user.isOrganizerPrivilegesRevoked();
    }

    public static String getRevocationMessage(Context context, User user) {
        if (user != null
                && user.getOrganizerRevocationMessage() != null
                && !user.getOrganizerRevocationMessage().trim().isEmpty()) {
            return user.getOrganizerRevocationMessage().trim();
        }
        return context.getString(R.string.organizer_revoked_default_message);
    }

    public static void checkCurrentUser(Context context, @NonNull AccessCallback callback) {
        if (WelcomeActivity.ROLE_ADMIN.equals(WelcomeActivity.getSessionRole(context))) {
            callback.onAllowed(null);
            return;
        }

        UserRepository.loadUserProfile(context, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                if (isRevoked(user)) {
                    callback.onRevoked(user);
                } else {
                    callback.onAllowed(user);
                }
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }
}
