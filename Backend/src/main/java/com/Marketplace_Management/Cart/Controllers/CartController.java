package com.Marketplace_Management.Cart.Controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Marketplace_Management.Shared.Constants.Http;
import com.Marketplace_Management.Cart.Constants.Message;
import com.Marketplace_Management.Cart.Contracts.ICartService;
import com.Marketplace_Management.Cart.DTOs.Commands.AddToCartCommand;
import com.Marketplace_Management.Cart.DTOs.Commands.RemoveFromCartCommand;
import com.Marketplace_Management.Cart.DTOs.Commands.UpdateCartItemCommand;
import com.Marketplace_Management.Cart.DTOs.Requests.AddItemRequest;
import com.Marketplace_Management.Cart.DTOs.Requests.UpdateItemRequest;
import com.Marketplace_Management.Cart.DTOs.Responses.CartResponse;
import com.Marketplace_Management.Shared.Annotation.Auth.Authenticated;
import com.Marketplace_Management.Shared.Security.SecurityUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final ICartService cartService;

    public CartController(ICartService cartService) {
        this.cartService = cartService;
    }

    @Authenticated
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyCart() {

        UUID userId = SecurityUtils.currentUserId();
        CartResponse cart = cartService.getByUserId(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        if (cart == null) {
            response.put("cart", null);
            response.put("message", "Cart is empty");
        } else {
            response.put("cart", cart);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    @Authenticated
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddItemRequest request) {

        AddToCartCommand command = AddToCartCommand.fromRequest(request);

        CartResponse response = cartService.addItem(command);

        return ResponseEntity.status(Http.OK).body(response);
    }

    @Authenticated
    @PutMapping("/items/{productVariantId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable UUID productVariantId,
            @Valid @RequestBody UpdateItemRequest request) {

        UUID userId = SecurityUtils.currentUserId();

        UpdateCartItemCommand command = new UpdateCartItemCommand();
        command.setUserId(userId);
        command.setProductVariantId(productVariantId);
        command.setQuantity(request.getQuantity());

        CartResponse response = cartService.updateItem(command);

        return ResponseEntity.status(Http.OK).body(response);
    }

    @Authenticated
    @DeleteMapping("/items/{productVariantId}")
    public ResponseEntity<Map<String, Object>> removeItem(@PathVariable UUID productVariantId) {
        UUID userId = SecurityUtils.currentUserId();

        RemoveFromCartCommand command = new RemoveFromCartCommand(userId, productVariantId);

        CartResponse response = cartService.removeItem(command);
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", Message.CART_ITEM_REMOVED);
        responseBody.put("data", response);

        return ResponseEntity.status(Http.OK).body(responseBody);
    }

    @Authenticated
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearCart() {
        UUID userId = SecurityUtils.currentUserId();

        cartService.clearCart(userId);
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "Cart cleared successfully");

        return ResponseEntity.status(Http.OK).body(responseBody);
    }
}
