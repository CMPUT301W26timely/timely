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

/**
 * Activity for creating or editing an event using a multi-step wizard interface.
 *
 * <p>This activity presents a {@link ViewPager2}-based wizard with the following steps:
 * <ol>
 *   <li><b>Basics</b> – Event name, description, location, poster, and capacity.</li>
 *   <li><b>Schedule</b> – Start/end dates and registration open/deadline dates.</li>
 *   <li><b>Settings</b> – Geo-requirement, waitlist cap, and pricing.</li>
 *   <li><b>Created</b> – Confirmation screen showing the generated QR code (create mode only).</li>
 * </ol>
 *
 * <p>When launched with {@code isEditMode = true} via the intent extras, the activity
 * pre-populates all fields from the intent and saves changes back to Firestore on publish.
 * In create mode, a new Firestore document is generated, the event is persisted, and a
 * QR code linking to {@code timely://event/<eventId>} is produced.
 *
 * <p>State across wizard steps is managed by {@link CreateEventViewModel}, which survives
 * configuration changes.
 *
 * @see CreateEventViewModel
 * @see WizardPagerAdapter
 * @see EventSchema
 */
public class CreateEventActivity extends AppCompatActivity {

    /** Hosts the individual wizard step fragments. */
    private ViewPager2 viewPager;

    /** Displays the name of the current wizard step. */
    private TextView tvStepName;

    /** Primary action button; label changes per step ("CONTINUE", "PUBLISH", "SAVE CHANGES", "DONE"). */
    private Button btnContinue;

    /** Navigates to the previous wizard step, or exits the activity on the first step. */
    private ImageButton btnBack;

    /** Dismisses the activity without saving. */
    private ImageButton btnDismiss;

    /** ViewModel that holds all event field values across wizard step navigations. */
    private CreateEventViewModel viewModel;

    /**
     * Initialises the activity, restores or pre-populates ViewModel state from intent extras,
     * sets up the {@link ViewPager2} wizard adapter, and wires navigation button listeners.
     *
     * <p>If the intent contains {@code isEditMode = true}, the following extras are read and
     * loaded into {@link #viewModel}:
     * <ul>
     *   <li>{@code editingEventId} – Firestore document ID of the event being edited.</li>
     *   <li>{@code name}, {@code description}, {@code location} – Basic event text fields.</li>
     *   <li>{@code price} – Ticket price as a {@code double}.</li>
     *   <li>{@code geoRequired} – Whether geolocation check-in is required.</li>
     *   <li>{@code waitlistCap} – Maximum waitlist size ({@code -1} for unlimited).</li>
     *   <li>{@code capacity} – Maximum number of attendees.</li>
     *   <li>{@code posterBase64} – Base64-encoded event poster image.</li>
     *   <li>{@code startDate} / {@code eventStart} – Event start date (primary / legacy key).</li>
     *   <li>{@code endDate} / {@code eventEnd} – Event end date (primary / legacy key).</li>
     *   <li>{@code registrationOpen} / {@code regOpen} – Registration open date.</li>
     *   <li>{@code registrationDeadline} / {@code regClose} – Registration deadline date.</li>
     * </ul>
     *
     * @param savedInstanceState If the activity is being re-created from a previous saved state,
     *                           this bundle contains that state; otherwise {@code null}.
     */
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
            viewModel.isPrivate = getIntent().getBooleanExtra("isPrivate", false);
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

    /**
     * Updates the step title label and the {@link #btnContinue} button text to reflect
     * the currently visible wizard step.
     *
     * <p>Step index mapping:
     * <ul>
     *   <li>0 – Basics</li>
     *   <li>1 – Schedule</li>
     *   <li>2 – Settings (triggers "PUBLISH" / "SAVE CHANGES")</li>
     *   <li>3 – Created (triggers "DONE")</li>
     * </ul>
     */
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

    /**
     * Parses a date string in {@code yyyy-MM-dd} format into a Firestore {@link Timestamp}.
     *
     * @param dateStr The date string to parse. May be {@code null} or empty.
     * @return A {@link Timestamp} representing midnight on the given date, or {@code null}
     *         if {@code dateStr} is null, blank, or cannot be parsed.
     */
    private Timestamp parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(dateStr.trim());
            return d != null ? new Timestamp(d) : null;
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Reads a string extra from the activity's intent, preferring a primary key and falling
     * back to a legacy key if the primary is absent.
     *
     * <p>This method supports backward compatibility with older intent callers that may use
     * different key names for the same field.
     *
     * @param primaryKey The preferred intent extra key to look up first.
     * @param legacyKey  The fallback intent extra key used if {@code primaryKey} is not present.
     * @return The string value found under either key, or an empty string if neither key
     *         is present in the intent.
     */
    private String readStringExtra(String primaryKey, String legacyKey) {
        String value = getIntent().getStringExtra(primaryKey);
        if (value == null) {
            value = getIntent().getStringExtra(legacyKey);
        }
        return value != null ? value : "";
    }

    /**
     * Applies the schedule-related date fields from {@link #viewModel} to the given
     * {@link Event} object.
     *
     * <p>Each date string is parsed via {@link #parseDate(String)} and converted to a
     * {@link java.util.Date}. The draw date is automatically derived from the registration
     * deadline using {@link EventSchema#calculateDrawDate(java.util.Date)}.
     *
     * @param event The {@link Event} instance whose schedule fields will be set.
     *              Modified in-place; must not be {@code null}.
     */
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

    /**
     * Validates the current ViewModel state and persists the event to Firestore.
     *
     * <p><b>Create mode:</b> A new Firestore document ID is generated, a fully populated
     * {@link Event} object is written with {@link SetOptions#merge()}, and on success a QR
     * code is generated via {@link #generateQr(String)} before advancing the wizard to the
     * final confirmation step.
     *
     * <p><b>Edit mode:</b> The existing Firestore document is fetched first, its mutable
     * fields are updated from the ViewModel, and the document is written back using
     * {@link SetOptions#merge()}. On success the activity finishes immediately.
     *
     * <p>The {@link #btnContinue} button is disabled during the Firestore operation to
     * prevent duplicate submissions and re-enabled (with an appropriate error toast) on
     * failure.
     *
     * <p><b>Validation:</b> Requires {@link CreateEventViewModel#name} to be non-blank and
     * {@link CreateEventViewModel#capacity} to be greater than zero; shows a toast and
     * returns early otherwise.
     */
    private void publishEvent() {
        if (viewModel.name.trim().isEmpty() || viewModel.capacity <= 0) {
            Toast.makeText(this, "Name and Capacity are required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnContinue.setEnabled(false);
        btnContinue.setText("PUBLISHING...");

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String eventId = viewModel.isEditMode ? viewModel.editingEventId : db.collection("events").document().getId();
        viewModel.generatedEventId = eventId;

        if (viewModel.isEditMode) {
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
                existing.setPrivate(viewModel.isPrivate);
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
            event.setPrivate(viewModel.isPrivate);
            applyScheduleFields(event);

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
                        // Private events do not generate a promotional QR code (US 02.01.02)
                        if (!viewModel.isPrivate) {
                            generateQr(eventId);
                        }
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

    /**
     * Generates a QR code {@link Bitmap} that encodes a deep-link URI for the given event
     * and stores the result in {@link CreateEventViewModel#generatedQr} so the confirmation
     * step fragment can display it.
     *
     * <p>The encoded URI follows the scheme {@code timely://event/<eventId>}.
     * The bitmap is produced at 600×600 pixels using {@link MultiFormatWriter} and
     * {@link BarcodeEncoder}. Any {@link WriterException} is caught and printed to the
     * error stream; in that case {@code viewModel.generatedQr} remains {@code null}.
     *
     * @param eventId The Firestore document ID of the newly created event. Must not be
     *                {@code null} or empty.
     */
    private void generateQr(String eventId) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    "timely://event/" + eventId,
                    BarcodeFormat.QR_CODE, 600, 600
            );
            Bitmap qrBitmap = new BarcodeEncoder().createBitmap(bitMatrix);
            viewModel.generatedQr = qrBitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}