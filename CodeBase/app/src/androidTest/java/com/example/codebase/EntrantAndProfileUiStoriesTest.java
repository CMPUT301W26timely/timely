package com.example.codebase;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

/**
 * Instrumentation coverage for the entrant/profile stories the team currently marks complete.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantAndProfileUiStoriesTest {

    private final ArrayList<String> seededEventIds = new ArrayList<>();
    private final ArrayList<String> seededNotificationIds = new ArrayList<>();
    private boolean seededUser;

    @Before
    public void setUp() {
        UiTestDataHelper.setDeviceId(UiTestDataHelper.TEST_DEVICE_ID);
        UiTestDataHelper.setSessionRole(WelcomeActivity.ROLE_USER);
    }

    @After
    public void tearDown() throws Exception {
        for (String notificationId : seededNotificationIds) {
            UiTestDataHelper.deleteNotification(UiTestDataHelper.TEST_DEVICE_ID, notificationId);
        }
        for (String eventId : seededEventIds) {
            UiTestDataHelper.deleteEvent(eventId);
        }
        if (seededUser) {
            UiTestDataHelper.deleteUser(UiTestDataHelper.TEST_DEVICE_ID);
        }
    }

    /** US 01.01.01 - Join waiting list for a specific event. */
    @Test
    public void us010101_joinWaitingList_showsJoinButtonOnEntrantEventDetail() throws Exception {
        String eventId = seedEntrantEvent("Joinable Event", new ArrayList<>());

        Intent intent = new Intent(UiTestDataHelper.context(), EntrantEventDetailActivity.class);
        intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, eventId);
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.btnJoinWaitingList)).check(matches(isDisplayed()));
        onView(withId(R.id.btnJoinWaitingList)).check(matches(isEnabled()));
    }

    /** US 01.01.02 - Leave waiting list for a specific event. */
    @Test
    public void us010102_leaveWaitingList_showsLeaveButtonWhenEntrantAlreadyJoined() throws Exception {
        ArrayList<String> waitingList = new ArrayList<>();
        waitingList.add(UiTestDataHelper.TEST_DEVICE_ID);
        String eventId = seedEntrantEvent("Joined Event", waitingList);

        Intent intent = new Intent(UiTestDataHelper.context(), EntrantEventDetailActivity.class);
        intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, eventId);
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.btnLeaveWaitingList)).check(matches(isDisplayed()));
        onView(withId(R.id.btnLeaveWaitingList)).check(matches(isEnabled()));
    }

    /** US 01.01.03 - Browse events. */
    @Test
    public void us010103_browseEvents_displaysSeededActiveEvent() throws Exception {
        seedEntrantEvent("Browseable Event", new ArrayList<>());

        ActivityScenario.launch(BrowseEventsActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Browse Events")).check(matches(isDisplayed()));
        onView(withId(R.id.editTextSearch)).check(matches(isDisplayed()));
        onView(withId(R.id.btnShowFilters)).check(matches(isDisplayed()));
        onView(withId(R.id.recyclerViewEvents)).check(matches(isDisplayed()));
        onView(withId(R.id.navExplore)).check(matches(isDisplayed()));
    }

    /** US 01.02.01 - Provide personal information. */
    @Test
    public void us010201_profileCreation_firstRunShowsRequiredFields() {
        Intent intent = new Intent(UiTestDataHelper.context(), ProfileSettingsActivity.class);
        intent.putExtra(ProfileSettingsActivity.EXTRA_FIRST_RUN, true);
        ActivityScenario.launch(intent);

        onView(withText("Create Your Profile")).check(matches(isDisplayed()));
        onView(withId(R.id.editTextName)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextPhone)).check(matches(isDisplayed()));
    }

    /** US 01.02.02 - Update profile information. */
    @Test
    public void us010202_updateProfile_profileScreenShowsStoredUserDetails() throws Exception {
        UiTestDataHelper.seedUser(UiTestDataHelper.TEST_DEVICE_ID,
                "Taylor Brooks",
                "taylor@example.com",
                "+1 780 555 0198");
        seededUser = true;

        ActivityScenario.launch(ProfileActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Taylor Brooks")).check(matches(isDisplayed()));
        onView(withText("taylor@example.com")).check(matches(isDisplayed()));
        onView(withId(R.id.btnEditProfile)).check(matches(isDisplayed()));
    }

    /** US 01.02.04 - Delete profile. */
    @Test
    public void us010204_deleteProfile_editScreenShowsDeleteActionAndConfirmationDialog() throws Exception {
        UiTestDataHelper.seedUser(UiTestDataHelper.TEST_DEVICE_ID,
                "Taylor Brooks",
                "taylor@example.com",
                "+1 780 555 0198");
        seededUser = true;

        ActivityScenario.launch(ProfileSettingsActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.buttonDeleteProfile)).perform(scrollTo(), click());
        onView(withText("Delete profile?")).check(matches(isDisplayed()));
        onView(withText("Delete Profile")).check(matches(isDisplayed()));
    }

    /** US 01.04.01 - Receive notification when chosen. */
    @Test
    public void us010401_notificationWhenChosen_notificationsScreenShowsSelectedStatus() throws Exception {
        String eventId = seedEntrantEvent("Chosen Event", new ArrayList<>());
        String notificationId = UiTestDataHelper.seedNotification(
                UiTestDataHelper.TEST_DEVICE_ID,
                eventId,
                "Lottery result",
                "You have been selected for the event.",
                "Selected",
                "selectedEntrants"
        );
        seededNotificationIds.add(notificationId);

        ActivityScenario.launch(NotificationsActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Selected")).check(matches(isDisplayed()));
        onView(withText("You have been selected for the event.")).check(matches(isDisplayed()));
    }

    /** US 01.04.02 - Receive notification when not chosen. */
    @Test
    public void us010402_notificationWhenNotChosen_notificationsScreenShowsNotSelectedStatus() throws Exception {
        String eventId = seedEntrantEvent("Not Selected Event", new ArrayList<>());
        String notificationId = UiTestDataHelper.seedNotification(
                UiTestDataHelper.TEST_DEVICE_ID,
                eventId,
                "Lottery result",
                "You were not selected in this draw.",
                "Not Selected",
                "cancelledEntrants"
        );
        seededNotificationIds.add(notificationId);

        ActivityScenario.launch(NotificationsActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Not Selected")).check(matches(isDisplayed()));
        onView(withText("You were not selected in this draw.")).check(matches(isDisplayed()));
    }

    /** US 01.04.03 - Opt out of organizer/admin notifications. */
    @Test
    public void us010403_optOutNotifications_notificationsScreenShowsOptedOutState() throws Exception {
        UiTestDataHelper.seedUser(UiTestDataHelper.TEST_DEVICE_ID,
                "Taylor Brooks",
                "taylor@example.com",
                "+1 780 555 0198",
                false);
        seededUser = true;

        ActivityScenario.launch(NotificationsActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Notifications are turned off.")).check(matches(isDisplayed()));
        onView(withText(containsString("Turn organizer and admin notifications back on")))
                .check(matches(isDisplayed()));
    }

    /** US 01.05.02 - Accept invitation to register. */
    @Test
    public void us010502_acceptInvitation_invitationsScreenShowsAcceptAction() throws Exception {
        String eventId = seedInvitationEvent("Invitation Accept Event");

        ActivityScenario.launch(InvitationsActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Invitation Accept Event")).check(matches(isDisplayed()));
        onView(withId(R.id.btnExploreAccept)).check(matches(isDisplayed()));
    }

    /** US 01.05.03 - Decline invitation to register. */
    @Test
    public void us010503_declineInvitation_invitationsScreenShowsDeclineAction() throws Exception {
        String eventId = seedInvitationEvent("Invitation Decline Event");

        ActivityScenario.launch(InvitationsActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Invitation Decline Event")).check(matches(isDisplayed()));
        onView(withId(R.id.btnExploreDecline)).check(matches(isDisplayed()));
    }

    /** US 01.05.04 - View total entrants on the waiting list. */
    @Test
    public void us010504_waitingListCount_eventDetailShowsTotalEntrants() throws Exception {
        ArrayList<String> waitingList = new ArrayList<>();
        waitingList.add(UiTestDataHelper.TEST_DEVICE_ID);
        waitingList.add("ui-test-device-002");
        waitingList.add("ui-test-device-003");
        String eventId = seedEntrantEvent("Counted Event", waitingList);

        Intent intent = new Intent(UiTestDataHelper.context(), EntrantEventDetailActivity.class);
        intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, eventId);
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.tvEntrantWaitingListCount))
                .check(matches(withText("Total waiting list entrants: 3")));
    }

    /** US 01.07.01 - Device-based identification with no password flow. */
    @Test
    public void us010701_deviceBasedAuthentication_profileSetupShowsStoredDeviceId() {
        Intent intent = new Intent(UiTestDataHelper.context(), ProfileSettingsActivity.class);
        intent.putExtra(ProfileSettingsActivity.EXTRA_FIRST_RUN, true);
        ActivityScenario.launch(intent);

        onView(withId(R.id.textViewDeviceId))
                .check(matches(withText(containsString(UiTestDataHelper.TEST_DEVICE_ID))));
    }

    private String seedEntrantEvent(String title, ArrayList<String> waitingList) throws Exception {
        String eventId = UiTestDataHelper.uniqueId("entrant-event");
        UiTestDataHelper.seedEvent(
                eventId,
                title,
                waitingList,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                UiTestDataHelper.daysFromToday(-1),
                UiTestDataHelper.daysFromToday(2),
                UiTestDataHelper.daysFromToday(5)
        );
        seededEventIds.add(eventId);
        return eventId;
    }

    private String seedInvitationEvent(String title) throws Exception {
        String eventId = UiTestDataHelper.uniqueId("invitation-event");
        ArrayList<String> selected = new ArrayList<>();
        selected.add(UiTestDataHelper.TEST_DEVICE_ID);
        UiTestDataHelper.seedEvent(
                eventId,
                title,
                new ArrayList<>(),
                selected,
                new ArrayList<>(),
                new ArrayList<>(),
                UiTestDataHelper.daysFromToday(-7),
                UiTestDataHelper.daysFromToday(-2),
                UiTestDataHelper.daysFromToday(-1)
        );
        seededEventIds.add(eventId);
        return eventId;
    }
}
