package com.example.codebase;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

/**
 * The second screen in the app's navigation graph.
 *
 * <p>This fragment provides a button that navigates back to {@code FirstFragment}
 * via the Navigation component action {@code R.id.action_SecondFragment_to_FirstFragment}.</p>
 */
public class SecondFragment extends Fragment {

    /**
     * Inflates the fragment layout.
     *
     * @param inflater           the {@link LayoutInflater} used to inflate the fragment's view
     * @param container          the parent {@link ViewGroup} the fragment UI will be attached to,
     *                           or {@code null} if there is no parent
     * @param savedInstanceState a {@link Bundle} containing the fragment's previously saved state,
     *                           or {@code null} if this is a fresh creation
     * @return the inflated {@link View} for this fragment
     */
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_second, container, false);
    }

    /**
     * Binds the navigation button after the view hierarchy has been created.
     *
     * <p>Locates {@code R.id.button_second} and attaches a click listener that navigates
     * to {@code FirstFragment} using the Navigation component. A {@code null} check is
     * performed on the button before attaching the listener to guard against layout
     * mismatches between variants.</p>
     *
     * @param view               the fully inflated {@link View} returned by
     *                           {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param savedInstanceState a {@link Bundle} containing the fragment's previously saved state,
     *                           or {@code null} if this is a fresh creation
     */
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button buttonSecond = view.findViewById(R.id.button_second);
        if (buttonSecond != null) {
            buttonSecond.setOnClickListener(v ->
                    NavHostFragment.findNavController(SecondFragment.this)
                            .navigate(R.id.action_SecondFragment_to_FirstFragment)
            );
        }
    }
}