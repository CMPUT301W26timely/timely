package com.example.codebase;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.io.FileOutputStream;

public class CreateQrFragment extends Fragment {
    private CreateEventViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_qr, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(CreateEventViewModel.class);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null && viewModel.generatedQr != null) {
            TextView tvEventNameDisplay = getView().findViewById(R.id.tvEventNameDisplay);
            ImageView ivQrCode = getView().findViewById(R.id.ivQrCode);
            Button btnShare = getView().findViewById(R.id.btnShare);

            tvEventNameDisplay.setText(viewModel.name);
            ivQrCode.setImageBitmap(viewModel.generatedQr);

            btnShare.setOnClickListener(v -> shareQrCode());
        }
    }

    private void shareQrCode() {
        try {
            File cachePath = new File(requireContext().getCacheDir(), "images");
            cachePath.mkdirs(); 
            File file = new File(cachePath, "qr_" + viewModel.generatedEventId + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            viewModel.generatedQr.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);

            if (contentUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                startActivity(Intent.createChooser(shareIntent, "Share Event QR"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error sharing QR code", Toast.LENGTH_SHORT).show();
        }
    }
}