package com.payment.models;

/**
 * Roles a user can be granted, mapped to Spring Security authorities with a {@code ROLE_} prefix.
 *
 * @author orvigas@gmail.com
 */
public enum Role {
  /** Standard user; can create and manage their own payments. */
  USER,
  /** Administrative access. */
  ADMIN
}
