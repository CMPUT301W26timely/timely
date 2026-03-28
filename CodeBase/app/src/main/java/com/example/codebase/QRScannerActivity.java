package com.example.codebase;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An {@link AppCompatActivity} that provides a live camera preview for scanning
 * QR codes associated with events.
 *
 * <p>This activity uses CameraX for camera lifecycle management and ML Kit's
 * {@link BarcodeScanner} for real-time QR code detection. When a valid QR code
 * is detected, the raw value is passed to {@link WelcomeActivity} as an
 * {@link Intent} extra under the key {@code "qr_result"} and this activity finishes.</p>
 *
 * <p>The scanning pipeline is:</p>
 * <ol>
 *     <li>CameraX binds a {@link Preview} and an {@link ImageAnalysis} use case to
 *         the activity lifecycle using the rear camera.</li>
 *     <li>Each camera frame is forwarded to {@link #processImage(ImageProxy, BarcodeScanner)}
 *         on a dedicated background {@link ExecutorService}.</li>
 *     <li>On a successful QR decode, {@link #qrIdentified} is set to {@code true} to
 *         prevent duplicate navigations before the activity finishes.</li>
 * </ol>
 *
 * <p>A cancel button is provided to dismiss the scanner without navigating anywhere.</p>
 *
 * <p><b>Note:</b> {@link #showEventDialog(String, Event)} is currently stubbed out.
 * The commented-out implementation contains incomplete dialog logic that should be
 * completed before enabling event preview from the scanner.</p>
 */
public class QRScannerActivity extends AppCompatActivity {

    /**
     * Request code used when requesting camera permission at runtime.
     * Reserved for future use if explicit permission prompting is added.
     */
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    /** Displays the live camera feed to the user. */
    private PreviewView previewView;

    /**
     * Single-threaded executor used to run camera image analysis off the main thread.
     * Shut down in {@link #onDestroy()} to release resources.
     */
    private ExecutorService cameraExecutor;

    /**
     * Guards against processing multiple QR codes in quick succession.
     * Set to {@code true} as soon as the first valid QR value is detected,
     * preventing duplicate navigations to {@link WelcomeActivity}.
     */
    private boolean qrIdentified = false;

    /**
     * Initializes the activity, sets up the cancel button, and starts the camera preview.
     *
     * @param savedInstanceState a {@link Bundle} containing the activity's previously saved state,
     *                           or {@code null} if this is a fresh launch
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        Button cancelButton = findViewById(R.id.button_cancel);
        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        cancelButton.setOnClickListener(v -> finish());

        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, CAMERA_PERMISSION_REQUEST);
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * Initializes and binds the CameraX use cases to this activity's lifecycle.
     *
     * <p>Two use cases are bound to the rear camera:</p>
     * <ul>
     *     <li>{@link Preview} — renders the live camera feed into {@link #previewView}.</li>
     *     <li>{@link ImageAnalysis} — forwards frames to
     *         {@link #processImage(ImageProxy, BarcodeScanner)} using a
     *         {@link ImageAnalysis#STRATEGY_KEEP_ONLY_LATEST} backpressure strategy,
     *         which drops older frames if the analyzer cannot keep up.</li>
     * </ul>
     *
     * <p>The {@link BarcodeScanner} is configured to detect only
     * {@link Barcode#FORMAT_QR_CODE} barcodes. If the camera provider future fails,
     * a {@link Toast} is shown with the error message.</p>
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build();
                BarcodeScanner barcodeScanner = BarcodeScanning.getClient(options);

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor,
                        imageProxy -> processImage(imageProxy, barcodeScanner));

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera init failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Fetches an {@link Event} document from Firestore by its ID and, if found,
     * presents it to the user via {@link #showEventDialog(String, Event)}.
     *
     * <p>If the document does not exist or cannot be normalized by
     * {@link EventSchema#normalizeLoadedEvent}, a {@link Toast} is shown and
     * {@link #qrIdentified} is reset to {@code false} so the scanner can
     * attempt another read.</p>
     *
     * @param eventId the Firestore document ID of the event to fetch; must not be {@code null}
     */
    private void fetchEventAndShowDialog(String eventId) {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Event event = EventSchema.normalizeLoadedEvent(documentSnapshot);
                    if (event != null) {
                        showEventDialog(eventId, event);
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        qrIdentified = false;
                    }
                });
    }

    /**
     * Displays a dialog presenting the scanned event's details to the user.
     *
     * <p><b>This method is currently a stub.</b> The full implementation — which
     * would inflate {@code R.layout.dialog_qr_result}, display the event poster and title,
     * and show a join waitlist button that is disabled when the waitlist is full —
     * has been commented out pending completion.</p>
     *
     * @param eventId the Firestore document ID of the event; must not be {@code null}
     * @param event   the normalized {@link Event} object to display; must not be {@code null}
     */
    private void showEventDialog(String eventId, Event event) {
        // TODO: Inflate R.layout.dialog_qr_result and display event poster, title,
        // and a join waitlist button. Disable the button when the waitlist is full.
    }

    /**
     * Processes a single camera frame through the ML Kit {@link BarcodeScanner}.
     *
     * <p>The {@link ImageProxy} is wrapped in an {@link InputImage} using its rotation
     * metadata to ensure correct orientation. On a successful scan:</p>
     * <ul>
     *     <li>The first non-null QR value found is handled if {@link #qrIdentified}
     *         is still {@code false}.</li>
     *     <li>{@link #qrIdentified} is set to {@code true} to block further processing.</li>
     *     <li>A navigation {@link Intent} to {@link WelcomeActivity} is launched on the
     *         main thread, carrying the raw QR value under the key {@code "qr_result"},
     *         and this activity finishes.</li>
     * </ul>
     *
     * <p>The {@link ImageProxy} is closed in all terminal paths — success, failure,
     * and cancellation — to prevent the CameraX analysis pipeline from stalling.</p>
     *
     * @param imageProxy the camera frame to analyze; must not be {@code null}
     * @param scanner    the configured {@link BarcodeScanner} to process the frame with;
     *                   must not be {@code null}
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImage(ImageProxy imageProxy, BarcodeScanner scanner) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        scanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null && !qrIdentified) {
                            qrIdentified = true;
                            runOnUiThread(() -> {
                                Intent intent = new Intent(this, WelcomeActivity.class);
                                intent.putExtra("qr_result", rawValue);
                                startActivity(intent);
                                finish();
                            });
                        }
                    }
                    imageProxy.close();
                })
                .addOnFailureListener(e -> imageProxy.close())
                .addOnCanceledListener(imageProxy::close);
    }

    /**
     * Shuts down the {@link #cameraExecutor} to release background thread resources
     * when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}