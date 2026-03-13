package com.example.codebase;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Displays details for a selected event.
 */
public class EventDetailsActivity extends AppCompatActivity {

    private TextView textViewDetailEventName;
    private TextView textViewDetailEventDate;
    private TextView textViewDetailEventLocation;
    private TextView textViewDetailEventDescription;

    // Format Date objects into readable text
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        textViewDetailEventName = findViewById(R.id.textViewDetailEventName);
        textViewDetailEventDate = findViewById(R.id.textViewDetailEventDate);
        textViewDetailEventLocation = findViewById(R.id.textViewDetailEventLocation);
        textViewDetailEventDescription = findViewById(R.id.textViewDetailEventDescription);

        String eventId = getIntent().getStringExtra("eventId");

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEvent(eventId);
    }

    private void loadEvent(String eventId) {
        EventRepository.loadEventById(eventId, new EventRepository.EventDetailsCallback() {
            @Override
            public void onEventLoaded(Event event) {
                if (event == null) {
                    Toast.makeText(EventDetailsActivity.this, "Event not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Use merged team Event model fields
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