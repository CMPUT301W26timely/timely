package com.example.codebase;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Displays details for a selected event.
 */
public class EventDetailsActivity extends AppCompatActivity {

    private TextView textViewDetailEventName;
    private TextView textViewDetailEventDate;
    private TextView textViewDetailEventLocation;
    private TextView textViewDetailEventDescription;

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

                textViewDetailEventName.setText(event.getName());
                textViewDetailEventDate.setText("Date: " + event.getDate());
                textViewDetailEventLocation.setText("Location: " + event.getLocation());
                textViewDetailEventDescription.setText(event.getDescription());
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