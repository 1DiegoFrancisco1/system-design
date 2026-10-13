package com.rappi.gateway.model;

public record CartItemInput(
        String menuItemId,
        int quantity,
        double clientPrice
) {}