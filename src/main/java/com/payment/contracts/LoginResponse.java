package com.payment.contracts;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable data contract for login response with JWT tokens.
 *
 * <p>This record encapsulates the authentication response including JWT access and refresh tokens.
 * All fields are immutable and set upon construction.
 *
 * @param accessToken JWT token for API authentication (Bearer token)
 * @param refreshToken JWT token for obtaining a new access token
 * @param tokenType authentication scheme (typically "Bearer")
 * @param expiresIn access token expiration time in seconds
 * @param userId unique identifier of the authenticated user
 * @author orvigas@gmail.com
 */
public record LoginResponse(
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,

    @Schema(description = "JWT refresh token")
    String refreshToken,

    @Schema(description = "Token type", example = "Bearer")
    String tokenType,

    @Schema(description = "Expiration time in seconds", example = "3600")
    Integer expiresIn,

    @Schema(description = "User ID", example = "user_123")
    String userId
) {}