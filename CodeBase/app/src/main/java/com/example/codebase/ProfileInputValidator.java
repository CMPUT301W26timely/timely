package com.example.codebase;

import java.util.regex.Pattern;

/**
 * Pure validation logic for entrant profile fields.
 * Keeping this separate makes the profile form easier to test.
 */
public final class ProfileInputValidator {

    public enum Field {
        NONE,
        NAME,
        EMAIL,
        PHONE
    }

    public static final class ValidationResult {
        private final Field field;

        private ValidationResult(Field field) {
            this.field = field;
        }

        public boolean isValid() {
            return field == Field.NONE;
        }

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

    public static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isValidEmail(String email) {
        String trimmedEmail = safeTrim(email);
        return !trimmedEmail.isEmpty() && EMAIL_PATTERN.matcher(trimmedEmail).matches();
    }

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
