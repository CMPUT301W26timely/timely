package com.example.codebase;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link AppCompatActivity} that allows an administrator to browse event posters,
 * select multiple events for poster deletion, and confirm deletion via a dialog.
 *
 * <p>Satisfies user stories <b>US 03.03.01</b> and <b>US 03.06.01</b>.</p>
 *
 * <p>The activity operates in two modes:</p>
 * <ul>
 *     <li><b>Browse mode</b>: event posters are displayed in a 2-column grid with no
 *         checkboxes. Tapping a poster opens it full screen. The bottom button reads
 *         "Select images to delete".</li>
 *     <li><b>Selection mode</b>: checkboxes appear on each card. The bottom button
 *         reads "Delete selected images" and triggers a confirmation dialog on tap.</li>
 * </ul>
 */
public class AdminBrowseImagesActivity extends AppCompatActivity {

    /** The RecyclerView displaying event poster cards in a 2-column grid. */
    private RecyclerView recyclerView;

    /** Bottom action button that toggles between selection mode and triggering deletion. */
    private Button btnSelectDelete;

    /** Bottom action button that switches out of selection mode. */
    private Button btnCancelSelection;

    /** Adapter backing the poster grid. */
    private ImageGridAdapter adapter;

    /** List of events loaded from Firestore, used as the data source for the grid. */
    private final List<Event> eventList = new ArrayList<>();

    /**
     * Parallel list tracking which events are currently selected for deletion.
     * Kept in sync with {@link #eventList} by index.
     */
    private final List<Boolean> selectedStates = new ArrayList<>();

    /** Whether the activity is currently in multi-select deletion mode. */
    private boolean isSelectionMode = false;

    /**
     * Initializes the activity, loads events from Firestore, and configures
     * the bottom action button.
     *
     * @param savedInstanceState a {@link Bundle} containing the activity's previously
     *                           saved state, or {@code null} if this is a fresh launch
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_images);

        recyclerView = findViewById(R.id.recyclerViewImages);
        btnSelectDelete = findViewById(R.id.btnSelectDelete);
        btnCancelSelection = findViewById(R.id.btnCancelSelection);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ImageGridAdapter(eventList);
        recyclerView.setAdapter(adapter);

        loadEventsFromFirestore();

        btnSelectDelete.setOnClickListener(v -> {
            if (!isSelectionMode) {
                enterSelectionMode();
            } else {
                confirmDeletion();
            }
        });

        btnCancelSelection.setOnClickListener(v -> exitSelectionMode());
        setupBottomNavigation();
    }

    /**
     * Wires the shared bottom navigation for the administrator profile screen.
     */
    private void setupBottomNavigation() {
        findViewById(R.id.navImage).setOnClickListener(v -> {
            // Already here
        });

        findViewById(R.id.navHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, BrowseEventsActivity.class));
            finish();
        });

        findViewById(R.id.navMyEvents).setOnClickListener(v -> navigateToAdminHome());


        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }

    /**
     * Returns to the shared administrator event browser.
     *
     * <p>This screen is often opened from a navigation action that already finished the
     * previous activity, so we start the admin home explicitly instead of relying on
     * {@link #finish()} to reveal an existing screen.</p>
     */
    private void navigateToAdminHome() {
        Intent intent = new Intent(this, OrganizerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Loads all event documents from the Firestore {@code events} collection that have
     * a poster set, and populates {@link #eventList}.
     */
    private void loadEventsFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventList.clear();
                    selectedStates.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = EventSchema.normalizeLoadedEvent(doc);
                        if (event != null && event.getPoster() != null && event.getPoster().getPosterImageBase64() != null) {
                            eventList.add(event);
                            selectedStates.add(false);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load images: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Switches the activity into selection mode, showing checkboxes on each card
     * and updating the bottom button label to "Delete selected images".
     */
    private void enterSelectionMode() {
        isSelectionMode = true;
        btnSelectDelete.setText("Delete selected images");
        btnCancelSelection.setVisibility(View.VISIBLE);
        adapter.setSelectionMode(true);
    }

    /**
     * Exits selection mode, hiding checkboxes, clearing all selections, and restoring
     * the bottom button label to "Select images to delete".
     */
    private void exitSelectionMode() {
        isSelectionMode = false;
        btnSelectDelete.setText("Select images to delete");
        btnCancelSelection.setVisibility(View.GONE);
        adapter.setSelectionMode(false);
        for (int i = 0; i < selectedStates.size(); i++) selectedStates.set(i, false);
        adapter.notifyDataSetChanged();
    }

    /**
     * Shows an {@link AlertDialog} asking the user to confirm deletion of the posters
     * of all currently selected events. On confirmation, the poster field is cleared
     * in Firestore for each selected event. On cancellation, selection mode is exited.
     */
    private void confirmDeletion() {
        List<Event> selected = getSelectedEvents();
        if (selected.isEmpty()) {
            Toast.makeText(this, "No images selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to delete these images?")
                .setPositiveButton("YES", (dialog, which) -> deleteSelectedPosters(selected))
                .setNegativeButton("NO", (dialog, which) -> exitSelectionMode())
                .show();
    }

    /**
     * Returns a list of {@link Event} objects that are currently selected.
     *
     * @return a new {@link List} containing only the selected events
     */
    private List<Event> getSelectedEvents() {
        List<Event> selected = new ArrayList<>();
        for (int i = 0; i < eventList.size(); i++) {
            if (selectedStates.get(i)) selected.add(eventList.get(i));
        }
        return selected;
    }

    /**
     * Clears the poster field on each selected event's Firestore document, then removes
     * those events from the local list and refreshes the grid. Shows a success toast
     * when all updates complete.
     *
     * @param selected the list of {@link Event} objects whose posters should be deleted
     */
    private void deleteSelectedPosters(List<Event> selected) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        int[] remaining = {selected.size()};

        for (Event event : selected) {
            db.collection("events").document(event.getId())
                    .update("poster", null)
                    .addOnSuccessListener(unused -> {
                        int index = eventList.indexOf(event);
                        if (index >= 0) {
                            eventList.remove(index);
                            selectedStates.remove(index);
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            adapter.notifyDataSetChanged();
                            exitSelectionMode();
                            Toast.makeText(this,
                                    "Images have been successfully removed!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to delete: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Opens a full-screen dialog displaying the poster of the given event.
     * Only accessible in browse mode.
     *
     * @param event the {@link Event} whose poster should be displayed full screen
     */
    private void showFullScreenImage(Event event) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView imgFull = dialog.findViewById(R.id.imgFullscreen);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseFullscreen);

        EventPoster poster = event.getPoster();
        if (poster != null && poster.getPosterImageBase64() != null) {
            Bitmap bitmap = EventPoster.decodeImage(poster.getPosterImageBase64());
            imgFull.setImageBitmap(bitmap);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // -----------------------------------------------------------------------------------------
    // Adapter
    // -----------------------------------------------------------------------------------------

    /**
     * {@link RecyclerView.Adapter} that displays event poster cards in a 2-column grid.
     * Supports a selection mode in which checkboxes are shown on each card and items
     * can be toggled for deletion.
     */
    class ImageGridAdapter extends RecyclerView.Adapter<ImageGridAdapter.ViewHolder> {

        /** The list of events to display. */
        private final List<Event> items;

        /** Whether checkboxes are currently visible on each card. */
        private boolean selectionMode = false;

        /**
         * Constructs the adapter with the given event list.
         *
         * @param items the list of {@link Event} objects to display
         */
        ImageGridAdapter(List<Event> items) {
            this.items = items;
        }

        /**
         * Enables or disables selection mode and refreshes all cards.
         *
         * @param enabled {@code true} to show checkboxes; {@code false} to hide them
         */
        void setSelectionMode(boolean enabled) {
            this.selectionMode = enabled;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Event event = items.get(position);

            holder.txtName.setText(event.getTitle() != null ? event.getTitle() : "Event");

            holder.selectionOverlay.setVisibility(
                    selectedStates.get(position) && selectionMode ? View.VISIBLE : View.GONE
            );
            // Decode and display the event poster
            EventPoster poster = event.getPoster();
            if (poster != null && poster.getPosterImageBase64() != null) {
                try {
                    Bitmap bitmap = EventPoster.decodeImage(poster.getPosterImageBase64());
                    holder.imgThumbnail.setImageBitmap(bitmap);
                } catch (Exception e) {
                    holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            holder.itemView.setOnClickListener(v -> {
                if (selectionMode) {
                    boolean newState = !selectedStates.get(position);
                    selectedStates.set(position, newState);
                    holder.selectionOverlay.setVisibility(newState ? View.VISIBLE : View.GONE);
                } else {
                    showFullScreenImage(event);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        /**
         * Holds references to the views within a single event poster card.
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgThumbnail;
            TextView txtName;
            View selectionOverlay = itemView.findViewById(R.id.selectionOverlay);

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
                txtName = itemView.findViewById(R.id.txtImageName);
            }
        }
    }
}