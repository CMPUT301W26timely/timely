package com.example.codebase;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Story-focused JUnit coverage for administrator browse flows.
 */
public class AdminUserStoriesTest {

    @Test
    public void us030401_browseEvents_includesClosedEventsForAdmins() {
        Event openEvent = makeEvent("Open Event", daysFromNow(4));
        Event closedEvent = makeEvent("Closed Event", daysFromNow(-4));

        List<Event> visibleEvents = AdminBrowseHelper.sortEventsForAdmin(
                Arrays.asList(closedEvent, openEvent)
        );

        assertEquals("Administrators should see both open and closed events", 2, visibleEvents.size());
        assertTrue(visibleEvents.contains(openEvent));
        assertTrue(visibleEvents.contains(closedEvent));
    }

    @Test
    public void us030401_browseEvents_ordersUpcomingEventsBeforeUndatedEvents() {
        Event upcomingEvent = makeEvent("Upcoming Event", daysFromNow(2));
        Event undatedEvent = makeEvent("Undated Event", null);

        List<Event> visibleEvents = AdminBrowseHelper.sortEventsForAdmin(
                Arrays.asList(undatedEvent, upcomingEvent)
        );

        assertEquals("Upcoming events should appear before undated ones",
                "Upcoming Event",
                visibleEvents.get(0).getTitle());
    }

    @Test
    public void us030501_browseProfiles_keepsIncompleteProfilesVisibleToAdmins() {
        User completeProfile = makeUser("user-001", "Alex Complete", "alex@example.com", "entrant");
        User incompleteProfile = makeUser("user-002", "", "", "entrant");

        List<User> profiles = AdminBrowseHelper.sortProfilesForAdmin(
                Arrays.asList(incompleteProfile, completeProfile)
        );

        assertEquals("Administrators should still see incomplete profiles", 2, profiles.size());
        assertTrue(AdminBrowseHelper.isProfileComplete(completeProfile));
        assertFalse(AdminBrowseHelper.isProfileComplete(incompleteProfile));
    }

    @Test
    public void us030501_browseProfiles_placesAdminAccountsBeforeEntrants() {
        User entrantProfile = makeUser("entrant-001", "Taylor Entrant", "taylor@example.com", "entrant");
        User adminProfile = makeUser("admin-001", "Avery Admin", "avery@example.com", "admin");

        List<User> profiles = AdminBrowseHelper.sortProfilesForAdmin(
                Arrays.asList(entrantProfile, adminProfile)
        );

        assertTrue("Admin accounts should sort ahead of entrant accounts",
                AdminBrowseHelper.isAdminProfile(profiles.get(0)));
        assertEquals("Avery Admin", profiles.get(0).getName());
    }

    @Test
    public void us030701_adminCanIdentifyRevokedOrganizerProfiles() {
        User organizer = makeUser("org-001", "Riley Organizer", "riley@example.com", "entrant");
        organizer.setOrganizerPrivilegesRevoked(true);

        assertTrue("Revoked organizer profiles should be detectable in admin tools",
                AdminBrowseHelper.isOrganizerRevoked(organizer));
    }

    private Event makeEvent(String title, Date startDate) {
        Event event = new Event();
        event.setTitle(title);
        event.setStartDate(startDate);
        return event;
    }

    private User makeUser(String deviceId, String name, String email, String role) {
        User user = new User(deviceId);
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private Date daysFromNow(int offsetDays) {
        return new Date(System.currentTimeMillis() + (offsetDays * 24L * 60L * 60L * 1000L));
    }
}
