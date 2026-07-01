package com.payment.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event representing the initiation of a payment.
 *
 * <p>Published when a new payment request is created and validated. This event triggers
 * downstream processing including charging, notifications, and analytics.
 *
 * @author orvigas@gmail.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiatedEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  /** The unique payment identifier. */
  @JsonProperty("payment_id")
  private String paymentId;

  /** The user who initiated the payment. */
  @JsonProperty("user_id")
  private String userId;

  /** The payment amount. */
  @JsonProperty("amount")
  private BigDecimal amount;

  /** The currency of the payment. */
  @JsonProperty("currency")
  private String currency;

  /** The merchant identifier. */
  @JsonProperty("merchant")
  private String merchant;

  /** Optional description of the payment. */
  @JsonProperty("description")
  private String description;

  /** Timestamp when the payment was initiated. */
  @JsonProperty("created_at")
  private LocalDateTime createdAt;

  /** Correlation ID for distributed tracing across services. */
  @JsonProperty("correlation_id")
  private String correlationId;
}