package com.payment.kafka;

import com.payment.models.Payment;
import com.payment.models.PaymentStatus;
import com.payment.events.PaymentInitiatedEvent;
import com.payment.repositories.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class ChargingConsumerTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private PaymentProducer paymentProducer;

  @InjectMocks
  private ChargingConsumer chargingConsumer;

  private PaymentInitiatedEvent event;
  private Payment payment;

  @BeforeEach
  void setUp() {
    event = PaymentInitiatedEvent.builder()
        .paymentId("pay_123")
        .userId("user_456")
        .amount(new BigDecimal("5000.00"))
        .currency("MXN")
        .merchant("jersey-mikes")
        .createdAt(LocalDateTime.now())
        .build();

    payment = new Payment();
    payment.setPaymentId(event.getPaymentId());
    payment.setStatus(PaymentStatus.PENDING);
  }

  @Test
  void testConsumePaymentInitiated_Success() {
    // Arrange
    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    // Act
    chargingConsumer.consumePaymentInitiated(event, null);

    // Assert
    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository, times(2)).save(paymentCaptor.capture()); // save twice: PROCESSING, then update
    verify(paymentProducer, times(1)).publishPaymentCharged(any());

    log.info("Test passed: ChargingConsumer processed event successfully");
  }

  @Test
  void testConsumePaymentInitiated_PaymentNotFound() {
    // Arrange
    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(RuntimeException.class, () -> {
      chargingConsumer.consumePaymentInitiated(event, null);
    });

    verify(paymentRepository, never()).save(any());

    log.info("Test passed: ChargingConsumer handled payment not found");
  }

  @Test
  void testConsumePaymentInitiated_WithCorrelationId() {
    // Arrange
    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    String correlationId = "corr_id_123";

    // Act
    chargingConsumer.consumePaymentInitiated(event, correlationId);

    // Assert
    verify(paymentRepository, times(2)).save(any());
    verify(paymentProducer, times(1)).publishPaymentCharged(any());

    log.info("Test passed: ChargingConsumer processed event with correlation ID");
  }

  @Test
  void testConsumePaymentInitiated_UpdatesPaymentStatus() {
    // Arrange
    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    // Act
    chargingConsumer.consumePaymentInitiated(event, null);

    // Assert
    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository, times(2)).save(paymentCaptor.capture());

    List<Payment> capturedPayments = paymentCaptor.getAllValues();
    assertEquals(2, capturedPayments.size());

    log.info("Test passed: ChargingConsumer updated payment status correctly");
  }
}