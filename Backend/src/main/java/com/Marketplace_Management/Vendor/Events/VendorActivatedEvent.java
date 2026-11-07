package com.Marketplace_Management.Vendor.Events;

import java.io.Serializable;
import java.util.UUID;

import com.Marketplace_Management.Shared.Events.DomainEvent;

public class VendorActivatedEvent extends DomainEvent implements Serializable {
    private UUID userId;

    public VendorActivatedEvent(UUID userId) {
        super();
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
