package com.payment.service;

import com.payment.dto.CreatePaymentRequest;
import com.payment.entity.Payment;
import com.payment.entity.PaymentStatus;
import com.payment.exception.InvalidPaymentException;
import com.payment.exception.PaymentNotFoundException;
import com.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private PaymentValidator paymentValidator;

  @InjectMocks
  private PaymentService paymentService;

  private CreatePaymentRequest validRequest;

  @BeforeEach
  void setUp() {
    validRequest = new CreatePaymentRequest();
    validRequest.setUserId("user123");
    validRequest.setAmount(new BigDecimal("5000.00"));
    validRequest.setCurrency("MXN");
    validRequest.setMerchant("jersey-mikes");
  }

  @Test
  void testCreatePaymentSuccess() {
    // Arrange
    Payment savedPayment = new Payment();
    savedPayment.setPaymentId("pay_123");
    savedPayment.setStatus(PaymentStatus.PENDING);

    when(paymentRepository.save(any())).thenReturn(savedPayment);

    // Act
    var response = paymentService.createPayment(validRequest);

    // Assert
    assertNotNull(response);
    assertEquals("pay_123", response.getPaymentId());
    assertEquals(PaymentStatus.PENDING, response.getStatus());
    verify(paymentRepository, times(1)).save(any());
  }

  @Test
  void testGetPaymentNotFound() {
    // Arrange
    when(paymentRepository.findByPaymentId("invalid")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(PaymentNotFoundException.class, () -> {
      paymentService.getPayment("invalid");
    });
  }

  @Test
  void testConfirmPaymentSuccess() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setStatus(PaymentStatus.PENDING);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    // Act
    var response = paymentService.confirmPayment("pay_123");

    // Assert
    assertNotNull(response);
    assertEquals("pay_123", response.getPaymentId());
    verify(paymentRepository, times(2)).save(any());
  }
}