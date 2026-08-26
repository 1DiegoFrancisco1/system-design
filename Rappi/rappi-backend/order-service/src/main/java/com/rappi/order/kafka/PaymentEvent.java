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
public class PaymentEvent {

  private UUID orderId;
  private UUID customerId;
  private String status;      // "SUCCESS" or "FAILED"
  private BigDecimal amount;
  private String reason;      // failure reason if failed
}