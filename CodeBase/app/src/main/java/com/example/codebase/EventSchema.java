package com.example.codebase;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Firestore schema helpers for event documents.
 */
public final class EventSchema {

    private static final int DRAW_OFFSET_DAYS = 3;

    private EventSchema() {
    }

    public static Event normalizeLoadedEvent(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null || !documentSnapshot.exists()) {
            return null;
        }

        Event event = normalizeLoadedEvent(documentSnapshot.toObject(Event.class), documentSnapshot.getId());
        if (event == null) {
            return null;
        }

        event.setWaitingList(toDeviceIdList(documentSnapshot.get("waitingList")));
        event.setSelectedEntrants(toDeviceIdList(documentSnapshot.get("selectedEntrants")));
        event.setEnrolledEntrants(toDeviceIdList(documentSnapshot.get("enrolledEntrants")));
        event.setCancelledEntrants(toDeviceIdList(documentSnapshot.get("cancelledEntrants")));
        return event;
    }

    public static Event normalizeLoadedEvent(Event event, String documentId) {
        if (event == null) {
            return null;
        }

        if ((event.getId() == null || event.getId().isEmpty()) && documentId != null) {
            event.setId(documentId);
        } else if (event.getId() != null && !event.getId().isEmpty()) {
            event.setEventId(event.getId());
        }

        if (event.getWaitingList() == null) {
            event.setWaitingList(new ArrayList<>());
        }
        if (event.getSelectedEntrants() == null) {
            event.setSelectedEntrants(new ArrayList<>());
        }
        if (event.getEnrolledEntrants() == null) {
            event.setEnrolledEntrants(new ArrayList<>());
        }
        if (event.getCancelledEntrants() == null) {
            event.setCancelledEntrants(new ArrayList<>());
        }

        if (event.getDrawDate() == null) {
            event.setDrawDate(calculateDrawDate(event.getRegistrationDeadline()));
        }

        if (event.getWinnersCount() == null && event.getMaxCapacity() != null) {
            event.setWinnersCount(event.getMaxCapacity());
        }

        return event;
    }

    public static Date calculateDrawDate(Date registrationDeadline) {
        if (registrationDeadline == null) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(registrationDeadline);
        calendar.add(Calendar.DAY_OF_YEAR, DRAW_OFFSET_DAYS);
        return calendar.getTime();
    }

    public static ArrayList<String> toDeviceIdList(Object rawValue) {
        ArrayList<String> deviceIds = new ArrayList<>();
        if (!(rawValue instanceof List)) {
            return deviceIds;
        }

        for (Object item : (List<?>) rawValue) {
            if (item instanceof String) {
                deviceIds.add((String) item);
            } else if (item instanceof Map) {
                Object deviceId = ((Map<?, ?>) item).get("deviceId");
                if (deviceId instanceof String && !((String) deviceId).isEmpty()) {
                    deviceIds.add((String) deviceId);
                }
            }
        }

        return deviceIds;
    }
}
