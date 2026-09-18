package com.rappi.driver.kafka;

import java.util.UUID;

public record DriverAssignedEvent(
        UUID orderId,
        UUID driverId
) {}