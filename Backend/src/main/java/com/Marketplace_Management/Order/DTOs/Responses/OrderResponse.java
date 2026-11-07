package com.Marketplace_Management.Order.DTOs.Responses;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.Marketplace_Management.Order.Entities.OrderEntity;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({"id", "userId", "status", "total", "name", "phone", "address", "note", "items", "createdAt", "updatedAt"})
public class OrderResponse {
    private UUID id;
    private UUID userId;
    private String status;
    private double total;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String name;
    private String phone;
    private String address;
    private String note;

    public static OrderResponse from(OrderEntity order) {
        OrderResponse r = new OrderResponse();
        r.id        = order.getId();
        r.userId    = order.getUserId();
        r.status    = order.getStatus();
        r.total     = order.getTotal();
        r.items     = order.getItems().stream().map(OrderItemResponse::from).toList();
        r.createdAt = order.getCreatedAt();
        r.updatedAt = order.getUpdatedAt();
        r.name      = order.getName();
        r.phone     = order.getPhone();
        r.address   = order.getAddress();
        r.note      = order.getNote();
        return r;
    }
}
