package com.Marketplace_Management.Order.Events;

import java.io.Serializable;
import java.util.UUID;

import com.Marketplace_Management.Shared.Events.DomainEvent;

public class OrderPlacedEvent extends DomainEvent implements Serializable {
    private UUID userId;

    public OrderPlacedEvent(UUID userId) {
        super();
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}

