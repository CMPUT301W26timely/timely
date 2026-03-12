package com.example.codebase;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;

public class QrDisplayActivity extends AppCompatActivity {
    private Bitmap qrBitmap;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_display); // Use the new layout wrapper

        eventId = getIntent().getStringExtra("event_id");
        String title = getIntent().getStringExtra("event_title");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish()); // Handle the new back button

        TextView tvTitle = findViewById(R.id.tvEventNameDisplay);
        TextView tvEyebrow = findViewById(R.id.tvQrEyebrow);
        ImageView ivQr = findViewById(R.id.ivQrCode);
        Button btnShare = findViewById(R.id.btnShare);

        tvTitle.setText(title != null ? title : "Event QR");
        tvEyebrow.setText("EVENT QR CODE");

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