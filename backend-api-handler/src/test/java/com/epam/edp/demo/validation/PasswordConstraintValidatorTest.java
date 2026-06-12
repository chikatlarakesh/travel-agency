package com.epam.edp.demo.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordConstraintValidatorTest {

    private static Validator validator;

    @Data
    static class TestForm {
        @ValidPassword
        private String password;

        TestForm(String password) {
            this.password = password;
        }
    }

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private Set<String> validate(String password) {
        return validator.validate(new TestForm(password))
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void validPassword_passes() {
        assertThat(validate("MyPass1!ab")).isEmpty();
    }

    @Test
    void exactMinLength_passes() {
        assertThat(validate("MyPass1!")).isEmpty();
    }

    @Test
    void exactMaxLength_passes() {
        assertThat(validate("MyPass1!abcdefgh")).isEmpty();
    }

    @Test
    void missingUppercase_fails() {
        Set<String> violations = validate("mypass1!");
        assertThat(violations).contains("Password must contain at least one uppercase letter");
    }

    @Test
    void missingLowercase_fails() {
        Set<String> violations = validate("MYPASS1!");
        assertThat(violations).contains("Password must contain at least one lowercase letter");
    }

    @Test
    void missingDigit_fails() {
        Set<String> violations = validate("MyPassword!");
        assertThat(violations).contains("Password must contain at least one digit");
    }

    @Test
    void missingSpecialChar_fails() {
        Set<String> violations = validate("MyPass123");
        assertThat(violations).contains("Password must contain at least one special character");
    }

    @Test
    void multipleRulesFail_reportsAll() {
        Set<String> violations = validate("mypass123");
        assertThat(violations)
                .contains("Password must contain at least one uppercase letter")
                .contains("Password must contain at least one special character");
    }

    @Test
    void nullPassword_passes() {
        // null is handled by @NotBlank — validator should not fire on null
        assertThat(validate(null)).isEmpty();
    }
}
