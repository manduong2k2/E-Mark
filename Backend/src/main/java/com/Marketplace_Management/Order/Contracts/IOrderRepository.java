package com.Marketplace_Management.Order.Contracts;

import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Order.DTOs.Commands.ListOrderCommand;
import com.Marketplace_Management.Order.DTOs.Responses.HistoryResponse;
import com.Marketplace_Management.Order.Entities.Order;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

public interface IOrderRepository {
    PaginatedResponse<HistoryResponse> findAll(ListOrderCommand command);
    Order create(Order order);
    Optional<Order> findById(UUID id);
    Order updateStatus(UUID id, String status);
    void delete(UUID id);
    PaginatedResponse<HistoryResponse> findByUserIdWithFilters(UUID userId, ListOrderCommand command);
}
