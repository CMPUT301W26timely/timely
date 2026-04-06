package com.example.codebase;

import java.util.Date;

/**
 * Firestore-backed record of where an entrant joined an event waitlist from.
 */
public class EntrantLocation {

    private String deviceId;
    private String entrantName;
    private double latitude;
    private double longitude;
    private Date capturedAt;

    public EntrantLocation() {
        // Required for Firestore deserialization.
    }

    public EntrantLocation(String deviceId,
                           String entrantName,
                           double latitude,
                           double longitude,
                           Date capturedAt) {
        this.deviceId = deviceId;
        this.entrantName = entrantName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capturedAt = capturedAt;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getEntrantName() {
        return entrantName;
    }

    public void setEntrantName(String entrantName) {
        this.entrantName = entrantName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Date getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Date capturedAt) {
        this.capturedAt = capturedAt;
    }

    public boolean hasValidCoordinates() {
        return latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0;
    }
}
