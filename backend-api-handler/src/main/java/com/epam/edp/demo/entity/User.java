package com.epam.edp.demo.entity;

import com.epam.edp.demo.entity.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String firstName;

    private String lastName;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private String role;

    private String imageUrl;

    private String phone;

    /**
     * Authentication provider for this account.
     * Defaults to "LOCAL" for email/password accounts — backward-compatible with all existing users.
     * Social login accounts use "GOOGLE", "FACEBOOK", etc.
     */
    @Builder.Default
    private String provider = "LOCAL";

    /**
     * The subject/ID returned by the OAuth2 provider (e.g. Google's "sub" claim).
     * Null for LOCAL accounts. Never used as a credential.
     */
    private String providerId;

    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Builder.Default
    private int failedAttempts = 0;

    private Instant lockExpiry;

    private Instant lastLoginAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

