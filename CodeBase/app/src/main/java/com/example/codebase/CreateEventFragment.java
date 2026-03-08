package com.example.codebase;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

public class CreateEventFragment extends Fragment {
    private FragmentCreateEventBinding binding;
    private Uri selectedImageUri;
    private ImageView previewImage;
    private final ActivityResultLauncher<String> pickImageLauncher =
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
        binding.buttonCancel.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventFragment.this)
                        .navigate(R.id.action_CreateEventFragment_to_FirstFragment)
        );

        return binding.getRoot();
    }

    private void saveEvent() {
        if (selectedImageUri == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference eventsRef = db.collection("events");
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("event_posters");

        String eventId = eventsRef.document().getId();
        StorageReference posterRef = storageRef.child(eventId + "/poster.jpg");

        posterRef.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return posterRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    Event event = new Event();
                    event.setTitle("My Event"); // replace with your actual form fields

                    EventPoster poster = new EventPoster(downloadUri.toString(), null);
                    event.setPoster(poster);

                    DocumentReference docRef = eventsRef.document(eventId);
                    docRef.set(event);
                });

        NavHostFragment.findNavController(CreateEventFragment.this)
                .navigate(R.id.action_CreateEventFragment_to_FirstFragment);
    }
}
