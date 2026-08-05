package com.rappi.order.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // Who placed the order
  @Column(nullable = false)
  private UUID customerId;

  // Which restaurant
  @Column(nullable = false)
  private UUID restaurantId;

  // Assigned driver (null until assigned)
  private UUID driverId;

  // Idempotency key — prevents duplicate orders
  @Column(unique = true, nullable = false)
  private String idempotencyKey;

  // Order status — maps to our 4 phases
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Column(nullable = false)
  private BigDecimal total;

  @Column(nullable = false)
  private String deliveryAddress;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (status == null) {
      status = OrderStatus.PENDING;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}