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

    /**
     * Default constructor required for Firestore document mapping.
     */
    public EntrantLocation() {
        // Required for Firestore deserialization.
    }

    /**
     * Creates a location record for an entrant waitlist join.
     *
     * @param deviceId entrant device ID
     * @param entrantName entrant display name
     * @param latitude recorded latitude
     * @param longitude recorded longitude
     * @param capturedAt when the location was captured
     */
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

    /**
     * Returns the entrant device ID.
     *
     * @return entrant device ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the entrant device ID.
     *
     * @param deviceId entrant device ID
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Returns the entrant display name shown on the map.
     *
     * @return entrant display name
     */
    public String getEntrantName() {
        return entrantName;
    }

    /**
     * Sets the entrant display name shown on the map.
     *
     * @param entrantName entrant display name
     */
    public void setEntrantName(String entrantName) {
        this.entrantName = entrantName;
    }

    /**
     * Returns the recorded latitude.
     *
     * @return latitude coordinate
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Sets the recorded latitude.
     *
     * @param latitude latitude coordinate
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Returns the recorded longitude.
     *
     * @return longitude coordinate
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Sets the recorded longitude.
     *
     * @param longitude longitude coordinate
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Returns when the location was captured.
     *
     * @return capture timestamp
     */
    public Date getCapturedAt() {
        return capturedAt;
    }

    /**
     * Sets when the location was captured.
     *
     * @param capturedAt capture timestamp
     */
    public void setCapturedAt(Date capturedAt) {
        this.capturedAt = capturedAt;
    }

    /**
     * Returns whether the stored latitude/longitude pair falls within valid bounds.
     *
     * @return {@code true} when both coordinates are valid Earth coordinates
     */
    public boolean hasValidCoordinates() {
        return latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0;
    }
}
