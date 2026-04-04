package com.example.codebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying an organizer's comments on their own event.
 *
 * <p>Each item shows the author name, comment body, and a formatted timestamp.
 * A delete button is visible on every row, allowing the organizer to remove any
 * comment (including those posted by entrants, per US 02.08.01).
 *
 * <p>Deletion is handled via the {@link OnDeleteClickListener} callback so that
 * the hosting activity ({@link EventDetailActivity}) can perform the Firestore
 * delete and refresh the list.
 *
 * <p>Comments are ordered chronologically (oldest first) as returned by the
 * Firestore query in {@link EventDetailActivity #loadComments()}.
 *
 * @see Comment
 * @see EventDetailActivity
 */
public class OrganizerCommentAdapter
        extends RecyclerView.Adapter<OrganizerCommentAdapter.CommentViewHolder> {

    /**
     * Callback interface for comment deletion.
     * Implemented by {@link EventDetailActivity} to perform the Firestore delete.
     */
    public interface OnDeleteClickListener {
        /**
         * Called when the delete button on a comment row is tapped.
         *
         * @param comment  The {@link Comment} to be deleted.
         * @param position The adapter position of the item (for optimistic UI removal).
         */
        void onDeleteClick(Comment comment, int position);
    }

    /** The live list of comments being displayed. */
    private final List<Comment> comments;

    /** Callback to the hosting activity for delete actions. */
    private final OnDeleteClickListener deleteListener;

    /** Formats {@link Comment#getTimestamp()} as a readable date-time string. */
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault());

    /**
     * Constructs the adapter.
     *
     * @param comments       Mutable list of comments to display; modified in-place on delete.
     * @param deleteListener Callback invoked when the user taps the delete button on a row.
     */
    public OrganizerCommentAdapter(List<Comment> comments,
                                   OnDeleteClickListener deleteListener) {
        this.comments       = comments;
        this.deleteListener = deleteListener;
    }

    /**
     * Inflates the {@code item_comment} layout for each row.
     *
     * @param parent   The parent RecyclerView.
     * @param viewType Unused — there is only one view type.
     * @return A new {@link CommentViewHolder} bound to the inflated view.
     */
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(v);
    }

    /**
     * Binds a {@link Comment} to the given {@link CommentViewHolder}.
     *
     * <p>The timestamp is formatted only when non-null; an empty string is used
     * as a fallback while Firestore's server-side {@code @ServerTimestamp} is
     * still pending.
     *
     * @param holder   The ViewHolder to populate.
     * @param position The adapter position.
     */
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);

        holder.tvAuthorName.setText(
                comment.getAuthorName() != null ? comment.getAuthorName() : "Organizer");
        holder.tvCommentText.setText(
                comment.getText() != null ? comment.getText() : "");
        holder.tvCommentTime.setText(
                comment.getTimestamp() != null
                        ? dateFormat.format(comment.getTimestamp())
                        : "");

        holder.btnDeleteComment.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos != RecyclerView.NO_ID && deleteListener != null) {
                deleteListener.onDeleteClick(comment, adapterPos);
            }
        });
    }

    /** @return The total number of comments in the list. */
    @Override
    public int getItemCount() { return comments.size(); }

    /**
     * ViewHolder for a single comment row.
     *
     * <p>Holds references to the author name, comment body, timestamp, and
     * the delete button.
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {

        /** Displays the comment author's name. */
        final TextView tvAuthorName;

        /** Displays the comment body text. */
        final TextView tvCommentText;

        /** Displays the formatted timestamp of the comment. */
        final TextView tvCommentTime;

        /** Tapping this removes the comment via {@link OnDeleteClickListener}. */
        final ImageButton btnDeleteComment;

        /**
         * Binds view references from the inflated {@code item_comment} layout.
         *
         * @param itemView The inflated comment row view.
         */
        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthorName     = itemView.findViewById(R.id.tvCommentAuthor);
            tvCommentText    = itemView.findViewById(R.id.tvCommentText);
            tvCommentTime    = itemView.findViewById(R.id.tvCommentTime);
            btnDeleteComment = itemView.findViewById(R.id.btnDeleteComment);
        }
    }
}