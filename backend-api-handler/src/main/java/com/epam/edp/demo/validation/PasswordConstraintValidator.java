package com.epam.edp.demo.validation;

import com.epam.edp.demo.util.PasswordValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Bean Validation adapter that delegates to the centralized PasswordValidator.
 * Single source of truth for password rules — no logic duplication.
 */
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private PasswordValidator passwordValidator;

    /** Spring-managed injection (used when running inside application context). */
    @Autowired(required = false)
    public void setPasswordValidator(PasswordValidator passwordValidator) {
        this.passwordValidator = passwordValidator;
    }

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // Fallback for non-Spring contexts (e.g., unit tests using Validation.buildDefaultValidatorFactory)
        if (this.passwordValidator == null) {
            this.passwordValidator = new PasswordValidator();
        }
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true; // null handled by @NotBlank
        }

        List<String> errors = passwordValidator.validate(password);
        if (errors.isEmpty()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        for (String error : errors) {
            context.buildConstraintViolationWithTemplate(error)
                    .addConstraintViolation();
        }
        return false;
    }
}
