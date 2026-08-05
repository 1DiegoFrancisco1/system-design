package com.rappi.order.controller;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceOrderRequest(
        UUID customerId,
        UUID restaurantId,
        String deliveryAddress,
        BigDecimal total,
        String idempotencyKey
) {}