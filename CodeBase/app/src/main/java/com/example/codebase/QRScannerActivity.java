package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private boolean qrIdentified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        Button cancelButton = findViewById(R.id.button_cancel);
        PreviewView previewView = findViewById(R.id.previewView);

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                BarcodeScannerOptions options = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();
                BarcodeScanner barcodeScanner = BarcodeScanning.getClient(options);

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> processImage(imageProxy, barcodeScanner));

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

    private void fetchEventAndShowDialog(String eventId){
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    CreateEventViewModel event = documentSnapshot.toObject(CreateEventViewModel.class);
                    if (event != null) {
                        showEventDialog(eventId, event);
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        qrIdentified = false;
                    }
                });
    }

    private void showEventDialog(String eventId, CreateEventViewModel event){
//        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_qr_result, null);
//        ImageView poster  = dialogView.findViewById(R.id.poster);
//        TextView title = dialogView.findViewById(R.id.eventTitle);
//        Button waitingListButton = dialogView.findViewById(R.id.joinWaitingListButton);
//
//        //int waitingListCount = Math.toIntExact(event.getWaitingList().stream().count());
//        int waitingListMax = event.capacity;
//
//        if (waitingListMax > 0 && waitingListCount >= waitingListMax){
//            waitingListButton.setEnabled(false);
//            waitingListButton.setText("Waiting list is full");
//        }
//        else {
//            waitingListButton.setEnabled(true);
//            waitingListButton.setText("Join Waiting List");
//        }
//
//        event.getPoster();
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImage(ImageProxy imageProxy, BarcodeScanner scanner) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }
        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

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
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
