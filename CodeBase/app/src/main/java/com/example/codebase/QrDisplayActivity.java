package com.example.codebase;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Generates and displays a QR code for a specific event, and allows the user to
 * share the QR code image via the Android share sheet.
 *
 * <p>The QR code encodes a deep-link URI in the format {@code timely://event/{eventId}},
 * rendered at 600×600 pixels using the ZXing library. The generated {@link Bitmap} is
 * displayed inline and cached to the app's internal cache directory when shared.
 *
 * <p>Expected {@link Intent} extras:
 * <ul>
 *   <li>{@code "event_id"} — the Firestore document ID of the event (required).</li>
 *   <li>{@code "event_title"} — the human-readable event title shown in the toolbar
 *       (optional; falls back to {@code "Event QR"}).</li>
 * </ul>
 */
public class QrDisplayActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";

    /**
     * The {@link Bitmap} containing the generated QR code.
     * {@code null} if QR generation failed in {@link #onCreate(Bundle)}.
     */
    private Bitmap qrBitmap;

    /**
     * Firestore document ID of the event, extracted from the launching {@link Intent}.
     * Used as the payload in the deep-link URI and as part of the cached image filename.
     */
    private String eventId;

    /** Cached title label view so it can be updated if the title is loaded asynchronously. */
    private TextView tvTitle;

    /**
     * Inflates the layout, reads intent extras, generates the QR code {@link Bitmap},
     * and wires the share button.
     *
     * <p>QR generation encodes {@code timely://event/{eventId}} as a
     * {@link BarcodeFormat#QR_CODE} at 600×600 px. If encoding fails a {@link Toast}
     * error is shown and {@link #qrBitmap} remains {@code null}.
     *
     * @param savedInstanceState If the activity is being re-created from a previous state,
     *                           this bundle contains the most recent data; otherwise {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_display);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        String title = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        ImageButton backButton = findViewById(R.id.toolbar);
        backButton.setOnClickListener(v -> finish());

        tvTitle   = findViewById(R.id.tvEventNameDisplay);
        TextView tvEyebrow = findViewById(R.id.tvQrEyebrow);
        ImageView ivQr     = findViewById(R.id.ivQrCode);
        Button btnShare    = findViewById(R.id.btnShare);

        tvTitle.setText(title != null && !title.trim().isEmpty() ? title : "Event QR");
        tvEyebrow.setText("EVENT QR CODE");

        if ((title == null || title.trim().isEmpty()) && eventId != null && !eventId.trim().isEmpty()) {
            loadEventTitle();
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event ID for QR code", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    "timely://event/" + eventId,
                    BarcodeFormat.QR_CODE, 600, 600
            );
            qrBitmap = new BarcodeEncoder().createBitmap(bitMatrix);
            ivQr.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR", Toast.LENGTH_SHORT).show();
        }

        btnShare.setOnClickListener(v -> shareQrCode());
    }

    private void loadEventTitle() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Event event = EventSchema.normalizeLoadedEvent(snapshot);
                    if (event != null && event.getTitle() != null && !event.getTitle().trim().isEmpty()) {
                        tvTitle.setText(event.getTitle().trim());
                    }
                });
    }

    /**
     * Writes {@link #qrBitmap} to the app's internal cache directory and launches the
     * Android share sheet so the user can send the QR code image to another app.
     *
     * <p>The bitmap is saved as a lossless PNG at
     * {@code <cacheDir>/images/qr_{eventId}.png} and exposed via {@link FileProvider}
     * using the authority {@code <packageName>.fileprovider}. A
     * {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} flag is added so the receiving app
     * can read the file without requiring additional permissions.
     *
     * <p>Does nothing if {@link #qrBitmap} is {@code null}. Shows a {@link Toast} error
     * message if any {@link Exception} is thrown during file I/O or URI resolution.
     */
    private void shareQrCode() {
        if (qrBitmap == null) return;
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "qr_" + eventId + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            if (contentUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                startActivity(Intent.createChooser(shareIntent, "Share Event QR"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sharing QR code", Toast.LENGTH_SHORT).show();
        }
    }
}
