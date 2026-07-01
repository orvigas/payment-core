package com.payment.contracts;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Immutable data contract for creating a payment.
 *
 * <p>This record encapsulates all required information to initiate a new payment transaction. All
 * fields are validated upon construction using Jakarta Bean Validation annotations.
 *
 * @param userId unique identifier of the user initiating the payment (required, non-blank)
 * @param amount transaction amount in the specified currency (required, minimum 0.01)
 * @param currency ISO 4217 three-letter currency code (required, must match pattern ^[A-Z]{3}$)
 * @param merchant identifier of the merchant receiving the payment (required, non-blank)
 * @param description optional description of the payment (max 255 characters)
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
public record CreatePaymentRequest(
    @NotBlank(message = "User ID is required")
    String userId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    String currency,

    @NotBlank(message = "Merchant is required")
    String merchant,

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    String description
) {}
