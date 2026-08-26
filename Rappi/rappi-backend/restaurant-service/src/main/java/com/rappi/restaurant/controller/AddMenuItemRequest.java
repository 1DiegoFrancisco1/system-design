package com.rappi.restaurant.controller;

import java.math.BigDecimal;

public record AddMenuItemRequest(
        String name,
        String category,
        BigDecimal price
) {}