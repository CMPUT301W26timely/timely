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
 * SendNotificationFragment — Bottom sheet dialog for organizer to send notifications.
 *
 * US 02.07.01 — Notify waiting list entrants
 * US 02.07.02 — Notify selected entrants
 * US 02.07.03 — Notify cancelled entrants
 *
 * Pre-fills a default message per recipient type.
 * Organizer can override the subject and message before sending.
 *
 * Firestore structure written:
 *   notifications/{deviceId}/messages/{autoId} → { eventId, eventTitle, title, message, type, sentAt, read }
 */
public class SendNotificationFragment extends BottomSheetDialogFragment {

    private static final String ARG_EVENT_ID    = "eventId";
    private static final String ARG_EVENT_TITLE = "eventTitle";

    private static final String TYPE_WAITING   = "waitingList";
    private static final String TYPE_SELECTED  = "selectedEntrants";
    private static final String TYPE_CANCELLED = "cancelledEntrants";

    private static final String DEFAULT_SUBJECT_WAITING   = "You're on the waiting list!";
    private static final String DEFAULT_SUBJECT_SELECTED  = "Congratulations! You've been selected!";
    private static final String DEFAULT_SUBJECT_CANCELLED = "Your participation has been cancelled";

    private String eventId;
    private String eventTitle;

    private List<String> waitingList   = new ArrayList<>();
    private List<String> selectedList  = new ArrayList<>();
    private List<String> cancelledList = new ArrayList<>();

    private String selectedType = TYPE_WAITING;

    private RadioButton rbWaiting, rbSelected, rbCancelled;
    private TextView tvWaitingCount, tvSelectedCount, tvCancelledCount;
    private EditText etSubject, etMessage;
    private TextView tvCharCount;
    private Button btnCancel, btnSend;

    public static SendNotificationFragment newInstance(String eventId, String eventTitle) {
        SendNotificationFragment f = new SendNotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_EVENT_TITLE, eventTitle);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId    = getArguments().getString(ARG_EVENT_ID);
            eventTitle = getArguments().getString(ARG_EVENT_TITLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.send_notification_fragment, container, false);
    }

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
                    tvCharCount.setText(s.length() + "/500");
                }
            });
        }

        btnCancel.setOnClickListener(v -> dismiss());
        btnSend.setOnClickListener(v   -> sendNotifications());
    }

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

                    // Waiting list — enabled only after registration start date
                    com.google.firebase.Timestamp regOpenTs = doc.getTimestamp("registrationOpen");
                    Date regOpen = regOpenTs != null ? regOpenTs.toDate() : null;
                    boolean waitingEnabled = regOpen != null && !today.before(regOpen);
                    rbWaiting.setEnabled(waitingEnabled);
                    tvWaitingCount.setText(waitingEnabled
                            ? waitingList.size() + " recipients"
                            : "Available from registration start date");

                    // Selected & Cancelled — enabled only after draw date
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

                    // Auto-select first available option
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

    private void selectType(String type) {
        selectedType = type;

        rbWaiting.setChecked(type.equals(TYPE_WAITING));
        rbSelected.setChecked(type.equals(TYPE_SELECTED));
        rbCancelled.setChecked(type.equals(TYPE_CANCELLED));

        // Pre-fill default message only if subject hasn't been customised
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
    }

    private void sendNotifications() {
        String subject = etSubject.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (subject.isEmpty()) { etSubject.setError("Subject is required");  return; }
        if (message.isEmpty()) { etMessage.setError("Message is required");  return; }

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

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        int total    = recipients.size();
        int[] sent   = {0};

        for (String deviceId : recipients) {
            Map<String, Object> notif = new HashMap<>();
            notif.put("eventId",    eventId);
            notif.put("eventTitle", eventTitle);
            notif.put("title",      subject);
            notif.put("message",    message);
            notif.put("type",       selectedType);
            notif.put("sentAt",     new Timestamp(new Date()));
            notif.put("read",       false);

            // notifications/{deviceId}/messages/{autoId}
            db.collection("notifications")
                    .document(deviceId)
                    .collection("messages")
                    .add(notif)
                    .addOnSuccessListener(ref -> {
                        sent[0]++;
                        if (sent[0] == total && isAdded()) {
                            Toast.makeText(getContext(),
                                    "Sent to " + total + " recipients",
                                    Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        btnSend.setEnabled(true);
                        btnSend.setText("Send Notification");
                        Toast.makeText(getContext(),
                                "Failed to send some notifications",
                                Toast.LENGTH_SHORT).show();
                    });
        }
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
}