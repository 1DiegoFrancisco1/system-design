package com.rappi.driver.kafka;

import java.util.UUID;

public record OrderAcceptedEvent(
        UUID orderId,
        UUID restaurantId,
        String decision,
        String reason,
        double pickupLat,
        double pickupLng
) {}