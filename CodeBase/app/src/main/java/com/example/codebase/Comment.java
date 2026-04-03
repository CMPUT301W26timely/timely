package com.example.codebase;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Represents a single comment posted on an event.
 *
 * <p>Comments are stored as documents inside the Firestore sub-collection:
 * {@code events/{eventId}/comments/{commentId}}
 *
 * <p>Each comment records the author's device ID, a display name (either the
 * organizer's profile name or a fallback), the text body, and a server-assigned
 * timestamp used for chronological ordering.
 *
 * <p>This class is compatible with Firestore's {@code toObject(Comment.class)}
 * deserialization — every field has a no-arg default and a matching setter.
 *
 * @see EventDetailActivity
 * @see OrganizerCommentAdapter
 */
public class Comment {

    /** Firestore document ID; injected manually after deserialization. Not stored in Firestore. */
    private String commentId;

    /** Device ID of the user who posted this comment. */
    private String authorDeviceId;

    /**
     * Display name shown alongside the comment.
     * Set to the organizer's profile name, or {@code "Organizer"} as a fallback.
     */
    private String authorName;

    /** The body text of the comment. */
    private String text;

    /**
     * Timestamp automatically assigned by Firestore when the document is written.
     * Used to sort comments in ascending chronological order.
     */
    @ServerTimestamp
    private Date timestamp;

    // ── Required no-arg constructor for Firestore deserialization ──────────────

    /** Default constructor required by Firestore. */
    public Comment() {}

    /**
     * Convenience constructor used when posting a new comment.
     *
     * @param authorDeviceId Device ID of the commenter.
     * @param authorName     Display name to show next to the comment.
     * @param text           The comment body text.
     */
    public Comment(String authorDeviceId, String authorName, String text) {
        this.authorDeviceId = authorDeviceId;
        this.authorName     = authorName;
        this.text           = text;
        // timestamp is set server-side via @ServerTimestamp
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return The local comment ID (not persisted in Firestore). */
    public String getCommentId()     { return commentId; }

    /** @return The device ID of the comment author. */
    public String getAuthorDeviceId() { return authorDeviceId; }

    /** @return The display name shown with this comment. */
    public String getAuthorName()    { return authorName; }

    /** @return The text body of this comment. */
    public String getText()          { return text; }

    /** @return The server timestamp when this comment was written. */
    public Date getTimestamp()       { return timestamp; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** @param commentId The Firestore document ID to attach after deserialization. */
    public void setCommentId(String commentId)         { this.commentId = commentId; }

    /** @param authorDeviceId The device ID of the comment author. */
    public void setAuthorDeviceId(String authorDeviceId) { this.authorDeviceId = authorDeviceId; }

    /** @param authorName The display name shown with this comment. */
    public void setAuthorName(String authorName)       { this.authorName = authorName; }

    /** @param text The body text of this comment. */
    public void setText(String text)                   { this.text = text; }

    /** @param timestamp The Firestore server timestamp for this comment. */
    public void setTimestamp(Date timestamp)           { this.timestamp = timestamp; }
}