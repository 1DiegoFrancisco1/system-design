package com.rappi.order.service;

import com.rappi.order.kafka.OrderEventProducer;
import com.rappi.order.model.Order;
import com.rappi.order.model.OrderStatus;
import com.rappi.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderEventProducer orderEventProducer;

  @Transactional
  public Order placeOrder(UUID customerId,
                          UUID restaurantId,
                          String deliveryAddress,
                          BigDecimal total,
                          String idempotencyKey) {

    // ── Step 1: Idempotency check ──────────────────────
    // Has this exact request been seen before?
    var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
      log.info("Duplicate order detected, returning existing: {}",
              existing.get().getId());
      return existing.get();
    }

    // ── Step 2: Create order ───────────────────────────
    var order = Order.builder()
            .customerId(customerId)
            .restaurantId(restaurantId)
            .deliveryAddress(deliveryAddress)
            .total(total)
            .idempotencyKey(idempotencyKey)
            .status(OrderStatus.PENDING)
            .build();

    // ── Step 3: Persist to database ────────────────────
    // Order exists in DB before Kafka event — zero order loss
    var savedOrder = orderRepository.save(order);
    log.info("Order created: {}", savedOrder.getId());

    // ── Step 4: Publish Kafka event ────────────────────
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
}