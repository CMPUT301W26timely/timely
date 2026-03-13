package com.example.codebase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProfileInputValidatorTest {

    @Test
    public void validate_acceptsRequiredFieldsWithoutPhone() {
        ProfileInputValidator.ValidationResult result =
                ProfileInputValidator.validate("Alex Johnson", "alex@example.com", "");

        assertTrue(result.isValid());
        assertEquals(ProfileInputValidator.Field.NONE, result.getField());
    }

    @Test
    public void validate_rejectsBlankName() {
        ProfileInputValidator.ValidationResult result =
                ProfileInputValidator.validate("   ", "alex@example.com", "");

        assertEquals(ProfileInputValidator.Field.NAME, result.getField());
    }

    @Test
    public void validate_rejectsInvalidEmail() {
        ProfileInputValidator.ValidationResult result =
                ProfileInputValidator.validate("Alex Johnson", "alex-at-example.com", "");

        assertEquals(ProfileInputValidator.Field.EMAIL, result.getField());
    }

    @Test
    public void validate_acceptsFormattedPhoneNumber() {
        ProfileInputValidator.ValidationResult result =
                ProfileInputValidator.validate(
                        "Alex Johnson",
                        "alex@example.com",
                        "+1 (780) 555-0198"
                );

        assertTrue(result.isValid());
    }

    @Test
    public void validate_rejectsMalformedPhoneNumber() {
        ProfileInputValidator.ValidationResult result =
                ProfileInputValidator.validate(
                        "Alex Johnson",
                        "alex@example.com",
                        "12-34"
                );

        assertEquals(ProfileInputValidator.Field.PHONE, result.getField());
    }

    @Test
    public void safeTrim_handlesNull() {
        assertEquals("", ProfileInputValidator.safeTrim(null));
    }
}
