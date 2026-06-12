package com.epam.edp.demo.service;

import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.entity.enums.AccountStatus;
import com.epam.edp.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountLockServiceTest {

    @Mock
    private UserRepository userRepository;

    private AccountLockService accountLockService;

    @BeforeEach
    void setUp() {
        // 5 max attempts, 15 min lock duration
        accountLockService = new AccountLockService(userRepository, 5, 15);
    }

    private User createUser() {
        return User.builder()
                .id("user-1")
                .email("test@example.com")
                .passwordHash("$2a$12$hash")
                .accountStatus(AccountStatus.ACTIVE)
                .failedAttempts(0)
                .build();
    }

    @Test
    @DisplayName("isLocked: returns false when lockExpiry is null")
    void isLocked_noLockExpiry_returnsFalse() {
        User user = createUser();
        user.setLockExpiry(null);

        assertThat(accountLockService.isLocked(user)).isFalse();
    }

    @Test
    @DisplayName("isLocked: returns true when lock has not expired")
    void isLocked_lockNotExpired_returnsTrue() {
        User user = createUser();
        user.setLockExpiry(Instant.now().plus(10, ChronoUnit.MINUTES));

        assertThat(accountLockService.isLocked(user)).isTrue();
    }

    @Test
    @DisplayName("isLocked: auto-resets and returns false when lock has expired")
    void isLocked_lockExpired_resetsAndReturnsFalse() {
        User user = createUser();
        user.setFailedAttempts(5);
        user.setLockExpiry(Instant.now().minus(1, ChronoUnit.MINUTES));

        assertThat(accountLockService.isLocked(user)).isFalse();
        assertThat(user.getFailedAttempts()).isEqualTo(0);
        assertThat(user.getLockExpiry()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("recordFailedAttempt: increments counter")
    void recordFailedAttempt_incrementsCounter() {
        User user = createUser();
        user.setFailedAttempts(2);

        accountLockService.recordFailedAttempt(user);

        assertThat(user.getFailedAttempts()).isEqualTo(3);
        assertThat(user.getLockExpiry()).isNull(); // Not at threshold yet
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("recordFailedAttempt: locks account at max attempts")
    void recordFailedAttempt_reachesMax_setsLockExpiry() {
        User user = createUser();
        user.setFailedAttempts(4); // One more will hit 5

        accountLockService.recordFailedAttempt(user);

        assertThat(user.getFailedAttempts()).isEqualTo(5);
        assertThat(user.getLockExpiry()).isNotNull();
        assertThat(user.getLockExpiry()).isAfter(Instant.now());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("recordFailedAttempt: below threshold does not lock")
    void recordFailedAttempt_belowMax_noLockExpiry() {
        User user = createUser();
        user.setFailedAttempts(0);

        accountLockService.recordFailedAttempt(user);

        assertThat(user.getFailedAttempts()).isEqualTo(1);
        assertThat(user.getLockExpiry()).isNull();
    }

    @Test
    @DisplayName("resetFailedAttempts: zeros counter and clears lock")
    void resetFailedAttempts_clearsAll() {
        User user = createUser();
        user.setFailedAttempts(5);
        user.setLockExpiry(Instant.now().plus(10, ChronoUnit.MINUTES));

        accountLockService.resetFailedAttempts(user);

        assertThat(user.getFailedAttempts()).isEqualTo(0);
        assertThat(user.getLockExpiry()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("recordFailedAttempt: beyond max does NOT extend lock (prevents permanent lock attack)")
    void recordFailedAttempt_beyondMax_doesNotExtendLock() {
        User user = createUser();
        user.setFailedAttempts(6); // Already past max
        Instant originalLockExpiry = Instant.now().plus(10, ChronoUnit.MINUTES);
        user.setLockExpiry(originalLockExpiry);

        accountLockService.recordFailedAttempt(user);

        assertThat(user.getFailedAttempts()).isEqualTo(7);
        // Lock expiry should NOT be overwritten (== not >=)
        assertThat(user.getLockExpiry()).isEqualTo(originalLockExpiry);
        verify(userRepository).save(user);
    }
}

