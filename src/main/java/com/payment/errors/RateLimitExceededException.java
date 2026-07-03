package com.payment.errors;

/**
 * Exception thrown when the rate limit for an operation is exceeded.
 *
 * @author orvigas@gmail.com
 */
public class RateLimitExceededException extends RuntimeException {

  /**
   * Constructs a new rate limit exceeded exception.
   *
   * @param message the exception message
   */
  public RateLimitExceededException(String message) {
    super(message);
  }

  /**
   * Constructs a new rate limit exceeded exception.
   *
   * @param message the exception message
   * @param cause the exception cause
   */
  public RateLimitExceededException(String message, Throwable cause) {
    super(message, cause);
  }
}
