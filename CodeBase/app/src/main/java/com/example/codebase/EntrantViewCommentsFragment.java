package com.example.codebase;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * A read-only {@link DialogFragment} that displays a filtered subset of comments
 * for an event from the entrant's perspective.
 *
 * <p>Implements US 01.08.02 — entrants may <em>view</em> comments but may
 * <strong>not</strong> delete them. Uses {@link EntrantCommentAdapter} (no
 * delete button) instead of the organizer-only {@link OrganizerCommentAdapter}.</p>
 *
 * <p>Two modes are supported:</p>
 * <ul>
 *   <li>{@link #MODE_ORGANIZER} — shows only comments posted by organizers.</li>
 *   <li>{@link #MODE_ENTRANT}  — shows only comments posted by entrants.</li>
 * </ul>
 *
 * <p>Filtering is done client-side from the list passed in via
 * {@link #newInstance}, so no second Firestore round-trip is required.</p>
 *
 * @see EntrantCommentAdapter
 * @see EntrantEventDetailActivity
 * @see Comment
 */
public class EntrantViewCommentsFragment extends DialogFragment {

    // ── Mode constants ─────────────────────────────────────────────────────────

    /** Show only comments where {@link Comment#isOrganizer()} is {@code true}. */
    public static final int MODE_ORGANIZER = 0;

    /** Show only comments where {@link Comment#isOrganizer()} is {@code false}. */
    public static final int MODE_ENTRANT = 1;

    // ── Bundle argument keys ───────────────────────────────────────────────────

    private static final String ARG_MODE = "mode";

    // ── Instance state ─────────────────────────────────────────────────────────

    /** Which comments to show: {@link #MODE_ORGANIZER} or {@link #MODE_ENTRANT}. */
    private int mode;

    /** The filtered comment list shown in the RecyclerView. */
    private final List<Comment> filteredList = new ArrayList<>();

    // ── Factory ────────────────────────────────────────────────────────────────

    /**
     * Creates a new instance pre-populated with the filtered comment list.
     *
     * @param allComments The full unfiltered comment list from the host activity.
     * @param mode        {@link #MODE_ORGANIZER} to show organizer comments,
     *                    or {@link #MODE_ENTRANT} to show all entrant comments.
     * @return A configured {@link EntrantViewCommentsFragment}.
     */
    public static EntrantViewCommentsFragment newInstance(
            List<Comment> allComments,
            int mode) {

        EntrantViewCommentsFragment f = new EntrantViewCommentsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, mode);
        f.setArguments(args);

        // Pre-filter client-side — no extra Firestore round-trip needed
        for (Comment c : allComments) {
            if (mode == MODE_ORGANIZER && c.isOrganizer()) {
                f.filteredList.add(c);
            } else if (mode == MODE_ENTRANT && !c.isOrganizer()) {
                f.filteredList.add(c);
            }
        }
        return f;
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL,
                android.R.style.Theme_Material_Light_Dialog_MinWidth);
        if (getArguments() != null) {
            mode = getArguments().getInt(ARG_MODE, MODE_ENTRANT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Reuse the same dialog layout as the organizer's ViewCommentsFragment
        return inflater.inflate(R.layout.fragment_view_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the dialog title based on which filter mode is active
        TextView tvTitle = view.findViewById(R.id.tvViewCommentsTitle);
        tvTitle.setText(mode == MODE_ORGANIZER
                ? getString(R.string.comments_organizer_title)
                : getString(R.string.comments_all_entrant_title));

        // Empty state label
        TextView tvEmpty = view.findViewById(R.id.tvViewCommentsEmpty);
        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);

        // Wire up the RecyclerView with the read-only EntrantCommentAdapter
        RecyclerView rv = view.findViewById(R.id.rvViewComments);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new EntrantCommentAdapter(filteredList));

        // Close button
        view.findViewById(R.id.btnViewCommentsClose)
                .setOnClickListener(v -> dismiss());
    }
}
