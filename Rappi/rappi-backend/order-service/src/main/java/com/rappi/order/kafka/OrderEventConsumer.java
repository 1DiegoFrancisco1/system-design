package com.rappi.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rappi.order.model.OrderStatus;
import com.rappi.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

  private final OrderService orderService;
  private final ObjectMapper objectMapper;

  // Listens to payment.done topic
  @KafkaListener(
          topics = "payment.done",
          groupId = "order-service-group"
  )
  public void onPaymentDone(String message) {
    try {
      log.info("Received payment.done event: {}", message);

      // Parse the event
      PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);

      if ("SUCCESS".equals(event.getStatus())) {
        // Payment succeeded → confirm the order
        orderService.updateStatus(event.getOrderId(), OrderStatus.CONFIRMED);
        log.info("Order {} confirmed after successful payment", event.getOrderId());
      } else {
        // Payment failed → cancel the order
        orderService.updateStatus(event.getOrderId(), OrderStatus.CANCELLED);
        log.info("Order {} cancelled after failed payment", event.getOrderId());
      }

    } catch (Exception e) {
      log.error("Failed to process payment.done event: {}", message, e);
    }
  }

  // Listen to order.accepted from Restaurant Service
  @KafkaListener(
          topics = "order.accepted",
          groupId = "order-service-group"
  )
  public void onOrderAccepted(String message) {
    try {
      log.info("Received order.accepted event: {}", message);

      OrderDecisionEvent event = objectMapper.readValue(message, OrderDecisionEvent.class);

      // Restaurant accepted → move order to PREPARING
      orderService.updateStatus(event.getOrderId(), OrderStatus.PREPARING);
      log.info("Order {} moved to PREPARING after restaurant accepted",
              event.getOrderId());

    } catch (Exception e) {
      log.error("Failed to process order.accepted event: {}", message, e);
    }
  }

  // Listen to order.rejected from Restaurant Service
  @KafkaListener(
          topics = "order.rejected",
          groupId = "order-service-group"
  )
  public void onOrderRejected(String message) {
    try {
      log.info("Received order.rejected event: {}", message);

      OrderDecisionEvent event = objectMapper.readValue(message, OrderDecisionEvent.class);

      // Restaurant rejected → cancel the order
      orderService.updateStatus(event.getOrderId(), OrderStatus.REJECTED);
      log.info("Order {} REJECTED by restaurant. Reason: {}",
              event.getOrderId(), event.getReason());

    } catch (Exception e) {
      log.error("Failed to process order.rejected event: {}", message, e);
    }
  }
}