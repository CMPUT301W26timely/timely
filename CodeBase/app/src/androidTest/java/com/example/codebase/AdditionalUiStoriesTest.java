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
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

/**
 * Additional instrumentation coverage for stories that were not already exercised by the
 * existing actor-specific UI suites.
 */
@RunWith(AndroidJUnit4.class)
public class AdditionalUiStoriesTest {

    private final ArrayList<String> seededEventIds = new ArrayList<>();
    private final ArrayList<String> seededUserIds = new ArrayList<>();
    private final ArrayList<String> seededNotificationIds = new ArrayList<>();
    private final ArrayList<String> seededNotificationLogIds = new ArrayList<>();
    private final ArrayList<String> seededPrivateInviteKeys = new ArrayList<>();
    private final ArrayList<String> seededCommentKeys = new ArrayList<>();
    private final ArrayList<String> seededLocationKeys = new ArrayList<>();

    @Before
    public void setUp() {
        useEntrantSession();
    }

    @After
    public void tearDown() throws Exception {
        for (String locationKey : seededLocationKeys) {
            String[] parts = locationKey.split("\\|", 2);
            UiTestDataHelper.deleteEntrantLocation(parts[0], parts[1]);
        }
        for (String commentKey : seededCommentKeys) {
            String[] parts = commentKey.split("\\|", 2);
            UiTestDataHelper.deleteComment(parts[0], parts[1]);
        }
        for (String privateInviteKey : seededPrivateInviteKeys) {
            String[] parts = privateInviteKey.split("\\|", 2);
            UiTestDataHelper.deletePrivateInvite(parts[0], parts[1]);
        }
        for (String notificationId : seededNotificationIds) {
            UiTestDataHelper.deleteNotification(UiTestDataHelper.TEST_DEVICE_ID, notificationId);
        }
        for (String logId : seededNotificationLogIds) {
            UiTestDataHelper.deleteNotificationLog(logId);
        }
        for (String eventId : seededEventIds) {
            UiTestDataHelper.deleteEvent(eventId);
        }
        for (String userId : seededUserIds) {
            UiTestDataHelper.deleteUser(userId);
        }
        useEntrantSession();
    }

    /** US 01.01.04 - Filter events based on availability and capacity. */
    @Test
    public void us010104_filterEvents_filterDialogShowsAvailabilityAndCapacityControls() {
        useEntrantSession();

        ActivityScenario.launch(BrowseEventsActivity.class);
        onView(withId(R.id.btnShowFilters)).perform(click());

        onView(withId(R.id.etAvailableFrom)).check(matches(isDisplayed()));
        onView(withId(R.id.etAvailableTo)).check(matches(isDisplayed()));
        onView(withId(R.id.actvCapacity)).check(matches(isDisplayed()));
    }

    /** US 01.01.05 - Search events by keyword. */
    @Test
    public void us010105_searchEvents_browseScreenAcceptsKeywordInput() throws Exception {
        useEntrantSession();
        seedEntrantEvent("Watercolour Basics");

        ActivityScenario.launch(BrowseEventsActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.editTextSearch))
                .perform(typeText("Watercolour"), closeSoftKeyboard())
                .check(matches(withText("Watercolour")));
    }

    /** US 01.01.06 - Combine keyword search with filters. */
    @Test
    public void us010106_searchWithFilters_browseScreenSupportsBothEntryPoints() {
        useEntrantSession();

        ActivityScenario.launch(BrowseEventsActivity.class);
        onView(withId(R.id.editTextSearch))
                .perform(typeText("music"), closeSoftKeyboard());
        onView(withId(R.id.btnShowFilters)).perform(click());
        onView(withId(R.id.btnApplyFilters)).check(matches(isDisplayed()));
    }

    /** US 01.05.01 - Another chance when a selected user declines. */
    @Test
    public void us010501_replacementChance_lotteryScreenShowsReplacementAction() throws Exception {
        useOrganizerSession();
        String eventId = seedWorkflowEvent("Replacement Draw Event");

        Intent intent = new Intent(UiTestDataHelper.context(), LotteryDrawActivity.class);
        intent.putExtra("EXTRA_EVENT_ID", eventId);
        intent.putExtra("EXTRA_EVENT_TITLE", "Replacement Draw Event");
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.drawReplacements)).check(matches(isDisplayed()));
    }

    /** US 01.05.06 - Receive private-event invitation notification. */
    @Test
    public void us010506_privateInviteNotification_notificationsScreenShowsInviteMessage() throws Exception {
        useEntrantSession();
        String eventId = seedPrivateInvitation("Private Invite Event", PrivateEventInvite.STATUS_PENDING);
        String notificationId = UiTestDataHelper.seedNotification(
                UiTestDataHelper.TEST_DEVICE_ID,
                eventId,
                "Private event invitation",
                "You have been personally invited to Private Invite Event.",
                "Private Invite",
                "privateInvite"
        );
        seededNotificationIds.add(notificationId);

        ActivityScenario.launch(NotificationsActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Private Invite")).check(matches(isDisplayed()));
        onView(withText(containsString("personally invited"))).check(matches(isDisplayed()));
    }

    /** US 01.05.07 - Accept or decline private-event invitation. */
    @Test
    public void us010507_privateInviteAcceptDecline_myInvitationsShowsActions() throws Exception {
        useEntrantSession();
        seedPrivateInvitation("Invite-Only Workshop", PrivateEventInvite.STATUS_PENDING);

        ActivityScenario.launch(InvitationsActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Invite-Only Workshop")).check(matches(isDisplayed()));
        onView(withId(R.id.btnExploreAccept)).check(matches(isDisplayed()));
        onView(withId(R.id.btnExploreDecline)).check(matches(isDisplayed()));
    }

    /** US 01.06.01 - View event details by scanning a promotional QR code. */
    @Test
    public void us010601_scanQr_qrScannerScreenLaunches() {
        useEntrantSession();

        ActivityScenario.launch(QRScannerActivity.class);

        onView(withId(R.id.button_cancel)).check(matches(isDisplayed()));
        onView(withId(R.id.previewView)).check(matches(isDisplayed()));
    }

    /** US 01.06.02 - Sign up from event details. */
    @Test
    public void us010602_signUpFromEventDetails_entrantDetailShowsJoinAction() throws Exception {
        useEntrantSession();
        String eventId = seedEntrantEvent("Detail Join Event");

        Intent intent = new Intent(UiTestDataHelper.context(), EntrantEventDetailActivity.class);
        intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, eventId);
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.btnJoinWaitingList)).check(matches(isDisplayed()));
    }

    /** US 01.08.01 - Post a comment on an event. */
    @Test
    public void us010801_postComment_entrantDetailShowsComposer() throws Exception {
        useEntrantSession();
        String eventId = seedEntrantEvent("Commentable Event");

        Intent intent = new Intent(UiTestDataHelper.context(), EntrantEventDetailActivity.class);
        intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, eventId);
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.etEntrantNewComment)).check(matches(isDisplayed()));
        onView(withId(R.id.btnEntrantPostComment)).check(matches(isDisplayed()));
    }

    /** US 01.08.02 - View comments on an event. */
    @Test
    public void us010802_viewComments_commentDialogShowsSeededEntrantComment() throws Exception {
        useEntrantSession();
        String eventId = seedEntrantEvent("View Comments Event");
        String commentId = UiTestDataHelper.seedComment(
                eventId,
                "entrant-commenter",
                "Jordan Reader",
                "Looking forward to this event.",
                false
        );
        seededCommentKeys.add(eventId + "|" + commentId);

        Intent intent = new Intent(UiTestDataHelper.context(), EntrantEventDetailActivity.class);
        intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, eventId);
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.btnViewAllEntrantComments)).perform(scrollTo(), click());
        UiTestDataHelper.waitForUi();

        onView(withText("Looking forward to this event.")).check(matches(isDisplayed()));
    }

    /** US 01.09.01 - Notification when invited as a co-organizer. */
    @Test
    public void us010901_coOrganizerInvitation_notificationsScreenShowsCoOrganizerStatus() throws Exception {
        useEntrantSession();
        String eventId = seedEntrantEvent("Co-Organizer Event");
        String notificationId = UiTestDataHelper.seedNotification(
                UiTestDataHelper.TEST_DEVICE_ID,
                eventId,
                "Co-organizer invitation",
                "You have been invited to help run this event.",
                "Co-Organizer",
                "coOrganizer"
        );
        seededNotificationIds.add(notificationId);

        ActivityScenario.launch(NotificationsActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Co-Organizer")).check(matches(isDisplayed()));
        onView(withText("You have been invited to help run this event.")).check(matches(isDisplayed()));
    }

    /** US 02.01.02 - Create a private event. */
    @Test
    public void us020102_privateEvent_createSettingsShowsPrivateToggle() {
        useOrganizerSession();

        ActivityScenario.launch(CreateEventActivity.class);
        onView(withId(R.id.btnContinue)).perform(click());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.swPrivateEvent)).check(matches(isDisplayed()));
    }

    /** US 02.01.03 - Invite private-event entrants by name, phone, or email. */
    @Test
    public void us020103_privateInvite_inviteScreenShowsSearchFields() throws Exception {
        useOrganizerSession();
        String eventId = seedPrivateEvent("Invite Search Event");

        Intent intent = new Intent(UiTestDataHelper.context(), InviteEntrantsActivity.class);
        intent.putExtra(InviteEntrantsActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(InviteEntrantsActivity.EXTRA_EVENT_TITLE, "Invite Search Event");
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.etInviteName)).check(matches(isDisplayed()));
        onView(withId(R.id.etInviteEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.etInvitePhone)).check(matches(isDisplayed()));
        onView(withId(R.id.btnInviteByContact)).check(matches(isDisplayed()));
    }

    /** US 02.02.01 - View waiting-list entrants. */
    @Test
    public void us020201_viewWaitingList_waitingListScreenShowsSeededEntrant() throws Exception {
        useOrganizerSession();
        String waitingUserId = "waiting-user-001";
        UiTestDataHelper.seedUser(waitingUserId, "Waiting Person", "waiting@example.com", "7805550001");
        seededUserIds.add(waitingUserId);

        String eventId = UiTestDataHelper.uniqueId("waiting-list-event");
        ArrayList<String> waiting = new ArrayList<>();
        waiting.add(waitingUserId);
        UiTestDataHelper.seedEvent(
                eventId,
                "Waiting List Event",
                waiting,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                UiTestDataHelper.daysFromToday(-2),
                UiTestDataHelper.daysFromToday(2),
                UiTestDataHelper.daysFromToday(4)
        );
        seededEventIds.add(eventId);

        Intent intent = new Intent(UiTestDataHelper.context(), WaitingListActivity.class);
        intent.putExtra(WaitingListActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(WaitingListActivity.EXTRA_EVENT_TITLE, "Waiting List Event");
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withText("Waiting Person")).check(matches(isDisplayed()));
    }

    /** US 02.02.02 - View a map of entrant join locations. */
    @Test
    public void us020202_viewEntrantMap_mapActivityShowsOsmdroidMap() throws Exception {
        useOrganizerSession();
        String eventId = seedEntrantEvent("Mapped Event");
        UiTestDataHelper.seedEntrantLocation(
                eventId,
                "map-user-001",
                "Mapped Entrant",
                53.5461,
                -113.4938
        );
        seededLocationKeys.add(eventId + "|" + "map-user-001");

        Intent intent = new Intent(UiTestDataHelper.context(), ViewEntrantMapActivity.class);
        intent.putExtra(ViewEntrantMapActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(ViewEntrantMapActivity.EXTRA_EVENT_TITLE, "Mapped Event");
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.entrantMapView)).check(matches(isDisplayed()));
    }

    /** US 02.05.02 - Configure how many attendees to sample. */
    @Test
    public void us020502_sampleCount_lotteryDrawScreenShowsSpotInput() throws Exception {
        useOrganizerSession();
        String eventId = seedWorkflowEvent("Sample Count Event");

        Intent intent = new Intent(UiTestDataHelper.context(), LotteryDrawActivity.class);
        intent.putExtra("EXTRA_EVENT_ID", eventId);
        intent.putExtra("EXTRA_EVENT_TITLE", "Sample Count Event");
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.numberOfSpotsEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.runLotteryDrawButton)).check(matches(isDisplayed()));
    }

    /** US 02.05.03 - Draw a replacement applicant. */
    @Test
    public void us020503_drawReplacement_lotteryDrawScreenShowsReplacementButton() throws Exception {
        useOrganizerSession();
        String eventId = seedWorkflowEvent("Replacement Applicant Event");

        Intent intent = new Intent(UiTestDataHelper.context(), LotteryDrawActivity.class);
        intent.putExtra("EXTRA_EVENT_ID", eventId);
        intent.putExtra("EXTRA_EVENT_TITLE", "Replacement Applicant Event");
        ActivityScenario.launch(intent);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.drawReplacements)).check(matches(isDisplayed()));
    }

    /** US 02.06.03 - View final list of enrolled entrants. */
    @Test
    public void us020603_viewFinalList_finalEntrantScreenShowsSeededEnrollees() {
        useOrganizerSession();
        Event event = new Event();
        event.setTitle("Final List Event");
        event.setMaxCapacity(10L);
        ArrayList<String> enrolled = new ArrayList<>();
        enrolled.add("entrantA");
        enrolled.add("entrantB");
        event.setEnrolledEntrants(enrolled);

        Intent intent = new Intent(UiTestDataHelper.context(), FinalEntrantListActivity.class);
        intent.putExtra("EXTRA_EVENT", event);
        ActivityScenario.launch(intent);

        onView(withText("entrantA")).check(matches(isDisplayed()));
        onView(withId(R.id.capacityProgressBar)).check(matches(isDisplayed()));
    }

    /** US 02.06.05 - Export final enrolled entrant list as CSV. */
    @Test
    public void us020605_exportFinalList_finalEntrantScreenShowsExportAction() {
        useOrganizerSession();
        Event event = new Event();
        event.setTitle("Export Event");
        event.setMaxCapacity(5L);
        event.setEnrolledEntrants(new ArrayList<>());

        Intent intent = new Intent(UiTestDataHelper.context(), FinalEntrantListActivity.class);
        intent.putExtra("EXTRA_EVENT", event);
        ActivityScenario.launch(intent);

        onView(withId(R.id.exportBtn)).check(matches(isDisplayed()));
    }

    /** US 02.08.01 - View and delete entrant comments on an event. */
    @Test
    public void us020801_manageEntrantComments_eventDetailShowsEntrantCommentTools() throws Exception {
        useOrganizerSession();
        String eventId = seedEntrantEvent("Organizer Comment Review Event");

        ActivityScenario.launch(buildEventDetailIntent(eventId, "Organizer Comment Review Event"));
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.btnViewEntrantComments)).perform(scrollTo());
        onView(withId(R.id.btnViewEntrantComments)).check(matches(isDisplayed()));
    }

    /** US 02.08.02 - Organizer comment on their event. */
    @Test
    public void us020802_organizerComments_eventDetailShowsOwnCommentTools() throws Exception {
        useOrganizerSession();
        String eventId = seedEntrantEvent("Organizer Comment Event");

        ActivityScenario.launch(buildEventDetailIntent(eventId, "Organizer Comment Event"));
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.btnViewMyComments)).perform(scrollTo());
        onView(withId(R.id.btnPostComment)).perform(scrollTo());
        onView(withId(R.id.btnViewMyComments)).check(matches(isDisplayed()));
        onView(withId(R.id.btnPostComment)).check(matches(isDisplayed()));
    }

    /** US 02.09.01 - Assign an entrant as co-organizer. */
    @Test
    public void us020901_assignCoOrganizer_eventDetailShowsAssignmentAction() throws Exception {
        useOrganizerSession();
        String eventId = seedEntrantEvent("Assign Co-Organizer Event");

        ActivityScenario.launch(buildEventDetailIntent(eventId, "Assign Co-Organizer Event"));
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.rowAssignCoOrganizer)).perform(scrollTo());
        onView(withId(R.id.rowAssignCoOrganizer)).check(matches(isDisplayed()));
    }

    /** US 03.01.01 - Remove events as an administrator. */
    @Test
    public void us030101_removeEvents_adminEventDetailShowsDeleteAction() throws Exception {
        useAdminSession();
        String eventId = seedEntrantEvent("Admin Deletable Event");

        ActivityScenario.launch(buildEventDetailIntent(eventId, "Admin Deletable Event"));
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.rowDeleteEvent)).perform(scrollTo());
        onView(withId(R.id.rowDeleteEvent)).check(matches(isDisplayed()));
    }

    /** US 03.02.01 - Remove profiles as an administrator. */
    @Test
    public void us030201_removeProfiles_adminProfilesScreenShowsDeleteButton() throws Exception {
        useAdminSession();
        UiTestDataHelper.seedUser("removable-profile", "Removable Person", "remove@example.com", "7805550101");
        seededUserIds.add("removable-profile");

        ActivityScenario.launch(AdminBrowseProfilesActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.btnDeleteProfileAdmin)).check(matches(isDisplayed()));
    }

    /** US 03.03.01 - Remove images as an administrator. */
    @Test
    public void us030301_removeImages_adminImagesScreenShowsDeleteSelectionAction() throws Exception {
        useAdminSession();
        seedPosterEvent("Poster Removal Event");

        ActivityScenario.launch(AdminBrowseImagesActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.btnSelectDelete)).check(matches(isDisplayed()));
    }

    /** US 03.06.01 - Browse uploaded images as an administrator. */
    @Test
    public void us030601_browseImages_adminImagesScreenShowsPosterCard() throws Exception {
        useAdminSession();
        seedPosterEvent("Poster Browser Event");

        ActivityScenario.launch(AdminBrowseImagesActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Poster Browser Event")).check(matches(isDisplayed()));
    }

    /** US 03.08.01 - Review notification logs sent by organizers. */
    @Test
    public void us030801_notificationLogs_adminNotificationsShowsSeededAuditEntry() throws Exception {
        useAdminSession();
        String eventId = seedEntrantEvent("Logged Event");
        String logId = UiTestDataHelper.seedNotificationLog(
                eventId,
                "Logged Event",
                "Selected",
                "Lottery update",
                "Three entrants were notified."
        );
        seededNotificationLogIds.add(logId);

        ActivityScenario.launch(NotificationsActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withText("Notification Logs")).check(matches(isDisplayed()));
        onView(withText("Logged Event")).check(matches(isDisplayed()));
    }

    /** US 03.09.01 - Admin can also use the app with an admin profile. */
    @Test
    public void us030901_adminProfile_adminSessionStillHasProfileRoute() {
        useAdminSession();

        ActivityScenario.launch(OrganizerActivity.class);
        UiTestDataHelper.waitForUi();

        onView(withId(R.id.navProfile)).check(matches(isDisplayed()));
        onView(withText("Browse Events")).check(matches(isDisplayed()));
    }

    /** US 03.10.01 - Remove event comments that violate policy. */
    @Test
    public void us031001_removeEventComments_adminEventDetailShowsModerationEntryPoint() throws Exception {
        useAdminSession();
        String eventId = seedEntrantEvent("Admin Comment Moderation Event");

        ActivityScenario.launch(buildEventDetailIntent(eventId, "Admin Comment Moderation Event"));
        UiTestDataHelper.waitForUi();

        onView(withText("All Event Comments")).perform(scrollTo());
        onView(withText("All Event Comments")).check(matches(isDisplayed()));
    }

    private void useEntrantSession() {
        UiTestDataHelper.setDeviceId(UiTestDataHelper.TEST_DEVICE_ID);
        UiTestDataHelper.setSessionRole(WelcomeActivity.ROLE_USER);
    }

    private void useOrganizerSession() {
        UiTestDataHelper.setDeviceId("organizer-ui-test");
        UiTestDataHelper.setSessionRole(WelcomeActivity.ROLE_USER);
    }

    private void useAdminSession() {
        UiTestDataHelper.setDeviceId(UiTestDataHelper.TEST_DEVICE_ID);
        UiTestDataHelper.setSessionRole(WelcomeActivity.ROLE_ADMIN);
    }

    private String seedEntrantEvent(String title) throws Exception {
        String eventId = UiTestDataHelper.uniqueId("additional-event");
        UiTestDataHelper.seedEvent(
                eventId,
                title,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                UiTestDataHelper.daysFromToday(-1),
                UiTestDataHelper.daysFromToday(3),
                UiTestDataHelper.daysFromToday(5)
        );
        seededEventIds.add(eventId);
        return eventId;
    }

    private String seedPrivateEvent(String title) throws Exception {
        String eventId = UiTestDataHelper.uniqueId("private-event");
        UiTestDataHelper.seedPrivateEvent(eventId, title);
        seededEventIds.add(eventId);
        return eventId;
    }

    private String seedPrivateInvitation(String title, String status) throws Exception {
        String eventId = seedPrivateEvent(title);
        UiTestDataHelper.seedPrivateInvite(
                eventId,
                UiTestDataHelper.TEST_DEVICE_ID,
                title,
                status
        );
        seededPrivateInviteKeys.add(eventId + "|" + UiTestDataHelper.TEST_DEVICE_ID);
        return eventId;
    }

    private String seedPosterEvent(String title) throws Exception {
        String eventId = UiTestDataHelper.uniqueId("poster-event");
        UiTestDataHelper.seedPosterEvent(eventId, title);
        seededEventIds.add(eventId);
        return eventId;
    }

    private String seedWorkflowEvent(String title) throws Exception {
        String eventId = UiTestDataHelper.uniqueId("workflow-event");
        ArrayList<String> waiting = new ArrayList<>();
        waiting.add("wait-a");
        waiting.add("wait-b");

        ArrayList<String> selected = new ArrayList<>();
        selected.add("selected-a");
        selected.add("selected-b");

        ArrayList<String> enrolled = new ArrayList<>();
        enrolled.add("selected-b");

        ArrayList<String> cancelled = new ArrayList<>();
        cancelled.add("cancelled-a");

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

    private Intent buildEventDetailIntent(String eventId, String title) {
        Intent intent = new Intent(UiTestDataHelper.context(), EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, title);
        return intent;
    }
}
