package com.payment.contracts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Immutable data contract for user login.
 *
 * <p>Both fields are validated on construction using Jakarta Bean Validation.
 *
 * @param username login username (required, non-blank)
 * @param password plaintext password, checked against the stored hash (required, non-blank)
 * @author orvigas@gmail.com
 */
public record LoginRequest(
    @Schema(description = "Username", example = "jdoe")
    @NotBlank(message = "username is required")
    String username,

    @Schema(description = "Password")
    @NotBlank(message = "password is required")
    String password
) {}