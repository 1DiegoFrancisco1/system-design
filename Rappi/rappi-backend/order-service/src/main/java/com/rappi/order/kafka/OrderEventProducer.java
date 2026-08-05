package com.rappi.order.kafka;

import com.rappi.order.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

  private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

  // Topic names — match exactly what we designed
  private static final String ORDER_PLACED    = "order.placed";
  private static final String ORDER_CONFIRMED = "order.confirmed";
  private static final String ORDER_CANCELLED = "order.cancelled";

  public void publishOrderPlaced(Order order) {
    var event = buildEvent(order);
    // Key = order.getId().toString() → partitioned by order_id
    kafkaTemplate.send(ORDER_PLACED, order.getId().toString(), event);
    log.info("Published order.placed for order: {}", order.getId());
  }

  public void publishOrderConfirmed(Order order) {
    var event = buildEvent(order);
    kafkaTemplate.send(ORDER_CONFIRMED, order.getId().toString(), event);
    log.info("Published order.confirmed for order: {}", order.getId());
  }

  public void publishOrderCancelled(Order order) {
    var event = buildEvent(order);
    kafkaTemplate.send(ORDER_CANCELLED, order.getId().toString(), event);
    log.info("Published order.cancelled for order: {}", order.getId());
  }

  // Builds the event payload that travels through Kafka
  private OrderEvent buildEvent(Order order) {
    return OrderEvent.builder()
            .orderId(order.getId())
            .customerId(order.getCustomerId())
            .restaurantId(order.getRestaurantId())
            .driverId(order.getDriverId())
            .status(order.getStatus().name())
            .total(order.getTotal())
            .deliveryAddress(order.getDeliveryAddress())
            .build();
  }
}