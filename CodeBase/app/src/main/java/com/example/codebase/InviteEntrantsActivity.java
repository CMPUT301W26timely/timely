package com.example.codebase;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Allows an organizer to invite entrants to a private event through targeted search.
 *
 * <p>The screen only reveals matching search results after the organizer enters a
 * name, email, and/or phone query, avoiding a full user-directory browser while
 * still satisfying private-event invite search requirements.</p>
 */
public class InviteEntrantsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";
    public static final String EXTRA_EVENT_TITLE = "eventTitle";

    private String eventId;
    private String eventTitle;

    private TextInputLayout inputLayoutName;
    private TextInputLayout inputLayoutEmail;
    private TextInputLayout inputLayoutPhone;
    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextView tvLoading;
    private TextView tvStatus;
    private TextView tvResultsLabel;
    private TextView tvEmptyResults;
    private Button btnInviteByContact;
    private RecyclerView rvInviteResults;
    private SearchResultsAdapter resultsAdapter;

    private final List<String> alreadyInvited = new ArrayList<>();
    private final List<String> coOrganizerIds = new ArrayList<>();
    private final List<User> loadedUsers = new ArrayList<>();
    private final List<InviteCandidate> visibleCandidates = new ArrayList<>();

    private String lastSearchName = "";
    private String lastSearchEmail = "";
    private String lastSearchPhone = "";
    private boolean isBusy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_entrants);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inputLayoutName = findViewById(R.id.inputLayoutInviteName);
        inputLayoutEmail = findViewById(R.id.inputLayoutInviteEmail);
        inputLayoutPhone = findViewById(R.id.inputLayoutInvitePhone);
        etName = findViewById(R.id.etInviteName);
        etEmail = findViewById(R.id.etInviteEmail);
        etPhone = findViewById(R.id.etInvitePhone);
        tvLoading = findViewById(R.id.tvInviteLoading);
        tvStatus = findViewById(R.id.tvInviteStatus);
        tvResultsLabel = findViewById(R.id.tvInviteResultsLabel);
        tvEmptyResults = findViewById(R.id.tvInviteEmpty);
        btnInviteByContact = findViewById(R.id.btnInviteByContact);
        rvInviteResults = findViewById(R.id.rvInviteResults);

        rvInviteResults.setLayoutManager(new LinearLayoutManager(this));
        rvInviteResults.setNestedScrollingEnabled(false);
        resultsAdapter = new SearchResultsAdapter();
        rvInviteResults.setAdapter(resultsAdapter);

        ImageButton btnBack = findViewById(R.id.btnInviteBack);
        btnBack.setOnClickListener(v -> finish());

        btnInviteByContact.setOnClickListener(v -> searchEntrants());

        loadEventState();
    }

    private void loadEventState() {
        setLoadingState(true);
        AppDatabase.getInstance().eventsRef
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    Event event = EventSchema.normalizeLoadedEvent(eventDoc);
                    alreadyInvited.clear();
                    if (event != null) {
                        alreadyInvited.addAll(event.getWaitingList());
                        alreadyInvited.addAll(event.getSelectedEntrants());
                        alreadyInvited.addAll(event.getEnrolledEntrants());
                    }

                    coOrganizerIds.clear();
                    if (event != null && event.getCoOrganizers() != null) {
                        coOrganizerIds.addAll(event.getCoOrganizers());
                    }

                    eventDoc.getReference()
                            .collection(PrivateEventInvite.SUBCOLLECTION)
                            .get()
                            .addOnSuccessListener(inviteDocs -> {
                                for (DocumentSnapshot inviteDoc : inviteDocs.getDocuments()) {
                                    PrivateEventInvite invite =
                                            inviteDoc.toObject(PrivateEventInvite.class);
                                    if (invite == null) {
                                        continue;
                                    }

                                    if (PrivateEventInvite.STATUS_PENDING.equals(invite.getStatus())
                                            || PrivateEventInvite.STATUS_ACCEPTED.equals(invite.getStatus())) {
                                        alreadyInvited.add(inviteDoc.getId());
                                    }
                                }
                                setLoadingState(false);
                                showStatus(
                                        getString(R.string.invite_ready_state),
                                        false
                                );
                            })
                            .addOnFailureListener(e -> {
                                setLoadingState(false);
                                Toast.makeText(this, "Failed to load invites", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show();
                });
    }

    private void searchEntrants() {
        clearErrors();

        String name = normalizeName(readText(etName));
        String email = normalizeEmail(readText(etEmail));
        String phone = normalizePhone(readText(etPhone));

        if (name.isEmpty() && email.isEmpty() && phone.isEmpty()) {
            inputLayoutName.setError(getString(R.string.invite_contact_required));
            inputLayoutEmail.setError(getString(R.string.invite_contact_required));
            inputLayoutPhone.setError(getString(R.string.invite_contact_required));
            return;
        }

        lastSearchName = name;
        lastSearchEmail = email;
        lastSearchPhone = phone;

        setLoadingState(true);
        showStatus(null, false);

        UserRepository.loadAllProfiles(new UserRepository.ProfilesCallback() {
            @Override
            public void onProfilesLoaded(List<User> users) {
                setLoadingState(false);
                loadedUsers.clear();
                loadedUsers.addAll(users);
                applySearchResults(users, name, email, phone);
            }

            @Override
            public void onError(Exception e) {
                setLoadingState(false);
                showStatus(getString(R.string.invite_lookup_failed), true);
            }
        });
    }

    private void applySearchResults(List<User> users, String name, String email, String phone) {
        String organizerDeviceId = DeviceIdManager.getOrCreateDeviceId(this);
        visibleCandidates.clear();
        for (User user : users) {
            if (user == null || TextUtils.isEmpty(user.getDeviceId())) {
                continue;
            }

            if (matchesQuery(user, name, email, phone)) {
                visibleCandidates.add(new InviteCandidate(
                        user,
                        canInviteUser(user, organizerDeviceId)
                ));
            }
        }

        resultsAdapter.notifyDataSetChanged();
        boolean hasResults = !visibleCandidates.isEmpty();
        tvResultsLabel.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        rvInviteResults.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        tvEmptyResults.setVisibility(hasResults ? View.GONE : View.VISIBLE);

        if (hasResults) {
            showStatus(getString(R.string.invite_results_found, visibleCandidates.size()), false);
        } else {
            showStatus(getString(R.string.invite_no_match), true);
        }
    }

    private boolean matchesQuery(User user, String name, String email, String phone) {
        boolean hasQuery = !name.isEmpty() || !email.isEmpty() || !phone.isEmpty();
        if (!hasQuery) {
            return false;
        }

        boolean nameMatches = name.isEmpty()
                || normalizeName(user.getName()).contains(name);
        boolean emailMatches = email.isEmpty()
                || normalizeEmail(user.getEmail()).equals(email);
        boolean phoneMatches = phone.isEmpty()
                || normalizePhone(user.getPhoneNumber()).equals(phone);

        return nameMatches && emailMatches && phoneMatches;
    }

    private boolean canInviteUser(User user, String organizerDeviceId) {
        String targetDeviceId = user.getDeviceId();
        if (TextUtils.isEmpty(targetDeviceId)) {
            return false;
        }
        if (organizerDeviceId.equals(targetDeviceId)) {
            return false;
        }
        if (coOrganizerIds.contains(targetDeviceId)) {
            return false;
        }
        return !alreadyInvited.contains(targetDeviceId);
    }

    private void createInvite(User user) {
        String targetDeviceId = user.getDeviceId();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        PrivateEventInvite invite = new PrivateEventInvite(
                targetDeviceId,
                eventId,
                eventTitle,
                PrivateEventInvite.STATUS_PENDING,
                new Date()
        );

        batch.set(db.collection("events")
                .document(eventId)
                .collection(PrivateEventInvite.SUBCOLLECTION)
                .document(targetDeviceId), invite);

        setLoadingState(true);
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    setLoadingState(false);
                    alreadyInvited.add(targetDeviceId);
                    sendPrivateInviteNotification(db, targetDeviceId, user.isNotificationsEnabled());
                    rerenderSearchResults();
                    showStatus(
                            getString(R.string.invite_success_summary, buildDisplayTarget(user)),
                            false
                    );
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    showStatus(getString(R.string.invite_save_failed), true);
                });
    }

    private void sendPrivateInviteNotification(FirebaseFirestore db,
                                               String deviceId,
                                               boolean notificationsEnabled) {
        if (!notificationsEnabled) {
            Toast.makeText(this,
                    "Entrant invited (they have opted out of notifications)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> notif = new HashMap<>();
        notif.put("userId", deviceId);
        notif.put("eventId", eventId);
        notif.put("eventTitle", eventTitle);
        notif.put("title", "You've been invited!");
        notif.put("message", "You've been personally invited to a private event: "
                + eventTitle + ". Accept the invitation before joining the waiting list.");
        notif.put("status", "Private Invite");
        notif.put("type", "privateInvite");
        notif.put("sentAt", new Timestamp(new Date()));
        notif.put("read", false);

        db.collection("notifications")
                .document(deviceId)
                .collection("messages")
                .add(notif)
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Invite saved, but notification failed",
                                Toast.LENGTH_SHORT).show());
    }

    private void setLoadingState(boolean isLoading) {
        isBusy = isLoading;
        tvLoading.setVisibility(isLoading ? TextView.VISIBLE : TextView.GONE);
        btnInviteByContact.setEnabled(!isLoading);
        etName.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPhone.setEnabled(!isLoading);
        if (resultsAdapter != null) {
            resultsAdapter.notifyDataSetChanged();
        }
    }

    private void clearErrors() {
        inputLayoutName.setError(null);
        inputLayoutEmail.setError(null);
        inputLayoutPhone.setError(null);
        inputLayoutName.setErrorEnabled(false);
        inputLayoutEmail.setErrorEnabled(false);
        inputLayoutPhone.setErrorEnabled(false);
    }

    private void rerenderSearchResults() {
        if (loadedUsers.isEmpty()) {
            return;
        }
        applySearchResults(loadedUsers, lastSearchName, lastSearchEmail, lastSearchPhone);
    }

    private void clearInputs() {
        etName.setText("");
        etEmail.setText("");
        etPhone.setText("");
    }

    private void showStatus(String message, boolean isError) {
        if (TextUtils.isEmpty(message)) {
            tvStatus.setVisibility(TextView.GONE);
            return;
        }
        tvStatus.setVisibility(TextView.VISIBLE);
        tvStatus.setText(message);
        tvStatus.setTextColor(getColor(isError ? R.color.historyCancelledText : R.color.primaryAccent));
    }

    private String readText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private String normalizeEmail(String rawEmail) {
        return rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String rawName) {
        return rawName == null ? "" : rawName.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        return rawPhone.replaceAll("[^0-9+]", "");
    }

    private String buildDisplayTarget(User user) {
        if (!TextUtils.isEmpty(user.getName())) {
            return user.getName();
        }
        if (!TextUtils.isEmpty(user.getEmail())) {
            return user.getEmail();
        }
        if (!TextUtils.isEmpty(user.getPhoneNumber())) {
            return user.getPhoneNumber();
        }
        return getString(R.string.invite_fallback_target);
    }

    private String buildSecondaryLine(User user) {
        String email = normalizeEmail(user.getEmail());
        String phone = user.getPhoneNumber() == null ? "" : user.getPhoneNumber().trim();

        if (!email.isEmpty() && !phone.isEmpty()) {
            return email + " • " + phone;
        }
        if (!email.isEmpty()) {
            return email;
        }
        if (!phone.isEmpty()) {
            return phone;
        }
        return user.getDeviceId();
    }

    private int resolveInviteButtonLabel(User user, boolean canInvite) {
        if (canInvite) {
            return R.string.btn_invite;
        }
        if (alreadyInvited.contains(user.getDeviceId())) {
            return R.string.btn_invited;
        }
        return R.string.btn_unavailable;
    }

    private final class SearchResultsAdapter
            extends RecyclerView.Adapter<SearchResultsAdapter.ResultViewHolder> {

        @NonNull
        @Override
        public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_invite_entrant, parent, false);
            return new ResultViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
            InviteCandidate candidate = visibleCandidates.get(position);
            User user = candidate.user;

            String displayName = !TextUtils.isEmpty(user.getName())
                    ? user.getName()
                    : getString(R.string.invite_fallback_target);

            holder.tvName.setText(displayName);
            holder.tvSub.setText(buildSecondaryLine(user));
            holder.tvSub.setVisibility(View.VISIBLE);

            boolean buttonEnabled = candidate.canInvite && !isBusy;
            holder.btnInvite.setEnabled(buttonEnabled);
            holder.btnInvite.setAlpha(buttonEnabled ? 1f : 0.65f);
            holder.btnInvite.setText(resolveInviteButtonLabel(user, candidate.canInvite));
            holder.btnInvite.setOnClickListener(v -> createInvite(user));
        }

        @Override
        public int getItemCount() {
            return visibleCandidates.size();
        }

        final class ResultViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName;
            private final TextView tvSub;
            private final Button btnInvite;

            ResultViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvInviteName);
                tvSub = itemView.findViewById(R.id.tvInviteSub);
                btnInvite = itemView.findViewById(R.id.btnInvite);
            }
        }
    }

    private static final class InviteCandidate {
        private final User user;
        private final boolean canInvite;

        InviteCandidate(User user, boolean canInvite) {
            this.user = user;
            this.canInvite = canInvite;
        }
    }
}
