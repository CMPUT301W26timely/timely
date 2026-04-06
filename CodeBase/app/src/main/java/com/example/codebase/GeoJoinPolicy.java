package com.example.codebase;

/**
 * Pure helper methods for geo-required waitlist joins.
 */
public final class GeoJoinPolicy {

    private GeoJoinPolicy() {
    }

    public static boolean requiresLocationCapture(Event event) {
        return event != null && event.isGeoEnabled();
    }

    public static boolean canCompleteJoin(boolean geoEnabled,
                                          boolean hasLocationPermission,
                                          EntrantLocation entrantLocation) {
        if (!geoEnabled) {
            return true;
        }
        return hasLocationPermission
                && entrantLocation != null
                && entrantLocation.hasValidCoordinates();
    }
}
