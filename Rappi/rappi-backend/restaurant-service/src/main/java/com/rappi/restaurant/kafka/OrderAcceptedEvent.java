package com.rappi.restaurant.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAcceptedEvent {

  private UUID orderId;
  private UUID restaurantId;
  private String decision;   // "ACCEPTED" or "REJECTED"
  private String reason;     // reason if rejected
}