package com.example.codebase;

import java.io.Serializable;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an event in the system
 */
public class Event implements Serializable {
    private String id;
    private EventPoster poster;
    private String title;
    private String description;
    private Date startDate;
    private Date endDate;
    private Date registrationOpen;
    private Date registrationDeadline;
    private int waitlistCap;
    private float price;
    private String organizerDeviceId;
    private String location;
    private Date drawDate;
    private Long maxCapacity;
    private Long winnersCount;
    private ArrayList<Entrant> waitingList;
    private ArrayList<Entrant> selectedEntrants;
    private ArrayList<Entrant> enrolledEntrants;
    private String status;
    private boolean geoEnabled;
    private ArrayList<Entrant>  cancelledEntrants;

    /**
     * Returns whether geolocation is enabled for this event.
     *
     * @return {@code true} if geolocation is enabled, {@code false} otherwise
     */
    public boolean isGeoEnabled() {
        return geoEnabled;
    }

    /**
     * Sets whether geolocation is enabled for this event.
     *
     * @param geoEnabled {@code true} to enable geolocation, {@code false} to disable it
     */
    public void setGeoEnabled(boolean geoEnabled) {
        this.geoEnabled = geoEnabled;
    }

    /**
     * Returns the current status of the event
     *
     * @return the event status
     */
    public String getStatus() {
        return status;
    }


    /**
     * Sets the current status of the event
     *
     * @param status the status string to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the identifier of this event
     *
     * @return the event ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the identifier of this event
     *
     * @param id the event ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the device ID of the organizer who created this event.
     *
     * @return the organizer's device ID
     */
    public String getOrganizerDeviceId() {
        return organizerDeviceId;
    }

    /**
     * Sets the device ID of the organizer who created this event.
     *
     * @param organizerDeviceId the organizer's device ID to set
     */
    public void setOrganizerDeviceId(String organizerDeviceId) {
        this.organizerDeviceId = organizerDeviceId;
    }

    /**
     * Returns the location where the event will be
     *
     * @return the event location
     */
    public String getLocation() {
        return location;
    }


    /**
     * Sets the location where the event will be
     *
     * @param location the event location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns the date on which the lottery draw will take place
     *
     * @return the draw date
     */
    public Date getDrawDate() {
        return drawDate;
    }

    /**
     * Sets the date on which the lottery draw will take place.
     *
     * @param drawDate the draw date to set
     */
    public void setDrawDate(Date drawDate) {
        this.drawDate = drawDate;
    }

    /**
     * Returns the maximum number of entrants allowed for this event.
     *
     * @return the maximum capacity
     */
    public Long getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Sets the maximum number of entrants allowed for this event.
     *
     * @param maxCapacity the maximum capacity to set
     */
    public void setMaxCapacity(Long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    /**
     * Returns the number of winners to be selected during the draw
     *
     * @return the winners count
     */
    public Long getWinnersCount() {
        return winnersCount;
    }

    /**
     * Sets the number of winners to be selected during the draw.
     *
     * @param winnersCount the winners count to set
     */
    public void setWinnersCount(Long winnersCount) {
        this.winnersCount = winnersCount;
    }

    /**
     * Returns the list of entrants currently on the waitlist.
     *
     * @return list of {@link Entrant} objects
     */
    public ArrayList<Entrant> getWaitingList() {
        return waitingList;
    }

    /**
     * Sets the list of entrants on the waitlist.
     *
     * @param waitingList the list of {@link Entrant} objects to set as the waiting list
     */
    public void setWaitingList(ArrayList<Entrant> waitingList) {
        this.waitingList = waitingList;
    }

    /**
     * Returns the list of entrants who have been selected in the draw
     *
     * @return the list of selected {@link Entrant} objects
     */
    public ArrayList<Entrant> getSelectedEntrants() {
        return selectedEntrants;
    }

    /**
     * Sets the list of entrants who have been selected in the draw.
     *
     * @param selectedEntrants the list of selected {@link Entrant} objects to set
     */
    public void setSelectedEntrants(ArrayList<Entrant> selectedEntrants) {
        this.selectedEntrants = selectedEntrants;
    }

    /**
     * Returns the list of entrants who have confirmed their enrollment in the event.
     *
     * @return the list of enrolled {@link Entrant} objects
     */
    public ArrayList<Entrant> getEnrolledEntrants() {
        return enrolledEntrants;
    }

    /**
     * Sets the list of entrants who have confirmed their enrollment in the event
     *
     * @param enrolledEntrants the list of enrolled {@link Entrant} objects to set
     */
    public void setEnrolledEntrants(ArrayList<Entrant> enrolledEntrants) {
        this.enrolledEntrants = enrolledEntrants;
    }

    /**
     * Returns the list of entrants who have canceled their participation.
     *
     * @return the list of canceled {@link Entrant} objects
     */
    public ArrayList<Entrant> getCancelledEntrants() {
        return cancelledEntrants;
    }

    /**
     * Sets the list of entrants who have canceled their participation.
     *
     * @param cancelledEntrants the list of cancelled {@link Entrant} objects to set
     */
    public void setCancelledEntrants(ArrayList<Entrant>  cancelledEntrants) {
        this.cancelledEntrants = cancelledEntrants;
    }

    /**
     * Returns the {@link EventPoster} for this event.
     *
     * @return the event poster
     */
    public EventPoster getPoster() {
        return poster;
    }

    /**
     * Sets the {@link EventPoster} for this event.
     *
     * @param poster the event poster to set
     */
    public void setPoster(EventPoster poster) {
        this.poster = poster;
    }

    /**
     * Returns the title of the event.
     *
     * @return the event title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the event.
     *
     * @param title the event title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the description of the event.
     *
     * @return the event description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the event.
     *
     * @param description the event description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the start date and time of the event.
     *
     * @return the event start date
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date and time of the event.
     *
     * @param startDate the event start date to set
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Returns the end date and time of the event.
     *
     * @return the event end date
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date and time of the event.
     *
     * @param endDate the event end date to set
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Returns the date and time when registration opens for this event.
     *
     * @return the registration open date
     */
    public Date getRegistrationOpen() {
        return registrationOpen;
    }

    /**
     * Sets the date and time when registration opens for this event.
     *
     * @param registrationOpen the registration open date to set
     */
    public void setRegistrationOpen(Date registrationOpen) {
        this.registrationOpen = registrationOpen;
    }

    /**
     * Returns the deadline by which entrants must register for the event.
     *
     * @return the registration deadline date
     */
    public Date getRegistrationDeadline() {
        return registrationDeadline;
    }

    /**
     * Sets the deadline by which entrants must register for the event.
     *
     * @param registrationDeadline the registration deadline date to set
     */
    public void setRegistrationDeadline(Date registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    /**
     * Returns the maximum number of entrants allowed on the waitlist.
     *
     * @return the waitlist capacity cap
     */
    public int getWaitlistCap() {
        return waitlistCap;
    }

    /**
     * Sets the maximum number of entrants allowed on the waitlist.
     *
     * @param waitlistCap the waitlist capacity cap to set
     */
    public void setWaitlistCap(int waitlistCap) {
        this.waitlistCap = waitlistCap;
    }

    /**
     * Returns the registration price for this event.
     *
     * @return the event price
     */
    public float getPrice() {
        return price;
    }

    /**
     * Sets the registration price for this event.
     *
     * @param price the event price to set
     */
    public void setPrice(float price) {
        this.price = price;
    }
}


