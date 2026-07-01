package com.payment.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing a payment transaction.
 *
 * <p>This entity maps to the 'payments' table in the database and represents a single payment
 * transaction. It includes comprehensive audit trails with automatic timestamp management and
 * database indexes for optimal query performance.
 *
 * <p>A payment transitions through the following statuses: PENDING → PROCESSING → COMPLETED,
 * with optional REFUNDED or FAILED states for terminal conditions.
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    /** Unique identifier for the payment (UUID). */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private String paymentId;

    /** Identifier of the user initiating the payment. */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /** Transaction amount in the specified currency. */
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    /** ISO 4217 three-letter currency code. */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Identifier of the merchant receiving the payment. */
    @Column(name = "merchant", nullable = false)
    private String merchant;

    /** Current status of the payment (PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    /** Optional description of the payment. */
    @Column(name = "description")
    private String description;

    /** Timestamp when the payment was created (immutable). */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the payment was last updated. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Timestamp when the payment was completed or refunded. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Lifecycle callback executed before entity persistence. Sets initial timestamps and default
     * status.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = PaymentStatus.PENDING;
    }

    /**
     * Lifecycle callback executed before entity update. Updates the modification timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}