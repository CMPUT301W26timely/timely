package com.example.codebase;

/**
 * Represents a lottery notification stored in Firestore.
 */
public class AppNotification {
    private String notificationId;
    private String userId;
    private String eventId;
    private String title;
    private String message;
    private String status;   // "Selected" or "Not Selected"
    private boolean read;

    public AppNotification() {
        // Required for Firestore
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public boolean isRead() {
        return read;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}