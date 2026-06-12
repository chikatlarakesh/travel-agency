package com.epam.edp.demo.seed;

import com.epam.edp.demo.entity.TravelAgent;
import com.epam.edp.demo.repository.TravelAgentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the {@code travel_agents} collection from {@code seed/travel_agents.json}
 * on application startup, but only if the collection is currently empty.
 *
 * <p>This ensures every developer's local MongoDB starts with the pre-defined
 * travel agent allowlist without any manual steps.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TravelAgentSeeder implements ApplicationRunner {

    private final TravelAgentRepository travelAgentRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Always delete and reseed to ensure schema updates (like new phone field) are applied
        travelAgentRepository.deleteAll();
        
        ClassPathResource resource = new ClassPathResource("seed/travel_agents.json");
        List<TravelAgent> agents = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<TravelAgent>>() {}
        );

        travelAgentRepository.saveAll(agents);
        log.info("seed.travel_agents.loaded count={}", agents.size());
    }
}
