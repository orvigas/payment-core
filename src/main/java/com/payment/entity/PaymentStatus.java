package com.payment.entity;

/**
 * Enumeration of payment statuses representing the lifecycle of a payment transaction.
 *
 * <p>A payment transitions through these statuses in the following order:
 * <ul>
 *   <li>{@code PENDING} - Payment has been created and awaits confirmation
 *   <li>{@code PROCESSING} - Payment is currently being processed
 *   <li>{@code COMPLETED} - Payment has been successfully completed
 *   <li>{@code FAILED} - Payment processing failed
 *   <li>{@code REFUNDED} - Payment has been refunded to the user
 * </ul>
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
public enum PaymentStatus {
  /** Payment has been created and awaits confirmation. */
  PENDING,
  /** Payment is currently being processed. */
  PROCESSING,
  /** Payment has been successfully completed. */
  COMPLETED,
  /** Payment processing failed. */
  FAILED,
  /** Payment has been refunded to the user. */
  REFUNDED
}