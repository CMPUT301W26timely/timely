package com.example.codebase;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A dialog fragment that allows an organizer to assign exactly one entrant as a
 * co-organizer for their event (US 02.09.01).
 *
 * <p>Rules enforced:
 * <ul>
 *   <li>Only one co-organizer is allowed per event. If one is already assigned,
 *       a warning is shown and no further assignment is possible.</li>
 *   <li>The selected entrant is added to {@code coOrganizers} and removed from
 *       all entrant pools.</li>
 *   <li>A notification is sent to the assigned entrant (US 01.09.01).</li>
 * </ul>
 *
 * <p>Call {@link #setDismissListener(Runnable)} before showing so the caller
 * can reload data when the dialog closes.
 */
public class AssignCoOrganizerFragment extends DialogFragment {

    private static final String ARG_EVENT_ID    = "event_id";
    private static final String ARG_EVENT_TITLE = "event_title";
    private static final String NOTIF_TYPE      = "coOrganizer";
    private static final String NOTIF_STATUS    = "Co-Organizer";

    // Views
    private TextView    tvDialogEventTitle;
    private ProgressBar progressBar;
    private TextView    tvEmpty;
    private EditText    etSearch;
    private RadioGroup  radioGroup;
    private Button      btnAssign;
    private Button      btnCancel;

    // State
    private String eventId;
    private String eventTitle;
    private FirebaseFirestore db;

    /** Called when the dialog is dismissed for any reason. */
    private Runnable dismissListener;

    /** Parallel lists — index N in names = index N in deviceIds. */
    private final List<String> entrantNames     = new ArrayList<>();
    private final List<String> entrantDeviceIds = new ArrayList<>();

    // ── Factory ───────────────────────────────────────────────────────────────

    public static AssignCoOrganizerFragment newInstance(String eventId, String eventTitle) {
        AssignCoOrganizerFragment fragment = new AssignCoOrganizerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID,    eventId);
        args.putString(ARG_EVENT_TITLE, eventTitle);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Sets a callback that fires whenever this dialog is dismissed (assigned,
     * cancelled, or back-pressed). Use this to reload the event in the host activity.
     *
     * @param listener Runnable to call on dismiss.
     */
    public void setDismissListener(Runnable listener) {
        this.dismissListener = listener;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId    = getArguments().getString(ARG_EVENT_ID);
            eventTitle = getArguments().getString(ARG_EVENT_TITLE);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_assign_co_organizer, container, false);

        tvDialogEventTitle = view.findViewById(R.id.tvCoOrgDialogEventTitle);
        progressBar        = view.findViewById(R.id.coOrgProgressBar);
        tvEmpty            = view.findViewById(R.id.tvCoOrgEmpty);
        etSearch           = view.findViewById(R.id.etCoOrgSearch);
        radioGroup         = view.findViewById(R.id.rgCoOrgEntrants);
        btnAssign          = view.findViewById(R.id.btnCoOrgAssign);
        btnCancel          = view.findViewById(R.id.btnCoOrgCancel);

        if (eventTitle != null) {
            tvDialogEventTitle.setText(eventTitle);
        }

        btnAssign.setEnabled(false);

        radioGroup.setOnCheckedChangeListener((group, checkedId) ->
                btnAssign.setEnabled(checkedId != -1));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntrants();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op.
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
        btnAssign.setOnClickListener(v -> onAssignClicked());

        loadAllEntrants();

        return view;
    }

    /** Fire the dismiss listener whenever the dialog goes away for any reason. */
    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) {
            dismissListener.run();
        }
    }

    // ── Load entrants ─────────────────────────────────────────────────────────

    private void loadAllEntrants() {
        showLoading(true);

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(eventSnapshot -> {
                    if (!isAdded()) return;

                    Event event = EventSchema.normalizeLoadedEvent(eventSnapshot);
                    if (event == null) {
                        showLoading(false);
                        showEmpty("Could not load event.");
                        return;
                    }

                    ArrayList<String> coOrgs = event.getCoOrganizers();

                    // MAX ONE co-organizer — block if already assigned
                    if (coOrgs != null && !coOrgs.isEmpty()) {
                        showLoading(false);
                        showWarning("A co-organizer is already assigned to this event.\n" +
                                "Only one co-organizer is allowed per event.");
                        // Hide Assign, rename Cancel to OK — it's just an info message
                        btnAssign.setVisibility(View.GONE);
                        btnCancel.setText("OK");
                        return;
                    }

                    String organizerDevId = event.getOrganizerDeviceId();

                    AppDatabase.getInstance().usersRef
                            .whereEqualTo("role", "entrant")
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!isAdded()) return;

                                for (QueryDocumentSnapshot doc : querySnapshot) {
                                    String devId = doc.getId();
                                    if (devId.equals(organizerDevId)) continue;

                                    String name = doc.getString("name");
                                    // Skip entrants with no name set
                                    if (name == null || name.trim().isEmpty()) continue;

                                    entrantDeviceIds.add(devId);
                                    entrantNames.add(name.trim());
                                }

                                showLoading(false);

                                if (entrantNames.isEmpty()) {
                                    showEmpty("No entrants available to assign.");
                                } else {
                                    filterEntrants();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                showLoading(false);
                                showEmpty("Failed to load entrants.");
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showLoading(false);
                    showEmpty("Failed to load event.");
                });
    }

    /**
     * Builds a RadioButton for each entrant inside the RadioGroup.
     * Uses RadioGroup instead of ListView to avoid rendering artifacts in dialogs.
     */
    private void populateRadioGroup() {
        radioGroup.removeAllViews();
        btnAssign.setEnabled(false);

        String query = etSearch.getText() == null
                ? ""
                : etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());

        for (int i = 0; i < entrantNames.size(); i++) {
            String entrantName = entrantNames.get(i);
            if (!query.isEmpty()
                    && !entrantName.toLowerCase(Locale.getDefault()).contains(query)) {
                continue;
            }

            RadioButton rb = new RadioButton(requireContext());
            rb.setId(View.generateViewId());
            rb.setTag(i);
            rb.setText(entrantName);
            rb.setTextSize(15f);
            rb.setTextColor(0xFF111111);
            rb.setPadding(8, 24, 8, 24);
            radioGroup.addView(rb);
        }

        if (radioGroup.getChildCount() == 0) {
            tvEmpty.setTextColor(0xFFAAAAAA);
            tvEmpty.setText("No entrants match your search.");
            tvEmpty.setVisibility(View.VISIBLE);
            radioGroup.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        radioGroup.setVisibility(View.VISIBLE);
    }

    private void filterEntrants() {
        if (entrantNames.isEmpty()) {
            return;
        }

        populateRadioGroup();
    }

    // ── Assignment ────────────────────────────────────────────────────────────

    private void onAssignClicked() {
        int checkedId = radioGroup.getCheckedRadioButtonId();
        if (checkedId == -1) {
            Toast.makeText(requireContext(), "Please select an entrant.", Toast.LENGTH_SHORT).show();
            return;
        }

        View checkedView = radioGroup.findViewById(checkedId);
        if (!(checkedView instanceof RadioButton)
                || !(((RadioButton) checkedView).getTag() instanceof Integer)) {
            Toast.makeText(requireContext(), "Please select an entrant.", Toast.LENGTH_SHORT).show();
            return;
        }

        int entrantIndex = (Integer) ((RadioButton) checkedView).getTag();
        if (entrantIndex < 0 || entrantIndex >= entrantDeviceIds.size()) {
            Toast.makeText(requireContext(), "Please select an entrant.", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetDeviceId = entrantDeviceIds.get(entrantIndex);
        String targetName     = entrantNames.get(entrantIndex);

        btnAssign.setEnabled(false);

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;

                    Event event = EventSchema.normalizeLoadedEvent(snapshot);
                    if (event == null) {
                        showError("Could not load event data.");
                        return;
                    }

                    // Server-side max-1 guard
                    ArrayList<String> coOrganizers = event.getCoOrganizers();
                    if (coOrganizers != null && !coOrganizers.isEmpty()) {
                        showError("A co-organizer is already assigned to this event.");
                        return;
                    }

                    // Null guard — coOrganizers may be null for events that never had one
                    if (coOrganizers == null) {
                        coOrganizers = new ArrayList<>();
                    }
                    coOrganizers.add(targetDeviceId);

                    ArrayList<String> waitingList       = event.getWaitingList();
                    ArrayList<String> selectedEntrants  = event.getSelectedEntrants();
                    ArrayList<String> enrolledEntrants  = event.getEnrolledEntrants();
                    ArrayList<String> cancelledEntrants = event.getCancelledEntrants();

                    // Null guards — any pool may be null on a brand new event
                    if (waitingList == null)       waitingList       = new ArrayList<>();
                    if (selectedEntrants == null)  selectedEntrants  = new ArrayList<>();
                    if (enrolledEntrants == null)  enrolledEntrants  = new ArrayList<>();
                    if (cancelledEntrants == null) cancelledEntrants = new ArrayList<>();

                    waitingList.remove(targetDeviceId);
                    selectedEntrants.remove(targetDeviceId);
                    enrolledEntrants.remove(targetDeviceId);
                    cancelledEntrants.remove(targetDeviceId);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("coOrganizers",     coOrganizers);
                    updates.put("waitingList",       waitingList);
                    updates.put("selectedEntrants",  selectedEntrants);
                    updates.put("enrolledEntrants",  enrolledEntrants);
                    updates.put("cancelledEntrants", cancelledEntrants);

                    db.collection("events").document(eventId)
                            .update(updates)
                            .addOnSuccessListener(v ->
                                    sendCoOrganizerNotification(targetDeviceId, targetName))
                            .addOnFailureListener(e ->
                                    showError("Failed to save: " + e.getMessage()));
                })
                .addOnFailureListener(e -> showError("Failed to load event: " + e.getMessage()));
    }

    // ── Notification (US 01.09.01) ────────────────────────────────────────────

    private void sendCoOrganizerNotification(String targetDeviceId, String targetName) {
        NotificationPreferenceHelper.resolveEnabledRecipients(
                Collections.singletonList(targetDeviceId),
                (enabledRecipients, optedOutCount) -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (enabledRecipients.isEmpty()) {
                        Toast.makeText(requireContext(),
                                targetName + " assigned as co-organizer. Notification is turned off for this user.",
                                Toast.LENGTH_LONG).show();
                        dismiss();
                        return;
                    }

                    persistCoOrganizerNotification(targetDeviceId, targetName);
                });
    }

    /**
     * Writes the co-organizer notification only after the target entrant has been
     * confirmed as still opted in to organizer/admin notifications.
     */
    private void persistCoOrganizerNotification(String targetDeviceId, String targetName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId",     targetDeviceId);
        notification.put("eventId",    eventId);
        notification.put("eventTitle", eventTitle != null ? eventTitle : "");
        notification.put("title",      getString(R.string.co_organizer_notification_title));
        notification.put("message",    getString(R.string.co_organizer_notification_message));
        notification.put("status",     NOTIF_STATUS);
        notification.put("type",       NOTIF_TYPE);
        notification.put("read",       false);
        notification.put("sentAt",     com.google.firebase.Timestamp.now());

        db.collection("notifications")
                .document(targetDeviceId)
                .collection("messages")
                .add(notification)
                .addOnSuccessListener(ref -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                targetName + " assigned as co-organizer and notified.",
                                Toast.LENGTH_SHORT).show();
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                targetName + " assigned, but notification failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                    dismiss();
                });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        etSearch.setEnabled(!loading);
        etSearch.setVisibility(loading ? View.GONE : View.VISIBLE);
        if (loading) {
            radioGroup.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            btnAssign.setEnabled(false);
        }
    }

    private void showEmpty(String message) {
        etSearch.setVisibility(View.GONE);
        tvEmpty.setTextColor(0xFFAAAAAA);
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
        radioGroup.setVisibility(View.GONE);
        btnAssign.setEnabled(false);
    }

    private void showWarning(String message) {
        etSearch.setVisibility(View.GONE);
        tvEmpty.setTextColor(0xFFCC3333);
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
        radioGroup.setVisibility(View.GONE);
        btnAssign.setEnabled(false);
    }

    private void showError(String message) {
        btnAssign.setEnabled(true);
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}