package com.epam.edp.demo.service;

/**
 * Abstraction for transactional email delivery.
 * Production implementation: {@link com.epam.edp.demo.service.impl.EmailServiceImpl} via AWS SES.
 */
public interface EmailService {

    /**
     * Sends a password-reset verification code to the specified recipient.
     *
     * @param toEmail   recipient email address
     * @param firstName recipient's first name (used in personalisation)
     * @param code      6-digit verification code (plain text – never stored)
     */
    void sendPasswordResetCode(String toEmail, String firstName, String code);

    /**
     * Sends an email-change confirmation link to the new address.
     *
     * @param toEmail           the new (unconfirmed) email address
     * @param confirmationToken the raw UUID token to embed in the link
     * @param userId            the user's ID (used to build the confirmation URL)
     */
    void sendEmailConfirmation(String toEmail, String confirmationToken, String userId);

    /**
     * Sends a 6-digit OTP to a prospective user during email verification (registration step 1).
     *
     * @param toEmail   recipient email address
     * @param firstName recipient's first name (used for personalisation)
     * @param code      6-digit verification code (plain text – never stored)
     */
    void sendRegistrationVerificationCode(String toEmail, String firstName, String code);
}
