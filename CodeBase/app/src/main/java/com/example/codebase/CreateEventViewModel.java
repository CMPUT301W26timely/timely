package com.example.codebase;

import android.graphics.Bitmap;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel that holds all user-entered state across the four steps of the
 * create/edit event wizard ({@link CreateEventActivity}).
 *
 * <p>Because this class extends {@link ViewModel}, its instance survives
 * configuration changes (e.g. screen rotation) for the lifetime of the host
 * activity. All wizard step fragments obtain the same instance via
 * {@link androidx.lifecycle.ViewModelProvider}, allowing them to share state
 * without passing data through intent extras or fragment arguments.
 *
 * <p>Fields are grouped by wizard step:
 * <ul>
 *   <li><b>Step 0 – Basics:</b> {@link #posterBase64}, {@link #name},
 *       {@link #description}, {@link #location}, {@link #price}</li>
 *   <li><b>Step 1 – Schedule:</b> {@link #startDate}, {@link #endDate},
 *       {@link #registrationOpen}, {@link #registrationDeadline},
 *       {@link #capacity}</li>
 *   <li><b>Step 2 – Settings:</b> {@link #waitlistLimit}, {@link #geoRequired}</li>
 *   <li><b>Step 3 – QR Result:</b> {@link #generatedQr}, {@link #generatedEventId}</li>
 *   <li><b>Edit mode:</b> {@link #isEditMode}, {@link #editingEventId}</li>
 * </ul>
 *
 * @see CreateEventActivity
 * @see CreateEventFragment
 */
public class CreateEventViewModel extends ViewModel {

    // -------------------------------------------------------------------------
    // Step 0: Basics
    // -------------------------------------------------------------------------

    /**
     * Base64-encoded JPEG string of the event poster image chosen by the
     * organiser. Empty string ({@code ""}) indicates no poster has been selected.
     *
     * <p>The image is resized to a maximum of 800×800 px and compressed at 60%
     * JPEG quality before encoding, keeping the value within Firestore's 1 MB
     * document size limit.
     *
     * @see CreateEventFragment
     */
    public String posterBase64 = "";

    /**
     * Display name of the event. Required; must be non-blank before the event
     * can be published.
     */
    public String name = "";

    /**
     * Optional free-text description of the event shown to prospective
     * attendees.
     */
    public String description = "";

    /**
     * Optional venue or address string for the event.
     */
    public String location = "";

    /**
     * Ticket price in the organiser's local currency. {@code 0.0} indicates a
     * free event.
     */
    public double price = 0.0;

    // -------------------------------------------------------------------------
    // Step 1: Schedule
    // -------------------------------------------------------------------------

    /**
     * Event start date as a {@code yyyy-MM-dd} string entered by the organiser.
     * Empty string if not yet set. Parsed to a {@link com.google.firebase.Timestamp}
     * by {@code CreateEventActivity} before writing to Firestore.
     */
    public String startDate = "";

    /**
     * Event end date as a {@code yyyy-MM-dd} string. Empty string if not yet set.
     *
     * @see #startDate
     */
    public String endDate = "";

    /**
     * Date on which registration opens, as a {@code yyyy-MM-dd} string.
     * Empty string if not yet set.
     *
     * @see #startDate
     */
    public String registrationOpen = "";

    /**
     * Registration deadline as a {@code yyyy-MM-dd} string. The draw date is
     * automatically derived from this value via
     * {@link EventSchema#calculateDrawDate(java.util.Date)}.
     * Empty string if not yet set.
     *
     * @see #startDate
     */
    public String registrationDeadline = "";

    /**
     * Maximum number of attendees the event can accommodate. Must be greater
     * than zero before the event can be published. Also used as the initial
     * winners count when writing to Firestore.
     */
    public int capacity = 0;

    // -------------------------------------------------------------------------
    // Step 2: Settings
    // -------------------------------------------------------------------------

    /**
     * Maximum number of entrants permitted on the waitlist. A value of
     * {@code -1} indicates an unlimited waitlist.
     */
    public int waitlistLimit = -1;

    /**
     * Whether attendees must be within a required geographic area to check in.
     * {@code true} enables geolocation verification; {@code false} disables it.
     */
    public boolean geoRequired = false;

    /**
     * Whether the event is private (US 02.01.02).
     *
     * <p>When {@code true}, the event will not appear in the public browse listing
     * and no promotional QR code will be generated. Entrants can only join via
     * organizer invitation (US 02.01.03).</p>
     */
    public boolean isPrivate = false;

    // -------------------------------------------------------------------------
    // Step 3: QR Result
    // -------------------------------------------------------------------------

    /**
     * QR code {@link Bitmap} generated after a successful event creation,
     * encoding the deep-link URI {@code timely://event/<eventId>}.
     * {@code null} until {@code CreateEventActivity#generateQr(String)} completes.
     */
    public Bitmap generatedQr = null;

    /**
     * Firestore document ID assigned to the newly created event.
     * Empty string until the event has been persisted to Firestore.
     */
    public String generatedEventId = "";

    // -------------------------------------------------------------------------
    // Edit mode
    // -------------------------------------------------------------------------

    /**
     * {@code true} when the wizard was launched to edit an existing event,
     * {@code false} for new event creation. Set from the launching intent in
     * {@code CreateEventActivity#onCreate}.
     */
    public boolean isEditMode = false;

    /**
     * Firestore document ID of the event being edited. Only meaningful when
     * {@link #isEditMode} is {@code true}; empty string otherwise.
     */
    public String editingEventId = "";
}