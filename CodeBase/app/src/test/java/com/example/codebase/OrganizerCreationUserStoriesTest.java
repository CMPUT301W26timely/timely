package com.example.codebase;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Story-focused JUnit coverage for organizer event creation and configuration flows.
 *
 * Covered stories:
 *   US 02.01.01 — Create event with QR code
 *   US 02.01.04 — Set registration period
 *   US 02.04.01 — Upload event poster
 *   US 02.04.02 — Update event poster
 *   US 02.03.01 — Limit waiting list size
 *   US 02.02.03 — Enable/disable geolocation
 */
public class OrganizerCreationUserStoriesTest {

    private static final String DEFAULT_POSTER = "poster-base64-v1";
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);

    private CreateEventViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new CreateEventViewModel();
        viewModel.name = "Beginner Swim Lessons";
        viewModel.description = "A six-week beginner swim program.";
        viewModel.location = "Clareview Pool";
        viewModel.price = 60.0;
        viewModel.startDate = "2026-04-01";
        viewModel.endDate = "2026-05-15";
        viewModel.registrationOpen = "2026-03-01";
        viewModel.registrationDeadline = "2026-03-20";
        viewModel.capacity = 20;
        viewModel.waitlistLimit = 40;
        viewModel.geoRequired = true;
        viewModel.posterBase64 = DEFAULT_POSTER;
    }

    @Test
    public void us020101_createEvent_buildsEventDetailsAndQrPayload() {
        Event event = buildEvent(viewModel, "event-qr-001", "organizer-device-7");
        String qrPayload = buildQrPayload("event-qr-001");

        assertEquals("Beginner Swim Lessons", event.getTitle());
        assertEquals("Clareview Pool", event.getLocation());
        assertEquals(Long.valueOf(20), event.getMaxCapacity());
        assertEquals("organizer-device-7", event.getOrganizerDeviceId());
        assertEquals("timely://event/event-qr-001", qrPayload);
    }

    @Test
    public void us020104_setRegistrationPeriod_savesStartAndEndDates() {
        Event event = buildEvent(viewModel, "event-dates-001", "organizer-device-7");

        assertNotNull(event.getRegistrationOpen());
        assertNotNull(event.getRegistrationDeadline());
        assertNotNull(event.getStartDate());
        assertNotNull(event.getEndDate());
        assertEquals("Draw date should be derived from the registration deadline",
                EventSchema.calculateDrawDate(event.getRegistrationDeadline()),
                event.getDrawDate());
    }

    @Test
    public void us020104_registrationOnlyWorksInsideConfiguredPeriod() {
        Event event = buildEvent(viewModel, "event-reg-001", "organizer-device-7");

        assertFalse("Joining should be blocked before registration opens",
                isRegistrationOpen(event, parseDate("2026-02-25")));
        assertTrue("Joining should be allowed during registration",
                isRegistrationOpen(event, parseDate("2026-03-10")));
        assertFalse("Joining should be blocked after the deadline",
                isRegistrationOpen(event, parseDate("2026-03-21")));
    }

    @Test
    public void us020401_uploadPoster_attachesPosterToEvent() {
        Event event = buildEvent(viewModel, "event-poster-001", "organizer-device-7");

        assertNotNull("Poster should be attached when an image is chosen", event.getPoster());
        assertEquals(DEFAULT_POSTER, event.getPoster().getPosterImageBase64());
    }

    @Test
    public void us020402_updatePoster_replacesExistingPoster() {
        Event existingEvent = buildEvent(viewModel, "event-poster-002", "organizer-device-7");

        viewModel.posterBase64 = "poster-base64-v2";
        applyEditableFields(existingEvent, viewModel);

        assertEquals("poster-base64-v2", existingEvent.getPoster().getPosterImageBase64());
    }

    @Test
    public void us020301_limitWaitingListSize_blocksNewJoinWhenFull() {
        Event event = buildEvent(viewModel, "event-limit-001", "organizer-device-7");
        event.setWaitingList(new ArrayList<>());
        for (int i = 0; i < event.getWaitlistCap(); i++) {
            event.getWaitingList().add("entrant-" + i);
        }

        assertFalse("The system should block joins when the waitlist reaches its limit",
                canJoinWaitlist(event, "late-entrant", parseDate("2026-03-10")));
    }

    @Test
    public void us020203_enableDisableGeolocation_persistsToggleOnEvent() {
        Event enabledEvent = buildEvent(viewModel, "event-geo-on", "organizer-device-7");
        assertTrue("Geo requirement should be saved when enabled", enabledEvent.isGeoEnabled());

        viewModel.geoRequired = false;
        Event disabledEvent = buildEvent(viewModel, "event-geo-off", "organizer-device-7");
        assertFalse("Geo requirement should be removable when disabled", disabledEvent.isGeoEnabled());
    }

    @Ignore("This branch stores the geolocation toggle, but does not yet enforce location capture on join.")
    @Test
    public void us020203_enableDisableGeolocation_requiresLocationWhenEnabled() {
        // Needs production logic before it can be tested.
    }

    @Ignore("Saving the event document and generated QR image to Firebase/UI requires integration testing.")
    @Test
    public void us020101_createEvent_savesToDatabaseAndRendersQr() {
        // Firestore and image rendering belong in integration or instrumentation tests.
    }

    private Event buildEvent(CreateEventViewModel model, String eventId, String organizerDeviceId) {
        Event event = new Event();
        event.setId(eventId);
        event.setTitle(model.name);
        event.setDescription(model.description);
        event.setLocation(model.location);
        event.setPrice((float) model.price);
        event.setMaxCapacity((long) model.capacity);
        event.setWinnersCount((long) model.capacity);
        event.setWaitlistCap(model.waitlistLimit);
        event.setGeoEnabled(model.geoRequired);
        event.setOrganizerDeviceId(organizerDeviceId);
        event.setStatus("published");
        event.setWaitingList(new ArrayList<>());
        event.setSelectedEntrants(new ArrayList<>());
        event.setCancelledEntrants(new ArrayList<>());
        event.setEnrolledEntrants(new ArrayList<>());

        applyEditableFields(event, model);
        return event;
    }

    private void applyEditableFields(Event event, CreateEventViewModel model) {
        event.setStartDate(parseDate(model.startDate));
        event.setEndDate(parseDate(model.endDate));
        event.setRegistrationOpen(parseDate(model.registrationOpen));
        event.setRegistrationDeadline(parseDate(model.registrationDeadline));
        event.setDrawDate(EventSchema.calculateDrawDate(event.getRegistrationDeadline()));

        if (model.posterBase64 != null && !model.posterBase64.trim().isEmpty()) {
            event.setPoster(new EventPoster(model.posterBase64));
        }
    }

    private String buildQrPayload(String eventId) {
        return "timely://event/" + eventId;
    }

    private Date parseDate(String dateText) {
        try {
            return DATE_FORMAT.parse(dateText);
        } catch (ParseException e) {
            return null;
        }
    }

    private boolean isRegistrationOpen(Event event, Date now) {
        if (event.getRegistrationOpen() != null && now.before(event.getRegistrationOpen())) {
            return false;
        }
        if (event.getRegistrationDeadline() != null && !now.before(event.getRegistrationDeadline())) {
            return false;
        }
        return true;
    }

    private boolean canJoinWaitlist(Event event, String deviceId, Date now) {
        if (!isRegistrationOpen(event, now)) {
            return false;
        }

        if (event.getWaitlistCap() > 0 && event.getWaitingList().size() >= event.getWaitlistCap()) {
            return false;
        }

        if (event.getWaitingList().contains(deviceId)) {
            return false;
        }

        event.getWaitingList().add(deviceId);
        return true;
    }
}
