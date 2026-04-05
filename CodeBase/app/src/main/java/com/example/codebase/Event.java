package com.example.codebase;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * Domain model representing a lottery event in the Timely system.
 *
 * <p>An event progresses through several lifecycle stages driven by its date fields
 * ({@link #registrationOpen}, {@link #registrationDeadline}, {@link #drawDate},
 * {@link #startDate}, {@link #endDate}) and the state of its entrant lists:
 * {@link #waitingList}, {@link #selectedEntrants}, {@link #enrolledEntrants},
 * {@link #cancelledEntrants}, and {@link #registeredEntrants}.
 *
 * <p>The class implements {@link Serializable} so that instances can be passed between
 * Android components via {@link android.os.Bundle} or {@link android.content.Intent} extras.
 *
 * <p><b>Note on ID fields:</b> {@link #id} and {@link #eventId} are kept in sync —
 * setting either via {@link #setId(String)} or {@link #setEventId(String)} updates both.
 */
public class Event implements Serializable {

    /**
     * Primary Firestore document ID. Kept in sync with {@link #eventId}.
     *
     * @see #setId(String)
     */
    private String id;

    /** Optional event poster, storing a Base64-encoded image and associated metadata. */
    private EventPoster poster;

    /** Human-readable event title. */
    private String title;

    /** Detailed description of the event. */
    private String description;

    /** Date and time when the event begins. */
    private Date startDate;

    /** Date and time when the event ends. */
    private Date endDate;

    /** Date and time from which entrants may join the waiting list. */
    private Date registrationOpen;

    /** Deadline after which no new waiting-list registrations are accepted. */
    private Date registrationDeadline;

    /**
     * Maximum number of entrants allowed on the waiting list.
     * {@code 0} indicates no cap.
     */
    private int waitlistCap;

    /** Participation fee for the event. {@code 0.0} indicates a free event. */
    private float price;

    /** Device ID of the organizer who created this event. */
    private String organizerDeviceId;

    /** Physical or descriptive location of the event. */
    private String location;

    /** Date and time when the lottery draw is performed. */
    private Date drawDate;

    /** URL pointing to the remotely hosted poster image (alternative to {@link #poster}). */
    private String posterUrl;

    /**
     * Maximum number of entrants who may be enrolled in the event.
     * {@code null} indicates no cap.
     */
    private Long maxCapacity;

    /**
     * Number of entrants to select during the lottery draw.
     * {@code null} indicates the draw picks all waiting-list entrants.
     */
    private Long winnersCount;

    /** Device IDs of entrants who have joined the waiting list. */
    private ArrayList<String> waitingList = new ArrayList<>();

    /** Device IDs of entrants chosen by the lottery draw but not yet responded. */
    private ArrayList<String> selectedEntrants = new ArrayList<>();

    /** Device IDs of entrants who accepted their lottery invitation. */
    private ArrayList<String> enrolledEntrants = new ArrayList<>();

    /**
     * Firestore-persisted status string (e.g. {@code "active"}, {@code "ended"}).
     * UI components derive display status from date fields instead; see
     * {@link OrganizerEventAdapter}.
     */
    private String status;

    /**
     * Whether geolocation verification is required when joining the waiting list.
     * When {@code true}, the entrant's location is recorded at registration time.
     */
    private boolean geoEnabled;

    /**
     * Whether this event is private.
     *
     * <p>Private events (US 02.01.02):
     * <ul>
     *   <li>Are not shown in the public event listing.</li>
     *   <li>Do not generate a promotional QR code.</li>
     *   <li>Accept entrants only via organizer invitation (US 02.01.03).</li>
     * </ul>
     */
    private boolean isPrivate;

    /** Device IDs of entrants who declined their invitation or were removed. */
    private ArrayList<String> cancelledEntrants = new ArrayList<>();

    /**
     * Device IDs of entrants who have ever registered for this event.
     *
     * <p>Unlike {@link #waitingList}, this list is append-only from the entrant's
     * perspective and is used to build a persistent registration history even
     * after the live entrant-state arrays change.</p>
     */
    private ArrayList<String> registeredEntrants = new ArrayList<>();

    /**
     * Device IDs of entrants assigned as co-organizers for this event.
     * Co-organizers are excluded from the entrant pool (waitingList, selectedEntrants, etc.)
     * for this event.
     */
    private ArrayList<String> coOrganizers = new ArrayList<>();

    /**
     * Secondary Firestore document ID field. Kept in sync with {@link #id}.
     *
     * @see #setEventId(String)
     */
    private String eventId;

    /**
     * Returns the secondary event ID field.
     *
     * @return The Firestore document ID stored in {@link #eventId}.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the secondary event ID and synchronises {@link #id} to the same value.
     *
     * @param eventId The Firestore document ID to assign.
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
        this.id = eventId;
    }

    /**
     * Returns whether geolocation verification is enabled for this event.
     *
     * @return {@code true} if entrants must provide their location when registering.
     */
    public boolean isGeoEnabled() {
        return geoEnabled;
    }

    /**
     * Sets whether geolocation verification is required for this event.
     *
     * @param geoEnabled {@code true} to require location verification on registration.
     */
    public void setGeoEnabled(boolean geoEnabled) {
        this.geoEnabled = geoEnabled;
    }

    /**
     * Returns whether this event is private.
     *
     * <p>Private events are not visible in the public event listing and do not
     * generate a promotional QR code (US 02.01.02). Entrants are invited directly
     * by the organizer (US 02.01.03).</p>
     *
     * @return {@code true} if the event is private; {@code false} if it is public.
     */
    public boolean isPrivate() {
        return isPrivate;
    }

    /**
     * Sets whether this event is private.
     *
     * @param isPrivate {@code true} to make the event private; {@code false} for public.
     */
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    /**
     * Returns the raw Firestore status string for this event.
     *
     * @return The status string (e.g. {@code "active"}), or {@code null} if not set.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the raw Firestore status string for this event.
     *
     * @param status The status value to store.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the primary Firestore document ID.
     *
     * @return The event's document ID, or {@code null} if not yet assigned.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the primary event ID and synchronises {@link #eventId} to the same value.
     *
     * @param id The Firestore document ID to assign.
     */
    public void setId(String id) {
        this.id = id;
        this.eventId = id;
    }

    /**
     * Returns the device ID of the organizer who created this event.
     *
     * @return The organizer's device ID, or {@code null} if not set.
     */
    public String getOrganizerDeviceId() {
        return organizerDeviceId;
    }

    /**
     * Sets the device ID of the event's organizer.
     *
     * @param organizerDeviceId The organizer's device ID.
     */
    public void setOrganizerDeviceId(String organizerDeviceId) {
        this.organizerDeviceId = organizerDeviceId;
    }

    /**
     * Returns the event location.
     *
     * @return The location string, or {@code null} if not set.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the event location.
     *
     * @param location A physical address or descriptive venue name.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns the date and time of the lottery draw.
     *
     * @return The draw date, or {@code null} if not yet scheduled.
     */
    public Date getDrawDate() {
        return drawDate;
    }

    /**
     * Sets the date and time of the lottery draw.
     *
     * @param drawDate The draw date to assign.
     */
    public void setDrawDate(Date drawDate) {
        this.drawDate = drawDate;
    }

    /**
     * Returns the URL of the remotely hosted poster image.
     *
     * @return The poster URL, or {@code null} if not set.
     */
    public String getPosterUrl() {
        return posterUrl;
    }

    /**
     * Sets the URL of the remotely hosted poster image.
     *
     * @param posterUrl The poster URL to assign.
     */
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    /**
     * Returns the maximum enrolment capacity for this event.
     *
     * @return The cap as a {@link Long}, or {@code null} if uncapped.
     */
    public Long getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Sets the maximum enrolment capacity for this event.
     *
     * @param maxCapacity The capacity cap, or {@code null} for no limit.
     */
    public void setMaxCapacity(Long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    /**
     * Returns the number of entrants to select during the lottery draw.
     *
     * @return The winner count as a {@link Long}, or {@code null} to select all.
     */
    public Long getWinnersCount() {
        return winnersCount;
    }

    /**
     * Sets the number of entrants to select during the lottery draw.
     *
     * @param winnersCount The winner count, or {@code null} to select all waiting-list entrants.
     */
    public void setWinnersCount(Long winnersCount) {
        this.winnersCount = winnersCount;
    }

    /**
     * Returns the list of device IDs for entrants on the waiting list.
     *
     * @return A non-null {@link ArrayList} of device ID strings.
     */
    public ArrayList<String> getWaitingList() {
        return waitingList;
    }

    /**
     * Replaces the waiting-list entrant collection.
     *
     * @param waitingList The new list of device IDs.
     */
    public void setWaitingList(ArrayList<String> waitingList) {
        this.waitingList = waitingList;
    }

    /**
     * Returns the list of device IDs for lottery-selected entrants awaiting response.
     *
     * @return A non-null {@link ArrayList} of device ID strings.
     */
    public ArrayList<String> getSelectedEntrants() {
        return selectedEntrants;
    }

    /**
     * Replaces the selected-entrants collection.
     *
     * @param selectedEntrants The new list of device IDs.
     */
    public void setSelectedEntrants(ArrayList<String> selectedEntrants) {
        this.selectedEntrants = selectedEntrants;
    }

    /**
     * Returns the list of device IDs for entrants who accepted their invitation.
     *
     * @return A non-null {@link ArrayList} of device ID strings.
     */
    public ArrayList<String> getEnrolledEntrants() {
        return enrolledEntrants;
    }

    /**
     * Replaces the enrolled-entrants collection.
     *
     * @param enrolledEntrants The new list of device IDs.
     */
    public void setEnrolledEntrants(ArrayList<String> enrolledEntrants) {
        this.enrolledEntrants = enrolledEntrants;
    }

    /**
     * Returns the list of device IDs for entrants who declined or were cancelled.
     *
     * @return A non-null {@link ArrayList} of device ID strings.
     */
    public ArrayList<String> getCancelledEntrants() {
        return cancelledEntrants;
    }

    /**
     * Replaces the cancelled-entrants collection.
     *
     * @param cancelledEntrants The new list of device IDs.
     */
    public void setCancelledEntrants(ArrayList<String> cancelledEntrants) {
        this.cancelledEntrants = cancelledEntrants;
    }

    /**
     * Returns the list of device IDs for entrants who have ever registered.
     *
     * @return A non-null {@link ArrayList} of device ID strings.
     */
    public ArrayList<String> getRegisteredEntrants() {
        return registeredEntrants;
    }

    /**
     * Replaces the registered-entrants history collection.
     *
     * @param registeredEntrants The new list of device IDs.
     */
    public void setRegisteredEntrants(ArrayList<String> registeredEntrants) {
        this.registeredEntrants = registeredEntrants;
    }

    /**
     * Returns the event poster object containing the Base64-encoded image.
     *
     * @return The {@link EventPoster}, or {@code null} if no poster has been set.
     */
    public EventPoster getPoster() {
        return poster;
    }

    /**
     * Sets the event poster.
     *
     * @param poster The {@link EventPoster} to assign.
     */
    public void setPoster(EventPoster poster) {
        this.poster = poster;
    }

    /**
     * Returns the event title.
     *
     * @return The title string, or {@code null} if not set.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the event title.
     *
     * @param title The human-readable event title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the event description.
     *
     * @return The description string, or {@code null} if not set.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the event description.
     *
     * @param description The detailed event description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the event start date and time.
     *
     * @return The start {@link Date}, or {@code null} if not set.
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets the event start date and time.
     *
     * @param startDate The start {@link Date} to assign.
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Returns the event end date and time.
     *
     * @return The end {@link Date}, or {@code null} if not set.
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Sets the event end date and time.
     *
     * @param endDate The end {@link Date} to assign.
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Returns the date from which entrants may join the waiting list.
     *
     * @return The registration open {@link Date}, or {@code null} if not set.
     */
    public Date getRegistrationOpen() {
        return registrationOpen;
    }

    /**
     * Sets the date from which entrants may join the waiting list.
     *
     * @param registrationOpen The registration open {@link Date} to assign.
     */
    public void setRegistrationOpen(Date registrationOpen) {
        this.registrationOpen = registrationOpen;
    }

    /**
     * Returns the registration deadline after which no new entrants may join.
     *
     * @return The deadline {@link Date}, or {@code null} if not set.
     */
    public Date getRegistrationDeadline() {
        return registrationDeadline;
    }

    /**
     * Sets the registration deadline.
     *
     * @param registrationDeadline The deadline {@link Date} to assign.
     */
    public void setRegistrationDeadline(Date registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    /**
     * Returns the maximum number of entrants permitted on the waiting list.
     *
     * @return The cap as a non-negative {@code int}; {@code 0} means no cap.
     */
    public int getWaitlistCap() {
        return waitlistCap;
    }

    /**
     * Sets the maximum number of entrants permitted on the waiting list.
     *
     * @param waitlistCap A non-negative integer; {@code 0} indicates no cap.
     */
    public void setWaitlistCap(int waitlistCap) {
        this.waitlistCap = waitlistCap;
    }

    /**
     * Returns the participation fee for the event.
     *
     * @return The price as a {@code float}; {@code 0.0} indicates a free event.
     */
    public float getPrice() {
        return price;
    }

    /**
     * Sets the participation fee for the event.
     *
     * @param price A non-negative value; use {@code 0.0} for a free event.
     */
    public void setPrice(float price) {
        this.price = price;
    }

    /**
     * Returns the list of device IDs for entrants assigned as co-organizers.
     *
     * @return A non-null {@link ArrayList} of device ID strings.
     */
    public ArrayList<String> getCoOrganizers() {
        return coOrganizers;
    }

    /**
     * Replaces the co-organizers collection.
     * Co-organizers are excluded from the entrant pool for this event.
     *
     * @param coOrganizers The new list of device IDs.
     */
    public void setCoOrganizers(ArrayList<String> coOrganizers) {
        this.coOrganizers = coOrganizers != null ? coOrganizers : new ArrayList<>();
    }
}