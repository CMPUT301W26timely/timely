package com.example.codebase;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Instrumentation coverage for organizer event-creation and configuration stories.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerCreationUiStoriesTest {

    @Before
    public void setUp() {
        UiTestDataHelper.setDeviceId(UiTestDataHelper.TEST_DEVICE_ID);
    }

    /** US 02.01.01 - Create event with QR code. */
    @Test
    public void us020101_createEventWizard_mentionsQrGenerationDuringPublishFlow() {
        ActivityScenario.launch(CreateEventActivity.class);

        onView(withId(R.id.btnContinue)).perform(click());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withText("A unique QR code will be generated when you publish."))
                .check(matches(isDisplayed()));
    }

    /** US 02.01.04 - Set registration period. */
    @Test
    public void us020104_registrationPeriod_scheduleStepShowsRegistrationFields() {
        ActivityScenario.launch(CreateEventActivity.class);

        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.etRegOpen)).check(matches(isDisplayed()));
        onView(withId(R.id.etRegClose)).check(matches(isDisplayed()));
    }

    /** US 02.02.03 - Enable or disable geolocation. */
    @Test
    public void us020203_geolocation_settingsStepShowsGeoToggle() {
        ActivityScenario.launch(CreateEventActivity.class);

        onView(withId(R.id.btnContinue)).perform(click());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.swGeoRequired)).check(matches(isDisplayed()));
    }

    /** US 02.03.01 - Optional waiting-list limit. */
    @Test
    public void us020301_waitlistLimit_settingsStepShowsWaitlistToggle() {
        ActivityScenario.launch(CreateEventActivity.class);

        onView(withId(R.id.btnContinue)).perform(click());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.swLimitWaitlist)).check(matches(isDisplayed()));
    }

    /** US 02.04.01 - Upload event poster. */
    @Test
    public void us020401_uploadPoster_basicsStepShowsPosterUploadArea() {
        ActivityScenario.launch(CreateEventActivity.class);

        onView(withId(R.id.posterUploadArea)).check(matches(isDisplayed()));
    }

    /** US 02.04.02 - Update event poster. */
    @Test
    public void us020402_updatePoster_editModeShowsEditableWizard() {
        Intent intent = new Intent(UiTestDataHelper.context(), CreateEventActivity.class);
        intent.putExtra("isEditMode", true);
        intent.putExtra("editingEventId", "event-edit-1");
        intent.putExtra("name", "Edit Poster Event");
        intent.putExtra("description", "Event already exists.");
        intent.putExtra("location", "South Hall");

        ActivityScenario.launch(intent);

        onView(withId(R.id.posterUploadArea)).check(matches(isDisplayed()));
        onView(withId(R.id.etEventName)).check(matches(withText("Edit Poster Event")));
        onView(withId(R.id.etDescription)).check(matches(withText("Event already exists.")));
    }
}
