package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    /** Returns the most recent active (non-used) token for the given email. */
    Optional<PasswordResetToken> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    /** Remove all reset tokens for the email (cleanup before issuing a new one). */
    void deleteByEmail(String email);
}

