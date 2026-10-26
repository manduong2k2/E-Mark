package com.Marketplace_Management.Order.Contracts;

import java.util.UUID;

import com.Marketplace_Management.Order.DTOs.Commands.ListOrderCommand;
import com.Marketplace_Management.Order.DTOs.Commands.PlaceOrderCommand;
import com.Marketplace_Management.Order.DTOs.Responses.HistoryResponse;
import com.Marketplace_Management.Order.DTOs.Responses.OrderResponse;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

public interface IOrderService {
    PaginatedResponse<HistoryResponse> list(ListOrderCommand command);
    OrderResponse findById(UUID id);
    PaginatedResponse<HistoryResponse> myOrders(ListOrderCommand command);
    OrderResponse placeOrder(PlaceOrderCommand command);
}
