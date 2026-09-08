package com.rappi.order.service;

import com.rappi.grpc.cart.*;
import com.rappi.order.kafka.OrderEventProducer;
import com.rappi.order.model.Order;
import com.rappi.order.model.OrderStatus;
import com.rappi.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderEventProducer orderEventProducer;
  private final CartValidationServiceGrpc.CartValidationServiceBlockingStub cartValidationStub;

  @Transactional
  public Order placeOrder(UUID customerId,
                          UUID restaurantId,
                          String deliveryAddress,
                          List<CartItemInput> items,
                          String idempotencyKey) {

    // ── Step 1: Idempotency check ──────────────────────
    var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
      log.info("Duplicate order detected, returning existing: {}",
              existing.get().getId());
      return existing.get();
    }

    // ── Step 2: Validate cart via gRPC (synchronous) ───
    var grpcRequest = ValidateCartRequest.newBuilder()
            .setRestaurantId(restaurantId.toString())
            .addAllItems(items.stream()
                    .map(item -> CartItem.newBuilder()
                            .setMenuItemId(item.menuItemId().toString())
                            .setQuantity(item.quantity())
                            .setClientPrice(item.clientPrice().toString())
                            .build())
                    .toList())
            .build();

    log.info("Calling Restaurant Service to validate cart via gRPC...");
    ValidateCartResponse validation = cartValidationStub.validateCart(grpcRequest);

    if (!validation.getValid()) {
      log.warn("Cart validation failed: {}", validation.getErrorsList());
      throw new RuntimeException("Cart validation failed: " + validation.getErrorsList());
    }

    // Use the AUTHORITATIVE total from the Restaurant Service, not the client's
    BigDecimal authoritativeTotal = new BigDecimal(validation.getAuthoritativeTotal());
    log.info("Cart validated. Authoritative total: {}", authoritativeTotal);

    // ── Step 3: Create order ───────────────────────────
    var order = Order.builder()
            .customerId(customerId)
            .restaurantId(restaurantId)
            .deliveryAddress(deliveryAddress)
            .total(authoritativeTotal)   // ← trusted total from gRPC
            .idempotencyKey(idempotencyKey)
            .status(OrderStatus.PENDING)
            .build();

    // ── Step 4: Persist to database ────────────────────
    var savedOrder = orderRepository.save(order);
    log.info("Order created: {}", savedOrder.getId());

    // ── Step 5: Publish Kafka event ────────────────────
    orderEventProducer.publishOrderPlaced(savedOrder);

    return savedOrder;
  }

  @Transactional
  public Order updateStatus(UUID orderId, OrderStatus newStatus) {
    var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    log.info("Order {} status: {} → {}", orderId, order.getStatus(), newStatus);
    order.setStatus(newStatus);
    return orderRepository.save(order);
  }

  public Order getOrder(UUID orderId) {
    return orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
  }

  // Simple record to carry cart item data into this method
  public record CartItemInput(UUID menuItemId, int quantity, BigDecimal clientPrice) {}
}