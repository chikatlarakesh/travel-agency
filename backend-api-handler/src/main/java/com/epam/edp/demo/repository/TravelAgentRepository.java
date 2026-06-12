package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.TravelAgent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TravelAgentRepository extends MongoRepository<TravelAgent, String> {

    @Override
    Optional<TravelAgent> findById(String id);

    boolean existsByEmailIgnoreCase(String email);

    Optional<TravelAgent> findByEmailIgnoreCase(String email);

    default TravelAgent findAny() {
        return findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No travel agents found in database"));
    }
}
