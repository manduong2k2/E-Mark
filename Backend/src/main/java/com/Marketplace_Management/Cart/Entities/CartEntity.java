package com.Marketplace_Management.Cart.Entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.Marketplace_Management.Cart.Constants.CartStatusEnum;
import com.Marketplace_Management.Shared.Entities.UuidEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "carts")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "items")
@ToString(exclude = "items")
@NoArgsConstructor
@SuperBuilder
public class CartEntity extends UuidEntity {
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = CartStatusEnum.NEW.name();

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<CartItemEntity> items = new ArrayList<>();

    public void addItem(CartItemEntity newItem) {
        requireEditable("add items to");
        if (newItem.getQuantity() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        items.stream().filter(item -> item.getProductVariantId().equals(newItem.getProductVariantId()))
                .findFirst().ifPresentOrElse(
                        item -> item.increaseQuantity(newItem.getQuantity()),
                        () -> {
                            newItem.setCart(this);
                            items.add(newItem);
                        });
    }

    public void removeItem(UUID productVariantId) {
        requireEditable("remove items from");
        if (!items.removeIf(item -> item.getProductVariantId().equals(productVariantId))) {
            throw new IllegalArgumentException("Item not found in cart: " + productVariantId);
        }
    }

    public void updateItemQuantity(UUID productVariantId, int quantity) {
        requireEditable("update items in");
        CartItemEntity item = items.stream().filter(candidate -> candidate.getProductVariantId().equals(productVariantId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Item not found in cart: " + productVariantId));
        if (quantity <= 0) {
            items.remove(item);
        } else {
            item.updateQuantity(quantity);
        }
    }

    public void clear() {
        requireEditable("clear");
        items.clear();
    }

    public void checkout() {
        if (!CartStatusEnum.NEW.name().equals(status)) {
            throw new IllegalStateException("Cart cannot be checked out with status: " + status);
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }
        status = CartStatusEnum.CHECKED_OUT.name();
    }

    public long getTotalItemCount() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    private void requireEditable(String action) {
        if (!CartStatusEnum.NEW.name().equals(status)) {
            throw new IllegalStateException("Cannot " + action + " a cart with status: " + status);
        }
    }
}
