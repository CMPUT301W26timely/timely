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
 * RecyclerView adapter for displaying comments to an entrant — view-only.
 *
 * <p>Reuses the {@code item_comment} layout shared with {@link OrganizerCommentAdapter}
 * but hides the delete button on every row, satisfying US 01.08.02 ("view comments,
 * no delete allowed").</p>
 *
 * <p>Comments are ordered chronologically (oldest first) as returned by the
 * Firestore query in {@link EntrantEventDetailActivity#loadComments()}.</p>
 *
 * @see Comment
 * @see EntrantEventDetailActivity
 * @see EntrantViewCommentsFragment
 */
public class EntrantCommentAdapter
        extends RecyclerView.Adapter<EntrantCommentAdapter.CommentViewHolder> {

    /** The live list of comments being displayed. */
    private final List<Comment> comments;

    /** Formats {@link Comment#getTimestamp()} as a readable date-time string. */
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault());

    /**
     * Constructs the adapter.
     *
     * @param comments Mutable list of comments to display.
     */
    public EntrantCommentAdapter(List<Comment> comments) {
        this.comments = comments;
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
     * <p>The delete button is hidden (GONE) because entrants may only view comments,
     * not remove them (US 01.08.02).</p>
     *
     * <p>The avatar initial is set to the first character of the author name so
     * the circular avatar looks populated.</p>
     *
     * @param holder   The ViewHolder to populate.
     * @param position The adapter position.
     */
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);

        String name = comment.getAuthorName() != null ? comment.getAuthorName() : "?";
        holder.tvAuthorName.setText(name);
        holder.tvCommentText.setText(
                comment.getText() != null ? comment.getText() : "");
        holder.tvCommentTime.setText(
                comment.getTimestamp() != null
                        ? dateFormat.format(comment.getTimestamp())
                        : "");

        // Set avatar initial
        if (holder.tvCommentAvatar != null) {
            holder.tvCommentAvatar.setText(
                    name.length() > 0 ? String.valueOf(name.charAt(0)).toUpperCase() : "?");
        }

        // Entrants may NOT delete comments — hide the button entirely
        holder.btnDeleteComment.setVisibility(View.GONE);
    }

    /** @return The total number of comments in the list. */
    @Override
    public int getItemCount() { return comments.size(); }

    /**
     * ViewHolder for a single comment row (view-only variant).
     *
     * <p>Holds references to the avatar, author name, comment body, timestamp, and
     * the delete button (which is set to GONE for entrants).</p>
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {

        /** Circular avatar showing the first letter of the author's name. */
        final TextView tvCommentAvatar;

        /** Displays the comment author's name. */
        final TextView tvAuthorName;

        /** Displays the comment body text. */
        final TextView tvCommentText;

        /** Displays the formatted timestamp of the comment. */
        final TextView tvCommentTime;

        /**
         * Delete button reference — set to {@link View#GONE} for entrant view
         * (US 01.08.02: no delete allowed).
         */
        final ImageButton btnDeleteComment;

        /**
         * Binds view references from the inflated {@code item_comment} layout.
         *
         * @param itemView The inflated comment row view.
         */
        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommentAvatar  = itemView.findViewById(R.id.tvCommentAvatar);
            tvAuthorName     = itemView.findViewById(R.id.tvCommentAuthor);
            tvCommentText    = itemView.findViewById(R.id.tvCommentText);
            tvCommentTime    = itemView.findViewById(R.id.tvCommentTime);
            btnDeleteComment = itemView.findViewById(R.id.btnDeleteComment);
        }
    }
}
