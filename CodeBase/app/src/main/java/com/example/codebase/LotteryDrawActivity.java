package com.example.codebase;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LotteryDrawActivity extends AppCompatActivity {

    TextView waitListView;
    TextView capacityView;
    TextView titleTextView;
    Button runLotteryBtn;
    EditText spotsEditText;

    Event event;
    String eventTitle;
    Long spotsAvailable;
    Integer waitingListSize;
    Integer numEnrolled;
    Integer numSelected;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lottery_draw);

        waitListView = findViewById(R.id.tvWaitingList);
        capacityView = findViewById(R.id.tvCapacity);
        titleTextView = findViewById(R.id.eventTitleText);
        runLotteryBtn = findViewById(R.id.runLotteryDrawButton);
        spotsEditText = findViewById(R.id.numberOfSpotsEditText);

        findViewById(R.id.btnBackCancelled).setOnClickListener(v -> finish());

        event = (Event) getIntent().getSerializableExtra("EXTRA_EVENT");

        eventTitle = event.getTitle();
        waitingListSize = event.getWaitingList().size();
        numEnrolled = event.getEnrolledEntrants().size();
        numSelected = event.getSelectedEntrants().size();
        spotsAvailable = event.getMaxCapacity() - (numSelected + numEnrolled);

        titleTextView.setText(eventTitle);
        waitListView.setText(waitingListSize.toString());
        capacityView.setText(spotsAvailable.toString());

        runLotteryBtn.setOnClickListener(v -> {
            if(runDraw(Integer.valueOf(spotsEditText.getText().toString())))
                Toast.makeText(this, "Sent out invites", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Invalid value", Toast.LENGTH_SHORT).show();

            spotsEditText.setText("");

            return;

        });

    }

    private boolean runDraw(Integer spots){
        if (spots <= 0 || spots > waitingListSize || spots > spotsAvailable)
            return false;

        ArrayList<String> possibleEntrants;
        List<String> newEntrants;

        possibleEntrants = event.getWaitingList();
        Collections.shuffle(possibleEntrants);
        newEntrants = possibleEntrants.subList(0, spots);

        AppDatabase.getInstance().addSelectedEntrants(event, newEntrants);

        return true;
    }
}
