package com.Marketplace_Management.Order.Security;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.Marketplace_Management.Order.Contracts.IOrderRepository;
import com.Marketplace_Management.Order.Entities.Order;
import com.Marketplace_Management.Shared.Security.SecurityUtils;

/** Used in @PreAuthorize("@orderSecurity.canViewOrder(#id)"): the order's owner or an admin. */
@Component
public class OrderSecurity {

    private final IOrderRepository orderRepository;

    public OrderSecurity(IOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public boolean canViewOrder(UUID orderId) {
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        return orderRepository.findById(orderId)
                .map(Order::getUserId)
                .map(SecurityUtils::isOwnerOrAdmin)
                .orElse(false);
    }
}
