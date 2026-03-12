package com.example.codebase;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.codebase.databinding.FragmentCreateEventBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CreateEventFragment extends Fragment {
    private FragmentCreateEventBinding binding;
    private Uri selectedImageUri;
    private ImageView previewImage;
    private ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null)
                    selectedImageUri = uri;
            });

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
        binding.posterUploadBox.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        binding.buttonPublishEvent.setOnClickListener(v -> saveEvent());
        binding.buttonCancel.setOnClickListener(v -> requireActivity().finish());

        return binding.getRoot();
    }

    private void saveEvent() {
        if (selectedImageUri == null) {
            Log.d("Firestore", "No image selected");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference eventsRef = db.collection("events");

        try {
            // Convert image to Base64 string
            InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedImageUri);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();
            String base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);

            int base64SizeBytes = base64Image.getBytes().length;
            int base64SizeKB = base64SizeBytes / 1024;

            Log.d("ImageSize", "Base64 size: " + base64SizeKB + " KB");

            int MAX_BASE64_KB = 850;

            if (base64SizeKB > MAX_BASE64_KB) {
                Toast.makeText(requireContext(),
                        "Poster image exceeds " + MAX_BASE64_KB + " KB.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String eventId = eventsRef.document().getId();

            Event event = new Event();

            EventPoster poster = new EventPoster(base64Image);
            event.setPoster(poster);
            event.setId("25");
            event.setTitle(binding.etEventTitle.getText().toString());
            event.setDescription(binding.etDescription.getText().toString());
            event.setLocation(binding.etLocation.getText().toString());
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
            try {
                Date startDate = formatter.parse(binding.etStartDate.getText().toString());
                event.setStartDate(startDate);
                Date endDate = formatter.parse(binding.etEndDate.getText().toString());
                event.setStartDate(endDate);
                Date regDate = formatter.parse(binding.etRegistrationDeadline.getText().toString());
                event.setStartDate(regDate);
            } catch (ParseException e) {

            }
            String deviceId = DeviceIdManager.getOrCreateDeviceId(requireContext());
            event.setOrganizerDeviceId(deviceId);

            eventsRef.document(eventId).set(event)
                    .addOnSuccessListener(unused -> {
                        Log.d("Firestore", "Event saved! ID: " + eventId);
                        requireActivity().finish();
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Failed to save event: " + e.getMessage()));

        } catch (Exception e) {
            Log.e("Firestore", "Failed to read image: " + e.getMessage());
        }
    }
}
