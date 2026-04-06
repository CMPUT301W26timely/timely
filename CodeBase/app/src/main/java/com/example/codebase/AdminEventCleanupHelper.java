package com.example.codebase;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Cleanup-aware admin event deletion helper.
 */
public final class AdminEventCleanupHelper {

    public interface Callback {
        void onSuccess();
        void onFailure(Exception e);
    }

    private AdminEventCleanupHelper() {
    }

    public static void deleteEvent(String eventId, Callback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("Missing event ID"));
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.android.gms.tasks.Task<QuerySnapshot> commentsTask = db.collection("events")
                .document(eventId)
                .collection("comments")
                .get();
        com.google.android.gms.tasks.Task<QuerySnapshot> entrantLocationsTask = db.collection("events")
                .document(eventId)
                .collection("entrantLocations")
                .get();
        com.google.android.gms.tasks.Task<QuerySnapshot> privateInvitesTask = db.collection("events")
                .document(eventId)
                .collection(PrivateEventInvite.SUBCOLLECTION)
                .get();
        com.google.android.gms.tasks.Task<QuerySnapshot> notificationsTask = db.collectionGroup("messages")
                .whereEqualTo("eventId", eventId)
                .get();
        com.google.android.gms.tasks.Task<QuerySnapshot> notificationLogsTask = db.collection("notificationLogs")
                .whereEqualTo("eventId", eventId)
                .get();

        Tasks.whenAllComplete(
                        commentsTask,
                        entrantLocationsTask,
                        privateInvitesTask,
                        notificationsTask,
                        notificationLogsTask)
                .addOnSuccessListener(results -> {
                    WriteBatch batch = db.batch();

                    deleteDocuments(batch, commentsTask);
                    deleteDocuments(batch, entrantLocationsTask);
                    deleteDocuments(batch, privateInvitesTask);
                    deleteDocuments(batch, notificationsTask);
                    deleteDocuments(batch, notificationLogsTask);

                    batch.delete(db.collection("events").document(eventId));

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) {
                                    callback.onFailure(e);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    private static void deleteDocuments(WriteBatch batch, List<DocumentSnapshot> docs) {
        List<DocumentSnapshot> safeDocs = docs != null ? docs : new ArrayList<>();
        for (DocumentSnapshot doc : safeDocs) {
            batch.delete(doc.getReference());
        }
    }

    private static void deleteDocuments(WriteBatch batch,
                                        com.google.android.gms.tasks.Task<QuerySnapshot> task) {
        if (task == null || !task.isSuccessful() || task.getResult() == null) {
            return;
        }
        deleteDocuments(batch, task.getResult().getDocuments());
    }
}
