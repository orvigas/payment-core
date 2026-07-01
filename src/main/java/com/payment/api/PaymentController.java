package com.payment.api;

import com.payment.dto.CreatePaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping
  public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
    log.info("POST /api/v1/payments - Creating payment");
    PaymentResponse response = paymentService.createPayment(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{paymentId}")
  public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
    log.info("GET /api/v1/payments/{} - Fetching payment", paymentId);
    PaymentResponse response = paymentService.getPayment(paymentId);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/{paymentId}/confirm")
  public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable String paymentId) {
    log.info("POST /api/v1/payments/{}/confirm - Confirming payment", paymentId);
    PaymentResponse response = paymentService.confirmPayment(paymentId);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/{paymentId}/refund")
  public ResponseEntity<PaymentResponse> refundPayment(@PathVariable String paymentId) {
    log.info("POST /api/v1/payments/{}/refund - Refunding payment", paymentId);
    PaymentResponse response = paymentService.refundPayment(paymentId);
    return ResponseEntity.ok(response);
  }
}