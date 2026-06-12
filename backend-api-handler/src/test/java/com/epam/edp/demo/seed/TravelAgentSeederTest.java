package com.epam.edp.demo.seed;

import com.epam.edp.demo.entity.TravelAgent;
import com.epam.edp.demo.repository.TravelAgentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelAgentSeederTest {

    @Mock
    private TravelAgentRepository travelAgentRepository;

    @InjectMocks
    private TravelAgentSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplicationArguments args = mock(ApplicationArguments.class);

    @Test
    @DisplayName("empty collection → loads agents from JSON file")
    void run_emptyCollection_loadsAgents() throws Exception {
        // Inject real ObjectMapper via field
        setObjectMapper();

        lenient().when(travelAgentRepository.count()).thenReturn(0L);

        seeder.run(args);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TravelAgent>> captor = ArgumentCaptor.forClass(List.class);
        verify(travelAgentRepository).saveAll(captor.capture());

        List<TravelAgent> saved = captor.getValue();
        assertThat(saved).isNotEmpty();
        assertThat(saved).allSatisfy(agent -> {
            assertThat(agent.getId()).isNotBlank();
            assertThat(agent.getName()).isNotBlank();
            assertThat(agent.getEmail()).isNotBlank();
        });
        assertThat(saved).extracting(TravelAgent::getEmail)
                .contains("priya.sharma@travelagency.com", "lakshmi.prasad@travelagency.com");
    }

    @Test
    @DisplayName("non-empty collection → still reseeds (refreshes schema updates)")
    void run_nonEmptyCollection_stillReseeds() throws Exception {
        setObjectMapper();
        lenient().when(travelAgentRepository.count()).thenReturn(3L);

        seeder.run(args);

        // Should still delete and reseed to apply schema updates
        verify(travelAgentRepository).deleteAll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TravelAgent>> captor = ArgumentCaptor.forClass(List.class);
        verify(travelAgentRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("seeds exactly 2 agents from the JSON file")
    void run_emptyCollection_seedsCorrectCount() throws Exception {
        setObjectMapper();
        lenient().when(travelAgentRepository.count()).thenReturn(0L);

        seeder.run(args);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TravelAgent>> captor = ArgumentCaptor.forClass(List.class);
        verify(travelAgentRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    /** Inject the real ObjectMapper into the seeder via reflection. */
    private void setObjectMapper() throws Exception {
        var field = TravelAgentSeeder.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(seeder, objectMapper);
    }
}
