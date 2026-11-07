package com.Marketplace_Management.Shared.Events;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.Marketplace_Management.Shared.Contracts.IEventPublisher;

@Component
public class RabbitMQEventPublisher implements IEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public RabbitMQEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(DomainEvent data, String queue) {
        rabbitTemplate.convertAndSend(queue, data);
    }

    @Override
    @Async
    public void publish(DomainEvent event, EventOptions options) {
        sendMessage(event, options.getDestination());
    }
}
