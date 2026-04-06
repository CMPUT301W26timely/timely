package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Legacy entry point kept for compatibility after search was folded into Explore.
 *
 * <p>Any stale route that still targets this activity is immediately forwarded to
 * {@link BrowseEventsActivity} so users land on the integrated Explore experience.</p>
 */
public class SearchEventsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, BrowseEventsActivity.class));
        finish();
    }
}
