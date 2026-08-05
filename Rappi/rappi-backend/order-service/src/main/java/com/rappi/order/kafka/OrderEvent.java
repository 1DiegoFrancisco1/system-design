package com.rappi.order.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

  private UUID orderId;
  private UUID customerId;
  private UUID restaurantId;
  private UUID driverId;
  private String status;
  private BigDecimal total;
  private String deliveryAddress;
}