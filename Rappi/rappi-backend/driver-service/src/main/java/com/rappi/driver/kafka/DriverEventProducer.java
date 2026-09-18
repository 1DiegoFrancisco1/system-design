package com.rappi.driver.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DriverEventProducer {

  private final KafkaTemplate<String, DriverAssignedEvent> kafkaTemplate;

  private static final String DRIVER_ASSIGNED = "driver.assigned";

  public void publishDriverAssigned(UUID orderId, UUID driverId) {
    var event = new DriverAssignedEvent(orderId, driverId);
    kafkaTemplate.send(DRIVER_ASSIGNED, orderId.toString(), event);
    log.info("Published driver.assigned - order {} -> driver {}", orderId, driverId);
  }
}
