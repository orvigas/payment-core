package com.payment.contracts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Immutable data contract for user login.
 *
 * <p>This record encapsulates the required information to authenticate a user. The userId field is
 * validated upon construction using Jakarta Bean Validation.
 *
 * @param userId unique identifier of the user attempting to login (required, non-blank)
 * @author orvigas@gmail.com
 */
public record LoginRequest(
    @Schema(description = "User ID", example = "user_123")
    @NotBlank(message = "userId is required")
    String userId
) {}