package com.ticketing.event.eventing_management.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TICKET_EXCHANGE = "tickets.exchange";
    public static final String TICKET_SOLD_ROUTING_KEY = "ticket.sold";
    public static final String TICKET_VALIDATED_ROUTING_KEY = "ticket.validated";

    public static final String TICKET_SOLD_QUEUE = "ticket.sold.queue";
    public static final String TICKET_VALIDATED_QUEUE = "ticket.validated.queue";

    @Bean
    public TopicExchange ticketExchange() {
        return new TopicExchange(TICKET_EXCHANGE);
    }

    @Bean
    public Queue ticketSoldQueue() {
        return new Queue(TICKET_SOLD_QUEUE, true);
    }

    @Bean
    public Binding ticketSoldBinding() {
        return BindingBuilder.bind(ticketSoldQueue()).to(ticketExchange()).with(TICKET_SOLD_ROUTING_KEY);
    }

    @Bean
    public Queue ticketValidatedQueue() {
        return new Queue(TICKET_VALIDATED_QUEUE, true);
    }

    @Bean
    public Binding ticketValidatedBinding() {
        return BindingBuilder.bind(ticketValidatedQueue()).to(ticketExchange()).with(TICKET_VALIDATED_ROUTING_KEY);
    }
}