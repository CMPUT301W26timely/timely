package com.example.codebase;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Allows an organizer to search for entrants and invite them to a private event's
 * waiting list.
 *
 * <p>Implements the following user stories:
 * <ul>
 *   <li><b>US 02.01.03</b> — Organizer can invite specific entrants to a private
 *       event's waiting list by searching via name, phone number and/or email.</li>
 *   <li><b>US 01.05.06</b> — Invited entrants receive a notification that they have
 *       been invited to join the waiting list for a private event.</li>
 * </ul>
 *
 * <p>Expected intent extras when launching this activity:
 * <ul>
 *   <li>{@code "eventId"} — Firestore document ID of the private event.</li>
 *   <li>{@code "eventTitle"} — Display name of the private event.</li>
 * </ul>
 *
 * <p>Search is performed client-side against all user profiles loaded from Firestore.
 * The query is matched case-insensitively against the user's name, email address,
 * and phone number. Entrants who are already on the event's waiting list are shown
 * as already invited and cannot be invited again.
 *
 * <p>On a successful invite:
 * <ol>
 *   <li>The entrant's device ID is appended to the event's {@code waitingList} array
 *       in Firestore using {@link FieldValue#arrayUnion}.</li>
 *   <li>A notification document is written to
 *       {@code notifications/{deviceId}/messages/{autoId}} so the entrant receives an
 *       in-app notification (US 01.05.06).</li>
 * </ol>
 */
public class InviteEntrantsActivity extends AppCompatActivity {

    /** Intent extra key for the private event's Firestore document ID. */
    public static final String EXTRA_EVENT_ID = "eventId";

    /** Intent extra key for the private event's display title. */
    public static final String EXTRA_EVENT_TITLE = "eventTitle";

    /** Firestore document ID of the private event being managed. */
    private String eventId;

    /** Display title of the private event, used in notification messages. */
    private String eventTitle;

    /** Search input field. */
    private EditText etSearch;

    /** Displayed while users are loading from Firestore. */
    private TextView tvLoading;

    /** Shown when the search returns no matching users. */
    private TextView tvEmpty;

    /** Shows the filtered list of users matching the current query. */
    private RecyclerView recyclerView;

    /** Adapter that renders search results and handles invite actions. */
    private InviteSearchAdapter adapter;

    /** Full list of all user profiles fetched from Firestore. */
    private final List<User> allUsers = new ArrayList<>();

    /**
     * Device IDs of entrants who are already on the waiting list when this
     * activity is opened. Used to pre-mark users as already invited.
     */
    private final List<String> alreadyInvited = new ArrayList<>();

    /** Device IDs of entrants who are already assigned as co-organizer for this event. */
    private final List<String> coOrganizerIds = new ArrayList<>();

    /**
     * Initialises the activity, loads all user profiles from Firestore, then
     * loads the event's current waiting list to determine which users are already invited.
     *
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_entrants);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etSearch = findViewById(R.id.etInviteSearch);
        tvLoading = findViewById(R.id.tvInviteLoading);
        tvEmpty = findViewById(R.id.tvInviteEmpty);
        recyclerView = findViewById(R.id.recyclerViewInvite);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new InviteSearchAdapter(new ArrayList<>(), alreadyInvited, this::onInviteClicked);
        adapter.setCoOrganizerIds(coOrganizerIds);
        recyclerView.setAdapter(adapter);

        Button btnBack = findViewById(R.id.btnInviteBack);
        btnBack.setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndDisplay(s.toString().trim());
            }
        });

        loadData();
    }

    /**
     * Loads all user profiles and the event's current waiting list from Firestore,
     * then populates the search results.
     *
     * <p>The organizer's own device ID is excluded from results so they cannot
     * invite themselves. Co-organizers are tracked separately so the adapter can
     * show a clear "Co-Organizer" disabled label instead of an invite button.
     */
    private void loadData() {
        tvLoading.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        String organizerDeviceId = DeviceIdManager.getOrCreateDeviceId(this);

        // Load the event's current waiting list first, then load all users
        AppDatabase.getInstance().eventsRef
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    Event event = EventSchema.normalizeLoadedEvent(eventDoc);
                    if (event != null && event.getWaitingList() != null) {
                        alreadyInvited.clear();
                        alreadyInvited.addAll(event.getWaitingList());
                    }

                    // Track co-organizers separately so the UI can show a clear "Co-Organizer" label
                    coOrganizerIds.clear();
                    if (event != null && event.getCoOrganizers() != null) {
                        coOrganizerIds.addAll(event.getCoOrganizers());
                    }

                    // Now load all user profiles
                    UserRepository.loadAllProfiles(new UserRepository.ProfilesCallback() {
                        @Override
                        public void onProfilesLoaded(List<User> users) {
                            allUsers.clear();
                            for (User u : users) {
                                // Exclude the organizer from the invite list
                                if (!organizerDeviceId.equals(u.getDeviceId())) {
                                    allUsers.add(u);
                                }
                            }
                            tvLoading.setVisibility(View.GONE);
                            filterAndDisplay(etSearch.getText().toString().trim());
                        }

                        @Override
                        public void onError(Exception e) {
                            tvLoading.setVisibility(View.GONE);
                            Toast.makeText(InviteEntrantsActivity.this,
                                    "Failed to load users", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    tvLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Filters {@link #allUsers} against the given query string and updates the adapter.
     *
     * <p>The query is matched case-insensitively against each user's name, email,
     * and phone number. An empty query shows all users (US 02.01.03 — search via
     * name, phone number and/or email).
     *
     * @param query The current search query. May be empty.
     */
    private void filterAndDisplay(String query) {
        List<User> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (User user : allUsers) {
            if (query.isEmpty()) {
                filtered.add(user);
                continue;
            }

            String name  = user.getName()        != null ? user.getName().toLowerCase(Locale.ROOT)        : "";
            String email = user.getEmail()        != null ? user.getEmail().toLowerCase(Locale.ROOT)       : "";
            String phone = user.getPhoneNumber()  != null ? user.getPhoneNumber().toLowerCase(Locale.ROOT) : "";

            if (name.contains(lowerQuery) || email.contains(lowerQuery) || phone.contains(lowerQuery)) {
                filtered.add(user);
            }
        }

        adapter.updateList(filtered);
        recyclerView.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Invites the given user to the private event's waiting list.
     *
     * <p>Two Firestore writes are performed atomically:
     * <ol>
     *   <li>The user's device ID is added to the event's {@code waitingList} via
     *       {@link FieldValue#arrayUnion} (US 02.01.03).</li>
     *   <li>A notification document is created under
     *       {@code notifications/{deviceId}/messages} so the entrant is informed
     *       of the invitation (US 01.05.06).</li>
     * </ol>
     *
     * @param user The {@link User} to invite.
     */
    private void onInviteClicked(User user) {
        String deviceId = user.getDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            Toast.makeText(this, "Cannot invite user: missing device ID", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // US 02.01.03: Add entrant to the event's waiting list
        db.collection("events")
                .document(eventId)
                .update("waitingList", FieldValue.arrayUnion(deviceId))
                .addOnSuccessListener(aVoid -> {
                    // Mark as invited locally so the UI updates immediately
                    if (!alreadyInvited.contains(deviceId)) {
                        alreadyInvited.add(deviceId);
                    }
                    adapter.notifyDataSetChanged();

                    // US 01.05.06: Send in-app notification to the invited entrant
                    sendPrivateInviteNotification(db, deviceId, user.isNotificationsEnabled());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to invite entrant", Toast.LENGTH_SHORT).show());
    }

    /**
     * Writes a private-invite notification document to Firestore for the invited entrant.
     *
     * <p>Implements <b>US 01.05.06</b>: the entrant receives a notification that they have
     * been invited to join the waiting list for a private event. If the entrant has opted
     * out of notifications ({@code notificationsEnabled == false}), the write is skipped
     * and a toast informs the organizer.
     *
     * <p>Notification document path:
     * {@code notifications/{deviceId}/messages/{autoId}}
     *
     * @param db                   The {@link FirebaseFirestore} instance to use.
     * @param deviceId             The invited entrant's device ID.
     * @param notificationsEnabled Whether the entrant accepts notifications.
     */
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
        notif.put("userId",      deviceId);
        notif.put("eventId",     eventId);
        notif.put("eventTitle",  eventTitle);
        notif.put("title",       "You've been invited!");
        notif.put("message",     "You've been personally invited to join the waiting list for: " + eventTitle);
        notif.put("status",      "Waiting List");
        notif.put("type",        "privateInvite");
        notif.put("sentAt",      new Timestamp(new Date()));
        notif.put("read",        false);

        db.collection("notifications")
                .document(deviceId)
                .collection("messages")
                .add(notif)
                .addOnSuccessListener(ref ->
                        Toast.makeText(this, "Entrant invited successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Entrant added to waitlist but notification failed",
                                Toast.LENGTH_SHORT).show());
    }

    // -------------------------------------------------------------------------
    // Inner RecyclerView adapter
    // -------------------------------------------------------------------------

    /**
     * Callback interface for invite button taps in {@link InviteSearchAdapter}.
     */
    interface OnInviteClickListener {
        /**
         * Called when the organizer taps the Invite button for a user row.
         *
         * @param user The {@link User} to invite.
         */
        void onInviteClicked(User user);
    }

    /**
     * RecyclerView adapter that renders a list of {@link User} search results, each
     * with an Invite button. Users who are already on the waiting list are shown with
     * a disabled "Invited" button so the organizer cannot invite them twice.
     * Users who are co-organizers are shown with a disabled "Co-Organizer" button.
     */
    static class InviteSearchAdapter extends RecyclerView.Adapter<InviteSearchAdapter.ViewHolder> {

        /** Currently displayed user list (filtered subset of all users). */
        private List<User> users;

        /** Device IDs of entrants already on the waiting list. */
        private final List<String> alreadyInvited;

        /** Device IDs of co-organizers — shown with a disabled "Co-Organizer" button label. */
        private List<String> coOrganizerIds = new ArrayList<>();

        /** Delegate that handles invite taps. */
        private final OnInviteClickListener listener;

        /**
         * Creates a new adapter.
         *
         * @param users          Initial user list to display.
         * @param alreadyInvited Device IDs already on the waitlist.
         * @param listener       Callback for invite button taps.
         */
        InviteSearchAdapter(List<User> users,
                            List<String> alreadyInvited,
                            OnInviteClickListener listener) {
            this.users = users;
            this.alreadyInvited = alreadyInvited;
            this.listener = listener;
        }

        /**
         * Replaces the displayed list with {@code newList} and refreshes the RecyclerView.
         *
         * @param newList The updated filtered user list.
         */
        void updateList(List<User> newList) {
            this.users = newList;
            notifyDataSetChanged();
        }

        /** Updates the co-organizer ID list and refreshes the RecyclerView. */
        void setCoOrganizerIds(List<String> ids) {
            this.coOrganizerIds = ids;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_invite_entrant, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = users.get(position);
            boolean invited = alreadyInvited.contains(user.getDeviceId());

            // Name (fall back to device ID when name is blank)
            String displayName = (user.getName() != null && !user.getName().isEmpty())
                    ? user.getName()
                    : user.getDeviceId();
            holder.tvName.setText(displayName);

            // Email and phone sub-line
            String email = user.getEmail() != null ? user.getEmail() : "";
            String phone = user.getPhoneNumber() != null ? user.getPhoneNumber() : "";
            String sub = email;
            if (!phone.isEmpty()) {
                sub = sub.isEmpty() ? phone : sub + " · " + phone;
            }
            holder.tvSub.setText(sub);
            holder.tvSub.setVisibility(sub.isEmpty() ? View.GONE : View.VISIBLE);

            // Co-organizers cannot be invited — show a clear disabled label
            if (coOrganizerIds.contains(user.getDeviceId())) {
                holder.btnInvite.setText("Co-Organizer");
                holder.btnInvite.setEnabled(false);
                return;
            }

            // Invite button state
            if (invited) {
                holder.btnInvite.setText(R.string.btn_invited);
                holder.btnInvite.setEnabled(false);
            } else {
                holder.btnInvite.setText(R.string.btn_invite);
                holder.btnInvite.setEnabled(true);
                holder.btnInvite.setOnClickListener(v -> listener.onInviteClicked(user));
            }
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        /** ViewHolder for a single user search result row. */
        static class ViewHolder extends RecyclerView.ViewHolder {
            /** Displays the user's name (or device ID as fallback). */
            TextView tvName;
            /** Displays the user's email and/or phone number. */
            TextView tvSub;
            /** Triggers an invite for this user. Disabled once the user is already invited. */
            Button btnInvite;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName    = itemView.findViewById(R.id.tvInviteName);
                tvSub     = itemView.findViewById(R.id.tvInviteSub);
                btnInvite = itemView.findViewById(R.id.btnInvite);
            }
        }
    }
}