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
 * Stores a hashed 6-digit verification code issued during registration step 1.
 * Also caches firstName and lastName so they are available when the user
 * completes registration in step 3.
 * <p>
 * The TTL index on {@code expiresAt} causes MongoDB to automatically delete
 * expired documents, so no scheduled cleanup is needed.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "registration_tokens")
public class RegistrationToken {

    @Id
    private String id;

    /** Email address of the prospective user. */
    @Indexed
    private String email;

    /** First name captured in step 1 – reused when creating the user in step 3. */
    private String firstName;

    /** Last name captured in step 1 – reused when creating the user in step 3. */
    private String lastName;

    /**
     * SHA-256 hash of the 6-digit verification code.
     * The plain-text code is only ever held in memory and sent by email.
     */
    private String codeHash;

    /**
     * Expiry timestamp. MongoDB TTL index auto-removes the document after 15 min.
     */
    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;

    /** Marks the token as consumed after a successful registration. */
    @Builder.Default
    private boolean used = false;

    private Instant createdAt;
}

