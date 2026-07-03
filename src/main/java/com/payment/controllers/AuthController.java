package com.payment.controllers;

import com.payment.contracts.LoginRequest;
import com.payment.contracts.LoginResponse;
import com.payment.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and token management endpoints")
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for userId: {}", request.userId());

        // In production, validate credentials against database
        // For now, accept any userId (load testing)

        String accessToken = tokenProvider.generateToken(request.userId());
        String refreshToken = tokenProvider.generateRefreshToken(request.userId());

        LoginResponse response = new LoginResponse(
            accessToken,
            refreshToken,
            "Bearer",
            3600,
            request.userId()
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<LoginResponse> refreshToken(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String refreshToken = authHeader.substring("Bearer ".length());

        if (!tokenProvider.isTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = tokenProvider.getUserIdFromToken(refreshToken);
        String newAccessToken = tokenProvider.generateToken(userId);

        LoginResponse response = new LoginResponse(
            newAccessToken,
            refreshToken,
            "Bearer",
            3600,
            userId
        );

        return ResponseEntity.ok(response);
    }
}