package com.rappi.gateway.model;

import java.util.List;

public record PlaceOrderInput(
        String customerId,
        String restaurantId,
        String deliveryAddress,
        List<CartItemInput> items,
        String idempotencyKey
) {}