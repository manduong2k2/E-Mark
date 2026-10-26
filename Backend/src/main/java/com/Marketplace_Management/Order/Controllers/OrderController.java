package com.Marketplace_Management.Order.Controllers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Marketplace_Management.Order.Contracts.IOrderService;
import com.Marketplace_Management.Order.DTOs.Commands.ListOrderCommand;
import com.Marketplace_Management.Order.DTOs.Commands.PlaceOrderCommand;
import com.Marketplace_Management.Order.DTOs.Requests.ListOrderRequest;
import com.Marketplace_Management.Order.DTOs.Requests.PlaceOrderRequest;
import com.Marketplace_Management.Order.DTOs.Responses.HistoryResponse;
import com.Marketplace_Management.Order.DTOs.Responses.OrderResponse;
import com.Marketplace_Management.Shared.Annotation.Auth.Authenticated;
import com.Marketplace_Management.Shared.Constants.UserRole;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final IOrderService orderService;

    public OrderController(IOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + UserRole.ADMIN + "')")
    public ResponseEntity<PaginatedResponse<HistoryResponse>> list(@Valid ListOrderRequest request) {
        ListOrderCommand command = ListOrderCommand.fromRequest(request);
        
        PaginatedResponse<HistoryResponse> response = orderService.list(command);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Authenticated
    public ResponseEntity<PaginatedResponse<HistoryResponse>> listByUser(@Valid ListOrderRequest request) {
        ListOrderCommand command = ListOrderCommand.fromRequest(request);

        PaginatedResponse<HistoryResponse> response = orderService.myOrders(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Authenticated
    public ResponseEntity<Map<String, Object>> place(@Valid @RequestBody PlaceOrderRequest request) {
        PlaceOrderCommand command = PlaceOrderCommand.fromRequest(request);

        OrderResponse orderRes = orderService.placeOrder(command);

        HashMap<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Order placed successfully");
        response.put("data", orderRes);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Authenticated
    @PreAuthorize("@orderSecurity.canViewOrder(#id)")
    public ResponseEntity<OrderResponse> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.findById(id));
    }
}
