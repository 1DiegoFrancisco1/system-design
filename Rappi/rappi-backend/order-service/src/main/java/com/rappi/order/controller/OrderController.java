package com.rappi.order.controller;

import com.rappi.order.model.Order;
import com.rappi.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

  private final OrderService orderService;

  // ── Place a new order ──────────────────────────────
  @PostMapping
  public ResponseEntity<Order> placeOrder(@RequestBody PlaceOrderRequest request) {
    log.info("Received order request from customer: {}", request.customerId());
    Order order = orderService.placeOrder(
            request.customerId(),
            request.restaurantId(),
            request.deliveryAddress(),
            request.total(),
            request.idempotencyKey()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(order);
  }

  // ── Get order by ID ────────────────────────────────
  @GetMapping("/{orderId}")
  public ResponseEntity<Order> getOrder(@PathVariable UUID orderId) {
    Order order = orderService.getOrder(orderId);
    return ResponseEntity.ok(order);
  }

  // ── Update order status ────────────────────────────
  @PatchMapping("/{orderId}/status")
  public ResponseEntity<Order> updateStatus(
          @PathVariable UUID orderId,
          @RequestBody UpdateStatusRequest request) {
    Order order = orderService.updateStatus(orderId, request.status());
    return ResponseEntity.ok(order);
  }
}