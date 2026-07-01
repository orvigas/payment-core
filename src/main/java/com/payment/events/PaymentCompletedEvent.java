package com.payment.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("payment_id")
  private String paymentId;

  @JsonProperty("completed_at")
  private LocalDateTime completedAt;

  @JsonProperty("correlation_id")
  private String correlationId;
}