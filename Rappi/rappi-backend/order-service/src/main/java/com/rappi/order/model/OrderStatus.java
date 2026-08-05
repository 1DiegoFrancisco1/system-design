package com.rappi.order.model;

public enum OrderStatus {
  PENDING,       // just created, payment processing
  CONFIRMED,     // payment done, restaurant notified
  PREPARING,     // restaurant accepted, driver pre-assigned
  READY,         // restaurant finished cooking
  PICKED_UP,     // driver picked up the order
  DELIVERED,     // order delivered successfully
  CANCELLED,     // cancelled at any stage
  REJECTED       // restaurant rejected the order
}