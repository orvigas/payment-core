package com.payment.contracts;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable data contract for creating a payment.
 *
 * <p>This record encapsulates all required information to initiate a new payment transaction. All
 * fields are validated upon construction using Jakarta Bean Validation annotations. The payment
 * owner is not part of this contract: it is taken from the authenticated caller's JWT so a client
 * cannot create a payment attributed to another user.
 *
 * @param amount transaction amount in the specified currency (required, minimum 0.01)
 * @param currency ISO 4217 three-letter currency code (required, must match pattern ^[A-Z]{3}$)
 * @param merchant identifier of the merchant receiving the payment (required, non-blank)
 * @param description optional description of the payment (max 255 characters)
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
public record CreatePaymentRequest(
    @Schema(description = "Transaction amount in the specified currency", example = "99.99")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount,

    @Schema(description = "ISO 4217 three-letter currency code", example = "USD")
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    String currency,

    @Schema(description = "Identifier of the merchant receiving the payment", example = "merchant-789")
    @NotBlank(message = "Merchant is required")
    String merchant,

    @Schema(description = "Optional description of the payment purpose", example = "Purchase of premium subscription")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    String description
) {}
