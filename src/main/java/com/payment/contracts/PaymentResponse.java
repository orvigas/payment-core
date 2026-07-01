package com.payment.contracts;

import com.payment.models.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "Unique identifier of the payment", example = "550e8400-e29b-41d4-a716-446655440000")
    String paymentId,

    @Schema(description = "Identifier of the user who initiated the payment", example = "user-12345")
    String userId,

    @Schema(description = "Transaction amount in the specified currency", example = "99.99")
    BigDecimal amount,

    @Schema(description = "ISO 4217 three-letter currency code", example = "USD")
    String currency,

    @Schema(description = "Identifier of the merchant receiving the payment", example = "merchant-789")
    String merchant,

    @Schema(description = "Current status of the payment in its lifecycle")
    PaymentStatus status,

    @Schema(description = "Optional description of the payment purpose", example = "Purchase of premium subscription")
    String description,

    @Schema(description = "Timestamp when the payment was created", example = "2026-07-01T14:00:00")
    LocalDateTime createdAt,

    @Schema(description = "Timestamp when the payment was last updated", example = "2026-07-01T14:05:00")
    LocalDateTime updatedAt,

    @Schema(description = "Timestamp when the payment was completed or refunded", example = "2026-07-01T14:05:30")
    LocalDateTime completedAt
) {}
