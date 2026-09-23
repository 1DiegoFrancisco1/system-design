package com.rappi.order.service;

import com.rappi.grpc.cart.*;
import com.rappi.order.kafka.OrderEventProducer;
import com.rappi.order.model.Order;
import com.rappi.order.model.OrderStatus;
import com.rappi.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

  // The fakes
  private OrderRepository orderRepository;
  private OrderEventProducer orderEventProducer;
  private CartValidationServiceGrpc.CartValidationServiceBlockingStub cartValidationStub;

  // the real thing we're testing
  private OrderService orderService;

  @BeforeEach
  void setUp() {
    // Create fresh fakes before each test
    orderRepository = mock(OrderRepository.class);
    orderEventProducer = mock(OrderEventProducer.class);
    cartValidationStub = mock(CartValidationServiceGrpc.CartValidationServiceBlockingStub.class);

    // Build the real service, injecting the fakes
    orderService = new OrderService(orderRepository, orderEventProducer, cartValidationStub);
  }

  @Test
  void doubleTap_sameIdempotencyKey_returnsExistingOrder_doesNotCreateTwice() {
    // ARRANGE
    String key = "double-tap-key";
    UUID customerId = UUID.randomUUID();
    UUID restaurantId = UUID.randomUUID();

    // An order that "already exists" for this key
    Order existingOrder = Order.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(key)
            .status(OrderStatus.PENDING)
            .build();

    // Tell the fake repo: "this key already exists"
    when(orderRepository.findByIdempotencyKey(key))
            .thenReturn(Optional.of(existingOrder));

    // ACT
    Order result = orderService.placeOrder(
            customerId, restaurantId, "Some address",
            List.of(new OrderService.CartItemInput(UUID.randomUUID(), 1, new BigDecimal("100.00"))),
            key
    );

    // ASSERT
    // 1. We got the existing order back
    assertEquals(existingOrder.getId(), result.getId());

    // 2. We NEVER saved a new order (the crucial idempotency guarantee)
    verify(orderRepository, never()).save(any());

    // 3. We NEVER published an event (no double charge)
    verify(orderEventProducer, never()).publishOrderPlaced(any());

    // 4. We NEVER even called gRPC validation (short-circuited early)
    verify(cartValidationStub, never()).validateCart(any());
  }

  @Test
  void newOrder_validCart_createsOrderAndPublishesEvent() {
    // ----------     ARRANGE     -----------------
    String key = "brand-new-key";
    UUID customerId = UUID.randomUUID();
    UUID restaurantId = UUID.randomUUID();
    UUID menuItemId = UUID.randomUUID();

    // No existing order for this key (new order)
    when(orderRepository.findByIdempotencyKey(key))
            .thenReturn(Optional.empty());

    // gRPC validation says: valid, authoritative total 200.00
    ValidateCartResponse validResponse = ValidateCartResponse.newBuilder()
            .setValid(true)
            .setAuthoritativeTotal("200.00")
            .build();
    when(cartValidationStub.validateCart(any()))
            .thenReturn(validResponse);

    // The repo returns whatever it's asked to save (echo it back)
    when(orderRepository.save(any(Order.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    // ACT
    Order result = orderService.placeOrder(
            customerId, restaurantId, "Condesa, CDMX",
            List.of(new OrderService.CartItemInput(menuItemId, 2, new BigDecimal("100.00"))),
            key
    );

    // ASSERT
    // 1. The order uses the AUTHORITATIVE total from gRPC, not the client's
    assertEquals(new BigDecimal("200.00"), result.getTotal());

    // 2. Status starts PENDING
    assertEquals(OrderStatus.PENDING, result.getStatus());

    // 3. It WAS saved (exactly once)
    verify(orderRepository, times(1)).save(any(Order.class));

    // 4. The event WAS published (exactly once)
    verify(orderEventProducer, times(1)).publishOrderPlaced(any(Order.class));

    // 5. gRPC validation WAS called
    verify(cartValidationStub, times(1)).validateCart(any());
  }

  @Test
  void invalidCart_throwsException_andCreatesNothing() {
    // ARRANGE
    String key = "invalid-cart-key";
    UUID customerId = UUID.randomUUID();
    UUID restaurantId = UUID.randomUUID();
    UUID menuItemId = UUID.randomUUID();

    // New key (not a duplicate)
    when(orderRepository.findByIdempotencyKey(key))
            .thenReturn(Optional.empty());

    // gRPC validation says: INVALID (e.g item unavailable)
    ValidateCartResponse invalidResponse = ValidateCartResponse.newBuilder()
            .setValid(false)
            .addErrors(ValidationError.newBuilder()
                    .setMenuItemId(menuItemId.toString())
                    .setReason("UNAVAILABLE")
                    .build())
            .build();
    when(cartValidationStub.validateCart(any()))
            .thenReturn(invalidResponse);

    // ACT + ASSERT
    // Assert that calling placeOrder THROWS
    assertThrows(RuntimeException.class, () -> {
      orderService.placeOrder(
              customerId, restaurantId, "Condesa, CDMX",
              List.of(new OrderService.CartItemInput(menuItemId, 1, new BigDecimal("100.00"))),
              key
      );
    });

    // And crucially - nothing harmful happened
    verify(orderRepository, never()).save(any());
    verify(orderEventProducer, never()).publishOrderPlaced(any());
  }
}
