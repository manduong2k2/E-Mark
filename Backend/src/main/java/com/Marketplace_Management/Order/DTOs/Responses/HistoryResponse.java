package com.Marketplace_Management.Order.DTOs.Responses;

import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"id", "userId", "status", "total", "name", "phone", "address", "note", "items", "createdAt", "updatedAt"})
public class HistoryResponse {
    private UUID id;
    private UUID userId;
    private String status;
    private double total;
    private String name;
    private String phone;
    private String address;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

