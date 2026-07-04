package com.payment.errors;

/**
 * Exception thrown when an authenticated user attempts to access or modify a payment owned by
 * someone else.
 *
 * <p>This unchecked exception is thrown when the caller's identity, taken from the JWT, does not
 * match the {@code userId} recorded on the payment being read, confirmed, or refunded.
 *
 * @author orvigas@gmail.com
 */
public class PaymentAccessDeniedException extends RuntimeException {
  /**
   * Constructs a PaymentAccessDeniedException with the specified detail message.
   *
   * @param message the detail message
   */
  public PaymentAccessDeniedException(String message) {
    super(message);
  }
}
