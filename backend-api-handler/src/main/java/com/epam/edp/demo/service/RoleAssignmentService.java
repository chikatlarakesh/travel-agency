package com.epam.edp.demo.service;

import com.epam.edp.demo.enums.UserRole;
import com.epam.edp.demo.repository.TravelAgentRepository;
import com.epam.edp.demo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Determines the role to assign to a newly-registered user.
 *
 * <p>Rule:
 * <ol>
 *   <li>If the email exists (case-insensitive) in the pre-defined {@code travel_agents} collection
 *       → assign {@link UserRole#TRAVEL_AGENT}.</li>
 *   <li>Otherwise → assign {@link UserRole#CUSTOMER}.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleAssignmentService {

    private final TravelAgentRepository travelAgentRepository;

    /**
     * Resolves the role for the given email address.
     *
     * @param email the registering user's email (must not be {@code null} or blank)
     * @return the role name string to persist on the {@code User} document
     */
    public String determineRole(String email) {
        if (email == null || email.isBlank()) {
            return UserRole.CUSTOMER.name();
        }

        if (travelAgentRepository.existsByEmailIgnoreCase(email)) {
            log.info("role.assignment.travel_agent email={}", SecurityUtils.maskEmail(email));
            return UserRole.TRAVEL_AGENT.name();
        }

        return UserRole.CUSTOMER.name();
    }
}
