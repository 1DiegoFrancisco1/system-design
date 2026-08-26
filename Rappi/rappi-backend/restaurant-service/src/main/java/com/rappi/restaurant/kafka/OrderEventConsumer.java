package com.rappi.restaurant.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rappi.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

  private final RestaurantService restaurantService;
  private final OrderEventProducer orderEventProducer;
  private final ObjectMapper objectMapper;

  // Listen to order.placed from the Order Service
  @KafkaListener(
          topics = "order.placed",
          groupId = "restaurant-service-group"
  )
  public void onOrderPlaced(String message) {
    try {
      log.info("Received order.placed event: {}", message);

      OrderEvent order = objectMapper.readValue(message, OrderEvent.class);

      // Check if the restaurant is open and can accept the order
      boolean canAccept = restaurantService.isRestaurantOpen(order.getRestaurantId());

      if (canAccept) {
        log.info("Restaurant {} accepting order {}",
                order.getRestaurantId(), order.getOrderId());
        orderEventProducer.publishOrderAccepted(
                order.getOrderId(),
                order.getRestaurantId()
        );
      } else {
        log.info("Restaurant {} rejecting order {} (closed)",
                order.getRestaurantId(), order.getOrderId());
        orderEventProducer.publishOrderRejected(
                order.getOrderId(),
                order.getRestaurantId(),
                "Restaurant is closed"
        );
      }

    } catch (Exception e) {
      log.error("Failed to process order.placed event: {}", message, e);
    }
  }
}