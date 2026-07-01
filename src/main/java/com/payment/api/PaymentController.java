package com.payment.api;

import com.payment.contracts.CreatePaymentRequest;
import com.payment.contracts.PaymentResponse;
import com.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * REST Controller for payment operations.
 *
 * <p>Provides endpoints for creating, retrieving, confirming, and refunding payments. All
 * endpoints are versioned under /api/v1 and operate on Payment resources.
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

  private final PaymentService paymentService;

  /**
   * Creates a new payment.
   *
   * @param request the payment creation request containing user, amount, currency, merchant, and
   *     description
   * @return ResponseEntity with HTTP 201 (Created) status and the created payment response
   */
  @PostMapping
  public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
    log.info("POST /api/v1/payments - Creating payment");
    PaymentResponse response = paymentService.createPayment(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Retrieves a payment by its ID.
   *
   * @param paymentId the unique identifier of the payment
   * @return ResponseEntity with HTTP 200 (OK) status and the payment details
   */
  @GetMapping("/{paymentId}")
  public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
    log.info("GET /api/v1/payments/{} - Fetching payment", paymentId);
    PaymentResponse response = paymentService.getPayment(paymentId);
    return ResponseEntity.ok(response);
  }

  /**
   * Confirms a pending payment.
   *
   * @param paymentId the unique identifier of the payment to confirm
   * @return ResponseEntity with HTTP 200 (OK) status and the updated payment details
   */
  @PostMapping("/{paymentId}/confirm")
  public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable String paymentId) {
    log.info("POST /api/v1/payments/{}/confirm - Confirming payment", paymentId);
    PaymentResponse response = paymentService.confirmPayment(paymentId);
    return ResponseEntity.ok(response);
  }

  /**
   * Refunds a confirmed payment.
   *
   * @param paymentId the unique identifier of the payment to refund
   * @return ResponseEntity with HTTP 200 (OK) status and the updated payment details
   */
  @PostMapping("/{paymentId}/refund")
  public ResponseEntity<PaymentResponse> refundPayment(@PathVariable String paymentId) {
    log.info("POST /api/v1/payments/{}/refund - Refunding payment", paymentId);
    PaymentResponse response = paymentService.refundPayment(paymentId);
    return ResponseEntity.ok(response);
  }
}