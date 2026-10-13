package com.rappi.gateway.model;

import java.util.List;

public record Restaurant(
        String id,
        String name,
        String address,
        boolean open,
        List<MenuItem> menu
) {}