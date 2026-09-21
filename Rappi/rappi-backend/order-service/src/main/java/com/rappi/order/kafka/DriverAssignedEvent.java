package com.rappi.order.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DriverAssignedEvent(
        UUID orderId,
        UUID driverId
) {
}
