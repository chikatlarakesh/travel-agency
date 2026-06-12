package com.epam.edp.demo.service;

import com.epam.edp.demo.enums.UserRole;
import com.epam.edp.demo.repository.TravelAgentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    @Mock
    private TravelAgentRepository travelAgentRepository;

    private RoleAssignmentService roleAssignmentService;

    @BeforeEach
    void setUp() {
        roleAssignmentService = new RoleAssignmentService(travelAgentRepository);
    }

    @Test
    @DisplayName("determineRole: email found in travel_agents collection → TRAVEL_AGENT")
    void determineRole_emailInTravelAgentList_returnsTravelAgent() {
        when(travelAgentRepository.existsByEmailIgnoreCase("agent@agency.com")).thenReturn(true);

        String role = roleAssignmentService.determineRole("agent@agency.com");

        assertThat(role).isEqualTo(UserRole.TRAVEL_AGENT.name());
        verify(travelAgentRepository).existsByEmailIgnoreCase("agent@agency.com");
    }

    @Test
    @DisplayName("determineRole: email not in travel_agents collection → CUSTOMER")
    void determineRole_emailNotInTravelAgentList_returnsCustomer() {
        when(travelAgentRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);

        String role = roleAssignmentService.determineRole("user@example.com");

        assertThat(role).isEqualTo(UserRole.CUSTOMER.name());
        verify(travelAgentRepository).existsByEmailIgnoreCase("user@example.com");
    }

    @Test
    @DisplayName("determineRole: email matching is case-insensitive")
    void determineRole_caseInsensitiveEmailMatch_returnsTravelAgent() {
        when(travelAgentRepository.existsByEmailIgnoreCase("AGENT@AGENCY.COM")).thenReturn(true);

        String role = roleAssignmentService.determineRole("AGENT@AGENCY.COM");

        assertThat(role).isEqualTo(UserRole.TRAVEL_AGENT.name());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("determineRole: null or blank email → CUSTOMER (no repository call)")
    void determineRole_nullOrBlankEmail_returnsCustomerWithoutRepositoryCall(String email) {
        String role = roleAssignmentService.determineRole(email);

        assertThat(role).isEqualTo(UserRole.CUSTOMER.name());
        verify(travelAgentRepository, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    @DisplayName("determineRole: result is consistent across multiple calls for same email")
    void determineRole_calledMultipleTimes_delegatesEachCallToRepository() {
        when(travelAgentRepository.existsByEmailIgnoreCase("repeat@example.com")).thenReturn(false);

        roleAssignmentService.determineRole("repeat@example.com");
        roleAssignmentService.determineRole("repeat@example.com");

        verify(travelAgentRepository, times(2)).existsByEmailIgnoreCase("repeat@example.com");
    }

    @Test
    @DisplayName("determineRole: role names match UserRole enum constants exactly")
    void determineRole_returnedRoleNames_matchUserRoleEnum() {
        when(travelAgentRepository.existsByEmailIgnoreCase("agent@test.com")).thenReturn(true);
        when(travelAgentRepository.existsByEmailIgnoreCase("user@test.com")).thenReturn(false);

        assertThat(roleAssignmentService.determineRole("agent@test.com"))
                .isEqualTo(UserRole.TRAVEL_AGENT.name())
                .isEqualTo("TRAVEL_AGENT");

        assertThat(roleAssignmentService.determineRole("user@test.com"))
                .isEqualTo(UserRole.CUSTOMER.name())
                .isEqualTo("CUSTOMER");
    }
}
