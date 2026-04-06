package com.example.codebase;

/**
 * Represents a user in the Timely Event Lottery System.
 * Users are identified primarily by their unique device ID and have a role
 * that determines their permissions within the app.
 */
public class User {
    private String deviceId;
    private String role;
    private String name;
    private String email;
    private String phoneNumber;
    private boolean notificationsEnabled;
    private boolean organizerPrivilegesRevoked;
    private String organizerRevocationMessage;
    private java.util.Date organizerRevokedAt;

    /**
     * Default constructor required for Firebase Firestore serialization.
     */
    public User() {
        // Required for Firestore
    }

    /**
     * Constructs a new User with the specified device ID.
     * The role defaults to "entrant".
     *
     * @param deviceId The unique identifier for the user's device.
     */
    public User(String deviceId) {
        this.deviceId = deviceId;
        this.role = "entrant";
        this.name = "";
        this.email = "";
        this.phoneNumber = "";
        this.notificationsEnabled = true;
        this.organizerPrivilegesRevoked = false;
        this.organizerRevocationMessage = "";
        this.organizerRevokedAt = null;
    }

    /**
     * Gets the unique device identifier for this user.
     * @return The device ID string.
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the unique device identifier for this user.
     * @param deviceId The device ID string to set.
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Gets the current role of the user.
     * @return The user's role.
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the role for the user.
     * @param role The role to assign to the user.
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Gets the name for the user.
     * @return The user's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name for the user.
     * @param name The name to assign to the user.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the email for the user.
     * @return The user's email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email for the user.
     * @param email The email to assign to the user.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the optional phone number for the user.
     * @return The user's phone number.
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the optional phone number for the user.
     * @param phoneNumber The phone number to assign to the user.
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns whether organizer/admin notifications are enabled for this user.
     *
     * @return {@code true} when notifications are allowed, {@code false} when opted out
     */
    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    /**
     * Sets whether organizer/admin notifications are enabled for this user.
     *
     * @param notificationsEnabled {@code true} to allow notifications, {@code false} to opt out
     */
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public boolean isOrganizerPrivilegesRevoked() {
        return organizerPrivilegesRevoked;
    }

    public void setOrganizerPrivilegesRevoked(boolean organizerPrivilegesRevoked) {
        this.organizerPrivilegesRevoked = organizerPrivilegesRevoked;
    }

    public String getOrganizerRevocationMessage() {
        return organizerRevocationMessage;
    }

    public void setOrganizerRevocationMessage(String organizerRevocationMessage) {
        this.organizerRevocationMessage = organizerRevocationMessage;
    }

    public java.util.Date getOrganizerRevokedAt() {
        return organizerRevokedAt;
    }

    public void setOrganizerRevokedAt(java.util.Date organizerRevokedAt) {
        this.organizerRevokedAt = organizerRevokedAt;
    }
}
