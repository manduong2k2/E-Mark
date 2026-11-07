package com.Marketplace_Management.Catalog.Events;

import java.io.Serializable;
import java.util.UUID;

import com.Marketplace_Management.Shared.Events.DomainEvent;

public class ProductArchivedEvent extends DomainEvent implements Serializable {
    private UUID productId;
    
    public ProductArchivedEvent(UUID productId) {
        super();
        this.productId = productId;
    }
    
    public UUID getProductId() {
        return productId;
    }
}
