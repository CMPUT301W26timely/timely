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
public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID    = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";

    private String eventId;
    private String eventTitle;

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

        // ── Poster image ──────────────────────────────────────────────────────
        String posterBase64 = doc.getString("posterBase64");
        if (posterBase64 != null && !posterBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(posterBase64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivHeroPoster.setImageBitmap(decodedByte);
            } catch (Exception e) {
                ivHeroPoster.setImageResource(R.drawable.bg_hero_image); // Fallback
            }
        } else {
            ivHeroPoster.setImageResource(R.drawable.bg_hero_image); // Fallback
        }

        // ── Basic fields ──────────────────────────────────────────────────────
        eventTitle = doc.getString("name") != null ? doc.getString("name") : 
                     (doc.getString("title") != null ? doc.getString("title") : "Untitled Event");
                     
        collapsingToolbar.setTitle(eventTitle);
        tvDetailTitle.setText(eventTitle);
        
        String location    = doc.getString("location")    != null ? doc.getString("location")    : "Location TBD";
        String description = doc.getString("description") != null ? doc.getString("description") : "No description provided.";

        tvDetailLocation.setText(location);
        tvDetailDescription.setText(description);

        // ── Timestamps ────────────────────────────────────────────────────────
        Timestamp startDateTs = doc.getTimestamp("eventStart");
        Timestamp endDateTs = doc.getTimestamp("eventEnd");
        
        Date startDate = startDateTs != null ? startDateTs.toDate() : null;
        Date endDate = endDateTs != null ? endDateTs.toDate() : null;

        String dateRangeStr = "Date TBD";
        if (startDate != null && endDate != null) {
            dateRangeStr = displayFormat.format(startDate) + " - " + displayFormat.format(endDate);
        } else if (startDate != null) {
            dateRangeStr = displayFormat.format(startDate);
        }
        tvDetailDate.setText(dateRangeStr);

        // ── Arrays ────────────────────────────────────────────────────────────
        List<?> waitingList      = (List<?>) doc.get("waitingList");
        List<?> selectedEntrants = (List<?>) doc.get("selectedEntrants");

        int waitingCount  = waitingList      != null ? waitingList.size()      : 0;
        int selectedCount = selectedEntrants != null ? selectedEntrants.size() : 0;

        tvWaitingCount.setText(String.valueOf(waitingCount));
        tvSelectedCount.setText(String.valueOf(selectedCount));

        // ── Status badge ──────────────────────────────────────────────────────
        Timestamp regDeadlineTs = doc.getTimestamp("regClose");
        Date regDeadline = regDeadlineTs != null ? regDeadlineTs.toDate() : null;
        String status = doc.getString("status");
        
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
}