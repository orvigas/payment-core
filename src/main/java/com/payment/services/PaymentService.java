package com.payment.services;

import com.payment.contracts.CreatePaymentRequest;
import com.payment.contracts.PaymentResponse;
import com.payment.models.Payment;
import com.payment.models.PaymentStatus;
import com.payment.errors.PaymentNotFoundException;
import com.payment.errors.InvalidPaymentException;
import com.payment.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * Service layer for payment operations.
 *
 * <p>Orchestrates business logic for payment processing, including creation, retrieval,
 * confirmation, and refunding. All operations are transactional to ensure data consistency.
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentValidator paymentValidator;

  /**
   * Creates a new payment transaction.
   *
   * <p>Validates the request, creates a new Payment entity with PENDING status, persists it to
   * the database, and returns the response.
   *
   * @param request the payment creation request
   * @return the created payment response
   * @throws InvalidPaymentException if the request fails validation
   */
  @Transactional
  public PaymentResponse createPayment(CreatePaymentRequest request) {
    log.info("Creating payment for user: {}, amount: {}", request.userId(), request.amount());

    paymentValidator.validateCreatePaymentRequest(request);

    Payment payment = new Payment();
    payment.setUserId(request.userId());
    payment.setAmount(request.amount());
    payment.setCurrency(request.currency());
    payment.setMerchant(request.merchant());
    payment.setDescription(request.description());
    payment.setStatus(PaymentStatus.PENDING);

    Payment saved = paymentRepository.save(payment);
    log.info("Payment created: {}", saved.getPaymentId());

    return mapToResponse(saved);
  }

  /**
   * Retrieves a payment by its unique identifier.
   *
   * @param paymentId the payment ID
   * @return the payment response
   * @throws PaymentNotFoundException if the payment does not exist
   */
  @Transactional(readOnly = true)
  public PaymentResponse getPayment(String paymentId) {
    log.debug("Fetching payment: {}", paymentId);

    Payment payment = paymentRepository.findByPaymentId(paymentId)
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

    return mapToResponse(payment);
  }

  /**
   * Confirms a pending payment.
   *
   * <p>Transitions the payment status from PENDING → PROCESSING → COMPLETED and sets the
   * completion timestamp.
   *
   * @param paymentId the payment ID
   * @return the confirmed payment response
   * @throws PaymentNotFoundException if the payment does not exist
   * @throws InvalidPaymentException if the payment is not in PENDING status
   */
  @Transactional
  public PaymentResponse confirmPayment(String paymentId) {
    log.info("Confirming payment: {}", paymentId);

    Payment payment = paymentRepository.findByPaymentId(paymentId)
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

    if (payment.getStatus() != PaymentStatus.PENDING) {
      throw new InvalidPaymentException("Only PENDING payments can be confirmed");
    }

    // Simulate processing
    payment.setStatus(PaymentStatus.PROCESSING);
    paymentRepository.save(payment);

    // Simulate completion
    payment.setStatus(PaymentStatus.COMPLETED);
    payment.setCompletedAt(LocalDateTime.now());
    Payment confirmed = paymentRepository.save(payment);

    log.info("Payment confirmed: {}", paymentId);
    return mapToResponse(confirmed);
  }

  /**
   * Refunds a completed payment.
   *
   * <p>Transitions the payment status from COMPLETED to REFUNDED.
   *
   * @param paymentId the payment ID
   * @return the refunded payment response
   * @throws PaymentNotFoundException if the payment does not exist
   * @throws InvalidPaymentException if the payment is not in COMPLETED status
   */
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

  /**
   * Maps a Payment entity to a PaymentResponse DTO.
   *
   * @param payment the payment entity
   * @return the payment response
   */
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