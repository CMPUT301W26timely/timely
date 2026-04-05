package com.example.codebase;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LotteryDrawActivity extends AppCompatActivity {

    TextView waitListView;
    TextView capacityView;
    TextView titleTextView;
    Button runLotteryBtn;
    EditText spotsEditText;

    Event event;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        waitListView = findViewById(R.id.tvWaitingList);
        capacityView = findViewById(R.id.tvCapacity);
        titleTextView = findViewById(R.id.eventTitleText);
        runLotteryBtn = findViewById(R.id.runLotteryDrawButton);
        spotsEditText = findViewById(R.id.numberOfSpotsEditText);

        findViewById(R.id.btnBackCancelled).setOnClickListener(v -> finish());

        event = (Event) getIntent().getSerializableExtra("EXTRA_EVENT");

        titleTextView.setText(event.getTitle());
        waitListView.setText(Integer.toString(event.getWaitingList().size()));
        capacityView.setText(event.getMaxCapacity().toString());

        runLotteryBtn.setOnClickListener(v -> {
            if(runDraw(Integer.valueOf(spotsEditText.getText().toString())))
                Toast.makeText(this, "Sent out invites", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Invalid value", Toast.LENGTH_SHORT).show();

            return;

        });

    }

    private boolean runDraw(Integer spots){
        if (spots <= 0)
            return false;



        return true;
    }
}
