package com.rappi.restaurant.kafka;

import com.rappi.restaurant.model.MenuItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

  private final KafkaTemplate<String, OrderAcceptedEvent> kafkaTemplate;

  private static final String ORDER_ACCEPTED = "order.accepted";
  private static final String ORDER_REJECTED = "order.rejected";

  public void publishOrderAccepted(UUID orderId, UUID restaurantId) {
    var event = OrderAcceptedEvent.builder()
            .orderId(orderId)
            .restaurantId(restaurantId)
            .decision("ACCEPTED")
            .build();
    kafkaTemplate.send(ORDER_ACCEPTED, orderId.toString(), event);
    log.info("Published order.accepted for order: {}", orderId);
  }

  public void publishOrderRejected(UUID orderId, UUID restaurantId, String reason) {
    var event = OrderAcceptedEvent.builder()
            .orderId(orderId)
            .restaurantId(restaurantId)
            .decision("REJECTED")
            .reason(reason)
            .build();
    kafkaTemplate.send(ORDER_REJECTED, orderId.toString(), event);
    log.info("Published order.rejected for order: {}", orderId);
  }
}