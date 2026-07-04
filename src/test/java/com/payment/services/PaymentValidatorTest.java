package com.payment.services;

import com.payment.contracts.CreatePaymentRequest;
import com.payment.errors.InvalidPaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentValidatorTest {

  private PaymentValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PaymentValidator();
  }

  @Test
  void testValidCreatePaymentRequest() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("100.50"),
        "USD",
        "merchant",
        "description"
    );

    assertDoesNotThrow(() -> validator.validateCreatePaymentRequest(request));
  }

  @Test
  void testValidCreatePaymentRequestWithoutDescription() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("100.50"),
        "USD",
        "merchant",
        null
    );

    assertDoesNotThrow(() -> validator.validateCreatePaymentRequest(request));
  }

  @Test
  void testNullRequest() {
    assertThrows(InvalidPaymentException.class, () -> {
      validator.validateCreatePaymentRequest(null);
    });
  }

  @Test
  void testNullAmount() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        null,
        "USD",
        "merchant",
        "description"
    );

    InvalidPaymentException exception = assertThrows(InvalidPaymentException.class, () -> {
      validator.validateCreatePaymentRequest(request);
    });
    assertTrue(exception.getMessage().contains("Amount"));
  }

  @Test
  void testAmountTooSmall() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("0.00"),
        "USD",
        "merchant",
        "description"
    );

    InvalidPaymentException exception = assertThrows(InvalidPaymentException.class, () -> {
      validator.validateCreatePaymentRequest(request);
    });
    assertTrue(exception.getMessage().contains("Amount must be between"));
  }

  @Test
  void testAmountAtMinBoundary() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("0.01"),
        "USD",
        "merchant",
        "description"
    );

    assertDoesNotThrow(() -> validator.validateCreatePaymentRequest(request));
  }

  @Test
  void testAmountTooLarge() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("1000000000.00"),
        "USD",
        "merchant",
        "description"
    );

    InvalidPaymentException exception = assertThrows(InvalidPaymentException.class, () -> {
      validator.validateCreatePaymentRequest(request);
    });
    assertTrue(exception.getMessage().contains("Amount must be between"));
  }

  @Test
  void testAmountAtMaxBoundary() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("999999999.99"),
        "USD",
        "merchant",
        "description"
    );

    assertDoesNotThrow(() -> validator.validateCreatePaymentRequest(request));
  }

  @Test
  void testNullCurrency() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("100.50"),
        null,
        "merchant",
        "description"
    );

    InvalidPaymentException exception = assertThrows(InvalidPaymentException.class, () -> {
      validator.validateCreatePaymentRequest(request);
    });
    assertTrue(exception.getMessage().contains("currency"));
  }

  @Test
  void testInvalidCurrencyTooShort() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("100.50"),
        "US",
        "merchant",
        "description"
    );

    InvalidPaymentException exception = assertThrows(InvalidPaymentException.class, () -> {
      validator.validateCreatePaymentRequest(request);
    });
    assertTrue(exception.getMessage().contains("currency"));
  }

  @Test
  void testInvalidCurrencyTooLong() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("100.50"),
        "USDA",
        "merchant",
        "description"
    );

    InvalidPaymentException exception = assertThrows(InvalidPaymentException.class, () -> {
      validator.validateCreatePaymentRequest(request);
    });
    assertTrue(exception.getMessage().contains("currency"));
  }

  @Test
  void testInvalidCurrencyWithLowercase() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("100.50"),
        "usd",
        "merchant",
        "description"
    );

    InvalidPaymentException exception = assertThrows(InvalidPaymentException.class, () -> {
      validator.validateCreatePaymentRequest(request);
    });
    assertTrue(exception.getMessage().contains("currency"));
  }

  @Test
  void testInvalidCurrencyWithNumbers() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("100.50"),
        "US1",
        "merchant",
        "description"
    );

    InvalidPaymentException exception = assertThrows(InvalidPaymentException.class, () -> {
      validator.validateCreatePaymentRequest(request);
    });
    assertTrue(exception.getMessage().contains("currency"));
  }

  @Test
  void testValidCurrencyFormats() {
    String[] validCurrencies = {"USD", "EUR", "GBP", "JPY", "MXN", "CAD"};

    for (String currency : validCurrencies) {
      CreatePaymentRequest request = new CreatePaymentRequest(
          new BigDecimal("100.50"),
          currency,
          "merchant",
          "description"
      );
      assertDoesNotThrow(() -> validator.validateCreatePaymentRequest(request));
    }
  }

  @Test
  void testNullMerchant() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("100.50"),
        "USD",
        null,
        "description"
    );

    InvalidPaymentException exception = assertThrows(InvalidPaymentException.class, () -> {
      validator.validateCreatePaymentRequest(request);
    });
    assertTrue(exception.getMessage().contains("Merchant"));
  }

  @Test
  void testBlankMerchant() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("100.50"),
        "USD",
        "   ",
        "description"
    );

    InvalidPaymentException exception = assertThrows(InvalidPaymentException.class, () -> {
      validator.validateCreatePaymentRequest(request);
    });
    assertTrue(exception.getMessage().contains("Merchant"));
  }
}
