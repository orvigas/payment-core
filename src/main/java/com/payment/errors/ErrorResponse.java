package com.payment.errors;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Standard error response structure for REST API errors.
 *
 * <p>This record represents the unified error response format returned by all REST endpoints when
 * an error occurs. The response includes timestamp, HTTP status code, error type, and detailed
 * message.
 *
 * @param timestamp when the error occurred
 * @param status HTTP status code
 * @param error error type (HTTP status reason phrase)
 * @param message descriptive error message
 * @author orvigas@gmail.com
 */
@Schema(description = "Error response structure for REST API errors")
public record ErrorResponse(
    @Schema(description = "Timestamp when the error occurred", example = "2026-07-01T14:00:00")
    LocalDateTime timestamp,

    @Schema(description = "HTTP status code", example = "404")
    int status,

    @Schema(description = "HTTP status reason phrase", example = "Not Found")
    String error,

    @Schema(description = "Descriptive error message", example = "Payment with ID 123 not found")
    String message
) {}
