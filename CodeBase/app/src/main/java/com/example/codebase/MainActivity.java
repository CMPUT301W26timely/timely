package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity — Acts as a router to the appropriate primary screen based on the session role.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sync role to Firestore
        UserRepository.syncRole(this);

        // Simple routing logic: both User and Admin currently land on OrganizerActivity
        // but we keep MainActivity as the entry point for future branching.
        Intent intent = new Intent(this, OrganizerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        finish();
    }
}