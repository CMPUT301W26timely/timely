package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button buttonBrowseEvents;
    private Button buttonOpenProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        UserRepository.syncRole(this);

        buttonBrowseEvents = findViewById(R.id.buttonBrowseEvents);
        buttonOpenProfile = findViewById(R.id.buttonOpenProfile);

        buttonBrowseEvents.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, BrowseEventsActivity.class)));

        buttonOpenProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileSettingsActivity.class)));
    }
}