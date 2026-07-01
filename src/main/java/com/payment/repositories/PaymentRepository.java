package com.payment.repositories;

import com.payment.models.Payment;
import com.payment.models.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Payment entity.
 *
 * <p>Provides CRUD operations and custom query methods for accessing payment data from the
 * database. All methods are automatically implemented by Spring Data JPA.
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

  /**
   * Finds a payment by its unique identifier.
   *
   * @param paymentId the payment ID
   * @return Optional containing the payment if found
   */
  Optional<Payment> findByPaymentId(String paymentId);

  /**
   * Finds all payments for a specific user.
   *
   * @param userId the user ID
   * @return list of payments belonging to the user
   */
  List<Payment> findByUserId(String userId);

  /**
   * Finds all payments for a user with a specific status.
   *
   * @param userId the user ID
   * @param status the payment status
   * @return list of payments matching the criteria
   */
  List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status);

  /**
   * Finds all payments with a specific status created within a date range.
   *
   * @param status the payment status
   * @param startDate the start date (inclusive)
   * @param endDate the end date (inclusive)
   * @return list of payments matching the criteria
   */
  List<Payment> findByStatusAndCreatedAtBetween(
      PaymentStatus status,
      LocalDateTime startDate,
      LocalDateTime endDate);

  /**
   * Counts the number of payments with a specific status.
   *
   * @param status the payment status
   * @return count of payments with the specified status
   */
  int countByStatus(PaymentStatus status);
}