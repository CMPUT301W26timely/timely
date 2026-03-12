package com.example.codebase;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CreateEventFragment extends Fragment {

    private CreateEventViewModel viewModel;
    private FrameLayout posterUploadArea;
    private ImageView ivPosterPreview;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            Bitmap bitmap;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireActivity().getContentResolver(), imageUri));
                            } else {
                                bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                            }

                            // 1. Resize the image to prevent breaking Firestore 1MB limit
                            int MAX_DIMENSION = 800;
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                                float ratio = Math.min((float) MAX_DIMENSION / width, (float) MAX_DIMENSION / height);
                                width = Math.round(width * ratio);
                                height = Math.round(height * ratio);
                                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                            }

                            // 2. Compress and convert to Base64
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos); // 60% quality
                            byte[] imageBytes = baos.toByteArray();
                            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                            // 3. Save to ViewModel and Show Preview
                            viewModel.posterBase64 = base64Image;
                            if (ivPosterPreview == null) {
                                ivPosterPreview = new ImageView(getContext());
                                ivPosterPreview.setLayoutParams(new FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT));
                                ivPosterPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                posterUploadArea.addView(ivPosterPreview, 0);
                            }
                            ivPosterPreview.setImageBitmap(bitmap);

                        } catch (IOException e) {
                            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_basics, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(CreateEventViewModel.class);

        posterUploadArea = view.findViewById(R.id.posterUploadArea);
        posterUploadArea.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        EditText etEventName = view.findViewById(R.id.etEventName);
        EditText etDescription = view.findViewById(R.id.etDescription);
        EditText etLocation = view.findViewById(R.id.etLocation);
        EditText etPrice = view.findViewById(R.id.etPrice);

        // Pre-fill if navigating back
        etEventName.setText(viewModel.name);
        etDescription.setText(viewModel.description);
        etLocation.setText(viewModel.location);
        if (viewModel.price > 0) etPrice.setText(String.valueOf(viewModel.price));

        etEventName.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { viewModel.name = s.toString(); }
        });
        etDescription.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { viewModel.description = s.toString(); }
        });
        etLocation.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { viewModel.location = s.toString(); }
        });
        etPrice.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try { viewModel.price = Double.parseDouble(s.toString()); } catch (NumberFormatException ignored) {}
            }
        });

        return view;
    }

    // Helper text watcher to keep code clean
    public abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}