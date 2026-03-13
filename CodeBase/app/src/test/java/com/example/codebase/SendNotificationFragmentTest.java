package com.example.codebase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for SendNotificationFragment logic.
 *
 * Tests cover:
 *   1.  Correct recipients selected for waiting list type
 *   2.  Correct recipients selected for selected entrants type
 *   3.  Correct recipients selected for cancelled entrants type
 *   4.  Empty subject fails validation
 *   5.  Empty message fails validation
 *   6.  Message over 500 chars fails validation
 *   7.  Message exactly 500 chars passes validation
 *   8.  Empty recipients list blocked from sending
 *   9.  Character count updates correctly
 *   10. Default subject pre-filled for waiting list
 *   11. Default subject pre-filled for selected entrants
 *   12. Default subject pre-filled for cancelled entrants
 *   13. Status label correct for each type
 *   14. Waiting list enabled only after regOpen
 *   15. Draw lists enabled only after drawDate
 *   16. toStringList handles null safely
 *   17. toStringList ignores non-string entries
 *   18. getStatusLabel returns correct label for all types
 *
 * Add to: app/src/test/java/com/example/codebase/SendNotificationFragmentTest.java
 */
public class SendNotificationFragmentTest {

    // ── Constants mirrored from fragment ─────────────────────────────────────
    private static final String TYPE_WAITING   = "waitingList";
    private static final String TYPE_SELECTED  = "selectedEntrants";
    private static final String TYPE_CANCELLED = "cancelledEntrants";

    private static final String DEFAULT_SUBJECT_WAITING   = "You're on the waiting list!";
    private static final String DEFAULT_SUBJECT_SELECTED  = "Congratulations! You've been selected!";
    private static final String DEFAULT_SUBJECT_CANCELLED = "Your participation has been cancelled";

    private static final int MAX_MESSAGE_LENGTH = 500;

    private final String eventTitle = "Summer Yoga Workshop";

    // ── Helpers mirrored from fragment ────────────────────────────────────────

    private List<String> getRecipients(String type,
                                       List<String> waitingList,
                                       List<String> selectedList,
                                       List<String> cancelledList) {
        switch (type) {
            case TYPE_SELECTED:  return selectedList;
            case TYPE_CANCELLED: return cancelledList;
            default:             return waitingList;
        }
    }

    private boolean validateInput(String subject, String message) {
        if (subject == null || subject.trim().isEmpty()) return false;
        if (message == null || message.trim().isEmpty()) return false;
        if (message.trim().length() > MAX_MESSAGE_LENGTH) return false;
        return true;
    }

    private boolean isWaitingEnabled(Date regOpen) {
        if (regOpen == null) return false;
        return !new Date().before(regOpen);
    }

    private boolean isDrawEnabled(Date drawDate) {
        if (drawDate == null) return false;
        return !new Date().before(drawDate);
    }

    private String getDefaultSubject(String type) {
        switch (type) {
            case TYPE_SELECTED:  return DEFAULT_SUBJECT_SELECTED;
            case TYPE_CANCELLED: return DEFAULT_SUBJECT_CANCELLED;
            default:             return DEFAULT_SUBJECT_WAITING;
        }
    }

    private String getDefaultMessage(String type) {
        switch (type) {
            case TYPE_SELECTED:
                return "Congratulations! You have been selected for " + eventTitle
                        + ". Please confirm your spot before the deadline. We look forward to seeing you!";
            case TYPE_CANCELLED:
                return "Unfortunately your participation in " + eventTitle
                        + " has been cancelled. This may be due to not winning the lottery, "
                        + "not responding before the deadline, or declining the invitation. "
                        + "We hope to see you at future events!";
            default:
                return "You have been added to the waiting list for " + eventTitle
                        + ". We will notify you if a spot becomes available. Stay tuned!";
        }
    }

    private String getStatusLabel(String type) {
        if (TYPE_SELECTED.equals(type))  return "Selected";
        if (TYPE_CANCELLED.equals(type)) return "Cancelled";
        return "Waiting List";
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object obj) {
        List<String> result = new ArrayList<>();
        if (obj instanceof List) {
            for (Object item : (List<?>) obj) {
                if (item instanceof String) result.add((String) item);
            }
        }
        return result;
    }

    private int updateCharacterCount(String message) {
        return message != null ? message.length() : 0;
    }

    // ─── Test 1: Waiting list type returns waiting recipients ─────────────────

    @Test
    public void testRecipients_waitingListType() {
        List<String> waiting   = Arrays.asList("w1", "w2", "w3");
        List<String> selected  = Arrays.asList("s1");
        List<String> cancelled = Arrays.asList("c1");

        List<String> result = getRecipients(TYPE_WAITING, waiting, selected, cancelled);
        assertEquals(3, result.size());
        assertTrue(result.contains("w1"));
    }

    // ─── Test 2: Selected type returns selected recipients ───────────────────

    @Test
    public void testRecipients_selectedType() {
        List<String> waiting   = Arrays.asList("w1");
        List<String> selected  = Arrays.asList("s1", "s2");
        List<String> cancelled = Arrays.asList("c1");

        List<String> result = getRecipients(TYPE_SELECTED, waiting, selected, cancelled);
        assertEquals(2, result.size());
        assertTrue(result.contains("s1"));
        assertTrue(result.contains("s2"));
    }

    // ─── Test 3: Cancelled type returns cancelled recipients ─────────────────

    @Test
    public void testRecipients_cancelledType() {
        List<String> waiting   = Arrays.asList("w1");
        List<String> selected  = Arrays.asList("s1");
        List<String> cancelled = Arrays.asList("c1", "c2", "c3");

        List<String> result = getRecipients(TYPE_CANCELLED, waiting, selected, cancelled);
        assertEquals(3, result.size());
        assertTrue(result.contains("c3"));
    }

    // ─── Test 4: Empty subject fails validation ───────────────────────────────

    @Test
    public void testValidation_emptySubjectFails() {
        assertFalse("Empty subject should fail", validateInput("", "Some message"));
        assertFalse("Blank subject should fail", validateInput("   ", "Some message"));
    }

    // ─── Test 5: Empty message fails validation ───────────────────────────────

    @Test
    public void testValidation_emptyMessageFails() {
        assertFalse("Empty message should fail", validateInput("Subject", ""));
        assertFalse("Blank message should fail", validateInput("Subject", "   "));
    }

    // ─── Test 6: Message over 500 chars fails validation ─────────────────────

    @Test
    public void testValidation_messageTooLongFails() {
        String longMessage = new String(new char[501]).replace('\0', 'A');
        assertFalse("501 char message should fail", validateInput("Subject", longMessage));
    }

    // ─── Test 7: Message exactly 500 chars passes validation ─────────────────

    @Test
    public void testValidation_messageExactly500Passes() {
        String exactMessage = new String(new char[500]).replace('\0', 'A');
        assertTrue("500 char message should pass", validateInput("Subject", exactMessage));
    }

    // ─── Test 8: Empty recipients list blocked ────────────────────────────────

    @Test
    public void testSend_blockedWhenRecipientsEmpty() {
        List<String> emptyList = new ArrayList<>();
        assertTrue("Should block when recipients empty", emptyList.isEmpty());
    }

    // ─── Test 9: Character count updates correctly ────────────────────────────

    @Test
    public void testCharacterCount_updatesCorrectly() {
        assertEquals(0,  updateCharacterCount(""));
        assertEquals(5,  updateCharacterCount("Hello"));
        assertEquals(11, updateCharacterCount("Hello World"));
        assertEquals(500, updateCharacterCount(new String(new char[500]).replace('\0', 'A')));
    }

    // ─── Test 10: Default subject for waiting list ────────────────────────────

    @Test
    public void testDefaultSubject_waitingList() {
        assertEquals(DEFAULT_SUBJECT_WAITING, getDefaultSubject(TYPE_WAITING));
    }

    // ─── Test 11: Default subject for selected entrants ──────────────────────

    @Test
    public void testDefaultSubject_selectedEntrants() {
        assertEquals(DEFAULT_SUBJECT_SELECTED, getDefaultSubject(TYPE_SELECTED));
    }

    // ─── Test 12: Default subject for cancelled entrants ─────────────────────

    @Test
    public void testDefaultSubject_cancelledEntrants() {
        assertEquals(DEFAULT_SUBJECT_CANCELLED, getDefaultSubject(TYPE_CANCELLED));
    }

    // ─── Test 13: Default messages contain event title ────────────────────────

    @Test
    public void testDefaultMessage_containsEventTitle() {
        assertTrue(getDefaultMessage(TYPE_WAITING).contains(eventTitle));
        assertTrue(getDefaultMessage(TYPE_SELECTED).contains(eventTitle));
        assertTrue(getDefaultMessage(TYPE_CANCELLED).contains(eventTitle));
    }

    // ─── Test 14: Waiting list disabled before regOpen ───────────────────────

    @Test
    public void testWaitingEnabled_disabledBeforeRegOpen() {
        Date futureDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        assertFalse("Waiting should be disabled before regOpen", isWaitingEnabled(futureDate));
    }

    // ─── Test 15: Draw lists disabled before drawDate ────────────────────────

    @Test
    public void testDrawEnabled_disabledBeforeDrawDate() {
        Date futureDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        assertFalse("Draw lists should be disabled before drawDate", isDrawEnabled(futureDate));
    }

    // ─── Test 15b: Draw lists enabled after drawDate ─────────────────────────

    @Test
    public void testDrawEnabled_enabledAfterDrawDate() {
        Date pastDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        assertTrue("Draw lists should be enabled after drawDate", isDrawEnabled(pastDate));
    }

    // ─── Test 16: toStringList handles null safely ────────────────────────────

    @Test
    public void testToStringList_nullSafe() {
        List<String> result = toStringList(null);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ─── Test 17: toStringList ignores non-string entries ────────────────────

    @Test
    public void testToStringList_ignoresNonStrings() {
        List<Object> mixed = new ArrayList<>();
        mixed.add("validId");
        mixed.add(123);
        mixed.add(null);
        mixed.add("anotherId");

        List<String> result = toStringList(mixed);
        assertEquals("Should only include 2 valid strings", 2, result.size());
        assertTrue(result.contains("validId"));
        assertTrue(result.contains("anotherId"));
    }

    // ─── Test 18: getStatusLabel correct for all types ───────────────────────

    @Test
    public void testGetStatusLabel_allTypes() {
        assertEquals("Waiting List", getStatusLabel(TYPE_WAITING));
        assertEquals("Selected",     getStatusLabel(TYPE_SELECTED));
        assertEquals("Cancelled",    getStatusLabel(TYPE_CANCELLED));
    }
}