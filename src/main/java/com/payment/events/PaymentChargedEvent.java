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
public class PaymentChargedEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("payment_id")
  private String paymentId;

  @JsonProperty("transaction_id")
  private String transactionId; // From processor

  @JsonProperty("charged_at")
  private LocalDateTime chargedAt;

  @JsonProperty("correlation_id")
  private String correlationId;

  @JsonProperty("success")
  private boolean success;

  @JsonProperty("error_message")
  private String errorMessage; // If failed
}