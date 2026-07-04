package com.payment.controllers;

import com.payment.contracts.CreatePaymentRequest;
import com.payment.contracts.PaymentResponse;
import com.payment.errors.RateLimitExceededException;
import com.payment.services.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

/**
 * REST Controller for payment operations.
 *
 * <p>
 * Provides endpoints for creating, retrieving, confirming, and refunding
 * payments. All
 * endpoints are versioned under /api/v1 and operate on Payment resources.
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment creation, retrieval, confirmation, and refund operations")
public class PaymentController {

  private final PaymentService paymentService;

  /**
   * Creates a new payment.
   *
   * @param request the payment creation request containing amount, currency, merchant, and
   *                description
   * @param authentication the authenticated caller; its name is the JWT subject and becomes the
   *                       payment's owner
   * @return ResponseEntity with HTTP 201 (Created) status and the created payment
   *         response
   */
  @PostMapping
  @Operation(summary = "Create a new payment", description = "Initiates a new payment transaction with the provided details. The payment is created in PENDING status and will be processed asynchronously.")
  @RateLimiter(name = "payment-creation", fallbackMethod = "createPaymentFallback")
  @ApiResponse(responseCode = "201", description = "Payment created successfully")
  @ApiResponse(responseCode = "400", description = "Invalid request payload (validation error)")
  @ApiResponse(responseCode = "500", description = "Internal server error")
  public ResponseEntity<PaymentResponse> createPayment(
      @Valid @RequestBody CreatePaymentRequest request, Authentication authentication) {
    log.info("POST /api/v1/payments - Creating payment");
    PaymentResponse response = paymentService.createPayment(request, authentication.getName());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Retrieves a payment by its ID.
   *
   * @param paymentId the unique identifier of the payment
   * @param authentication the authenticated caller; only the payment's owner may read it
   * @return ResponseEntity with HTTP 200 (OK) status and the payment details
   */
  @GetMapping("/{paymentId}")
  @Operation(summary = "Retrieve a payment", description = "Fetches the details of a specific payment transaction by its unique identifier.")
  @ApiResponse(responseCode = "200", description = "Payment retrieved successfully")
  @ApiResponse(responseCode = "403", description = "Payment belongs to a different user")
  @ApiResponse(responseCode = "404", description = "Payment not found")
  @ApiResponse(responseCode = "500", description = "Internal server error")
  public ResponseEntity<PaymentResponse> getPayment(
      @Parameter(description = "Unique identifier of the payment (UUID)") @PathVariable String paymentId,
      Authentication authentication) {
    log.info("GET /api/v1/payments/{} - Fetching payment", paymentId);
    PaymentResponse response = paymentService.getPayment(paymentId, authentication.getName());
    return ResponseEntity.ok(response);
  }

  /**
   * Confirms a pending payment.
   *
   * @param paymentId the unique identifier of the payment to confirm
   * @param authentication the authenticated caller; only the payment's owner may confirm it
   * @return ResponseEntity with HTTP 200 (OK) status and the updated payment
   *         details
   */
  @PostMapping("/{paymentId}/confirm")
  @Operation(summary = "Confirm a payment", description = "Transitions a PENDING payment to PROCESSING status. The payment will then be charged and processed asynchronously.")
  @ApiResponse(responseCode = "200", description = "Payment confirmed successfully")
  @ApiResponse(responseCode = "403", description = "Payment belongs to a different user")
  @ApiResponse(responseCode = "404", description = "Payment not found")
  @ApiResponse(responseCode = "400", description = "Payment cannot be confirmed (invalid status)")
  @ApiResponse(responseCode = "500", description = "Internal server error")
  public ResponseEntity<PaymentResponse> confirmPayment(
      @Parameter(description = "Unique identifier of the payment (UUID)") @PathVariable String paymentId,
      Authentication authentication) {
    log.info("POST /api/v1/payments/{}/confirm - Confirming payment", paymentId);
    PaymentResponse response = paymentService.confirmPayment(paymentId, authentication.getName());
    return ResponseEntity.ok(response);
  }

  /**
   * Refunds a confirmed payment.
   *
   * @param paymentId the unique identifier of the payment to refund
   * @param authentication the authenticated caller; only the payment's owner may refund it
   * @return ResponseEntity with HTTP 200 (OK) status and the updated payment
   *         details
   */
  @PostMapping("/{paymentId}/refund")
  @Operation(summary = "Refund a payment", description = "Initiates a refund for a COMPLETED payment. The refund will be processed asynchronously and the payment status will transition to REFUNDED.")
  @ApiResponse(responseCode = "200", description = "Refund initiated successfully")
  @ApiResponse(responseCode = "403", description = "Payment belongs to a different user")
  @ApiResponse(responseCode = "404", description = "Payment not found")
  @ApiResponse(responseCode = "400", description = "Payment cannot be refunded (invalid status)")
  @ApiResponse(responseCode = "500", description = "Internal server error")
  public ResponseEntity<PaymentResponse> refundPayment(
      @Parameter(description = "Unique identifier of the payment (UUID)") @PathVariable String paymentId,
      Authentication authentication) {
    log.info("POST /api/v1/payments/{}/refund - Refunding payment", paymentId);
    PaymentResponse response = paymentService.refundPayment(paymentId, authentication.getName());
    return ResponseEntity.ok(response);
  }

  /**
   * Fallback for createPayment when the rate limit is exceeded.
   *
   * <p>Only matches {@link RequestNotPermitted}, the exception Resilience4j throws when a
   * permit can't be acquired - not {@code Exception} in general, otherwise unrelated failures
   * from {@code createPayment} (e.g. a database error) would get reported to callers as a
   * false 429 instead of surfacing as the real error.
   *
   * @param request the payment creation request
   * @param authentication the authenticated caller
   * @param ex the rate limit exception
   * @throws RateLimitExceededException when the rate limit is exceeded
   */
  public ResponseEntity<PaymentResponse> createPaymentFallback(
      CreatePaymentRequest request, Authentication authentication, RequestNotPermitted ex) {
    log.warn("Payment creation rate limit exceeded");
    throw new RateLimitExceededException("Payment creation rate limit exceeded. Please try again later.");
  }
}