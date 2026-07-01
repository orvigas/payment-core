package com.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

/**
 * Main entry point for the Payment Core System application.
 *
 * <p>This Spring Boot application provides a scalable, production-ready payment processing system
 * with REST API endpoints, PostgreSQL database integration, and comprehensive error handling.
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Payment Core API",
        version = "1.0.0",
        description = "RESTful API for scalable payment processing with Kafka event streaming, comprehensive validation, and asynchronous notifications",
        contact = @Contact(name = "Orlando Villegas", email = "orvigas@gmail.com"),
        license = @License(name = "Proprietary")
    )
)
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