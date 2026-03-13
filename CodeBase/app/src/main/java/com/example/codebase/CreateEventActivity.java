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
import com.google.firebase.firestore.SetOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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
        if (getIntent().getBooleanExtra("isEditMode", false)) {
            viewModel.isEditMode = true;
            viewModel.editingEventId = getIntent().getStringExtra("editingEventId");
            viewModel.name = getIntent().getStringExtra("name") != null ? getIntent().getStringExtra("name") : "";
            viewModel.description = getIntent().getStringExtra("description") != null ? getIntent().getStringExtra("description") : "";
            viewModel.location = getIntent().getStringExtra("location") != null ? getIntent().getStringExtra("location") : "";
            viewModel.price = getIntent().getDoubleExtra("price", 0.0);
            viewModel.geoRequired = getIntent().getBooleanExtra("geoRequired", false);
            viewModel.waitlistLimit = getIntent().getIntExtra("waitlistCap", -1);
            viewModel.capacity = getIntent().getIntExtra("capacity", 0);
            viewModel.posterBase64 = getIntent().getStringExtra("posterBase64") != null ? getIntent().getStringExtra("posterBase64") : "";
            viewModel.startDate = readStringExtra("startDate", "eventStart");
            viewModel.endDate = readStringExtra("endDate", "eventEnd");
            viewModel.registrationOpen = readStringExtra("registrationOpen", "regOpen");
            viewModel.registrationDeadline = readStringExtra("registrationDeadline", "regClose");
        }


        viewPager = findViewById(R.id.viewPager);
        tvStepName = findViewById(R.id.tvStepName);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);
        btnDismiss = findViewById(R.id.btnDismiss);

        viewPager.setAdapter(new WizardPagerAdapter(this));
        viewPager.setUserInputEnabled(false);

        if (viewModel.isEditMode)
            tvStepName.setText("Edit Event");

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
            btnContinue.setText(viewModel.isEditMode ? "SAVE CHANGES" : getString(R.string.btn_publish));
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

    private String readStringExtra(String primaryKey, String legacyKey) {
        String value = getIntent().getStringExtra(primaryKey);
        if (value == null) {
            value = getIntent().getStringExtra(legacyKey);
        }
        return value != null ? value : "";
    }

    private void applyScheduleFields(Event event) {
        Timestamp startTs = parseDate(viewModel.startDate);
        Timestamp endTs = parseDate(viewModel.endDate);
        Timestamp registrationOpenTs = parseDate(viewModel.registrationOpen);
        Timestamp registrationDeadlineTs = parseDate(viewModel.registrationDeadline);

        event.setStartDate(startTs != null ? startTs.toDate() : null);
        event.setEndDate(endTs != null ? endTs.toDate() : null);
        event.setRegistrationOpen(registrationOpenTs != null ? registrationOpenTs.toDate() : null);
        event.setRegistrationDeadline(registrationDeadlineTs != null ? registrationDeadlineTs.toDate() : null);
        event.setDrawDate(EventSchema.calculateDrawDate(event.getRegistrationDeadline()));
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

        String eventId = viewModel.isEditMode ? viewModel.editingEventId : db.collection("events").document().getId();
        viewModel.generatedEventId = eventId;

        if (viewModel.isEditMode){
            db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
                Event existing = EventSchema.normalizeLoadedEvent(doc);
                if (existing == null) {
                    btnContinue.setEnabled(true);
                    btnContinue.setText("SAVE CHANGES");
                    Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show();
                    return;
                }

                existing.setTitle(viewModel.name);
                existing.setDescription(viewModel.description);
                existing.setLocation(viewModel.location);
                existing.setPrice((float) viewModel.price);
                existing.setMaxCapacity((long) viewModel.capacity);
                existing.setWinnersCount((long) viewModel.capacity);
                existing.setWaitlistCap(viewModel.waitlistLimit);
                existing.setGeoEnabled(viewModel.geoRequired);
                applyScheduleFields(existing);

                if (viewModel.posterBase64 != null && !viewModel.posterBase64.isEmpty())
                    existing.setPoster(new EventPoster(viewModel.posterBase64));

                db.collection("events").document(eventId).set(existing, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Event updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            btnContinue.setEnabled(true);
                            btnContinue.setText("SAVE CHANGES");
                            Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
                        });
            });

        } else {
            Event event = new Event();
            event.setId(eventId);
            event.setTitle(viewModel.name);
            event.setDescription(viewModel.description);
            event.setLocation(viewModel.location);
            event.setPrice((float) viewModel.price);
            event.setMaxCapacity((long) viewModel.capacity);
            event.setWinnersCount((long) viewModel.capacity);
            event.setWaitlistCap(viewModel.waitlistLimit);
            event.setGeoEnabled(viewModel.geoRequired);
            applyScheduleFields(event);


            // Poster Image (Base64)
            if (viewModel.posterBase64 != null && !viewModel.posterBase64.isEmpty())
                event.setPoster(new EventPoster(viewModel.posterBase64));

            event.setOrganizerDeviceId(DeviceIdManager.getOrCreateDeviceId(this));
            event.setStatus("published");
            event.setWaitingList(new ArrayList<>());
            event.setSelectedEntrants(new ArrayList<>());
            event.setCancelledEntrants(new ArrayList<>());
            event.setEnrolledEntrants(new ArrayList<>());

            db.collection("events").document(eventId).set(event, SetOptions.merge())
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
