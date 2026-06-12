package com.epam.edp.demo.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator();
    }

    @Test
    @DisplayName("valid password passes all rules")
    void validate_validPassword_returnsNoErrors() {
        List<String> errors = validator.validate("StrongP1!");
        assertThat(errors).isEmpty();
        assertThat(validator.isValid("StrongP1!")).isTrue();
    }

    @Test
    @DisplayName("null password returns error")
    void validate_null_returnsError() {
        List<String> errors = validator.validate(null);
        assertThat(errors).contains("Password is required");
    }

    @Test
    @DisplayName("empty password returns error")
    void validate_empty_returnsError() {
        List<String> errors = validator.validate("");
        assertThat(errors).contains("Password is required");
    }

    @Test
    @DisplayName("too short password returns error")
    void validate_tooShort_returnsError() {
        List<String> errors = validator.validate("Ab1!xyz");
        assertThat(errors).anyMatch(e -> e.contains("at least 8"));
    }

    @Test
    @DisplayName("too long password returns error")
    void validate_tooLong_returnsError() {
        List<String> errors = validator.validate("Abcdefgh1!1234567");
        assertThat(errors).anyMatch(e -> e.contains("at most 16"));
    }

    @Test
    @DisplayName("missing uppercase returns error")
    void validate_noUppercase_returnsError() {
        List<String> errors = validator.validate("abcdefg1!");
        assertThat(errors).anyMatch(e -> e.contains("uppercase"));
    }

    @Test
    @DisplayName("missing lowercase returns error")
    void validate_noLowercase_returnsError() {
        List<String> errors = validator.validate("ABCDEFG1!");
        assertThat(errors).anyMatch(e -> e.contains("lowercase"));
    }

    @Test
    @DisplayName("missing digit returns error")
    void validate_noDigit_returnsError() {
        List<String> errors = validator.validate("Abcdefgh!");
        assertThat(errors).anyMatch(e -> e.contains("digit"));
    }

    @Test
    @DisplayName("missing special character returns error")
    void validate_noSpecialChar_returnsError() {
        List<String> errors = validator.validate("Abcdefg1");
        assertThat(errors).anyMatch(e -> e.contains("special character"));
    }

    @Test
    @DisplayName("multiple violations returns all errors")
    void validate_multipleViolations_returnsAllErrors() {
        List<String> errors = validator.validate("abc");
        // Should flag: too short, no uppercase, no digit, no special char
        assertThat(errors.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("exactly 8 chars valid password passes")
    void validate_exactly8Chars_passes() {
        assertThat(validator.isValid("Abcdef1!")).isTrue();
    }

    @Test
    @DisplayName("exactly 16 chars valid password passes")
    void validate_exactly16Chars_passes() {
        assertThat(validator.isValid("Abcdefghijk12!ab")).isTrue();
    }
}

