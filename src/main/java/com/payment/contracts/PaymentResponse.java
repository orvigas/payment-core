package com.payment.contracts;

import com.payment.models.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable data contract representing a payment response.
 *
 * <p>This record contains the complete details of a payment transaction, including metadata,
 * status, and timestamps. It is returned by all payment-related API endpoints.
 *
 * @param paymentId unique identifier of the payment
 * @param userId identifier of the user who initiated the payment
 * @param amount transaction amount in the specified currency
 * @param currency ISO 4217 three-letter currency code
 * @param merchant identifier of the merchant receiving the payment
 * @param status current status of the payment (PENDING, CONFIRMED, REFUNDED)
 * @param description optional description of the payment
 * @param createdAt timestamp when the payment was created
 * @param updatedAt timestamp when the payment was last updated
 * @param completedAt timestamp when the payment was completed or refunded
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
public record PaymentResponse(
    String paymentId,
    String userId,
    BigDecimal amount,
    String currency,
    String merchant,
    PaymentStatus status,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime completedAt
) {}
