package com.payment.service;

import com.payment.dto.CreatePaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.entity.Payment;
import com.payment.entity.PaymentStatus;
import com.payment.exception.PaymentNotFoundException;
import com.payment.exception.InvalidPaymentException;
import com.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentValidator paymentValidator;

  @Transactional
  public PaymentResponse createPayment(CreatePaymentRequest request) {
    log.info("Creating payment for user: {}, amount: {}", request.getUserId(), request.getAmount());

    paymentValidator.validateCreatePaymentRequest(request);

    Payment payment = new Payment();
    payment.setUserId(request.getUserId());
    payment.setAmount(request.getAmount());
    payment.setCurrency(request.getCurrency());
    payment.setMerchant(request.getMerchant());
    payment.setDescription(request.getDescription());
    payment.setStatus(PaymentStatus.PENDING);

    Payment saved = paymentRepository.save(payment);
    log.info("Payment created: {}", saved.getPaymentId());

    return mapToResponse(saved);
  }

  @Transactional(readOnly = true)
  public PaymentResponse getPayment(String paymentId) {
    log.debug("Fetching payment: {}", paymentId);

    Payment payment = paymentRepository.findByPaymentId(paymentId)
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

    return mapToResponse(payment);
  }

  @Transactional
  public PaymentResponse confirmPayment(String paymentId) {
    log.info("Confirming payment: {}", paymentId);

    Payment payment = paymentRepository.findByPaymentId(paymentId)
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

    if (payment.getStatus() != PaymentStatus.PENDING) {
      throw new InvalidPaymentException("Only PENDING payments can be confirmed");
    }

    // Simular procesamiento
    payment.setStatus(PaymentStatus.PROCESSING);
    paymentRepository.save(payment);

    // Simular completado
    payment.setStatus(PaymentStatus.COMPLETED);
    payment.setCompletedAt(LocalDateTime.now());
    Payment confirmed = paymentRepository.save(payment);

    log.info("Payment confirmed: {}", paymentId);
    return mapToResponse(confirmed);
  }

  @Transactional
  public PaymentResponse refundPayment(String paymentId) {
    log.info("Refunding payment: {}", paymentId);

    Payment payment = paymentRepository.findByPaymentId(paymentId)
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

    if (payment.getStatus() != PaymentStatus.COMPLETED) {
      throw new InvalidPaymentException("Only COMPLETED payments can be refunded");
    }

    payment.setStatus(PaymentStatus.REFUNDED);
    Payment refunded = paymentRepository.save(payment);

    log.info("Payment refunded: {}", paymentId);
    return mapToResponse(refunded);
  }

  private PaymentResponse mapToResponse(Payment payment) {
    return new PaymentResponse(
        payment.getPaymentId(),
        payment.getUserId(),
        payment.getAmount(),
        payment.getCurrency(),
        payment.getMerchant(),
        payment.getStatus(),
        payment.getDescription(),
        payment.getCreatedAt(),
        payment.getUpdatedAt(),
        payment.getCompletedAt());
  }
}