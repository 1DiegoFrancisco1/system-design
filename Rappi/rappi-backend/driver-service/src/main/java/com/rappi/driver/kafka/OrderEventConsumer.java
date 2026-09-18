package com.rappi.driver.kafka;

import tools.jackson.databind.json.JsonMapper;
import com.rappi.driver.redis.DriverLocationService;
import com.rappi.driver.service.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
  private final DriverLocationService locationService;
  private final DriverService driverService;
  private final DriverEventProducer driverEventProducer;
  private final JsonMapper objectMapper;

  private static final double SEARCH_RADIUS_KM = 5.0;

  @KafkaListener(
          topics = "order.accepted",
          groupId = "driver-service-group"
  )
  public void onOrderAccepted(String message) {
    try {
      log.info("Received order.accepted event: {}", message);

      OrderAcceptedEvent event = objectMapper.readValue(message, OrderAcceptedEvent.class);

      // 1. Find nearby drivers (closest first)
      List<String> candidates = locationService.findNearbyDrivers(
              event.pickupLng(),
              event.pickupLat(),
              SEARCH_RADIUS_KM
      );

      if (candidates.isEmpty()) {
        log.warn("No drivers found near order {} - needs retry/queue", event.orderId());
        return; // (a real system would retry or widen the radius)
      }

      // 2. Try to claim each candidate until one succeeds (atomic SREM)
      for (String driverIdStr : candidates) {
        boolean claimed = locationService.tryClaimDriver(driverIdStr);

        // NOTE: triple-write (Redis + Postgres + Kafka), not crash-safe.
        // Production: saga with compensation — release Redis claim if Postgres/Kafka fail.
        if (claimed) {
          UUID driverId = UUID.fromString(driverIdStr);

          // 3. Assign in Postgres -> status ON_DELIVERY
          driverService.assignToOrder(driverId, event.orderId());
          // 4. Announce it
          driverEventProducer.publishDriverAssigned(event.orderId(), driverId);

          log.info("Order {} assigned to driver {}", event.orderId(), driverId);
          return; // done - stop trying other candidates
        }

        log.info("Driver {} was already taken, trying next...", driverIdStr);
      }

      // If we get here, every nearby driver was already claimed
      log.warn("All nearby drivers busy for order {} - needs retry", event.orderId());
    } catch (Exception e) {
      log.error("Failed to process order.accepted event: {}", message, e);
    }
  }
}
