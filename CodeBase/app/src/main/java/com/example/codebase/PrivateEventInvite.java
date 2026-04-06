package com.example.codebase;

import java.util.Date;

/**
 * Represents a stored invitation for a private event.
 *
 * <p>Private-event invitations are stored as documents beneath
 * {@code events/{eventId}/privateInvites/{deviceId}} and track the invited
 * entrant, the related event, the current invitation status, and the times the
 * invite was issued and responded to.</p>
 */
public class PrivateEventInvite {

    /** Firestore sub-collection used to store private-event invitation documents. */
    public static final String SUBCOLLECTION = "privateInvites";

    /** Invitation status for entrants who still need to respond. */
    public static final String STATUS_PENDING = "pending";

    /** Invitation status for entrants who accepted the invite. */
    public static final String STATUS_ACCEPTED = "accepted";

    /** Invitation status for entrants who declined the invite. */
    public static final String STATUS_DECLINED = "declined";

    /** Device ID of the invited entrant. */
    private String deviceId;

    /** Event ID the invite belongs to. */
    private String eventId;

    /** Human-readable event title used by invitation surfaces. */
    private String eventTitle;

    /** Current invitation status. */
    private String status;

    /** Time the invitation was first created. */
    private Date invitedAt;

    /** Time the entrant responded, if any response has been recorded. */
    private Date respondedAt;

    /** Default constructor required for Firestore deserialization. */
    public PrivateEventInvite() {
    }

    /**
     * Creates a new private-event invitation model.
     *
     * @param deviceId invited entrant device ID
     * @param eventId related event ID
     * @param eventTitle event title shown to the entrant
     * @param status initial invitation status
     * @param invitedAt time the invite was created
     */
    public PrivateEventInvite(String deviceId, String eventId, String eventTitle, String status, Date invitedAt) {
        this.deviceId = deviceId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.status = status;
        this.invitedAt = invitedAt;
    }

    /** @return the invited entrant device ID */
    public String getDeviceId() {
        return deviceId;
    }

    /** @param deviceId the invited entrant device ID */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /** @return the related event ID */
    public String getEventId() {
        return eventId;
    }

    /** @param eventId the related event ID */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /** @return the event title shown in invitation UIs */
    public String getEventTitle() {
        return eventTitle;
    }

    /** @param eventTitle the event title shown in invitation UIs */
    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    /** @return the current invitation status */
    public String getStatus() {
        return status;
    }

    /** @param status the current invitation status */
    public void setStatus(String status) {
        this.status = status;
    }

    /** @return the time the invitation was created */
    public Date getInvitedAt() {
        return invitedAt;
    }

    /** @param invitedAt the time the invitation was created */
    public void setInvitedAt(Date invitedAt) {
        this.invitedAt = invitedAt;
    }

    /** @return the time the entrant responded, if any */
    public Date getRespondedAt() {
        return respondedAt;
    }

    /** @param respondedAt the time the entrant responded, if any */
    public void setRespondedAt(Date respondedAt) {
        this.respondedAt = respondedAt;
    }
}
