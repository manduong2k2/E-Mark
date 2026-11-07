package com.Marketplace_Management.Cart.Contracts;

import java.util.UUID;

import com.Marketplace_Management.Cart.DTOs.Commands.AddToCartCommand;
import com.Marketplace_Management.Cart.DTOs.Commands.CheckoutCartCommand;
import com.Marketplace_Management.Cart.DTOs.Commands.RemoveFromCartCommand;
import com.Marketplace_Management.Cart.DTOs.Commands.UpdateCartItemCommand;
import com.Marketplace_Management.Cart.DTOs.Responses.CartResponse;

public interface ICartService {
    CartResponse addItem(AddToCartCommand command);
    CartResponse removeItem(RemoveFromCartCommand command);
    CartResponse updateItem(UpdateCartItemCommand command);
    CartResponse checkout(CheckoutCartCommand command);
    CartResponse getByUserId(UUID userId);
    CartResponse getById(UUID id);
    void clearCart(UUID userId);
    void removeItemsByProductVariantId(UUID productVariantId);
}
