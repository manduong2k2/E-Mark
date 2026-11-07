package com.Marketplace_Management.Cart.Entities;

import java.util.UUID;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "cart_items")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "cart")
@ToString(exclude = "cart")
@NoArgsConstructor
@SuperBuilder
public class CartItemEntity extends UuidEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cart_id", nullable = false)
    private CartEntity cart;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public void increaseQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        quantity += amount;
    }

    public void decreaseQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (quantity - amount < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        quantity -= amount;
    }

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }
}
