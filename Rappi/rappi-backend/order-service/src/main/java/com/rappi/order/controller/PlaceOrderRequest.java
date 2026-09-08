package com.rappi.order.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(
        UUID customerId,
        UUID restaurantId,
        String deliveryAddress,
        List<CartItemRequest> items,
        String idempotencyKey
) {
  public record CartItemRequest(
          UUID menuItemId,
          int quantity,
          BigDecimal clientPrice
  ) {}
}