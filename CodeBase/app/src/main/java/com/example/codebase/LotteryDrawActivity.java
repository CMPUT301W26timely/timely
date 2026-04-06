package com.example.codebase;

import static android.view.View.GONE;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LotteryDrawActivity extends AppCompatActivity {
    TextView waitListView;
    TextView capacityView;
    TextView titleTextView;
    Button runLotteryBtn;
    Button drawReplacementBtn;
    EditText spotsEditText;
    CardView replacementCard;
    TextView actionText;
    TextView drawResultsText;

    Event event;
    String eventTitle;
    Long spotsAvailable;
    Integer waitingListSize;
    Integer numEnrolled;
    Integer numSelected;
    Integer numCancelled;

    String actionString = " spots have become vacant due to declined invitations";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lottery_draw);

        waitListView = findViewById(R.id.tvWaitingList);
        capacityView = findViewById(R.id.tvCapacity);
        titleTextView = findViewById(R.id.eventTitleText);
        runLotteryBtn = findViewById(R.id.runLotteryDrawButton);
        drawReplacementBtn = findViewById(R.id.drawReplacements);
        spotsEditText = findViewById(R.id.numberOfSpotsEditText);
        replacementCard = findViewById(R.id.replacementCard);
        actionText = findViewById(R.id.actionTextView);
        drawResultsText = findViewById(R.id.drawResultsTextView);

        findViewById(R.id.btnBackCancelled).setOnClickListener(v -> finish());

        event = (Event) getIntent().getSerializableExtra("EXTRA_EVENT");

        eventTitle = event.getTitle();
        waitingListSize = event.getWaitingList().size();
        numEnrolled = event.getEnrolledEntrants().size();
        numSelected = event.getSelectedEntrants().size();
        spotsAvailable = event.getMaxCapacity() - (numSelected + numEnrolled);
        numCancelled = event.getCancelledEntrants().size();

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

        drawReplacementBtn.setOnClickListener(v -> {

            Toast.makeText(this, "Redrawing replacements", Toast.LENGTH_SHORT).show();
            AppDatabase.getInstance().deleteCancelledEntrants(event, event.getCancelledEntrants());
            runDraw(numCancelled);
            replacementCard.setVisibility(GONE);
            drawResultsText.setVisibility(GONE);

        });

        if (numCancelled > 0){
            actionText.setText(numCancelled + actionString);
        }else{
            replacementCard.setVisibility(GONE);
            drawResultsText.setVisibility(GONE);
        }

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
        AppDatabase.getInstance().deleteWaitingEntrants(event, newEntrants);

        return true;
    }

}
