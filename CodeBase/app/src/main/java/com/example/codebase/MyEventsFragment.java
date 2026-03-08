package com.example.codebase;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.codebase.databinding.FragmentCreateEventBinding;
import com.example.codebase.databinding.FragmentMyEventsBinding;

public class MyEventsFragment extends Fragment {

    private FragmentMyEventsBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentMyEventsBinding.inflate(inflater, container, false);
        binding.buttonAddEvent.setOnClickListener(v ->
                NavHostFragment.findNavController(MyEventsFragment.this)
                        .navigate(R.id.action_MyEventsFragment_to_CreateEventFragment)
        );

        return binding.getRoot();
    }
}
