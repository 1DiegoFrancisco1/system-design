package com.rappi.driver.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rappi.driver.redis.DriverLocationService;
import com.rappi.driver.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderEventConsumerTest {

  private DriverLocationService locationService;
  private DriverService driverService;
  private DriverEventProducer driverEventProducer;
  private JsonMapper objectMapper;
  private OrderEventConsumer consumer;

  @BeforeEach
  void setUp() {
    locationService = mock(DriverLocationService.class);
    driverService = mock(DriverService.class);
    driverEventProducer = mock(DriverEventProducer.class);
    objectMapper = JsonMapper.builder().build();   // real — cheap pure logic

    consumer = new OrderEventConsumer(
            locationService, driverService, driverEventProducer, objectMapper);
  }

  @Test
  void assignmentFails_compensates_releasesDriverBackToAvailable() {
    // ── ARRANGE ──────────────────────────────────
    UUID orderId = UUID.randomUUID();
    String driverId = UUID.randomUUID().toString();

    // There IS a nearby driver, and we successfully claim them
    when(locationService.findNearbyDrivers(anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(List.of(driverId));
    when(locationService.tryClaimDriver(driverId))
            .thenReturn(true);   // claim succeeds

    // But the forward step FAILS — Postgres write throws
    when(driverService.assignToOrder(any(), any()))
            .thenThrow(new RuntimeException("simulated DB failure"));

    // The order.accepted JSON as it arrives from Kafka
    String message = """
            {"orderId":"%s","restaurantId":"%s","decision":"ACCEPTED",
             "reason":null,"pickupLat":19.4194,"pickupLng":-99.1605}
            """.formatted(orderId, UUID.randomUUID());

    // ── ACT ──────────────────────────────────────
    consumer.onOrderAccepted(message);

    // ── ASSERT ───────────────────────────────────
    // COMPENSATION: the driver must be released back into the available set
    verify(locationService, times(1)).markAvailable(UUID.fromString(driverId));

    // And we must NOT have announced a (failed) assignment
    verify(driverEventProducer, never()).publishDriverAssigned(any(), any());
  }
}