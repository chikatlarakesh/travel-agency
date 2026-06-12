package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String CHARSET = "UTF-8";

    private final SesClient sesClient;

    @Value("${app.mail.from:noreply@travel-agency.com}")
    private String fromAddress;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // -----------------------------------------------------------------------
    // EmailService implementation
    // -----------------------------------------------------------------------

    @Override
    public void sendPasswordResetCode(String toEmail, String firstName, String code) {
        String resetLink = frontendUrl + "/reset-password?email=" +
                java.net.URLEncoder.encode(toEmail, java.nio.charset.StandardCharsets.UTF_8) +
                "&code=" + code;

        String subject = "Reset your password";
        String htmlBody = """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #333; line-height: 1.6;">
                    <h2 style="color: #0B3857;">Password Reset Request</h2>
                    <p>Hi %s,</p>
                    <p>We received a request to reset your password. Click the button below to set a new password:</p>
                    <p style="margin: 24px 0;">
                      <a href="%s"
                         style="background-color: #027EAC; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block;">
                        Reset Password
                      </a>
                    </p>
                    <p>Or copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; color: #027EAC;">%s</p>
                    <p>This link expires in <strong>15 minutes</strong>.</p>
                    <p>If you did not request a password reset, please ignore this email.</p>
                  </body>
                </html>
                """.formatted(firstName, resetLink, resetLink);
        String textBody = """
                Password Reset Request

                Hi %s,

                We received a request to reset your password.
                Click the link below to reset it (expires in 15 minutes):

                %s

                If you did not request a password reset, please ignore this email.
                """.formatted(firstName, resetLink);

        sendEmail(toEmail, subject, htmlBody, textBody);
        log.info("email.password-reset.sent recipient={}", maskEmail(toEmail));
    }

    @Override
    public void sendEmailConfirmation(String toEmail, String confirmationToken, String userId) {
        String confirmationLink = frontendUrl + "/profile/confirm-email?token=" + confirmationToken + "&userId=" + userId;
        String subject = "Confirm your new email address";
        String htmlBody = """
                <html>
                  <body style="font-family: Arial, sans-serif; line-height: 1.5; color: #1f2937;">
                    <p>Hello,</p>
                    <p>You requested to change the email address on your Travel Agency account.</p>
                    <p>Please confirm your new email by clicking the link below:</p>
                    <p><a href="%s">Confirm your new email address</a></p>
                    <p>If the link is not clickable, copy this URL into your browser:</p>
                    <p>%s</p>
                    <p>This link expires in 24 hours. If you did not make this request, you can safely ignore this email.</p>
                    <p>Travel Agency Team</p>
                  </body>
                </html>
                """.formatted(confirmationLink, confirmationLink);
        String textBody = """
                Hello,

                You requested to change the email address on your Travel Agency account.

                Please confirm your new email by clicking the link below:
                %s

                This link expires in 24 hours. If you did not make this request, you can safely ignore this email.

                Travel Agency Team
                """.formatted(confirmationLink);

        sendEmail(toEmail, subject, htmlBody, textBody);
        log.info("email.confirmation.sent recipient={}", maskEmail(toEmail));
    }

    @Override
    public void sendRegistrationVerificationCode(String toEmail, String firstName, String code) {
        String subject = "Verify your email address";
        String htmlBody = """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #333; line-height: 1.6;">
                    <h2 style="color: #0B3857;">Email Verification</h2>
                    <p>Hi %s,</p>
                    <p>Thank you for registering with Travel Agency. Use the verification code below to confirm your email address:</p>
                    <div style="margin: 24px 0; text-align: center;">
                      <span style="display: inline-block; background-color: #E7F9FF; color: #0B3857; font-size: 32px; font-weight: bold; letter-spacing: 8px; padding: 16px 32px; border-radius: 8px; border: 2px solid #027EAC;">
                        %s
                      </span>
                    </div>
                    <p>This code expires in <strong>15 minutes</strong>.</p>
                    <p>If you did not create an account, please ignore this email.</p>
                  </body>
                </html>
                """.formatted(firstName, code);
        String textBody = """
                Email Verification

                Hi %s,

                Thank you for registering with Travel Agency.
                Your verification code is: %s

                This code expires in 15 minutes.

                If you did not create an account, please ignore this email.
                """.formatted(firstName, code);

        sendEmail(toEmail, subject, htmlBody, textBody);
        log.info("email.registration-verification.sent recipient={}", maskEmail(toEmail));
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void sendEmail(String toEmail, String subject, String htmlBody, String textBody) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(fromAddress)
                .destination(Destination.builder()
                        .toAddresses(toEmail)
                        .build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).charset(CHARSET).build())
                        .body(Body.builder()
                                .html(Content.builder().data(htmlBody).charset(CHARSET).build())
                                .text(Content.builder().data(textBody).charset(CHARSET).build())
                                .build())
                        .build())
                .build();

        try {
            SendEmailResponse result = sesClient.sendEmail(request);
            log.debug("email.sent messageId={}", result.messageId());
        } catch (Exception e) {
            log.error("email.send.failed recipient={} error={}", maskEmail(toEmail), e.getMessage(), e);
            throw new RuntimeException("Failed to send email to " + maskEmail(toEmail), e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String masked = local.length() <= 2
                ? "**"
                : local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1);
        return masked + "@" + parts[1];
    }
}
