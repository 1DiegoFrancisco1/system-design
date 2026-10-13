package com.rappi.gateway.model;

public record MenuItem(
        String id,
        String name,
        String category,
        double price,
        boolean available
) {}