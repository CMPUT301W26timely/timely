package com.example.codebase;

import android.content.Context;
import android.widget.SimpleAdapter;

import java.util.List;
import java.util.Map;

/**
 * Simple adapter for displaying notification rows.
 */
public class NotificationListAdapter extends SimpleAdapter {

    public NotificationListAdapter(Context context, List<? extends Map<String, ?>> data) {
        super(
                context,
                data,
                R.layout.item_notification,
                new String[]{"status", "message"},
                new int[]{R.id.tvNotificationStatus, R.id.tvNotificationMessage}
        );
    }
}