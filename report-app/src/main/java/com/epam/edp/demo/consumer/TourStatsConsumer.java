package com.epam.edp.demo.consumer;

import com.epam.edp.demo.config.RabbitMqConfig;
import com.epam.edp.demo.dto.ReviewStatsEvent;
import com.epam.edp.demo.dto.TourStatsEvent;
import com.epam.edp.demo.entity.TourStatDocument;
import com.epam.edp.demo.repository.TourStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourStatsConsumer {

    private final TourStatRepository tourStatRepository;

    @RabbitListener(queues = RabbitMqConfig.BOOKING_QUEUE)
    public void handleBookingEvent(TourStatsEvent event) {
        try {
            TourStatDocument doc = tourStatRepository.findByBookingId(event.getBookingId())
                    .orElseGet(() -> TourStatDocument.builder()
                            .bookingId(event.getBookingId())
                            .build());

            doc.setTravelAgentId(event.getTravelAgentId());
            doc.setAgentName(event.getAgentName());
            doc.setAgentEmail(event.getAgentEmail());
            doc.setTourId(event.getTourId());
            doc.setTourName(event.getTourName());
            doc.setCountry(event.getCountry());
            doc.setCity(event.getCity());
            doc.setBookingStatus(event.getBookingStatus());
            doc.setTouristCount(event.getTouristCount());
            doc.setRevenue(event.getRevenue());
            doc.setEventTimestamp(event.getEventTimestamp());

            tourStatRepository.save(doc);
            log.info("consumer.booking bookingId={} status={}", event.getBookingId(), event.getBookingStatus());
        } catch (Exception ex) {
            log.error("consumer.booking.error bookingId={} reason={}", event.getBookingId(), ex.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMqConfig.REVIEW_QUEUE)
    public void handleReviewEvent(ReviewStatsEvent event) {
        try {
            // Update feedbackScore on all stat documents for this tour + agent combination
            List<TourStatDocument> docs = tourStatRepository
                    .findByTourIdAndEventTimestampBetween(event.getTourId(),
                            java.time.Instant.EPOCH, java.time.Instant.now());

            docs.stream()
                    .filter(d -> event.getTravelAgentId() == null
                            || event.getTravelAgentId().equals(d.getTravelAgentId()))
                    .findFirst()
                    .ifPresent(doc -> {
                        doc.setFeedbackScore((double) event.getRating());
                        tourStatRepository.save(doc);
                        log.info("consumer.review tourId={} rating={}", event.getTourId(), event.getRating());
                    });
        } catch (Exception ex) {
            log.error("consumer.review.error tourId={} reason={}", event.getTourId(), ex.getMessage());
        }
    }
}
