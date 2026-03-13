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
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

/**
 * Instrumentation coverage for organizer workflow and notification-management stories.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerWorkflowUiStoriesTest {

    private final ArrayList<String> seededEventIds = new ArrayList<>();

    @Before
    public void setUp() {
        UiTestDataHelper.setDeviceId(UiTestDataHelper.TEST_DEVICE_ID);
    }

    @After
    public void tearDown() throws Exception {
        for (String eventId : seededEventIds) {
            UiTestDataHelper.deleteEvent(eventId);
        }
    }

    /** US 02.05.01 - Send a notification to chosen entrants. */
    @Test
    public void us020501_notifyChosenEntrants_notificationSheetSupportsSelectedRecipients() throws Exception {
        String eventId = seedOrganizerWorkflowEvent("Selected Notification Event");

        ActivityScenario.launch(buildEventDetailIntent(eventId, "Selected Notification Event"));
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.rowSendNotification)).perform(scrollTo(), click());
        onView(withId(R.id.selectedRadioButton)).perform(click());
        onView(withId(R.id.subjectEditText))
                .check(matches(withText("Congratulations! You've been selected!")));
    }

    /** US 02.06.01 - View invited entrants. */
    @Test
    public void us020601_viewInvitedEntrants_screenShowsSelectedEntrantStatusTabs() throws Exception {
        String eventId = seedOrganizerWorkflowEvent("Invited Entrants Event");

        Intent intent = new Intent(UiTestDataHelper.context(), InvitedEntrantsActivity.class);
        intent.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_TITLE, "Invited Entrants Event");
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.tabPending)).check(matches(isDisplayed()));
        onView(withId(R.id.tabAccepted)).check(matches(isDisplayed()));
        onView(withId(R.id.tabDeclined)).check(matches(isDisplayed()));
        onView(withText("chosenA")).check(matches(isDisplayed()));
    }

    /** US 02.06.02 - View cancelled entrants. */
    @Test
    public void us020602_viewCancelledEntrants_screenShowsCancelledAndDeclinedTabs() throws Exception {
        String eventId = seedOrganizerWorkflowEvent("Cancelled Entrants Event");

        Intent intent = new Intent(UiTestDataHelper.context(), CancelledEntrantsActivity.class);
        intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_TITLE, "Cancelled Entrants Event");
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.tabCancelledDeclined)).check(matches(isDisplayed()));
        onView(withId(R.id.tabCancelledCancelled)).check(matches(isDisplayed()));
        onView(withText("chosenC")).check(matches(isDisplayed()));
    }

    /** US 02.06.04 - Cancel entrant that did not sign up. */
    @Test
    public void us020604_cancelEntrant_pendingEntrantRowShowsCancelAction() throws Exception {
        String eventId = seedOrganizerWorkflowEvent("Cancel Pending Entrant Event");

        Intent intent = new Intent(UiTestDataHelper.context(), InvitedEntrantsActivity.class);
        intent.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_TITLE, "Cancel Pending Entrant Event");
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(allOf(withId(R.id.btnCancelEntrant), isDisplayed())).check(matches(isDisplayed()));
    }

    /** US 02.07.01 - Send notifications to waiting-list entrants. */
    @Test
    public void us020701_notifyWaitingList_notificationSheetSupportsWaitingRecipients() throws Exception {
        String eventId = seedOrganizerWorkflowEvent("Waiting List Notification Event");

        ActivityScenario.launch(buildEventDetailIntent(eventId, "Waiting List Notification Event"));
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.rowSendNotification)).perform(scrollTo(), click());
        onView(withId(R.id.waitingListRadioButton)).perform(click());
        onView(withId(R.id.subjectEditText))
                .check(matches(withText("You're on the waiting list!")));
    }

    /** US 02.07.02 - Send notifications to selected entrants. */
    @Test
    public void us020702_notifySelected_notificationSheetShowsSelectedRecipientOption() throws Exception {
        String eventId = seedOrganizerWorkflowEvent("Selected Recipient Event");

        ActivityScenario.launch(buildEventDetailIntent(eventId, "Selected Recipient Event"));
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.rowSendNotification)).perform(scrollTo(), click());
        onView(withId(R.id.selectedRadioButton)).check(matches(isDisplayed()));
    }

    /** US 02.07.03 - Send notifications to cancelled entrants. */
    @Test
    public void us020703_notifyCancelled_notificationSheetSupportsCancelledRecipients() throws Exception {
        String eventId = seedOrganizerWorkflowEvent("Cancelled Recipient Event");

        ActivityScenario.launch(buildEventDetailIntent(eventId, "Cancelled Recipient Event"));
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.rowSendNotification)).perform(scrollTo(), click());
        onView(withId(R.id.cancelledRadioButton)).perform(click());
        onView(withId(R.id.subjectEditText))
                .check(matches(withText("Your participation has been cancelled")));
    }

    private Intent buildEventDetailIntent(String eventId, String title) {
        Intent intent = new Intent(UiTestDataHelper.context(), EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, title);
        return intent;
    }

    private String seedOrganizerWorkflowEvent(String title) throws Exception {
        String eventId = UiTestDataHelper.uniqueId("workflow-event");
        ArrayList<String> waiting = new ArrayList<>();
        waiting.add("wait1");
        waiting.add("wait2");

        ArrayList<String> selected = new ArrayList<>();
        selected.add("chosenA");
        selected.add("chosenB");
        selected.add("chosenC");

        ArrayList<String> enrolled = new ArrayList<>();
        enrolled.add("chosenB");

        ArrayList<String> cancelled = new ArrayList<>();
        cancelled.add("chosenC");
        cancelled.add("removedX");

        UiTestDataHelper.seedEvent(
                eventId,
                title,
                waiting,
                selected,
                enrolled,
                cancelled,
                UiTestDataHelper.daysFromToday(-10),
                UiTestDataHelper.daysFromToday(-3),
                UiTestDataHelper.daysFromToday(-1)
        );
        seededEventIds.add(eventId);
        return eventId;
    }
}
