package com.example.codebase;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Utility class providing Firestore schema normalization helpers for {@link Event} documents.
 *
 * <p>This class centralizes all logic for safely deserializing and normalizing {@link Event}
 * objects loaded from Firestore, including:</p>
 * <ul>
 *     <li>Resolving missing or mismatched document IDs</li>
 *     <li>Ensuring all entrant lists are non-null</li>
 *     <li>Deriving a draw date from the registration deadline when absent</li>
 *     <li>Defaulting {@code winnersCount} to {@code maxCapacity} when not explicitly set</li>
 *     <li>Converting raw Firestore list values to typed {@code List<String>} device ID lists</li>
 * </ul>
 *
 * <p>This is a non-instantiable utility class. All methods are static.</p>
 */
public final class EventSchema {

    /**
     * The number of days after the registration deadline that the lottery draw date
     * is scheduled when no explicit draw date is stored in Firestore.
     */
    private static final int DRAW_OFFSET_DAYS = 3;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private EventSchema() {
    }

    /**
     * Deserializes and fully normalizes an {@link Event} from a Firestore
     * {@link DocumentSnapshot}.
     *
     * <p>This overload handles the complete deserialization pipeline:</p>
     * <ol>
     *     <li>Converts the snapshot to an {@link Event} via {@link DocumentSnapshot#toObject}.</li>
     *     <li>Delegates shared normalization logic to
     *         {@link #normalizeLoadedEvent(Event, String)}.</li>
     *     <li>Explicitly re-reads the entrant lists ({@code waitingList},
     *         {@code selectedEntrants}, {@code enrolledEntrants},
     *         {@code cancelledEntrants}, {@code registeredEntrants})
     *         directly from the raw snapshot to handle both legacy {@code Map}-based
     *         and current {@code String}-based Firestore formats via
     *         {@link #toDeviceIdList(Object)}.</li>
     * </ol>
     *
     * @param documentSnapshot the Firestore {@link DocumentSnapshot} to deserialize;
     *                         may be {@code null}
     * @return a normalized {@link Event}, or {@code null} if the snapshot is {@code null},
     *         does not exist, or cannot be deserialized
     */
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
        event.setRegisteredEntrants(toDeviceIdList(documentSnapshot.get("registeredEntrants")));
        event.setCoOrganizers(toDeviceIdList(documentSnapshot.get("coOrganizers")));
        return event;
    }

    /**
     * Normalizes an {@link Event} object that has already been deserialized from Firestore,
     * applying defaults and repairs for fields that may be missing or inconsistent.
     *
     * <p>Normalization steps applied in order:</p>
     * <ol>
     *     <li><b>ID resolution</b> — if the event's {@code id} field is absent, it is set
     *         from {@code documentId}; if it is already present, it is also written to
     *         {@code eventId} for legacy compatibility.</li>
     *     <li><b>Entrant lists</b> — any {@code null} list ({@code waitingList},
     *         {@code selectedEntrants}, {@code enrolledEntrants},
     *         {@code cancelledEntrants}, {@code registeredEntrants})
     *         is replaced with an empty {@link ArrayList}.</li>
     *     <li><b>Draw date</b> — if {@code drawDate} is absent, it is calculated as
     *         {@value DRAW_OFFSET_DAYS} days after {@code registrationDeadline} via
     *         {@link #calculateDrawDate(Date)}.</li>
     *     <li><b>Winners count</b> — if {@code winnersCount} is absent but
     *         {@code maxCapacity} is set, {@code winnersCount} defaults to
     *         {@code maxCapacity}.</li>
     * </ol>
     *
     * @param event      the partially deserialized {@link Event} to normalize;
     *                   may be {@code null}
     * @param documentId the Firestore document ID to apply if the event has no ID set;
     *                   may be {@code null}
     * @return the normalized {@link Event}, or {@code null} if {@code event} is {@code null}
     */
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
        if (event.getRegisteredEntrants() == null) {
            event.setRegisteredEntrants(new ArrayList<>());
        }
        if (event.getCoOrganizers() == null) {
            event.setCoOrganizers(new ArrayList<>());
        }

        if (event.getDrawDate() == null) {
            event.setDrawDate(calculateDrawDate(event.getRegistrationDeadline()));
        }

        if (event.getWinnersCount() == null && event.getMaxCapacity() != null) {
            event.setWinnersCount(event.getMaxCapacity());
        }

        return event;
    }

    /**
     * Calculates the lottery draw date as {@value DRAW_OFFSET_DAYS} days after
     * the given registration deadline.
     *
     * <p>This method is used as a fallback when no explicit draw date is stored
     * in Firestore for an event.</p>
     *
     * @param registrationDeadline the event's registration close date;
     *                             may be {@code null}
     * @return a {@link Date} representing the calculated draw date,
     *         or {@code null} if {@code registrationDeadline} is {@code null}
     */
    public static Date calculateDrawDate(Date registrationDeadline) {
        if (registrationDeadline == null) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(registrationDeadline);
        calendar.add(Calendar.DAY_OF_YEAR, DRAW_OFFSET_DAYS);
        return calendar.getTime();
    }

    /**
     * Converts a raw Firestore list value into a typed {@link ArrayList} of device ID strings.
     *
     * <p>Firestore may return entrant list entries in one of two formats depending on
     * how the data was originally written:</p>
     * <ul>
     *     <li><b>String</b> — the entry is a plain device ID string and is added directly.</li>
     *     <li><b>Map</b> — the entry is a legacy object with a {@code "deviceId"} key;
     *         the value is extracted and added if it is a non-empty string.</li>
     * </ul>
     *
     * <p>Entries that are neither a {@link String} nor a {@link Map}, and {@code Map} entries
     * whose {@code "deviceId"} value is absent, non-string, or empty, are silently skipped.</p>
     *
     * @param rawValue the raw value returned by {@link DocumentSnapshot#get(String)};
     *                 expected to be a {@link List}, but safely handles any type
     * @return an {@link ArrayList} of non-null, non-empty device ID strings;
     *         never {@code null}, but may be empty if {@code rawValue} is not a
     *         {@link List} or contains no valid entries
     */
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
