package com.payment.repository;

import com.payment.entity.Payment;
import com.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

  Optional<Payment> findByPaymentId(String paymentId);

  List<Payment> findByUserId(String userId);

  List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status);

  List<Payment> findByStatusAndCreatedAtBetween(
      PaymentStatus status,
      LocalDateTime startDate,
      LocalDateTime endDate);

  int countByStatus(PaymentStatus status);
}