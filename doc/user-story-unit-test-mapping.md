# User Story Unit Test Mapping

This project uses local JUnit 4 tests in `CodeBase/app/src/test/java/com/example/codebase`.

The attached JUnit notes emphasize:

- isolated tests
- happy-path coverage
- alternative and boundary-path coverage
- `@Before` setup for shared fixtures
- `@Ignore` for cases that are not unit-testable yet

The mapping below shows where each requested story is covered.

| User Story | Primary JUnit Coverage | Notes |
| --- | --- | --- |
| US 01.01.01 Join waiting list | `EntrantUserStoriesTest.us010101_joinWaitingList_addsEntrantWhenEventIsOpen` and `EntrantUserStoriesTest.us010101_joinWaitingList_preventsDuplicateJoining` | Covers join rules and duplicate prevention. Firestore write itself still needs integration testing. |
| US 01.01.02 Leave waiting list | `EntrantUserStoriesTest.us010102_leaveWaitingList_removesEntrantAndUpdatesList` | Covers removal and updated waiting list count. |
| US 01.01.03 Browse events | `EntrantUserStoriesTest.us010103_browseEvents_onlyShowsActiveEvents`, `EntrantUserStoriesTest.us010103_browseEvents_exposesNameDateAndLocation` | Activity launch on click is marked with `@Ignore` because it needs Android instrumentation. |
| US 01.02.02 Update profile information | `ProfileAndIdentityUserStoriesTest.us010202_updateProfile_acceptsEditedNameEmailAndPhone`, `ProfileAndIdentityUserStoriesTest.us010202_updateProfile_displaysUpdatedDataCorrectly`, `ProfileInputValidatorTest` | Covers edit validation and displayed profile state. Firestore persistence is outside local-unit scope. |
| US 01.04.01 Notification when chosen | `ProfileAndIdentityUserStoriesTest.us010401_notificationWhenChosen_usesSelectedStatusAndEventLink`, `SelectedNotificationCheckerTest` | Event-page navigation from the notification is marked with `@Ignore` because it needs Android instrumentation. |
| US 01.04.02 Notification when not chosen | `ProfileAndIdentityUserStoriesTest.us010402_notificationWhenNotChosen_usesNotSelectedStatusAndEventLink`, `SelectedNotificationCheckerTest` | Covers status and event-link data carried by the notification model. |
| US 01.05.02 Accept invitation | `EntrantUserStoriesTest.us010502_acceptInvitation_changesStatusToEnrolled` | Uses the real `Invitations` production class. |
| US 01.05.03 Decline invitation | `EntrantUserStoriesTest.us010503_declineInvitation_changesStatusToCancelled` | Replacement draw is marked with `@Ignore` because the production flow does not implement it yet. |
| US 01.07.01 Device-based authentication | `ProfileAndIdentityUserStoriesTest.us010701_deviceBasedAuthentication_defaultsToEntrantWithDeviceId`, `ProfileAndIdentityUserStoriesTest.us010701_deviceBasedAuthentication_shortensDeviceIdForDisplay`, `SplashActivityTest` | SharedPreferences persistence is marked with `@Ignore` because it needs Android context or instrumentation. |
| US 02.01.01 Create event with QR code | `OrganizerCreationUserStoriesTest.us020101_createEvent_buildsEventDetailsAndQrPayload` | Firestore save and QR bitmap rendering are marked with `@Ignore` because they need integration/UI coverage. |
| US 02.01.04 Set registration period | `OrganizerCreationUserStoriesTest.us020104_setRegistrationPeriod_savesStartAndEndDates`, `OrganizerCreationUserStoriesTest.us020104_registrationOnlyWorksInsideConfiguredPeriod`, `EventDetailActivityTest` | Covers saved date fields and registration-window rules. |
| US 02.03.01 Limit waiting list size | `OrganizerCreationUserStoriesTest.us020301_limitWaitingListSize_blocksNewJoinWhenFull` | Covers limit enforcement in isolated business logic. |
| US 02.02.03 Enable/disable geolocation | `OrganizerCreationUserStoriesTest.us020203_enableDisableGeolocation_persistsToggleOnEvent` | Location-capture enforcement is marked with `@Ignore` because the current branch only stores the toggle. |
| US 02.04.01 Upload event poster | `OrganizerCreationUserStoriesTest.us020401_uploadPoster_attachesPosterToEvent` | Covers poster data being attached to the event model. |
| US 02.04.02 Update event poster | `OrganizerCreationUserStoriesTest.us020402_updatePoster_replacesExistingPoster` | Covers poster replacement in event-edit logic. |
| US 02.05.01 Notify chosen entrants | `OrganizerWorkflowUserStoriesTest.us020501_notifyChosenEntrants_createsLoggableSelectedNotificationRecord`, `SendNotificationFragmentTest` | Firestore delivery/log persistence still needs integration coverage. |
| US 02.06.01 View selected/invited entrants | `OrganizerWorkflowUserStoriesTest.us020601_viewSelectedEntrants_displaysChosenEntrantsByStatus`, `InvitedEntrantsActivityTest` | Covers accepted, pending, and declined grouping. |
| US 02.06.02 View cancelled entrants | `OrganizerWorkflowUserStoriesTest.us020602_viewCancelledEntrants_displaysCancelledAndDeclinedUsers`, `CancelledEntrantsActivityTest` | Covers declined versus organizer-cancelled grouping. |
| US 02.06.04 Cancel entrant | `OrganizerWorkflowUserStoriesTest.us020604_cancelEntrant_movesUserFromSelectedToCancelled`, `InvitedEntrantsActivityTest` | Covers selected-to-cancelled transition. |
| US 02.07.01 Send notifications to waiting list | `OrganizerWorkflowUserStoriesTest.us020701_sendNotificationsToWaitingList_targetsWaitingEntrants`, `SendNotificationFragmentTest` | Covers recipient targeting and message validation rules. |
| US 02.07.02 Send notifications to selected entrants | `OrganizerWorkflowUserStoriesTest.us020702_sendNotificationsToSelectedEntrants_targetsSelectedEntrants`, `SendNotificationFragmentTest` | Covers recipient targeting and selected-status labeling. |
| US 02.07.03 Send notifications to cancelled entrants | `OrganizerWorkflowUserStoriesTest.us020703_sendNotificationsToCancelledEntrants_targetsCancelledEntrants`, `SendNotificationFragmentTest` | Covers recipient targeting and cancelled-status labeling. |

## How These Tests Are Written

1. Use `@Before` to build shared fixtures such as `Event`, `User`, or `CreateEventViewModel`.
2. Keep each `@Test` focused on one business rule or one acceptance criterion.
3. Use local helper methods to isolate logic that would otherwise require Firebase or Android UI.
4. Mark gaps honestly with `@Ignore` when a criterion currently needs instrumentation or missing production logic.
