package com.example.codebase;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.intent.rule.IntentsRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * Consolidated UI and Intent tests for the Entrant role.
 * Covers Waiting List, Profile, Notifications, and Invitations.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EntrantFunctionalityTest {

    @Rule
    public ActivityScenarioRule<WelcomeActivity> activityRule =
            new ActivityScenarioRule<>(WelcomeActivity.class);

    @Rule
    public IntentsRule intentsRule = new IntentsRule();

    @Test
    public void test0_CreateProfile() {
        // Choose Entrant Role
        onView(withId(R.id.btnEntrant)).perform(click());
        
        // Wait for SplashActivity to finish its registration check and decide whether to redirect
        waitFor(5000); 

        // Check if we are on the mandatory Profile Settings screen (First Run)
        boolean onSettings = false;
        try {
            onView(withId(R.id.editTextName)).check(matches(isDisplayed()));
            onSettings = true;
        } catch (NoMatchingViewException ignored) {}

        if (!onSettings) {
            // If not automatically redirected, go to Profile -> Edit manually
            onView(withId(R.id.navProfile)).perform(click());
            waitFor(1000);
            onView(withId(R.id.btnEditProfile)).perform(click());
            waitFor(1000);
        }

        // Fill in details using replaceText (more reliable for tests)
        onView(withId(R.id.editTextName)).perform(replaceText("Initial Entrant"), closeSoftKeyboard());
        onView(withId(R.id.editTextEmail)).perform(replaceText("initial@example.com"), closeSoftKeyboard());
        onView(withId(R.id.editTextPhone)).perform(replaceText("1234567890"), closeSoftKeyboard());
        
        // Click the Save/Continue button
        onView(withId(R.id.buttonSaveChanges)).perform(click());
        
        // After saving on first run, it takes you to OrganizerActivity (main) or Profile summary
        waitFor(3000); 

        // Verify registration was successful by checking profile tab
        onView(withId(R.id.navProfile)).perform(click());
        waitFor(1000);
        onView(withId(R.id.tvName)).check(matches(withText("Initial Entrant")));
        onView(withId(R.id.tvEmail)).check(matches(withText("initial@example.com")));
    }

    private void enterAppAsEntrant() {
        onView(withId(R.id.btnEntrant)).perform(click());
        waitFor(3000);
    }

    @Test
    public void test1_JoinWaitingList() {
        enterAppAsEntrant();
        onView(withId(R.id.navExplore)).perform(click());
        waitFor(2000);

        try {
            onView(withText("Swimming Lessons")).perform(click());
            waitFor(1500);

            onView(withId(R.id.btnJoinWaitingList)).perform(click());
            waitFor(500);
            onView(withText("Confirm")).inRoot(isDialog()).perform(click());
            waitFor(2000);

            onView(withId(R.id.btnJoinWaitingList)).check(matches(not(isEnabled())));
            onView(withId(R.id.btnLeaveWaitingList)).check(matches(isEnabled()));
        } catch (NoMatchingViewException e) {
            Assert.fail("Test failed: 'Swimming Lessons' event not found in list.");
        }
    }

    @Test
    public void test2_LeaveWaitingList() {
        enterAppAsEntrant();
        onView(withId(R.id.navExplore)).perform(click());
        waitFor(2000);

        try {
            onView(withText("Swimming Lessons")).perform(click());
            waitFor(1500);

            onView(withId(R.id.btnLeaveWaitingList)).perform(click());
            waitFor(500);
            onView(withText("Confirm")).inRoot(isDialog()).perform(click());
            waitFor(2000);

            onView(withId(R.id.btnLeaveWaitingList)).check(matches(not(isEnabled())));
            onView(withId(R.id.btnJoinWaitingList)).check(matches(isEnabled()));
        } catch (NoMatchingViewException e) {
            Assert.fail("Test failed: 'Swimming Lessons' event not found in list.");
        }
    }

    @Test
    public void test3_UpdateProfileInformation() {
        enterAppAsEntrant();
        onView(withId(R.id.navProfile)).perform(click());
        waitFor(1000);
        onView(withId(R.id.btnEditProfile)).perform(click());

        onView(withId(R.id.editTextName)).perform(replaceText("Espresso User"), closeSoftKeyboard());
        onView(withId(R.id.editTextEmail)).perform(replaceText("espresso@test.com"), closeSoftKeyboard());
        onView(withId(R.id.buttonSaveChanges)).perform(click());
        waitFor(3000);

        onView(withId(R.id.tvName)).check(matches(withText("Espresso User")));
        onView(withId(R.id.tvEmail)).check(matches(withText("espresso@test.com")));
    }

    @Test
    public void test4_AcceptInvitation() {
        enterAppAsEntrant();
        onView(withId(R.id.navNotifications)).perform(click());
        waitFor(2000);

        try {
            onView(withId(R.id.invitationSummaryInclude)).perform(click());
            waitFor(1500);
            onView(withId(R.id.btnExploreAccept)).perform(click());
            waitFor(2000);
        } catch (Exception | AssertionError e) {
            Assert.fail("Accept Invitation flow failed.");
        }
    }

    @Test
    public void test5_DeclineInvitation() {
        enterAppAsEntrant();
        onView(withId(R.id.navNotifications)).perform(click());
        waitFor(2000);

        try {
            onView(withId(R.id.invitationSummaryInclude)).perform(click());
            waitFor(1500);
            onView(withId(R.id.btnExploreDecline)).perform(click());
            waitFor(2000);
        } catch (Exception | AssertionError e) {
            Assert.fail("Decline Invitation flow failed.");
        }
    }

    private void waitFor(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { e.printStackTrace(); }
    }
}
