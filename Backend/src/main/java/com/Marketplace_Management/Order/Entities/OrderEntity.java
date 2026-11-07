package com.Marketplace_Management.Order.Entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, exclude = "items")
@ToString(exclude = "items")
public class OrderEntity extends UuidEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "note", nullable = true)
    private String note;

    @Column(name = "total", nullable = false)
    private double total;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();

    public void changeStatus(String status) {
        this.status = status;
    }

    public double calculateTotal() {
        return items.stream().mapToDouble(OrderItemEntity::calculateTotal).sum();
    }
}
