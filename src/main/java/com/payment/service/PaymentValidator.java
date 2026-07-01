package com.payment.service;

import com.payment.contracts.CreatePaymentRequest;
import com.payment.errors.InvalidPaymentException;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Validator component for payment operations.
 *
 * <p>Provides validation logic for payment requests before they are processed. Validates required
 * fields, formats, and business constraints such as amount limits.
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
@Component
public class PaymentValidator {

  /** Minimum payment amount. */
  private static final BigDecimal MIN_AMOUNT = BigDecimal.valueOf(0.01);
  /** Maximum payment amount. */
  private static final BigDecimal MAX_AMOUNT = BigDecimal.valueOf(999_999_999.99);

  /**
   * Validates a payment creation request.
   *
   * <p>Checks that:
   * <ul>
   *   <li>Request is not null
   *   <li>User ID is provided and not blank
   *   <li>Amount is provided and within valid range [0.01, 999,999,999.99]
   *   <li>Currency is a valid 3-letter ISO code
   *   <li>Merchant is provided and not blank
   * </ul>
   *
   * @param request the payment creation request to validate
   * @throws InvalidPaymentException if any validation check fails
   */
  public void validateCreatePaymentRequest(CreatePaymentRequest request) {
    if (request == null) {
      throw new InvalidPaymentException("Request cannot be null");
    }

    if (request.userId() == null || request.userId().isBlank()) {
      throw new InvalidPaymentException("User ID is required");
    }

    if (request.amount() == null) {
      throw new InvalidPaymentException("Amount is required");
    }

    if (request.amount().compareTo(MIN_AMOUNT) < 0 ||
        request.amount().compareTo(MAX_AMOUNT) > 0) {
      throw new InvalidPaymentException("Amount must be between " + MIN_AMOUNT + " and " + MAX_AMOUNT);
    }

    if (request.currency() == null || !request.currency().matches("^[A-Z]{3}$")) {
      throw new InvalidPaymentException("Valid 3-letter ISO currency code is required");
    }

    if (request.merchant() == null || request.merchant().isBlank()) {
      throw new InvalidPaymentException("Merchant is required");
    }
  }
}