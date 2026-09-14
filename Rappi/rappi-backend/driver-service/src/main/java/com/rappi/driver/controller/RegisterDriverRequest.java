package com.rappi.driver.controller;

public record RegisterDriverRequest(
        String name,
        String vehiclePlate
) {}