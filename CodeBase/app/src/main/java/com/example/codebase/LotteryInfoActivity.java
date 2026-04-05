package com.example.codebase;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link AppCompatActivity} that displays an informational screen explaining
 * how the lottery system works.
 *
 * <p>Presents a four-step visual walkthrough covering:</p>
 * <ol>
 *     <li>Joining the waitlist</li>
 *     <li>The fair random draw process</li>
 *     <li>The notification window for winners</li>
 *     <li>The redraw process for unclaimed tickets</li>
 * </ol>
 */
public class LotteryInfoActivity extends AppCompatActivity{

    /**
     * Initializes the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lottery_info);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
