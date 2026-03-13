/**
 * Package containing fragments and core components for the application UI.
 *
 * <p>This package includes classes responsible for displaying different
 * sections of the application using Android Fragments.</p>
 */
package com.example.codebase;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * HomeFragment represents the main or default screen of the application.
 *
 * <p>This fragment inflates the {@code fragment_home} layout and displays it
 * when the user navigates to the Home section of the app.</p>
 *
 * <p>It acts as a placeholder fragment and can be extended later to include
 * additional UI components and logic.</p>
 */
public class HomeFragment extends Fragment {

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * <p>This method inflates the {@code fragment_home.xml} layout file and
     * returns the root view for this fragment.</p>
     *
     * @param inflater The LayoutInflater object used to inflate views in the fragment.
     * @param container The parent view that the fragment's UI should attach to.
     * @param savedInstanceState If non-null, this fragment is being recreated from
     *                           a previous saved state.
     * @return The root View for the fragment's user interface.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_home, container, false);
    }
}
