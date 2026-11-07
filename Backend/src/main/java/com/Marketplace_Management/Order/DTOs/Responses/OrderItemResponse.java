package com.Marketplace_Management.Order.DTOs.Responses;

import java.util.List;
import java.util.UUID;

import com.Marketplace_Management.Order.Entities.OrderItem;

import lombok.Data;

@Data
public class OrderItemResponse {
    private UUID id;
    private UUID productId;
    private int quantity;
    private double total;
    private String productName;
    private String productSku;
    private double price;
    private List<String> productImages;
    private String productDescription;

    public static OrderItemResponse from(OrderItem item) {
        OrderItemResponse r = new OrderItemResponse();
        r.id                    = item.getId();
        r.productId             = item.getProductId();
        r.quantity              = item.getQuantity();
        r.total                 = item.getTotal();
        r.productName           = item.getProductName();
        r.productSku            = item.getProductSku();
        r.price                 = item.getProductPrice();
        r.productImages         = item.getProductImages();
        r.productDescription    = item.getProductDescription();
        return r;
    }
}
