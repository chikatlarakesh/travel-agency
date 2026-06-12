package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.EmailVerificationToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends MongoRepository<EmailVerificationToken, String> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    /** Revoke all pending tokens for a user when a new email-change is initiated. */
    void deleteAllByUserId(String userId);
}
