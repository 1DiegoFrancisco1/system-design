package com.rappi.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rappi.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class OrderEventConsumerTest {
  private OrderService orderService;
  private OrderEventProducer orderEventProducer; // if your consumer needs it
  private ObjectMapper objectMapper;
  private OrderEventConsumer consumer;

  @BeforeEach
  void setUp() {
    orderService = mock(OrderService.class);
    objectMapper = new ObjectMapper(); // a REAL one - parsing is cheap & we want real JSON behavior

    // adjust constructor args to match your actual consumer
    consumer = new OrderEventConsumer(orderService, objectMapper);
  }

  @Test
  void onDriverAssigned_setsDriverOnOrder() {
    // ARRANGE
    UUID orderId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();

    // The raw JSON exactly as it would arrive from Kafka
    String message = """
            {"orderId":"%s","driverId":"%s"}
            """.formatted(orderId, driverId);

    // ACT
    // Just call the listener method directly - no Kafka needed
    consumer.onDriverAssigned(message);

    // ASSERT
    // The consumer should have told the service to assign the driver
    verify(orderService, times(1)).assignDriver(orderId, driverId);
  }
}
