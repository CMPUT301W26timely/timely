package com.example.codebase;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * EventDetailActivity — Refined event detail screen.
 */
public class EventDetailActivity extends AppCompatActivity implements FinalEntrantListFragment.FinalEntrantListListener{

    public static final String EXTRA_EVENT_ID    = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";

    private String eventId;
    private String eventTitle;

    //All event details should be stored in a event class
    private Event event;

    private TextView tvDetailTitle;
    private TextView tvDetailDate;
    private TextView tvDetailLocation;
    private TextView tvDetailDescription;
    private TextView tvStatusBadge;
    private TextView tvWaitingCount;
    private TextView tvSelectedCount;
    private android.widget.ImageView ivHeroPoster;
    private View     progressBar;
    
    private CollapsingToolbarLayout collapsingToolbar;
    private MaterialToolbar toolbar;

    private final SimpleDateFormat displayFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        eventId    = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        // ── Bind views ────────────────────────────────────────────────────────
        tvDetailTitle       = findViewById(R.id.tvDetailTitle);
        tvDetailDate        = findViewById(R.id.tvDetailDate);
        tvDetailLocation    = findViewById(R.id.tvDetailLocation);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvStatusBadge       = findViewById(R.id.tvDetailStatusBadge);
        tvWaitingCount      = findViewById(R.id.tvWaitingCount);
        tvSelectedCount     = findViewById(R.id.tvSelectedCount);
        ivHeroPoster        = findViewById(R.id.ivHeroPoster);
        progressBar         = findViewById(R.id.detailProgressBar);
        
        collapsingToolbar   = findViewById(R.id.collapsingToolbar);
        toolbar             = findViewById(R.id.toolbar);
        AppBarLayout appbar = findViewById(R.id.appbar);

        Button btnViewWaitingList = findViewById(R.id.btnViewWaitingList);
        Button btnViewQr = findViewById(R.id.btnViewQr);
        // ── Stats Boxes ───────────────────────────────────────────────────────
//        findViewById(R.id.boxViewEnrolled).setOnClickListener(v ->
//                Toast.makeText(this,
//                    "Add nav to Final Entrants frag",
//                    Toast.LENGTH_SHORT).show()
//        );

        // ── Toolbar / Back button ─────────────────────────────────────────────
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        if (eventTitle != null) {
            collapsingToolbar.setTitle(eventTitle);
        }

        // ── Animate Back Button & Title ─────────────────────────────────────────
        appbar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            int colorWhite = getColor(R.color.white);
            int colorDark = getColor(R.color.textPrimary);
            
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                int totalScroll = appBarLayout.getTotalScrollRange();
                if (totalScroll == 0) return;
                
                float percentage = (float) Math.abs(verticalOffset) / (float) totalScroll;
                
                // When collapsed (scrolled up > 80%), switch back icon to dark so it's visible on light bg
                if (percentage > 0.8f) {
                    toolbar.setNavigationIconTint(colorDark);
                } else {
                    toolbar.setNavigationIconTint(colorWhite);
                }
            }
        });

        // ── Actions ──────────────────────────────────────────────────────────
        btnViewWaitingList.setOnClickListener(v ->
                Toast.makeText(this, "View waiting list coming soon", Toast.LENGTH_SHORT).show()
        );

        btnViewQr.setOnClickListener(v -> {
            Intent intent = new Intent(this, QrDisplayActivity.class);
            intent.putExtra("event_id", eventId);
            intent.putExtra("event_title", eventTitle);
            startActivity(intent);
        });

        loadEventDetails();
    }

    private void loadEventDetails() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::onEventLoaded)
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
                });
    }

    private void onEventLoaded(DocumentSnapshot doc) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);

        if (!doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Event event = doc.toObject(Event.class);
        // ── Poster image ──────────────────────────────────────────────────────
        String posterBase64 = event.getPoster().getPosterImageBase64();
        if (posterBase64 != null && !posterBase64.isEmpty()) {
            try {
                ivHeroPoster.setImageBitmap(EventPoster.decodeImage(posterBase64));
            } catch (Exception e) {
                ivHeroPoster.setImageResource(R.drawable.bg_hero_image); // Fallback
            }
        } else {
            ivHeroPoster.setImageResource(R.drawable.bg_hero_image); // Fallback
        }

        // ── Basic fields ──────────────────────────────────────────────────────
        eventTitle = (event.getTitle()) != null ? event.getTitle() :
                     (event.getTitle() != null ? event.getTitle() : "Untitled Event");
                     
        collapsingToolbar.setTitle(eventTitle);
        tvDetailTitle.setText(eventTitle);
        
        String location    = event.getLocation()    != null ? event.getLocation()    : "Location TBD";
        String description = event.getDescription() != null ? event.getDescription() : "No description provided.";

        tvDetailLocation.setText(location);
        tvDetailDescription.setText(description);

        // ── Timestamps ────────────────────────────────────────────────────────
        
        Date startDate = event.getStartDate() != null ? event.getStartDate() : null;
        Date endDate = event.getEndDate() != null ? event.getEndDate() : null;

        String dateRangeStr = "Date TBD";
        if (startDate != null && endDate != null) {
            dateRangeStr = displayFormat.format(startDate) + " - " + displayFormat.format(endDate);
        } else if (startDate != null) {
            dateRangeStr = displayFormat.format(startDate);
        }
        tvDetailDate.setText(dateRangeStr);

        // ── Arrays ────────────────────────────────────────────────────────────
        List<?> waitingList      = (List<?>) event.getWaitingList();
        List<?> selectedEntrants = (List<?>) event.getSelectedEntrants();

        int waitingCount  = waitingList      != null ? waitingList.size()      : 0;
        int selectedCount = selectedEntrants != null ? selectedEntrants.size() : 0;

        tvWaitingCount.setText(String.valueOf(waitingCount));
        tvSelectedCount.setText(String.valueOf(selectedCount));

        // ── Status badge ──────────────────────────────────────────────────────
        Date regDeadline = event.getRegistrationDeadline() != null ? event.getRegistrationDeadline() : null;
        String status = event.getStatus();
        
        if ("draft".equalsIgnoreCase(status)) {
            tvStatusBadge.setText("DRAFT");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_closed);
            tvStatusBadge.setTextColor(getColor(R.color.textSecondary));
        } else if (regDeadline != null && new Date().before(regDeadline)) {
            tvStatusBadge.setText("REGISTRATION OPEN");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_open);
            tvStatusBadge.setTextColor(getColor(R.color.primaryAccent));
        } else {
            tvStatusBadge.setText("REGISTRATION CLOSED");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_closed);
            tvStatusBadge.setTextColor(getColor(R.color.textSecondary));
        }
    }

    @Override
    public Event getEvent() {
        return event;
    }
}