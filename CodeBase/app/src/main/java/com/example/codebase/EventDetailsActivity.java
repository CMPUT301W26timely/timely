package com.example.codebase;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Displays read-only details for a selected event.
 *
 * <p>Receives a Firestore event document ID via the {@code "eventId"} intent extra,
 * fetches the corresponding {@link Event} through {@link EventRepository}, and
 * populates four text fields: name, date, location, and description.
 *
 * <p>The activity finishes immediately with a toast if the intent extra is missing
 * or blank, or if the repository cannot find the requested event.
 *
 * @see EventRepository
 * @see Event
 */
public class EventDetailsActivity extends AppCompatActivity {

    /** Displays the event title. */
    private TextView textViewDetailEventName;

    /** Displays the event start date formatted as {@code yyyy-MM-dd}, or "Date: Not set". */
    private TextView textViewDetailEventDate;

    /** Displays the event location prefixed with {@code "Location: "}. */
    private TextView textViewDetailEventLocation;

    /** Displays the event description. */
    private TextView textViewDetailEventDescription;

    /** Formats {@link java.util.Date} objects as {@code yyyy-MM-dd} strings for display. */
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    /**
     * Initialises the activity, binds views, validates the {@code "eventId"} intent
     * extra, and triggers the Firestore load via {@link #loadEvent(String)}.
     *
     * <p>If the {@code "eventId"} extra is absent or empty, a toast is shown and
     * the activity finishes without making a network call.
     *
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        textViewDetailEventName        = findViewById(R.id.textViewDetailEventName);
        textViewDetailEventDate        = findViewById(R.id.textViewDetailEventDate);
        textViewDetailEventLocation    = findViewById(R.id.textViewDetailEventLocation);
        textViewDetailEventDescription = findViewById(R.id.textViewDetailEventDescription);

        String eventId = getIntent().getStringExtra("eventId");

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEvent(eventId);
    }

    /**
     * Fetches the {@link Event} for the given ID via {@link EventRepository} and
     * populates the UI on success.
     *
     * <p>On success:
     * <ul>
     *   <li>If the returned event is {@code null}, a toast is shown and the activity
     *       finishes.</li>
     *   <li>Otherwise, {@link Event#getTitle()}, {@link Event#getLocation()},
     *       {@link Event#getDescription()}, and {@link Event#getStartDate()} are
     *       written to their respective {@link TextView} fields. The start date is
     *       formatted with {@link #dateFormat}; if {@code null}, "Date: Not set" is
     *       displayed.</li>
     * </ul>
     *
     * <p>On error, a long toast is shown with the exception message.
     *
     * @param eventId The Firestore document ID of the event to load.
     */
    private void loadEvent(String eventId) {
        EventRepository.loadEventById(eventId, new EventRepository.EventDetailsCallback() {
            @Override
            public void onEventLoaded(Event event) {
                if (event == null) {
                    Toast.makeText(EventDetailsActivity.this,
                            "Event not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                textViewDetailEventName.setText(event.getTitle());
                textViewDetailEventLocation.setText("Location: " + event.getLocation());
                textViewDetailEventDescription.setText(event.getDescription());

                if (event.getStartDate() != null) {
                    textViewDetailEventDate.setText("Date: " + dateFormat.format(event.getStartDate()));
                } else {
                    textViewDetailEventDate.setText("Date: Not set");
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(EventDetailsActivity.this,
                        "Failed to load event: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}