package com.example.codebase;

/**
 * Represents an event that entrants can browse and join.
 */
public class Event {
    private String eventId;
    private String name;
    private String date;
    private String location;
    private String description;
    private boolean active;

    public Event() {
        // Required for Firestore
    }

    public Event(String eventId, String name, String date, String location, String description, boolean active) {
        this.eventId = eventId;
        this.name = name;
        this.date = date;
        this.location = location;
        this.description = description;
        this.active = active;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}