package com.epam.edp.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Stores a pending email-change request.
 * The raw token is never persisted; only its SHA-256 hash is stored (same pattern as RefreshToken).
 * MongoDB TTL index on {@code expiresAt} auto-purges expired documents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    private String id;

    /** SHA-256 hash of the raw token sent to the user. */
    @Indexed(unique = true)
    private String tokenHash;

    /** The user who initiated the email change. */
    @Indexed
    private String userId;

    /** The new email address to switch to after verification. */
    private String newEmail;

    /** TTL: MongoDB will delete the document after this instant. */
    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;

    @Builder.Default
    private boolean used = false;
}
