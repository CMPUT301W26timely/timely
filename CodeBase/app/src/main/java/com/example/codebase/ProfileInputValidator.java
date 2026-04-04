package com.example.codebase;

import java.util.regex.Pattern;

/**
 * Pure validation logic for entrant profile fields.
 * Keeping this separate makes the profile form easier to test.
 */
public final class ProfileInputValidator {

    /**
     * Identifies the first profile field that failed validation.
     */
    public enum Field {
        NONE,
        NAME,
        EMAIL,
        PHONE
    }

    /**
     * Immutable validation result returned by {@link #validate(String, String, String)}.
     */
    public static final class ValidationResult {
        private final Field field;

        private ValidationResult(Field field) {
            this.field = field;
        }

        /**
         * Returns whether all supplied profile fields passed validation.
         *
         * @return {@code true} when no invalid field was detected
         */
        public boolean isValid() {
            return field == Field.NONE;
        }

        /**
         * Returns the first field that failed validation.
         *
         * @return the invalid field, or {@link Field#NONE} when validation succeeded
         */
        public Field getField() {
            return field;
        }
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PHONE_ALLOWED_CHARACTERS = Pattern.compile(
            "^[0-9+()\\-\\s.]{7,}$"
    );

    private ProfileInputValidator() {
        // Utility class.
    }

    /**
     * Validates the profile fields required by the onboarding and edit-profile flows.
     *
     * <p>Validation rules:
     * <ul>
     *   <li>Name must not be blank.</li>
     *   <li>Email must be present and syntactically valid.</li>
     *   <li>Phone number is optional, but if supplied it must match the accepted format.</li>
     * </ul>
     *
     * @param name the entrant name entered in the UI
     * @param email the email address entered in the UI
     * @param phoneNumber the optional phone number entered in the UI
     * @return a {@link ValidationResult} describing the first invalid field, if any
     */
    public static ValidationResult validate(String name, String email, String phoneNumber) {
        if (safeTrim(name).isEmpty()) {
            return new ValidationResult(Field.NAME);
        }

        if (safeTrim(email).isEmpty() || !isValidEmail(email)) {
            return new ValidationResult(Field.EMAIL);
        }

        if (!isValidPhone(phoneNumber)) {
            return new ValidationResult(Field.PHONE);
        }

        return new ValidationResult(Field.NONE);
    }

    /**
     * Trims surrounding whitespace and safely handles {@code null} values.
     *
     * @param value the raw field value, possibly {@code null}
     * @return the trimmed string, or an empty string when {@code value} is {@code null}
     */
    public static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Validates an email address against the app's accepted pattern.
     *
     * @param email the email address to validate
     * @return {@code true} if the email is non-blank and matches the regex
     */
    public static boolean isValidEmail(String email) {
        String trimmedEmail = safeTrim(email);
        return !trimmedEmail.isEmpty() && EMAIL_PATTERN.matcher(trimmedEmail).matches();
    }

    /**
     * Validates an optional phone number.
     *
     * <p>Blank input is treated as valid because the phone number is optional.
     *
     * @param phoneNumber the phone number to validate
     * @return {@code true} if the field is blank or within the accepted character and digit limits
     */
    public static boolean isValidPhone(String phoneNumber) {
        String trimmedPhone = safeTrim(phoneNumber);
        if (trimmedPhone.isEmpty()) {
            return true;
        }

        if (!PHONE_ALLOWED_CHARACTERS.matcher(trimmedPhone).matches()) {
            return false;
        }

        int digitCount = countDigits(trimmedPhone);
        return digitCount >= 7 && digitCount <= 15;
    }

    private static int countDigits(String value) {
        int digits = 0;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                digits++;
            }
        }
        return digits;
    }
}
