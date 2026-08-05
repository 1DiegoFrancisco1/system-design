package com.rappi.order.repository;

import com.rappi.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

  // For idempotency check — does this key already exist?
  Optional<Order> findByIdempotencyKey(String idempotencyKey);

  // Find all orders for a customer
  java.util.List<Order> findByCustomerId(UUID customerId);

  // Find all orders for a restaurant
  java.util.List<Order> findByRestaurantId(UUID restaurantId);
}