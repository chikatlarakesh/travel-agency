package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.RegistrationToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RegistrationTokenRepository extends MongoRepository<RegistrationToken, String> {

    /** Returns the most recent active (non-used) token for the given email. */
    Optional<RegistrationToken> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    /** Remove all registration tokens for the email (cleanup before issuing a new one). */
    void deleteByEmail(String email);
}

