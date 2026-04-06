package com.example.codebase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Shared helper for the second bottom-navigation slot, which is role-dependent.
 *
 * <p>Entrants use that slot for registration history, while administrators use the
 * same location for the profile browser. Centralising the logic keeps the label,
 * icon, and click destination consistent across screens.</p>
 */
public final class RoleAwareNavHelper {

    private RoleAwareNavHelper() {
    }

    /**
     * Returns whether the current app session is using the administrator role.
     */
    public static boolean isAdminSession(Context context) {
        return WelcomeActivity.ROLE_ADMIN.equals(WelcomeActivity.getSessionRole(context));
    }

    /**
     * Applies the correct icon and label to the shared second nav slot.
     *
     * @param context current context
     * @param iconView nav icon to update
     * @param labelView nav label to update
     */
    public static void configureSecondaryNav(Context context,
                                             ImageView iconView,
                                             TextView labelView) {
        if (!isAdminSession(context)) {
            return;
        }

        labelView.setText(R.string.admin_nav_profiles);
        iconView.setImageResource(R.drawable.ic_nav_profile);
        iconView.setContentDescription(context.getString(R.string.admin_nav_profiles));
    }

    /**
     * Starts the correct activity for the shared second nav slot and optionally
     * finishes the caller so navigation behaves like the rest of the app.
     */
    public static void openSecondaryNav(Activity activity, boolean finishCurrent) {
        Intent intent = new Intent(
                activity,
                isAdminSession(activity) ? AdminBrowseProfilesActivity.class : HistoryActivity.class
        );
        activity.startActivity(intent);
        if (finishCurrent) {
            activity.finish();
        }
    }
}
