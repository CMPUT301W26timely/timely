package com.example.codebase;

public class User {
    private String deviceId;
    private String role;

    public User() {
        // Required for Firestore
    }

    public User(String deviceId) {
        this.deviceId = deviceId;
        this.role = "entrant";
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
