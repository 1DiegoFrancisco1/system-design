package com.rappi.restaurant.controller;

public record CreateRestaurantRequest(
        String name,
        String address,
        double latitude,
        double longitude
) {}