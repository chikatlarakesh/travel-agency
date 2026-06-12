package com.epam.edp.demo.integration;

import com.epam.edp.demo.config.RabbitMqConfig;
import com.epam.edp.demo.dto.TourStatsEvent;
import com.epam.edp.demo.entity.TourStatDocument;
import com.epam.edp.demo.repository.TourStatRepository;
import com.epam.edp.demo.service.TravelAgentPerformanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ReportIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry reg) {
        reg.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        reg.add("spring.rabbitmq.host",    rabbit::getHost);
        reg.add("spring.rabbitmq.port",    rabbit::getAmqpPort);
    }

    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired TourStatRepository tourStatRepository;
    @Autowired TravelAgentPerformanceService taService;

    @BeforeEach
    void clean() {
        tourStatRepository.deleteAll();
    }

    @Test
    void eventPublished_consumedAndStored() {
        String bookingId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        TourStatsEvent event = TourStatsEvent.builder()
                .bookingId(bookingId)
                .travelAgentId("agent-1")
                .agentName("Alice")
                .agentEmail("alice@test.com")
                .tourId("tour-1")
                .tourName("Paris Tour")
                .country("France")
                .city("Paris")
                .bookingStatus("FINISHED")
                .touristCount(4)
                .revenue(4000.0)
                .eventTimestamp(now)
                .build();

        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.BOOKING_ROUTING, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<TourStatDocument> doc = tourStatRepository.findByBookingId(bookingId);
            assertThat(doc).isPresent();
            assertThat(doc.get().getBookingStatus()).isEqualTo("FINISHED");
            assertThat(doc.get().getTouristCount()).isEqualTo(4);
        });
    }

    @Test
    void storedEvent_appearsInReport() {
        Instant now = Instant.now();
        Instant from = now.minus(30, ChronoUnit.DAYS);

        TourStatDocument doc = TourStatDocument.builder()
                .bookingId(UUID.randomUUID().toString())
                .travelAgentId("agent-2")
                .agentName("Bob")
                .agentEmail("bob@test.com")
                .tourId("tour-2")
                .tourName("Rome Tour")
                .country("Italy")
                .city("Rome")
                .bookingStatus("FINISHED")
                .touristCount(3)
                .revenue(3000.0)
                .feedbackScore(4.5)
                .eventTimestamp(now.minus(15, ChronoUnit.DAYS))
                .build();

        tourStatRepository.save(doc);

        var rows = taService.buildReport(from, now);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getAgentName()).isEqualTo("Bob");
        assertThat(rows.get(0).getToursSold()).isEqualTo(3);
        assertThat(rows.get(0).getRevenueUsd()).isEqualTo(3000.0);
    }
}
