package com.epam.edp.demo.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized, reusable password validation component.
 * Enforces application-specific password policy (overrides NIST defaults per business requirement):
 *
 * <ul>
 *   <li>Length: 8–16 characters</li>
 *   <li>At least one uppercase letter (A–Z)</li>
 *   <li>At least one lowercase letter (a–z)</li>
 *   <li>At least one numeric digit (0–9)</li>
 *   <li>At least one special character (!@#$%^&* etc.)</li>
 * </ul>
 *
 * <p>Note: Although NIST SP 800-63B discourages strict composition rules,
 * this is a business/UI requirement for this application.</p>
 */
@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 16;
    private static final String CHARACTERS_SUFFIX = " characters";

    /**
     * Validate a password against the policy.
     * Returns a list of failed rule descriptions (empty = valid).
     *
     * @param password the raw password to validate
     * @return list of validation error messages; empty if password is valid
     */
    public List<String> validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password is required");
            return errors;
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + CHARACTERS_SUFFIX);
        }

        if (password.length() > MAX_LENGTH) {
            errors.add("Password must be at most " + MAX_LENGTH + CHARACTERS_SUFFIX);
        }

        if (!password.matches(".*[A-Z].*")) {
            errors.add("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            errors.add("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*[0-9].*")) {
            errors.add("Password must contain at least one digit");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~].*")) {
            errors.add("Password must contain at least one special character");
        }

        return errors;
    }

    /**
     * Convenience method: returns true if password meets all rules.
     */
    public boolean isValid(String password) {
        return validate(password).isEmpty();
    }
}

