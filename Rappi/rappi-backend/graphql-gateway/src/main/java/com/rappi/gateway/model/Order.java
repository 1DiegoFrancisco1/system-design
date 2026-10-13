package com.rappi.gateway.model;

public record Order(
        String id,
        String customerId,
        String restaurantId,
        String driverId,
        String status,
        double total,
        String deliveryAddress
) {}