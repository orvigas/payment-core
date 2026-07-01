package com.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Payment Core System application.
 *
 * <p>This Spring Boot application provides a scalable, production-ready payment processing system
 * with REST API endpoints, PostgreSQL database integration, and comprehensive error handling.
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
@SpringBootApplication
public class PaymentCoreApplication {

  /**
   * Starts the Payment Core application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(PaymentCoreApplication.class, args);
  }
}