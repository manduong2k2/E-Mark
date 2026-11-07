package com.Marketplace_Management.Delivery.Entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

import org.hibernate.annotations.Nationalized;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

@Entity
@Table(name = "delivery_trackings", indexes = {
    @Index(name = "idx_delivery_tracking_delivery", columnList = "deliveryId")
})
@Data
@EqualsAndHashCode(callSuper = false)
public class DeliveryTracking extends UuidEntity {
    
    @Nationalized
    private String status;
    
    private LocalDateTime timestamp;

    @Nationalized
    private String notes;
    
    @ManyToOne
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;
}
