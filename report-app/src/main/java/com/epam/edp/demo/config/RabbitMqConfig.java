package com.epam.edp.demo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE        = "tour.stats.exchange";
    public static final String BOOKING_QUEUE   = "tour.stats.queue";
    public static final String BOOKING_ROUTING = "booking.state.changed";
    public static final String REVIEW_QUEUE    = "review.stats.queue";
    public static final String REVIEW_ROUTING  = "review.submitted";

    @Bean
    public TopicExchange tourStatsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue bookingStatsQueue() {
        return new Queue(BOOKING_QUEUE, true);
    }

    @Bean
    public Queue reviewStatsQueue() {
        return new Queue(REVIEW_QUEUE, true);
    }

    @Bean
    public Binding bookingBinding(Queue bookingStatsQueue, TopicExchange tourStatsExchange) {
        return BindingBuilder.bind(bookingStatsQueue).to(tourStatsExchange).with(BOOKING_ROUTING);
    }

    @Bean
    public Binding reviewBinding(Queue reviewStatsQueue, TopicExchange tourStatsExchange) {
        return BindingBuilder.bind(reviewStatsQueue).to(tourStatsExchange).with(REVIEW_ROUTING);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
