package com.epam.edp.demo.service;

import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Manages account lockout after repeated failed login attempts.
 * State persisted in MongoDB — survives pod restarts and shared across cluster.
 */
@Slf4j
@Service
public class AccountLockService {

    private final UserRepository userRepository;
    private final int maxFailedAttempts;
    private final Duration lockDuration;

    public AccountLockService(
            UserRepository userRepository,
            @Value("${security.account-lock.max-attempts:5}") int maxFailedAttempts,
            @Value("${security.account-lock.duration-minutes:15}") int lockDurationMinutes) {
        this.userRepository = userRepository;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDuration = Duration.ofMinutes(lockDurationMinutes);
    }

    /**
     * Check if the user account is currently locked.
     * If the lock has expired, auto-reset and return false.
     */
    public boolean isLocked(User user) {
        if (user.getLockExpiry() == null) {
            return false;
        }
        if (Instant.now().isAfter(user.getLockExpiry())) {
            // Lock expired — auto-reset
            resetFailedAttempts(user);
            return false;
        }
        return true;
    }

    /**
     * Record a failed login attempt. If threshold reached, lock the account.
     * Only sets lock expiry when threshold is FIRST reached (== not >=)
     * to prevent attackers from perpetually extending the lock window.
     */
    public void recordFailedAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);

        if (attempts == maxFailedAttempts) {
            user.setLockExpiry(Instant.now().plus(lockDuration));
            log.warn("auth.account.locked userId={} failedAttempts={}", user.getId(), attempts);
        }

        userRepository.save(user);
    }

    /**
     * Reset failed attempts counter and clear lock. Called on successful login.
     */
    public void resetFailedAttempts(User user) {
        user.setFailedAttempts(0);
        user.setLockExpiry(null);
        userRepository.save(user);
    }
}

