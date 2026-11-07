package com.Marketplace_Management.Catalog.Events;

import java.io.Serializable;
import java.util.UUID;

import com.Marketplace_Management.Shared.Events.DomainEvent;

public class ProductOutStockEvent extends DomainEvent implements Serializable{
    private UUID productId;
    private String newStatus;
    
    public ProductOutStockEvent(UUID productId, String newStatus) {
        super();
        this.productId = productId;
        this.newStatus = newStatus;
    }
    
    public UUID getProductId() {
        return productId;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
}
