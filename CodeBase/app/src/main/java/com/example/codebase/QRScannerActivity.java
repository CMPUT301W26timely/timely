package com.example.codebase;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;

import java.util.concurrent.ExecutorService;

public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;

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

    }
}
