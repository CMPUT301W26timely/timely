package com.example.codebase;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView tvStepName;
    private Button btnContinue;
    private ImageButton btnBack, btnDismiss;
    private CreateEventViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        viewModel = new ViewModelProvider(this).get(CreateEventViewModel.class);

        viewPager = findViewById(R.id.viewPager);
        tvStepName = findViewById(R.id.tvStepName);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);
        btnDismiss = findViewById(R.id.btnDismiss);

        viewPager.setAdapter(new WizardPagerAdapter(this));
        viewPager.setUserInputEnabled(false);

        btnDismiss.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() > 0) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                updateStepUI();
            } else {
                finish();
            }
        });
        
        btnContinue.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current == 2) {
                publishEvent();
            } else if (current == 3) {
                finish(); // "DONE" clicked
            } else {
                viewPager.setCurrentItem(current + 1);
                updateStepUI();
            }
        });

        updateStepUI();
    }

    private void updateStepUI() {
        int pos = viewPager.getCurrentItem();
        int[] titleRes = {R.string.step_basics, R.string.step_schedule, R.string.step_settings, R.string.step_created};
        tvStepName.setText(titleRes[pos]);
        
        if (pos == 2) {
            btnContinue.setText(R.string.btn_publish);
            btnContinue.setEnabled(true);
        } else if (pos == 3) {
            btnContinue.setText(R.string.btn_done);
            btnContinue.setEnabled(true);
        } else {
            btnContinue.setText(R.string.btn_continue);
            btnContinue.setEnabled(true);
        }
    }

    private Timestamp parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            // Very simple parser for YYYY-MM-DD
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(dateStr.trim());
            return d != null ? new Timestamp(d) : null;
        } catch (ParseException e) {
            return null; // Ignore parse failures for now
        }
    }

    private void publishEvent() {
        // Basic Validation
        if (viewModel.name.trim().isEmpty() || viewModel.capacity <= 0) {
            Toast.makeText(this, "Name and Capacity are required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnContinue.setEnabled(false); // Prevent double submission
        btnContinue.setText("PUBLISHING...");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String eventId = db.collection("events").document().getId();
        viewModel.generatedEventId = eventId;
        
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("organizerId", DeviceIdManager.getOrCreateDeviceId(this));
        event.put("status", "published");
        event.put("createdAt", new Timestamp(new Date()));
        
        // Populate exactly matching the Prompt Schema
        event.put("name", viewModel.name);
        event.put("description", viewModel.description);
        event.put("location", viewModel.location);
        event.put("price", viewModel.price);
        event.put("capacity", viewModel.capacity);
        event.put("waitlistLimit", viewModel.waitlistLimit);
        event.put("geoEnabled", viewModel.geoRequired);

        // Parsed Timestamps
        event.put("eventStart", parseDate(viewModel.eventStart));
        event.put("eventEnd", parseDate(viewModel.eventEnd));
        event.put("regOpen", parseDate(viewModel.regOpen));
        event.put("regClose", parseDate(viewModel.regClose));

        // Poster Image (Base64)
        if (viewModel.posterBase64 != null && !viewModel.posterBase64.isEmpty()) {
            event.put("posterBase64", viewModel.posterBase64);
        }
        
        db.collection("events").document(eventId).set(event)
            .addOnSuccessListener(aVoid -> {
                generateQr(eventId);
                viewPager.setCurrentItem(3);
                updateStepUI();
            })
            .addOnFailureListener(e -> {
                btnContinue.setEnabled(true);
                btnContinue.setText(R.string.btn_publish);
                Toast.makeText(this, "Failed to publish", Toast.LENGTH_SHORT).show();
            });
    }

    private void generateQr(String eventId) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                "timely://event/" + eventId,
                BarcodeFormat.QR_CODE, 600, 600
            );
            Bitmap qrBitmap = new BarcodeEncoder().createBitmap(bitMatrix);
            viewModel.generatedQr = qrBitmap; // Store in ViewModel so Step 3 can grab it
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}