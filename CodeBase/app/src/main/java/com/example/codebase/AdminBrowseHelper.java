package com.example.codebase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Pure helper methods shared by the administrator browse screens.
 *
 * <p>The methods here stay Android-free on purpose so they can be covered with
 * fast local JUnit tests.</p>
 */
public final class AdminBrowseHelper {

    private AdminBrowseHelper() {
    }

    /**
     * Returns all non-null events sorted for the administrator event browser.
     *
     * <p>Upcoming events appear first, undated events last, and title acts as a
     * stable tie-breaker so the admin list stays predictable between refreshes.</p>
     *
     * @param events raw event list from Firestore
     * @return sorted copy safe for UI display
     */
    public static List<Event> sortEventsForAdmin(List<Event> events) {
        List<Event> sortedEvents = new ArrayList<>();
        if (events == null) {
            return sortedEvents;
        }

        for (Event event : events) {
            if (event != null) {
                sortedEvents.add(event);
            }
        }

        sortedEvents.sort((left, right) -> {
            int dateComparison = compareDates(left.getStartDate(), right.getStartDate());
            if (dateComparison != 0) {
                return dateComparison;
            }
            return safeText(left.getTitle()).compareToIgnoreCase(safeText(right.getTitle()));
        });
        return sortedEvents;
    }

    /**
     * Returns all non-null profiles sorted for the administrator profile browser.
     *
     * <p>Administrator accounts appear first, then all remaining profiles are sorted
     * by display name with device ID as a stable fallback.</p>
     *
     * @param users raw user list from Firestore
     * @return sorted copy safe for UI display
     */
    public static List<User> sortProfilesForAdmin(List<User> users) {
        List<User> sortedUsers = new ArrayList<>();
        if (users == null) {
            return sortedUsers;
        }

        for (User user : users) {
            if (user != null) {
                sortedUsers.add(user);
            }
        }

        sortedUsers.sort((left, right) -> {
            int roleComparison = roleSortKey(left).compareTo(roleSortKey(right));
            if (roleComparison != 0) {
                return roleComparison;
            }

            int nameComparison = profileSortKey(left).compareToIgnoreCase(profileSortKey(right));
            if (nameComparison != 0) {
                return nameComparison;
            }

            return safeText(left.getDeviceId()).compareToIgnoreCase(safeText(right.getDeviceId()));
        });
        return sortedUsers;
    }

    /**
     * Returns whether a profile includes the minimum contact data the app expects.
     *
     * @param user the profile to inspect
     * @return {@code true} when the profile has both name and email
     */
    public static boolean isProfileComplete(User user) {
        return !safeText(user != null ? user.getName() : null).isEmpty()
                && !safeText(user != null ? user.getEmail() : null).isEmpty();
    }

    /**
     * Returns whether the given profile belongs to an administrator.
     *
     * @param user the profile to inspect
     * @return {@code true} when the stored role resolves to admin
     */
    public static boolean isAdminProfile(User user) {
        return "admin".equalsIgnoreCase(safeText(user != null ? user.getRole() : null));
    }

    /**
     * Returns whether the given profile has lost organizer privileges.
     *
     * @param user the profile to inspect
     * @return {@code true} when organizer access is revoked
     */
    public static boolean isOrganizerRevoked(User user) {
        return user != null && user.isOrganizerPrivilegesRevoked();
    }

    private static int compareDates(Date left, Date right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private static String profileSortKey(User user) {
        String name = safeText(user != null ? user.getName() : null);
        if (!name.isEmpty()) {
            return name;
        }
        return safeText(user != null ? user.getDeviceId() : null);
    }

    private static String roleSortKey(User user) {
        return isAdminProfile(user) ? "0" : "1";
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
