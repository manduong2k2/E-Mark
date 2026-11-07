package com.Marketplace_Management.Delivery.Entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@Table(name = "deliveries", indexes = {
    @Index(name = "idx_delivery_order", columnList = "orderId"),
    @Index(name = "idx_delivery_user", columnList = "userId"),
    @Index(name = "idx_delivery_shipping_address", columnList = "shipping_address_id")
})
@EqualsAndHashCode(callSuper = false)

public class Delivery extends UuidEntity{

    @Column(nullable = false)
    private UUID orderId;
    
    @Column(nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private String status;
    
    @Nationalized
    private String recipientName;
    
    private String recipientPhone;
    
    private LocalDateTime estimatedDeliveryDate;
    
    private LocalDateTime actualDeliveryDate;

    @Min(0)
    @Column(nullable = false, columnDefinition = "DOUBLE DEFAULT 0")
    private Double fee;

    @Nationalized
    private String notes;

    @OneToMany(mappedBy = "delivery")
    private List<DeliveryTracking> tracking;

    @ManyToOne
    @JoinColumn(name = "shipping_address_id", nullable = false)
    private Address shippingAddress;

    @ManyToOne
    @JoinColumn(name = "delivery_method_id", nullable = false)
    private DeliveryMethod deliveryMethod;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<DeliveryItem> items;
}
