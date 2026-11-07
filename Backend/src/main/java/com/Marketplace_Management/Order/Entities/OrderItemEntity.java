package com.Marketplace_Management.Order.Entities;

import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
@Table(name = "order_items")
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, exclude = "order")
@ToString(exclude = "order")
public class OrderItemEntity extends UuidEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private OrderEntity order;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "total", nullable = false)
    private double total;

    @Column(name = "product_sku")
    private String productSku;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_price", nullable = false)
    private double productPrice;

    @Column(name = "product_images")
    private List<String> productImages;

    @Column(name = "product_description")
    private String productDescription;

    public void plusOne() {
        quantity++;
    }

    public void minusOne() {
        quantity--;
    }

    public double calculateTotal() {
        return quantity * productPrice;
    }
}
