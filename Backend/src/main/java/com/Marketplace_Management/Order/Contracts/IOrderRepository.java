package com.Marketplace_Management.Order.Contracts;

import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Order.DTOs.Commands.ListOrderCommand;
import com.Marketplace_Management.Order.DTOs.Responses.HistoryResponse;
import com.Marketplace_Management.Order.Entities.OrderEntity;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

public interface IOrderRepository {
    PaginatedResponse<HistoryResponse> findAll(ListOrderCommand command);
    OrderEntity create(OrderEntity order);
    Optional<OrderEntity> findById(UUID id);
    OrderEntity updateStatus(UUID id, String status);
    void delete(UUID id);
    PaginatedResponse<HistoryResponse> findByUserIdWithFilters(UUID userId, ListOrderCommand command);
}
