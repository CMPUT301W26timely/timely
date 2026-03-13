package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Temporary home screen providing quick access to entrant user stories.
 *
 * <p>On creation, this activity performs two background operations:
 * <ul>
 *   <li>Synchronises the current user's role from Firestore via
 *       {@link UserRepository#syncRole(android.content.Context)}.</li>
 *   <li>Checks for unread lottery-result notifications (selected / not selected)
 *       via {@link SelectedNotificationChecker#checkAndShow(android.content.Context)}
 *       and surfaces any pending alerts to the user.</li>
 * </ul>
 *
 * <p>Navigation options:
 * <ul>
 *   <li><b>Browse Events</b> — launches {@link BrowseEventsActivity}.</li>
 *   <li><b>Open Profile</b> — launches {@link ProfileActivity}.</li>
 * </ul>
 */
public class MainActivity extends AppCompatActivity {

    /** Navigates to {@link BrowseEventsActivity} to view and join event waiting lists. */
    private Button buttonBrowseEvents;

    /** Navigates to {@link ProfileActivity} to view and edit the current user's profile. */
    private Button buttonOpenProfile;

    /**
     * Inflates the layout, triggers role sync and notification checks, and wires
     * navigation button click listeners.
     *
     * @param savedInstanceState If the activity is being re-created from a previous state,
     *                           this bundle contains the most recent data; otherwise {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        UserRepository.syncRole(this);

        // Check unread chosen/not chosen notifications when app opens.
        SelectedNotificationChecker.checkAndShow(this);

        buttonBrowseEvents = findViewById(R.id.buttonBrowseEvents);
        buttonOpenProfile  = findViewById(R.id.buttonOpenProfile);

        buttonBrowseEvents.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, BrowseEventsActivity.class)));

        buttonOpenProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
    }
}