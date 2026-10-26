package com.Marketplace_Management.Order.Services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.Marketplace_Management.Cart.Contracts.ICartService;
import com.Marketplace_Management.Cart.DTOs.Responses.CartResponse;
import com.Marketplace_Management.Order.Constants.OrderStatusEnum;
import com.Marketplace_Management.Order.Contracts.IOrderRepository;
import com.Marketplace_Management.Order.Contracts.IOrderService;
import com.Marketplace_Management.Order.DTOs.Commands.ListOrderCommand;
import com.Marketplace_Management.Order.DTOs.Commands.PlaceOrderCommand;
import com.Marketplace_Management.Order.DTOs.Responses.HistoryResponse;
import com.Marketplace_Management.Order.DTOs.Responses.OrderResponse;
import com.Marketplace_Management.Order.Events.OrderPlacedEvent;
import com.Marketplace_Management.Order.Models.Order;
import com.Marketplace_Management.Order.Models.OrderItem;
import com.Marketplace_Management.Shared.Configuration.RabbitMqQueues.OrderQueueConfig;
import com.Marketplace_Management.Shared.Contracts.IEventPublisher;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Errors.Exceptions.BadRequestException;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;
import com.Marketplace_Management.Shared.Events.EventOptions;
import com.Marketplace_Management.Shared.Security.SecurityUtils;

import jakarta.transaction.Transactional;

@Service
public class OrderService implements IOrderService{
    private final IOrderRepository repository;
    private final IEventPublisher eventPublisher;
    private final ICartService cartService;

    public OrderService(IOrderRepository repository, IEventPublisher eventPublisher, ICartService cartService) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.cartService = cartService;
    }

    public PaginatedResponse<HistoryResponse> list(ListOrderCommand command) {
        return repository.findAll(command);
    }

    public PaginatedResponse<HistoryResponse> myOrders(ListOrderCommand command) {
        return repository.findByUserIdWithFilters(SecurityUtils.currentUserId(), command);
    }

    public OrderResponse findById(UUID id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderCommand command) {
        var cart = cartService.getByUserId(SecurityUtils.currentUserId());
        CartResponse cartResponse = cart != null ? CartResponse.from(cart) : null;

        if(cartResponse == null || cartResponse.getItems() == null || cartResponse.getItems().isEmpty()) {
            throw new BadRequestException("Cart not found or empty");
        }

        Order order = Order.builder()
            .userId(SecurityUtils.currentUserId())
            .status(OrderStatusEnum.PENDING.getValue())
            .items(cartResponse.getItems().stream().<OrderItem>map(item -> OrderItem.builder()
                .productId(item.getProductVariantId())
                .quantity(item.getQuantity())
                .total(item.getProductPrice() * item.getQuantity())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .productPrice(item.getProductPrice())
                .productImages(item.getProductImages())
                .productDescription(item.getProductDescription())
                .build()
            ).toList())
            // Recipient entered at checkout (may differ from the account name)
            .name(command.getName())
            .phone(command.getPhone())
            .address(command.getAddress())
            .note(command.getNote())
            .build();

        order.setTotal(order.getItems().stream().mapToDouble(OrderItem::getTotal).sum());

        Order created = repository.create(order);

        eventPublisher.publish(
            new OrderPlacedEvent(created.getUserId()),
            new EventOptions(OrderQueueConfig.ORDER_PLACED_QUEUE, false)
        );

        return OrderResponse.from(created);
    }
}
