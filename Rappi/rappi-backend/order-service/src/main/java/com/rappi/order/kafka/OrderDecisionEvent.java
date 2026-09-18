package com.rappi.order.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDecisionEvent {

  private UUID orderId;
  private UUID restaurantId;
  private String decision;   // "ACCEPTED" or "REJECTED"
  private String reason;
}