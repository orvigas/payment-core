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
public class PaymentFailedEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("payment_id")
  private String paymentId;

  @JsonProperty("reason")
  private String reason;

  @JsonProperty("failed_at")
  private LocalDateTime failedAt;

  @JsonProperty("retry_count")
  private int retryCount;

  @JsonProperty("correlation_id")
  private String correlationId;
}