package com.example.codebase;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Shared guard for admin-only activities.
 */
public final class AdminAccessHelper {

    private AdminAccessHelper() {
    }

    public static boolean isAdminSession(Context context) {
        return WelcomeActivity.ROLE_ADMIN.equals(WelcomeActivity.getSessionRole(context));
    }

    public static boolean finishIfNotAdmin(AppCompatActivity activity, String message) {
        if (isAdminSession(activity)) {
            return false;
        }

        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        activity.finish();
        return true;
    }
}
