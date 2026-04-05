package com.example.codebase;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bottom sheet dialog that allows an organizer to compose and send notifications to
 * a selected group of event entrants.
 *
 * <p>Implements the following user stories:
 * <ul>
 *   <li><b>US 02.07.01</b> — Notify waiting-list entrants.</li>
 *   <li><b>US 02.07.02</b> — Notify selected entrants.</li>
 *   <li><b>US 02.07.03</b> — Notify cancelled entrants.</li>
 * </ul>
 *
 * <p>Recipient group availability is gated by event dates:
 * <ul>
 *   <li><b>Waiting list</b> — enabled once the registration start date has passed.</li>
 *   <li><b>Selected / Cancelled</b> — enabled once the draw date has passed.</li>
 * </ul>
 *
 * <p>A default subject and message are pre-filled for each recipient type, but the
 * organizer may override both. Messages are capped at {@link #MAX_MESSAGE_LENGTH}
 * characters, with a live character counter.
 *
 * <p>Firestore document written per recipient:
 * <pre>
 *   notifications/{deviceId}/messages/{autoId}
 *   → { userId, eventId, eventTitle, title, message, status, type, sentAt, read }
 * </pre>
 *
 * <p>Use {@link #newInstance(String, String)} to create an instance with the required
 * arguments.
 */
public class SendNotificationFragment extends BottomSheetDialogFragment {

    /** Fragment argument key for the Firestore event document ID. */
    private static final String ARG_EVENT_ID    = "eventId";

    /** Fragment argument key for the human-readable event title. */
    private static final String ARG_EVENT_TITLE = "eventTitle";

    /** Recipient type constant for the event waiting list. */
    private static final String TYPE_WAITING   = "waitingList";

    /** Recipient type constant for selected (lottery-winning) entrants. */
    private static final String TYPE_SELECTED  = "selectedEntrants";

    /** Recipient type constant for cancelled entrants. */
    private static final String TYPE_CANCELLED = "cancelledEntrants";

    /** Default notification subject for waiting-list recipients. */
    private static final String DEFAULT_SUBJECT_WAITING   = "You're on the waiting list!";

    /** Default notification subject for selected recipients. */
    private static final String DEFAULT_SUBJECT_SELECTED  = "Congratulations! You've been selected!";

    /** Default notification subject for cancelled recipients. */
    private static final String DEFAULT_SUBJECT_CANCELLED = "Your participation has been cancelled";

    /** Maximum allowed length (in characters) for the notification message body. */
    private static final int MAX_MESSAGE_LENGTH = 500;

    /** Firestore document ID of the event for which notifications are being sent. */
    private String eventId;

    /** Human-readable title of the event, used in default message templates. */
    private String eventTitle;

    /** Device IDs of entrants currently on the waiting list. */
    private List<String> waitingList   = new ArrayList<>();

    /** Device IDs of entrants who were selected in the lottery draw. */
    private List<String> selectedList  = new ArrayList<>();

    /** Device IDs of entrants whose participation was cancelled. */
    private List<String> cancelledList = new ArrayList<>();

    /** The recipient type currently selected via the radio buttons; defaults to {@link #TYPE_WAITING}. */
    private String selectedType = TYPE_WAITING;

    /** Radio button for selecting the waiting-list recipient group. */
    private RadioButton rbWaiting;

    /** Radio button for selecting the selected-entrants recipient group. */
    private RadioButton rbSelected;

    /** Radio button for selecting the cancelled-entrants recipient group. */
    private RadioButton rbCancelled;

    /** Displays the recipient count (or availability message) for the waiting list. */
    private TextView tvWaitingCount;

    /** Displays the recipient count (or availability message) for selected entrants. */
    private TextView tvSelectedCount;

    /** Displays the recipient count (or availability message) for cancelled entrants. */
    private TextView tvCancelledCount;

    /** Input field for the notification subject line. */
    private EditText etSubject;

    /** Input field for the notification message body. */
    private EditText etMessage;

    /** Displays the current message length relative to {@link #MAX_MESSAGE_LENGTH}. */
    private TextView tvCharCount;

    /** Dismisses the bottom sheet without sending any notifications. */
    private Button btnCancel;

    /** Validates input and dispatches notifications to all recipients in the selected group. */
    private Button btnSend;

    /** Default label restored after validation or delivery errors. */
    private static final String SEND_BUTTON_TEXT = "Send";

    /**
     * Creates a new {@link SendNotificationFragment} with the required event arguments.
     *
     * @param eventId    The Firestore document ID of the event.
     * @param eventTitle The human-readable event title shown in default message templates.
     * @return A configured {@link SendNotificationFragment} instance.
     */
    public static SendNotificationFragment newInstance(String eventId, String eventTitle) {
        SendNotificationFragment f = new SendNotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_EVENT_TITLE, eventTitle);
        f.setArguments(args);
        return f;
    }

    /**
     * Reads {@link #ARG_EVENT_ID} and {@link #ARG_EVENT_TITLE} from the fragment arguments.
     *
     * @param savedInstanceState Unused.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId    = getArguments().getString(ARG_EVENT_ID);
            eventTitle = getArguments().getString(ARG_EVENT_TITLE);
        }
    }

    /**
     * Inflates the bottom sheet layout.
     *
     * @param inflater           The {@link LayoutInflater} to inflate the view.
     * @param container          The parent view, or {@code null}.
     * @param savedInstanceState Unused.
     * @return The inflated root {@link View}.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.send_notification_fragment, container, false);
    }

    /**
     * Binds views, wires radio button and button listeners, attaches the character-count
     * {@link TextWatcher}, and triggers the initial event data load.
     *
     * @param view               The root view returned by {@link #onCreateView}.
     * @param savedInstanceState Unused.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rbWaiting   = view.findViewById(R.id.waitingListRadioButton);
        rbSelected  = view.findViewById(R.id.selectedRadioButton);
        rbCancelled = view.findViewById(R.id.cancelledRadioButton);

        tvWaitingCount   = view.findViewById(R.id.waitingListDetailsTextView);
        tvSelectedCount  = view.findViewById(R.id.selectedEntrantsDetailsTextView);
        tvCancelledCount = view.findViewById(R.id.cancelledEntrantsDetailsTextView);

        etSubject   = view.findViewById(R.id.subjectEditText);
        etMessage   = view.findViewById(R.id.messageEditText);
        tvCharCount = view.findViewById(R.id.tvCharCount);
        btnCancel   = view.findViewById(R.id.cancellButton);
        btnSend     = view.findViewById(R.id.sendButton);

        loadEventLists();

        rbWaiting.setOnClickListener(v   -> selectType(TYPE_WAITING));
        rbSelected.setOnClickListener(v  -> selectType(TYPE_SELECTED));
        rbCancelled.setOnClickListener(v -> selectType(TYPE_CANCELLED));

        selectType(TYPE_WAITING);

        if (etMessage != null && tvCharCount != null) {
            etMessage.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateCharacterCount(s.length());
                }
            });
        }

        btnCancel.setOnClickListener(v -> dismiss());
        btnSend.setOnClickListener(v   -> sendNotifications());
        updateCharacterCount(etMessage.getText().length());
    }

    /**
     * Fetches the event's entrant lists and date fields from Firestore, then updates
     * the radio button states and recipient counts accordingly.
     *
     * <p>Availability rules applied after the fetch:
     * <ul>
     *   <li>Waiting-list option is enabled only if {@code registrationOpen} is non-null
     *       and is in the past.</li>
     *   <li>Selected and Cancelled options are enabled only if {@code drawDate} is
     *       non-null and is in the past.</li>
     * </ul>
     *
     * <p>The first available option is auto-selected. If no option is available,
     * {@link #btnSend} is disabled with a "Not available yet" label.
     */
    private void loadEventLists() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || !isAdded()) return;

                    waitingList   = toStringList(doc.get("waitingList"));
                    selectedList  = toStringList(doc.get("selectedEntrants"));
                    cancelledList = toStringList(doc.get("cancelledEntrants"));

                    Date today = new Date();

                    // Waiting list — enabled only after registration start date.
                    com.google.firebase.Timestamp regOpenTs = doc.getTimestamp("registrationOpen");
                    Date regOpen = regOpenTs != null ? regOpenTs.toDate() : null;
                    boolean waitingEnabled = regOpen != null && !today.before(regOpen);
                    rbWaiting.setEnabled(waitingEnabled);
                    tvWaitingCount.setText(waitingEnabled
                            ? waitingList.size() + " recipients"
                            : "Available from registration start date");

                    // Selected & Cancelled — enabled only after draw date.
                    com.google.firebase.Timestamp drawTs = doc.getTimestamp("drawDate");
                    Date drawDate = drawTs != null ? drawTs.toDate() : null;
                    boolean drawEnabled = drawDate != null && !today.before(drawDate);
                    rbSelected.setEnabled(drawEnabled);
                    rbCancelled.setEnabled(drawEnabled);
                    tvSelectedCount.setText(drawEnabled
                            ? selectedList.size() + " recipients"
                            : "Available after draw date");
                    tvCancelledCount.setText(drawEnabled
                            ? cancelledList.size() + " recipients"
                            : "Available after draw date");

                    // Auto-select the first available option.
                    if (waitingEnabled) {
                        selectType(TYPE_WAITING);
                    } else if (drawEnabled) {
                        selectType(TYPE_SELECTED);
                    } else {
                        btnSend.setEnabled(false);
                        btnSend.setText("Not available yet");
                    }
                });
    }

    /**
     * Updates {@link #selectedType}, synchronises the radio button checked states, and
     * pre-fills the subject and message fields with the default template for the chosen
     * type — but only if the subject has not been customised by the organizer.
     *
     * <p>The subject is considered uncustomised if it is empty or matches one of the
     * three default subject constants.
     *
     * @param type One of {@link #TYPE_WAITING}, {@link #TYPE_SELECTED}, or
     *             {@link #TYPE_CANCELLED}.
     */
    private void selectType(String type) {
        selectedType = type;

        rbWaiting.setChecked(type.equals(TYPE_WAITING));
        rbSelected.setChecked(type.equals(TYPE_SELECTED));
        rbCancelled.setChecked(type.equals(TYPE_CANCELLED));

        // Pre-fill default message only if subject has not been customised.
        String current = etSubject.getText().toString().trim();
        boolean isDefault = current.isEmpty()
                || current.equals(DEFAULT_SUBJECT_WAITING)
                || current.equals(DEFAULT_SUBJECT_SELECTED)
                || current.equals(DEFAULT_SUBJECT_CANCELLED);

        if (isDefault) {
            switch (type) {
                case TYPE_WAITING:
                    etSubject.setText(DEFAULT_SUBJECT_WAITING);
                    etMessage.setText("You have been added to the waiting list for " + eventTitle
                            + ". We will notify you if a spot becomes available. Stay tuned!");
                    break;
                case TYPE_SELECTED:
                    etSubject.setText(DEFAULT_SUBJECT_SELECTED);
                    etMessage.setText("Congratulations! You have been selected for " + eventTitle
                            + ". Please confirm your spot before the deadline. We look forward to seeing you!");
                    break;
                case TYPE_CANCELLED:
                    etSubject.setText(DEFAULT_SUBJECT_CANCELLED);
                    etMessage.setText("Unfortunately your participation in " + eventTitle
                            + " has been cancelled. This may be due to not winning the lottery, not responding before the deadline, or declining the invitation. We hope to see you at future events!");
                    break;
            }
        }

        updateCharacterCount(etMessage.getText().length());
    }

    /**
     * Validates the composed notification and writes one Firestore document per
     * recipient under {@code notifications/{deviceId}/messages/{autoId}}.
     *
     * <p>Validation rules (evaluated in order):
     * <ol>
     *   <li>Subject must not be empty.</li>
     *   <li>Message must not be empty.</li>
     *   <li>Message must not exceed {@link #MAX_MESSAGE_LENGTH} characters.</li>
     *   <li>The selected recipient list must contain at least one device ID.</li>
     * </ol>
     *
     * <p>While sending is in progress, {@link #btnSend} is disabled and labelled
     * "Sending…". On complete success a {@link Toast} confirms the count of recipients
     * and the sheet is dismissed. On any single failure, the button is re-enabled and
     * a {@link Toast} error is shown; already-sent documents are not rolled back.
     */
    private void sendNotifications() {
        String subject = etSubject.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (subject.isEmpty()) { etSubject.setError("Subject is required");  return; }
        if (message.isEmpty()) { etMessage.setError("Message is required");  return; }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            etMessage.setError("Message must be 500 characters or less");
            return;
        }

        List<String> recipients;
        switch (selectedType) {
            case TYPE_SELECTED:  recipients = selectedList;  break;
            case TYPE_CANCELLED: recipients = cancelledList; break;
            default:             recipients = waitingList;   break;
        }

        if (recipients == null || recipients.isEmpty()) {
            Toast.makeText(getContext(), "No recipients in this list", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);
        btnSend.setText("Sending...");

        // Respect entrant opt-out preferences before writing notification records.
        NotificationPreferenceHelper.resolveEnabledRecipients(recipients,
                (enabledRecipients, optedOutCount) -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (enabledRecipients.isEmpty()) {
                        resetSendButton();
                        Toast.makeText(getContext(),
                                "All recipients have opted out of notifications",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    dispatchNotifications(enabledRecipients, subject, message, optedOutCount);
                });
    }

    /**
     * Writes one Firestore notification document per eligible recipient after opt-out
     * preferences have been applied.
     *
     * @param recipients recipients who still allow notifications
     * @param subject notification title
     * @param message notification body
     * @param optedOutCount number of recipients skipped because they opted out
     */
    private void dispatchNotifications(List<String> recipients,
                                       String subject,
                                       String message,
                                       int optedOutCount) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        int total = recipients.size();
        int[] sent = {0};
        boolean[] failed = {false};

        for (String deviceId : recipients) {
            Map<String, Object> notif = new HashMap<>();
            notif.put("userId",     deviceId);
            notif.put("eventId",    eventId);
            notif.put("eventTitle", eventTitle);
            notif.put("title",      subject);
            notif.put("message",    message);
            notif.put("status",     getStatusLabel(selectedType));
            notif.put("type",       selectedType);
            notif.put("sentAt",     new Timestamp(new Date()));
            notif.put("read",       false);

            db.collection("notifications")
                    .document(deviceId)
                    .collection("messages")
                    .add(notif)
                    .addOnSuccessListener(ref -> {
                        if (failed[0]) {
                            return;
                        }

                        sent[0]++;
                        if (sent[0] == total && isAdded()) {
                            String messageText = optedOutCount > 0
                                    ? "Sent to " + total + " recipients. " + optedOutCount + " opted out."
                                    : "Sent to " + total + " recipients";
                            Toast.makeText(getContext(), messageText, Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || failed[0]) {
                            return;
                        }
                        failed[0] = true;
                        resetSendButton();
                        Toast.makeText(getContext(),
                                "Failed to send some notifications",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /** Restores the send button after the compose flow is interrupted by an error. */
    private void resetSendButton() {
        btnSend.setEnabled(true);
        btnSend.setText(SEND_BUTTON_TEXT);
    }

    /**
     * Safely converts a Firestore field value to a {@link List} of {@link String}s.
     *
     * <p>Non-string items within the list are silently skipped. Returns an empty list
     * if {@code obj} is {@code null} or not a {@link List}.
     *
     * @param obj The raw Firestore field value to convert.
     * @return A non-null {@link List} containing only the {@link String} elements.
     */
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

    /**
     * Returns the human-readable status label corresponding to the given recipient type.
     *
     * @param type One of {@link #TYPE_WAITING}, {@link #TYPE_SELECTED}, or
     *             {@link #TYPE_CANCELLED}.
     * @return {@code "Selected"}, {@code "Cancelled"}, or {@code "Waiting List"}.
     */
    private String getStatusLabel(String type) {
        if (TYPE_SELECTED.equals(type))  return "Selected";
        if (TYPE_CANCELLED.equals(type)) return "Cancelled";
        return "Waiting List";
    }

    /**
     * Updates {@link #tvCharCount} to reflect the current message length relative to
     * {@link #MAX_MESSAGE_LENGTH} (e.g. {@code "120/500"}).
     *
     * @param currentLength The current character count of the message field.
     */
    private void updateCharacterCount(int currentLength) {
        if (tvCharCount != null) {
            tvCharCount.setText(currentLength + "/" + MAX_MESSAGE_LENGTH);
        }
    }
}
