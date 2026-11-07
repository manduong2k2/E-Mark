package com.Marketplace_Management.Delivery.Entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

@Entity
@Data
@Table(name = "delivery_items", indexes = {
    @Index(name = "idx_delivery_item_delivery", columnList = "delivery_id"),
    @Index(name = "idx_delivery_item_order_item", columnList = "order_item_id")
})
@EqualsAndHashCode(callSuper = false)
public class DeliveryItem extends UuidEntity {
    private UUID orderItemId;
    private UUID productVariantId;
    private String productName;
    private String productSku;
    private int quantity;
    private int delivered;
    
    @ManyToOne
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;
}
