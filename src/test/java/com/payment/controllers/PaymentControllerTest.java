package com.payment.controllers;

import com.payment.contracts.CreatePaymentRequest;
import com.payment.contracts.PaymentResponse;
import com.payment.models.PaymentStatus;
import com.payment.errors.InvalidPaymentException;
import com.payment.errors.PaymentAccessDeniedException;
import com.payment.errors.PaymentNotFoundException;
import com.payment.services.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PaymentController}.
 *
 * @author orvigas@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

  @Mock
  private PaymentService paymentService;

  @InjectMocks
  private PaymentController paymentController;

  private CreatePaymentRequest createPaymentRequest;
  private PaymentResponse paymentResponse;
  private Authentication authentication;

  @BeforeEach
  void setUp() {
    createPaymentRequest = new CreatePaymentRequest(
        new BigDecimal("1000.00"),
        "USD",
        "merchant-name",
        "Test payment"
    );

    paymentResponse = new PaymentResponse(
        "pay_123",
        "user123",
        new BigDecimal("1000.00"),
        "USD",
        "merchant-name",
        PaymentStatus.PENDING,
        "Test payment",
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
    );

    authentication = new UsernamePasswordAuthenticationToken("user123", null);
  }

  @Test
  void testCreatePaymentSuccess() {
    when(paymentService.createPayment(any(CreatePaymentRequest.class), anyString()))
        .thenReturn(paymentResponse);

    ResponseEntity<PaymentResponse> response = paymentController.createPayment(createPaymentRequest, authentication);
    PaymentResponse body = response.getBody();

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(body);
    assertEquals("pay_123", body.paymentId());
    assertEquals(PaymentStatus.PENDING, body.status());
    verify(paymentService).createPayment(any(CreatePaymentRequest.class), anyString());
  }

  @Test
  void testCreatePaymentPassesAuthenticatedOwner() {
    when(paymentService.createPayment(any(CreatePaymentRequest.class), anyString()))
        .thenReturn(paymentResponse);

    paymentController.createPayment(createPaymentRequest, authentication);

    verify(paymentService).createPayment(createPaymentRequest, "user123");
  }

  @Test
  void testCreatePaymentWithValidation() {
    when(paymentService.createPayment(any(CreatePaymentRequest.class), anyString()))
        .thenThrow(new InvalidPaymentException("Invalid amount"));

    assertThrows(InvalidPaymentException.class, () -> {
      paymentController.createPayment(createPaymentRequest, authentication);
    });
  }

  @Test
  void testGetPaymentSuccess() {
    when(paymentService.getPayment(anyString(), anyString()))
        .thenReturn(paymentResponse);

    ResponseEntity<PaymentResponse> response = paymentController.getPayment("pay_123", authentication);
    PaymentResponse body = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(body);
    assertEquals("pay_123", body.paymentId());
    verify(paymentService).getPayment("pay_123", "user123");
  }

  @Test
  void testGetPaymentNotFound() {
    when(paymentService.getPayment(anyString(), anyString()))
        .thenThrow(new PaymentNotFoundException("Payment not found: pay_999"));

    assertThrows(PaymentNotFoundException.class, () -> {
      paymentController.getPayment("pay_999", authentication);
    });
  }

  @Test
  void testGetPaymentDeniedForDifferentUser() {
    when(paymentService.getPayment(anyString(), anyString()))
        .thenThrow(new PaymentAccessDeniedException("Payment does not belong to the authenticated user"));

    assertThrows(PaymentAccessDeniedException.class, () -> {
      paymentController.getPayment("pay_123", authentication);
    });
  }

  @Test
  void testConfirmPaymentSuccess() {
    PaymentResponse confirmedResponse = new PaymentResponse(
        "pay_123",
        "user123",
        new BigDecimal("1000.00"),
        "USD",
        "merchant-name",
        PaymentStatus.COMPLETED,
        "Test payment",
        LocalDateTime.now(),
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    when(paymentService.confirmPayment(anyString(), anyString()))
        .thenReturn(confirmedResponse);

    ResponseEntity<PaymentResponse> response = paymentController.confirmPayment("pay_123", authentication);
    PaymentResponse body = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(body);
    assertEquals(PaymentStatus.COMPLETED, body.status());
    verify(paymentService).confirmPayment("pay_123", "user123");
  }

  @Test
  void testConfirmPaymentInvalidStatus() {
    when(paymentService.confirmPayment(anyString(), anyString()))
        .thenThrow(new InvalidPaymentException("Only PENDING payments can be confirmed"));

    assertThrows(InvalidPaymentException.class, () -> {
      paymentController.confirmPayment("pay_123", authentication);
    });
  }

  @Test
  void testConfirmPaymentNotFound() {
    when(paymentService.confirmPayment(anyString(), anyString()))
        .thenThrow(new PaymentNotFoundException("Payment not found: pay_999"));

    assertThrows(PaymentNotFoundException.class, () -> {
      paymentController.confirmPayment("pay_999", authentication);
    });
  }

  @Test
  void testConfirmPaymentDeniedForDifferentUser() {
    when(paymentService.confirmPayment(anyString(), anyString()))
        .thenThrow(new PaymentAccessDeniedException("Payment does not belong to the authenticated user"));

    assertThrows(PaymentAccessDeniedException.class, () -> {
      paymentController.confirmPayment("pay_123", authentication);
    });
  }

  @Test
  void testRefundPaymentSuccess() {
    PaymentResponse refundedResponse = new PaymentResponse(
        "pay_123",
        "user123",
        new BigDecimal("1000.00"),
        "USD",
        "merchant-name",
        PaymentStatus.REFUNDED,
        "Test payment",
        LocalDateTime.now(),
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    when(paymentService.refundPayment(anyString(), anyString()))
        .thenReturn(refundedResponse);

    ResponseEntity<PaymentResponse> response = paymentController.refundPayment("pay_123", authentication);
    PaymentResponse body = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(body);
    assertEquals(PaymentStatus.REFUNDED, body.status());
    verify(paymentService).refundPayment("pay_123", "user123");
  }

  @Test
  void testRefundPaymentInvalidStatus() {
    when(paymentService.refundPayment(anyString(), anyString()))
        .thenThrow(new InvalidPaymentException("Only COMPLETED payments can be refunded"));

    assertThrows(InvalidPaymentException.class, () -> {
      paymentController.refundPayment("pay_123", authentication);
    });
  }

  @Test
  void testRefundPaymentNotFound() {
    when(paymentService.refundPayment(anyString(), anyString()))
        .thenThrow(new PaymentNotFoundException("Payment not found: pay_999"));

    assertThrows(PaymentNotFoundException.class, () -> {
      paymentController.refundPayment("pay_999", authentication);
    });
  }

  @Test
  void testRefundPaymentDeniedForDifferentUser() {
    when(paymentService.refundPayment(anyString(), anyString()))
        .thenThrow(new PaymentAccessDeniedException("Payment does not belong to the authenticated user"));

    assertThrows(PaymentAccessDeniedException.class, () -> {
      paymentController.refundPayment("pay_123", authentication);
    });
  }

  @Test
  void testCreatePaymentReturnsPaymentId() {
    when(paymentService.createPayment(any(CreatePaymentRequest.class), anyString()))
        .thenReturn(paymentResponse);

    ResponseEntity<PaymentResponse> response = paymentController.createPayment(createPaymentRequest, authentication);

    assertNotNull(response.getBody());
    assertEquals("pay_123", response.getBody().paymentId());
  }

  @Test
  void testCreatePaymentReturnsPendingStatus() {
    when(paymentService.createPayment(any(CreatePaymentRequest.class), anyString()))
        .thenReturn(paymentResponse);

    ResponseEntity<PaymentResponse> response = paymentController.createPayment(createPaymentRequest, authentication);

    assertNotNull(response.getBody());
    assertEquals(PaymentStatus.PENDING, response.getBody().status());
  }

  @Test
  void testGetPaymentReturnsCorrectStatus() {
    when(paymentService.getPayment(anyString(), anyString()))
        .thenReturn(paymentResponse);

    ResponseEntity<PaymentResponse> response = paymentController.getPayment("pay_123", authentication);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testConfirmPaymentReturnsCompletedStatus() {
    PaymentResponse confirmedResponse = new PaymentResponse(
        "pay_123",
        "user123",
        new BigDecimal("1000.00"),
        "USD",
        "merchant-name",
        PaymentStatus.COMPLETED,
        "Test payment",
        LocalDateTime.now(),
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    when(paymentService.confirmPayment(anyString(), anyString()))
        .thenReturn(confirmedResponse);

    ResponseEntity<PaymentResponse> response = paymentController.confirmPayment("pay_123", authentication);

    assertEquals(PaymentStatus.COMPLETED, response.getBody().status());
  }

  @Test
  void testRefundPaymentReturnsRefundedStatus() {
    PaymentResponse refundedResponse = new PaymentResponse(
        "pay_123",
        "user123",
        new BigDecimal("1000.00"),
        "USD",
        "merchant-name",
        PaymentStatus.REFUNDED,
        "Test payment",
        LocalDateTime.now(),
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    when(paymentService.refundPayment(anyString(), anyString()))
        .thenReturn(refundedResponse);

    ResponseEntity<PaymentResponse> response = paymentController.refundPayment("pay_123", authentication);

    assertEquals(PaymentStatus.REFUNDED, response.getBody().status());
  }

  @Test
  void testCreatePaymentWithDifferentAuthenticatedUser() {
    Authentication differentUserAuth = new UsernamePasswordAuthenticationToken("different_user", null);

    PaymentResponse response = new PaymentResponse(
        "pay_456",
        "different_user",
        new BigDecimal("500.00"),
        "USD",
        "merchant-name",
        PaymentStatus.PENDING,
        "Different user payment",
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
    );

    when(paymentService.createPayment(any(CreatePaymentRequest.class), anyString()))
        .thenReturn(response);

    ResponseEntity<PaymentResponse> result = paymentController.createPayment(createPaymentRequest, differentUserAuth);

    assertEquals("different_user", result.getBody().userId());
    assertEquals("pay_456", result.getBody().paymentId());
    verify(paymentService).createPayment(createPaymentRequest, "different_user");
  }

  @Test
  void testGetPaymentVerifiesServiceCall() {
    when(paymentService.getPayment("pay_123", "user123"))
        .thenReturn(paymentResponse);

    paymentController.getPayment("pay_123", authentication);

    verify(paymentService).getPayment("pay_123", "user123");
  }

  @Test
  void testConfirmPaymentVerifiesServiceCall() {
    PaymentResponse confirmedResponse = new PaymentResponse(
        "pay_123",
        "user123",
        new BigDecimal("1000.00"),
        "USD",
        "merchant-name",
        PaymentStatus.COMPLETED,
        "Test payment",
        LocalDateTime.now(),
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    when(paymentService.confirmPayment("pay_123", "user123"))
        .thenReturn(confirmedResponse);

    paymentController.confirmPayment("pay_123", authentication);

    verify(paymentService).confirmPayment("pay_123", "user123");
  }

  @Test
  void testRefundPaymentVerifiesServiceCall() {
    PaymentResponse refundedResponse = new PaymentResponse(
        "pay_123",
        "user123",
        new BigDecimal("1000.00"),
        "USD",
        "merchant-name",
        PaymentStatus.REFUNDED,
        "Test payment",
        LocalDateTime.now(),
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    when(paymentService.refundPayment("pay_123", "user123"))
        .thenReturn(refundedResponse);

    paymentController.refundPayment("pay_123", authentication);

    verify(paymentService).refundPayment("pay_123", "user123");
  }

  @Test
  void testCreatePaymentWithLargeAmount() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("999999.99"),
        "USD",
        "merchant-name",
        "Large payment"
    );

    PaymentResponse response = new PaymentResponse(
        "pay_999",
        "user123",
        new BigDecimal("999999.99"),
        "USD",
        "merchant-name",
        PaymentStatus.PENDING,
        "Large payment",
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
    );

    when(paymentService.createPayment(any(CreatePaymentRequest.class), anyString()))
        .thenReturn(response);

    ResponseEntity<PaymentResponse> result = paymentController.createPayment(request, authentication);

    assertEquals(new BigDecimal("999999.99"), result.getBody().amount());
  }
}
