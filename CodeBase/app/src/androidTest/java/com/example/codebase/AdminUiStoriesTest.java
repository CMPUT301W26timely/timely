package com.example.codebase;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Instrumentation coverage for administrator browse stories.
 */
@RunWith(AndroidJUnit4.class)
public class AdminUiStoriesTest {

    private final ArrayList<String> seededEventIds = new ArrayList<>();
    private final ArrayList<String> seededUserIds = new ArrayList<>();

    @Before
    public void setUp() {
        UiTestDataHelper.setDeviceId(UiTestDataHelper.TEST_DEVICE_ID);
        UiTestDataHelper.setSessionRole(WelcomeActivity.ROLE_ADMIN);
    }

    @After
    public void tearDown() throws Exception {
        for (String eventId : seededEventIds) {
            UiTestDataHelper.deleteEvent(eventId);
        }
        for (String userId : seededUserIds) {
            UiTestDataHelper.deleteUser(userId);
        }
        UiTestDataHelper.setSessionRole(WelcomeActivity.ROLE_USER);
    }

    /** US 03.04.01 - Browse all events as an administrator. */
    @Test
    public void us030401_browseEvents_adminHomeShowsClosedSystemEvents() throws Exception {
        seedClosedAdminEvent("Archived Admin Event");

        ActivityScenario.launch(OrganizerActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Browse Events")).check(matches(isDisplayed()));
        onView(withText("Archived Admin Event")).check(matches(isDisplayed()));
        onView(withId(R.id.navSearchLabel)).check(matches(withText("Profiles")));
    }

    /** US 03.05.01 - Browse user profiles as an administrator. */
    @Test
    public void us030501_browseProfiles_adminCanOpenProfileDirectory() throws Exception {
        seedAdminProfile("admin-profile-001", "Avery Admin", "avery.admin@example.com");
        seedAdminProfile("entrant-profile-001", "Taylor Entrant", "taylor.entrant@example.com");

        ActivityScenario.launch(OrganizerActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.navSearch)).perform(click());
        UiTestDataHelper.waitForUi();

        onView(withText("Browse Profiles")).check(matches(isDisplayed()));
        onView(withText("Avery Admin")).check(matches(isDisplayed()));
        onView(withText("Taylor Entrant")).check(matches(isDisplayed()));
    }

    private void seedAdminProfile(String deviceId, String name, String email) throws Exception {
        UiTestDataHelper.seedUser(deviceId, name, email, "+1 780 555 0101");
        seededUserIds.add(deviceId);
    }

    private void seedClosedAdminEvent(String title) throws Exception {
        String eventId = UiTestDataHelper.uniqueId("admin-event");
        UiTestDataHelper.seedEvent(
                eventId,
                title,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                UiTestDataHelper.daysFromToday(-15),
                UiTestDataHelper.daysFromToday(-10),
                UiTestDataHelper.daysFromToday(-7)
        );
        seededEventIds.add(eventId);
    }
}
