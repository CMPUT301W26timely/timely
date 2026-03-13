/**
 * Package containing core application components such as adapters,
 * fragments, and UI-related classes used throughout the app.
 */
package com.example.codebase;

import android.content.Context;
import android.widget.SimpleAdapter;

import java.util.List;
import java.util.Map;

/**
 * Adapter used to populate a list of notifications in a ListView.
 *
 * <p>This class extends {@link SimpleAdapter} to bind notification data
 * to the layout {@code item_notification}. Each notification row displays
 * a status and a message.</p>
 *
 * <p>The adapter maps the keys {@code "status"} and {@code "message"}
 * from the data source to the corresponding TextViews in the layout.</p>
 */
public class NotificationListAdapter extends SimpleAdapter {

    /**
     * Constructs a NotificationListAdapter.
     *
     * <p>The adapter binds notification data to the UI layout used for
     * displaying individual notification rows.</p>
     *
     * @param context The current context, used to access resources and layouts.
     * @param data A list of maps containing notification data where each map
     *             represents a single notification with keys "status" and "message".
     */
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
