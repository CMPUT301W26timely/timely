package com.example.codebase;

import java.util.Date;

/**
 * Admin-visible audit entry for organizer-sent notifications.
 */
public class NotificationLogEntry {

    private String eventId;
    private String eventTitle;
    private String senderDeviceId;
    private String targetGroup;
    private String title;
    private String message;
    private long recipientCount;
    private Date sentAt;

    /**
     * Default constructor required for Firestore document mapping.
     */
    public NotificationLogEntry() {
        // Required for Firestore.
    }

    /**
     * Returns the related event ID.
     *
     * @return Firestore event document ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the related event ID.
     *
     * @param eventId Firestore event document ID
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Returns the related event title.
     *
     * @return event title shown in admin logs
     */
    public String getEventTitle() {
        return eventTitle;
    }

    /**
     * Sets the related event title.
     *
     * @param eventTitle event title shown in admin logs
     */
    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    /**
     * Returns the sender device ID recorded for the notification batch.
     *
     * @return organizer or system sender device ID
     */
    public String getSenderDeviceId() {
        return senderDeviceId;
    }

    /**
     * Sets the sender device ID recorded for the notification batch.
     *
     * @param senderDeviceId organizer or system sender device ID
     */
    public void setSenderDeviceId(String senderDeviceId) {
        this.senderDeviceId = senderDeviceId;
    }

    /**
     * Returns the recipient-group label for this audit entry.
     *
     * @return target recipient group label
     */
    public String getTargetGroup() {
        return targetGroup;
    }

    /**
     * Sets the recipient-group label for this audit entry.
     *
     * @param targetGroup target recipient group label
     */
    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    /**
     * Returns the notification title that was sent.
     *
     * @return notification subject/title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the notification title that was sent.
     *
     * @param title notification subject/title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the notification body that was sent.
     *
     * @return notification message body
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the notification body that was sent.
     *
     * @param message notification message body
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the number of recipients represented by the log entry.
     *
     * @return recipient count
     */
    public long getRecipientCount() {
        return recipientCount;
    }

    /**
     * Sets the number of recipients represented by the log entry.
     *
     * @param recipientCount number of recipients included in the send
     */
    public void setRecipientCount(long recipientCount) {
        this.recipientCount = recipientCount;
    }

    /**
     * Returns when the notification batch was sent.
     *
     * @return send timestamp
     */
    public Date getSentAt() {
        return sentAt;
    }

    /**
     * Sets when the notification batch was sent.
     *
     * @param sentAt send timestamp
     */
    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }
}
