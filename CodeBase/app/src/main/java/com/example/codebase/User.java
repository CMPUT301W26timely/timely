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
     * Gets the current role of the user (e.g., "entrant").
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
     * @return The users name.
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
     * @return The users email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email for the user.
     * @param email The name to assign to the user.
     */
    public void setEmail(String email) {
        this.email = email;
    }
}