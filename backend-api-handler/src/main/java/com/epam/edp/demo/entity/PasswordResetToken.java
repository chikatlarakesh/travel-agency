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
 * Stores a hashed 6-digit verification code for password reset.
 * The TTL index on {@code expiresAt} causes MongoDB to automatically
 * delete expired documents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    private String id;

    /** Email address of the user requesting the reset. */
    @Indexed
    private String email;

    /** SHA-256 hash of the 6-digit verification code. Never stored in plain text. */
    private String codeHash;

    /**
     * Expiry timestamp. MongoDB TTL index removes the document automatically
     * after this time passes.
     */
    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;

    /** Marks the token as consumed after a successful password reset. */
    @Builder.Default
    private boolean used = false;

    private Instant createdAt;
}

