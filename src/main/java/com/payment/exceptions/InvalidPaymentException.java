package com.payment.exceptions;

/**
 * Exception thrown when a payment request or operation is invalid.
 *
 * <p>This unchecked exception is thrown when a payment fails validation checks, such as invalid
 * amount, currency, or state transitions.
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
public class InvalidPaymentException extends RuntimeException {
  /**
   * Constructs an InvalidPaymentException with the specified detail message.
   *
   * @param message the detail message
   */
  public InvalidPaymentException(String message) {
    super(message);
  }
}