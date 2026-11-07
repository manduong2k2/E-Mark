package com.Marketplace_Management.Cart.DTOs.Responses;

import java.util.List;
import java.util.UUID;

import com.Marketplace_Management.Cart.Entities.Cart;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({"id", "status", "totalItemCount", "total", "items"})
public class CartResponse {
    private UUID id;
    private String status;
    private List<CartItemResponse> items;
    private double total;
    private long totalItemCount;

    public static CartResponse from(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setStatus(cart.getStatus());
        response.setItems(
            cart.getItems().stream()
                .map(CartItemResponse::from)
                .toList()
        );
        response.setTotalItemCount(cart.getTotalItemCount());
        response.setTotal(response.getItems().stream().mapToDouble(item -> item.getSubTotal()).sum());
        return response;
    }
}
