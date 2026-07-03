package com.payment.errors;

/**
 * Thrown when login credentials are missing, unknown, or don't match. Carries a single generic
 * message regardless of the underlying cause so responses don't reveal whether a username exists.
 *
 * @author orvigas@gmail.com
 */
public class InvalidCredentialsException extends RuntimeException {

  /**
   * Constructs an InvalidCredentialsException with the specified detail message.
   *
   * @param message the detail message
   */
  public InvalidCredentialsException(String message) {
    super(message);
  }
}
