package com.example.codebase;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DialogFragment} that displays a filtered subset of comments for an event.
 *
 * <p>Used by {@link EventDetailActivity} to implement US 02.08.02 "View Entrant Comments"
 * and "View My Comments" buttons. Filtering is done client-side from the list passed in
 * via {@link #newInstance}, so no second Firestore round-trip is needed.
 *
 * @see EventDetailActivity
 * @see Comment
 * @see OrganizerCommentAdapter
 */
public class ViewCommentsFragment extends DialogFragment
        implements OrganizerCommentAdapter.OnDeleteClickListener {

    // ── Constants ──────────────────────────────────────────────────────────────

    /** Show only comments where {@link Comment#isOrganizer()} is {@code true}. */
    public static final int MODE_ORGANIZER = 0;

    /** Show only comments where {@link Comment#isOrganizer()} is {@code false}. */
    public static final int MODE_ENTRANT = 1;

    private static final String ARG_EVENT_ID = "eventId";
    private static final String ARG_MODE = "mode";

    // ── Instance state ─────────────────────────────────────────────────────────

    /** The Firestore event document ID. */
    private String eventId;

    /** Which comments to show: {@link #MODE_ORGANIZER} or {@link #MODE_ENTRANT}. */
    private int mode;

    /** The filtered comment list shown in the RecyclerView. */
    private final List<Comment> filteredList = new ArrayList<>();

    /** Adapter bound to {@link #filteredList}. */
    private OrganizerCommentAdapter adapter;

    /** Shown when {@link #filteredList} is empty. */
    private TextView tvEmpty;

    // ── Factory ────────────────────────────────────────────────────────────────

    /**
     * Creates a new instance of this fragment.
     *
     * @param eventId     The Firestore event document ID.
     * @param allComments The full unfiltered comment list from the host activity.
     * @param mode        {@link #MODE_ORGANIZER} or {@link #MODE_ENTRANT}.
     * @return A configured {@link ViewCommentsFragment}.
     */
    public static ViewCommentsFragment newInstance(
            String eventId,
            List<Comment> allComments,
            int mode) {

        ViewCommentsFragment f = new ViewCommentsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putInt(ARG_MODE, mode);
        f.setArguments(args);

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
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_MinWidth);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            mode = getArguments().getInt(ARG_MODE, MODE_ENTRANT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tvViewCommentsTitle);
        tvTitle.setText(mode == MODE_ORGANIZER
                ? getString(R.string.comments_my_title)
                : getString(R.string.comments_entrant_title));

        tvEmpty = view.findViewById(R.id.tvViewCommentsEmpty);
        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);

        RecyclerView rv = view.findViewById(R.id.rvViewComments);
        adapter = new OrganizerCommentAdapter(filteredList, this);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        view.findViewById(R.id.btnViewCommentsClose).setOnClickListener(v -> dismiss());
    }

    // ── OrganizerCommentAdapter.OnDeleteClickListener ──────────────────────────

    @Override
    public void onDeleteClick(Comment comment, int position) {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .document(comment.getCommentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    if (position < filteredList.size()) {
                        filteredList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, filteredList.size());
                    }
                    if (tvEmpty != null) {
                        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                getString(R.string.comments_delete_failed),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}