package com.payment.service;

import com.payment.dto.CreatePaymentRequest;
import com.payment.exception.InvalidPaymentException;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class PaymentValidator {

  private static final BigDecimal MIN_AMOUNT = BigDecimal.valueOf(0.01);
  private static final BigDecimal MAX_AMOUNT = BigDecimal.valueOf(999_999_999.99);

  public void validateCreatePaymentRequest(CreatePaymentRequest request) {
    if (request == null) {
      throw new InvalidPaymentException("Request cannot be null");
    }

    if (request.getUserId() == null || request.getUserId().isBlank()) {
      throw new InvalidPaymentException("User ID is required");
    }

    if (request.getAmount() == null) {
      throw new InvalidPaymentException("Amount is required");
    }

    if (request.getAmount().compareTo(MIN_AMOUNT) < 0 ||
        request.getAmount().compareTo(MAX_AMOUNT) > 0) {
      throw new InvalidPaymentException("Amount must be between " + MIN_AMOUNT + " and " + MAX_AMOUNT);
    }

    if (request.getCurrency() == null || !request.getCurrency().matches("^[A-Z]{3}$")) {
      throw new InvalidPaymentException("Valid 3-letter ISO currency code is required");
    }

    if (request.getMerchant() == null || request.getMerchant().isBlank()) {
      throw new InvalidPaymentException("Merchant is required");
    }
  }
}