package com.example.codebase;

/**
 * Represents a lottery notification stored in Firestore.
 *
 * Each notification is written by the organizer and read by the entrant.
 * Documents are stored at:
 * {@code notifications/{deviceId}/messages/{autoId}}
 *
 * The {@code read} flag is set to {@code false} on creation and updated
 * to {@code true} once the entrant has viewed the notification.
 */
public class AppNotification {

    /** Unique ID of this notification document (Firestore auto-generated). */
    private String notificationId;

    /** Device ID of the entrant this notification was sent to. */
    private String userId;

    /** Firestore document ID of the event this notification relates to. */
    private String eventId;

    /** Display title of the event this notification relates to. */
    private String eventTitle;

    /** Subject line of the notification shown in the notification header. */
    private String title;

    /** Body text of the notification shown to the entrant. */
    private String message;

    /**
     * Outcome status of the entrant in the lottery.
     * Expected values: {@code "Selected"}, {@code "Cancelled"},
     * or {@code "Waiting List"}.
     */
    private String status;

    /**
     * Recipient group this notification was sent to.
     * Expected values: {@code "selectedEntrants"}, {@code "cancelledEntrants"},
     * {@code "waitingList"}, or {@code "privateInvite"}.
     */
    private String type;

    /**
     * Whether the entrant has read this notification.
     * {@code false} on creation, updated to {@code true} after viewing.
     */
    private boolean read;

    /**
     * Required no-argument constructor for Firestore deserialization.
     * Do not use directly — Firestore calls this when mapping documents
     * to {@link AppNotification} objects via {@code toObject()}.
     */
    public AppNotification() {
        // Required for Firestore
    }

    /**
     * Returns the unique notification document ID.
     *
     * @return the notification ID, or {@code null} if not set
     */
    public String getNotificationId() {
        return notificationId;
    }

    /**
     * Sets the unique notification document ID.
     *
     * @param notificationId the Firestore document ID for this notification
     */
    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    /**
     * Returns the device ID of the entrant this notification was sent to.
     *
     * @return the user/device ID, or {@code null} if not set
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the device ID of the entrant this notification was sent to.
     *
     * @param userId the entrant's device ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Returns the Firestore document ID of the related event.
     *
     * @return the event ID, or {@code null} if not set
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the Firestore document ID of the related event.
     *
     * @param eventId the event document ID
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Returns the display title of the related event.
     *
     * @return the event title, or {@code null} if not set
     */
    public String getEventTitle() {
        return eventTitle;
    }

    /**
     * Sets the display title of the related event.
     *
     * @param eventTitle the event title to display
     */
    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    /**
     * Returns the subject line of the notification.
     *
     * @return the notification title/subject, or {@code null} if not set
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the subject line of the notification.
     *
     * @param title the notification subject line
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the body text of the notification.
     *
     * @return the notification message body, or {@code null} if not set
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the body text of the notification.
     *
     * @param message the notification message body
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the lottery outcome status for this notification.
     * Expected values: {@code "Selected"}, {@code "Cancelled"},
     * or {@code "Waiting List"}.
     *
     * @return the status string, or {@code null} if not set
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the lottery outcome status for this notification.
     *
     * @param status the outcome status e.g. {@code "Selected"}
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the recipient group type this notification was sent to.
     * Expected values: {@code "selectedEntrants"}, {@code "cancelledEntrants"},
     * {@code "waitingList"}, or {@code "privateInvite"}.
     *
     * @return the type string, or {@code null} if not set
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the recipient group type this notification was sent to.
     *
     * @param type the recipient group type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns whether the entrant has read this notification.
     *
     * @return {@code true} if the notification has been read, {@code false} otherwise
     */
    public boolean isRead() {
        return read;
    }

    /**
     * Sets the read state of this notification.
     * Should be set to {@code true} once the entrant has viewed it.
     *
     * @param read {@code true} to mark as read, {@code false} to mark as unread
     */
    public void setRead(boolean read) {
        this.read = read;
    }
}
