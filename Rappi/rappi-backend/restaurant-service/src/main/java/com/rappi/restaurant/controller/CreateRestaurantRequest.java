package com.rappi.restaurant.controller;

public record CreateRestaurantRequest(
        String name,
        String address
) {}