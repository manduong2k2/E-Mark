package com.Marketplace_Management.Shared.Contracts;

import com.Marketplace_Management.Shared.Events.EventOptions;
import com.Marketplace_Management.Shared.Events.DomainEvent;

public interface IEventPublisher {
    void publish(DomainEvent event, EventOptions options);
}
