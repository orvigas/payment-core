package com.payment.services;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payment.contracts.CreatePaymentRequest;
import com.payment.contracts.PaymentResponse;
import com.payment.errors.InvalidPaymentException;
import com.payment.errors.PaymentNotFoundException;
import com.payment.events.PaymentInitiatedEvent;
import com.payment.kafka.PaymentProducer;
import com.payment.models.Payment;
import com.payment.models.PaymentStatus;
import com.payment.observability.CustomMetrics;
import com.payment.repositories.PaymentRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.instrument.Timer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service layer for payment operations.
 *
 * <p>
 * Orchestrates business logic for payment processing, including creation,
 * retrieval,
 * confirmation, and refunding. All operations are transactional to ensure data
 * consistency.
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
@Data
@Service
@Slf4j
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentValidator paymentValidator;
  private final PaymentProducer paymentProducer;
  private final CustomMetrics customMetrics;

  /**
   * Creates a new payment transaction.
   *
   * <p>
   * Validates the request, creates a new Payment entity with PENDING status,
   * persists it to
   * the database, and returns the response.
   *
   * @param request the payment creation request
   * @return the created payment response
   * @throws InvalidPaymentException if the request fails validation
   */
  @Transactional
  public PaymentResponse createPayment(CreatePaymentRequest request) {
    log.info("Creating payment for user: {}, amount: {}", request.userId(), request.amount());

    Timer.Sample sample = customMetrics.startPaymentProcessing();

    try {
      // Validate
      paymentValidator.validateCreatePaymentRequest(request);

      // Create entity
      Payment payment = new Payment();
      payment.setUserId(request.userId());
      payment.setAmount(request.amount());
      payment.setCurrency(request.currency());
      payment.setMerchant(request.merchant());
      payment.setDescription(request.description());
      payment.setStatus(PaymentStatus.PENDING);

      // Save
      Payment saved = paymentRepository.save(payment);
      customMetrics.incrementPaymentCreated();
      log.info("Payment created: {}", saved.getPaymentId());

      // Publish event
      PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
          .paymentId(saved.getPaymentId())
          .userId(saved.getUserId())
          .amount(saved.getAmount())
          .currency(saved.getCurrency())
          .merchant(saved.getMerchant())
          .description(saved.getDescription())
          .createdAt(saved.getCreatedAt())
          .build();

      paymentProducer.publishPaymentInitiated(event);

      return mapToResponse(saved);

    } finally {
      customMetrics.recordPaymentProcessing(sample);
    }
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
   * <p>
   * Transitions the payment status from PENDING → PROCESSING → COMPLETED and sets
   * the
   * completion timestamp.
   *
   * @param paymentId the payment ID
   * @return the confirmed payment response
   * @throws PaymentNotFoundException if the payment does not exist
   * @throws InvalidPaymentException  if the payment is not in PENDING status
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
   * <p>
   * Transitions the payment status from COMPLETED to REFUNDED.
   *
   * @param paymentId the payment ID
   * @return the refunded payment response
   * @throws PaymentNotFoundException if the payment does not exist
   * @throws InvalidPaymentException  if the payment is not in COMPLETED status
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
   * Charges a payment asynchronously through the external processor.
   *
   * <p>Guarded by a circuit breaker, retry, and time limiter. When the circuit
   * opens or all retries are exhausted, {@link #chargerFallback(String, Exception)}
   * returns the payment in its current state so the charge can be retried later.
   *
   * @param paymentId the payment identifier
   * @return future completing with the charged payment, or failing if the charge
   *         is rejected or the payment does not exist
   */
  @CircuitBreaker(name = "charger-service", fallbackMethod = "chargerFallback")
  @Retry(name = "charger-retry")
  @TimeLimiter(name = "charger-timeout")
  public CompletableFuture<PaymentResponse> chargePaymentAsync(String paymentId) {
    log.info("Charging payment: {} (with resilience patterns)", paymentId);

    return CompletableFuture.supplyAsync(() -> {
      Payment payment = paymentRepository.findByPaymentId(paymentId)
          .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

      Timer.Sample sample = customMetrics.startChargeProcessing();

      try {
        // Simulate external call
        boolean success = callExternalProcessor(payment);

        if (success) {
          customMetrics.incrementChargeSuccess();
          payment.setStatus(PaymentStatus.COMPLETED);
        } else {
          customMetrics.incrementChargeFailure();
          throw new RuntimeException("Charge failed");
        }

        paymentRepository.save(payment);
        return mapToResponse(payment);

      } finally {
        customMetrics.recordChargeProcessing(sample);
      }
    });
  }

  /**
   * Fallback for {@link #chargePaymentAsync(String)} when the circuit breaker is
   * open or the charge attempt fails.
   *
   * <p>Returns the payment in its current state instead of failing the request;
   * the charge is expected to be retried later.
   *
   * @param paymentId the payment identifier
   * @param ex the failure that triggered the fallback
   * @return future completing with the payment in its current state
   * @throws PaymentNotFoundException if the payment does not exist
   */
  public CompletableFuture<PaymentResponse> chargerFallback(String paymentId, Exception ex) {
    log.warn("Charger fallback triggered for payment: {}. Error: {}", paymentId, ex.getMessage());
    customMetrics.incrementChargeFailure();

    // Return pending response (retry later)
    Payment payment = paymentRepository.findByPaymentId(paymentId)
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

    return CompletableFuture.completedFuture(mapToResponse(payment));
  }

  /**
   * Calls the external payment processor.
   *
   * <p>Simulated with a 90% success rate. Package-private so tests can stub the
   * outcome deterministically.
   *
   * @param payment the payment to charge
   * @return true if the charge succeeded
   */
  boolean callExternalProcessor(Payment payment) {
    return Math.random() < 0.9;
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