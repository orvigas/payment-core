package com.payment.exceptions;

/**
 * Exception thrown when a requested payment cannot be found.
 *
 * <p>This unchecked exception is thrown when an attempt is made to retrieve, confirm, or refund a
 * payment that does not exist in the database.
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
public class PaymentNotFoundException extends RuntimeException {
  /**
   * Constructs a PaymentNotFoundException with the specified detail message.
   *
   * @param message the detail message
   */
  public PaymentNotFoundException(String message) {
    super(message);
  }
}