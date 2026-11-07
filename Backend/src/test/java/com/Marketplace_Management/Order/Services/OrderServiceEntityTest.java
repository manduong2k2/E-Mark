package com.Marketplace_Management.Order.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Marketplace_Management.Cart.Contracts.ICartService;
import com.Marketplace_Management.Cart.DTOs.Responses.CartItemResponse;
import com.Marketplace_Management.Cart.DTOs.Responses.CartResponse;
import com.Marketplace_Management.Order.Contracts.IOrderRepository;
import com.Marketplace_Management.Order.DTOs.Commands.PlaceOrderCommand;
import com.Marketplace_Management.Order.Entities.Order;
import com.Marketplace_Management.Order.Events.OrderPlacedEvent;
import com.Marketplace_Management.Shared.Contracts.IEventPublisher;
import com.Marketplace_Management.Shared.Errors.Exceptions.BadRequestException;
import com.Marketplace_Management.Shared.Events.EventOptions;
import com.Marketplace_Management.Shared.Security.SecurityUtils;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OrderServiceEntityTest {
    @Mock IOrderRepository repository;
    @Mock IEventPublisher events;
    @Mock ICartService carts;
    @InjectMocks OrderService service;

    @Test
    void checkoutBuildsTheEntityGraphFromCartDtosAndPreservesSnapshotsAndTotals() {
        UUID userId = UUID.randomUUID();
        CartItemResponse first = item(2, 5);
        CartItemResponse second = item(3, 7);
        CartResponse cart = new CartResponse();
        cart.setItems(List.of(first, second));
        when(carts.getByUserId(userId)).thenReturn(cart);
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
        when(repository.create(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            assertEquals(31, order.getTotal());
            order.getItems().forEach(item -> {
                assertSame(order, item.getOrder());
                assertEquals(item.getQuantity() * item.getProductPrice(), item.getTotal());
                item.setId(UUID.randomUUID());
            });
            order.setId(UUID.randomUUID());
            order.setCreatedAt(created);
            return order;
        });

        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::currentUserId).thenReturn(userId);
            var response = service.placeOrder(PlaceOrderCommand.builder()
                    .name("Recipient").phone("0900000000").address("Delivery address").note("Note").build());

            assertEquals(31, response.getTotal());
            assertEquals(2, response.getItems().size());
            assertEquals(first.getProductVariantId(), response.getItems().get(0).getProductId());
            assertEquals(first.getProductImages(), response.getItems().get(0).getProductImages());
            assertEquals("Recipient", response.getName());
            assertEquals(created, response.getCreatedAt());
            var json = JsonMapper.builder().build().valueToTree(response.getItems().get(0));
            assertFalse(json.has("order"));
        }
        var event = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(events).publish(event.capture(), any(EventOptions.class));
        assertEquals(userId, event.getValue().getUserId());
    }

    @Test
    void emptyCartCannotProduceAnOrderOrPublishAnEvent() {
        UUID userId = UUID.randomUUID();
        when(carts.getByUserId(userId)).thenReturn(null);
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::currentUserId).thenReturn(userId);
            assertThrows(BadRequestException.class, () -> service.placeOrder(PlaceOrderCommand.builder().build()));
        }
        verify(repository, never()).create(any());
        verifyNoInteractions(events);
    }

    private CartItemResponse item(int quantity, double price) {
        CartItemResponse item = new CartItemResponse();
        item.setProductVariantId(UUID.randomUUID());
        item.setProductName("Product");
        item.setProductSku("SKU");
        item.setProductPrice(price);
        item.setQuantity(quantity);
        item.setProductImages(List.of("http://api/image.png"));
        item.setProductDescription("Description");
        return item;
    }
}
