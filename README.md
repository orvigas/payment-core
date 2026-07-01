# Payment Core System

[![Build Status](https://github.com/orvigas/payment-core/actions/workflows/ci.yml/badge.svg)](https://github.com/orvigas/payment-core/actions)
[![Code Coverage](https://codecov.io/gh/orvigas/payment-core/branch/main/graph/badge.svg)](https://codecov.io/gh/orvigas/payment-core)

A scalable, production-ready payment processing system built with Spring Boot 3, PostgreSQL, and modern Java features. This system provides RESTful APIs for payment operations with comprehensive error handling, validation, and transactional integrity.

## Features

- RESTful API for payment operations (Create, Retrieve, Confirm, Refund)
- Event-driven architecture with Apache Kafka for asynchronous payment processing
- Immutable data contracts using Java Records
- PostgreSQL database with strategic indexing for optimal query performance
- Comprehensive unit and integration testing with JUnit 5 and Mockito
- Testcontainers for isolated database and Kafka testing
- Docker & Docker Compose support for local development
- Global exception handling with structured error responses
- Request validation with Jakarta Bean Validation
- Transactional consistency with Spring `@Transactional`
- Structured logging with SLF4J
- Complete Javadoc documentation for all classes
- Spring Boot Actuator for health checks and monitoring

## Tech Stack

### Core Framework

- **Language:** Java 21 (LTS with Records, Pattern Matching, Virtual Threads support)
- **Framework:** Spring Boot 3.5.0
- **Spring Version:** 6.2.x (via Spring Boot BOM)
- **Build Tool:** Apache Maven 3.8+
- **Compiler Plugin:** Maven Compiler 3.13.0
- **JPA Provider:** Hibernate ORM (via Spring Boot)

### Database & Data Access

- **Database:** PostgreSQL 15 (with Alpine Linux in Docker)
- **Connection Pool:** HikariCP (included in Spring Boot)
- **ORM:** Spring Data JPA with Hibernate
- **Migrations:** Automatic DDL with `spring.jpa.hibernate.ddl-auto`

### Event-Driven & Messaging

- **Message Broker:** Apache Kafka 3.x
- **Spring Kafka:** 3.1.2 for producer/consumer integration
- **Events:** PaymentInitiatedEvent, PaymentCompletedEvent, PaymentFailedEvent, PaymentChargedEvent
- **Embedded Kafka:** spring-kafka-test for integration testing with embedded brokers

### Testing & Quality

- **Unit Testing:** JUnit 5 (Jupiter)
- **Mocking:** Mockito 5.2.0 with inline agent support
- **Integration Testing:** Testcontainers for PostgreSQL and Kafka
- **Code Coverage:** JaCoCo 0.8.12 (Java 23 compatible)
- **Surefire Plugin:** Maven Surefire 3.5.3
- **Test Database:** H2 in-memory database for test profile isolation

### API & Validation

- **REST Framework:** Spring Web MVC
- **Validation Framework:** Jakarta Bean Validation (Jakarta.validation)
- **JSON Processing:** Jackson (included in Spring Boot)
- **Logging:** SLF4J with Logback
- **Boilerplate Reduction:** Lombok 1.18.36

### DevOps & Containerization

- **Containerization:** Docker with Dockerfile
- **Container Orchestration:** Docker Compose
- **Health Checks:** Spring Boot Actuator
- **Monitoring:** Actuator endpoints (/health, /info)

## Quick Start

### Prerequisites

- Java 21 LTS or higher
- Maven 3.8 or higher
- Docker & Docker Compose (for local development)

### Local Development

```bash
# Clone repo
git clone https://github.com/orvigas/payment-core.git
cd payment-core

# Build
mvn clean package

# Start with Docker Compose
docker-compose up -d

# Check health
curl http://localhost:8080/health
```

### API Endpoints

#### Create Payment

```bash
POST /api/v1/payments
Content-Type: application/json

{
  "userId": "user123",
  "amount": 5000.00,
  "currency": "MXN",
  "merchant": "jersey-mikes",
  "description": "Order #123"
}
```

#### Get Payment

```bash
GET /api/v1/payments/{paymentId}
```

#### Confirm Payment

```bash
POST /api/v1/payments/{paymentId}/confirm
```

#### Refund Payment

```bash
POST /api/v1/payments/{paymentId}/refund
```

## Testing

```bash
# Run all tests
mvn test

# Run with coverage report
mvn clean test jacoco:report

# View coverage report (opens in browser)
open target/site/jacoco/index.html
```

### Kafka Integration Testing

```bash
# Run Kafka integration tests
mvn test -Dtest=KafkaIntegrationTest

# Run all tests including Kafka tests
mvn test

# View test logs for Kafka events
mvn test -Dtest=KafkaIntegrationTest -X 2>&1 | grep -i kafka
```

**Test Configuration:**
- Uses `@EmbeddedKafka` for isolated Kafka broker in test
- Uses H2 in-memory database for test profile (`application-test.yml`)
- Typed consumer factories for proper event deserialization
- Tests verify event serialization, publication, and consumption

### Code Coverage

- **Current:** 93% code coverage with comprehensive unit and integration test suite (135 tests)
- **Coverage Report:** Run `mvn clean test jacoco:report` to generate detailed coverage report at `target/site/jacoco/index.html`

## Architecture

### Layered Design

The project follows a classic layered architecture pattern:

1. **Controllers Layer** (`com.payment.controllers`): REST endpoints and request/response handling
   - `PaymentController`: Exposes `/api/v1/payments` endpoints for all payment operations

2. **Services Layer** (`com.payment.services`): Business logic and orchestration
   - `PaymentService`: Core payment operations (create, retrieve, confirm, refund) with transactional consistency
   - `PaymentValidator`: Request validation logic and business rule enforcement

3. **Data Access Layer** (`com.payment.repositories`): Database interactions
   - `PaymentRepository`: Spring Data JPA interface for Payment entity with optimized queries

4. **Exception Handling** (`com.payment.errors`): Centralized error responses
   - `GlobalExceptionHandler`: Maps exceptions to structured HTTP error responses
   - Custom exceptions: `PaymentNotFoundException`, `InvalidPaymentException`

5. **Data Model** (`com.payment.models`): JPA entities
   - `Payment`: Main entity with UUID for paymentId, strategic indexing for performance
   - `PaymentStatus`: Enum for payment lifecycle states (PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED)

6. **API Contracts** (`com.payment.contracts`): Request/response DTOs
   - `CreatePaymentRequest`: Immutable request contract with validation (Java Record)
   - `PaymentResponse`: Immutable response contract (Java Record)

7. **Event Streaming** (`com.payment.kafka` and `com.payment.events`): Event-driven architecture
   - `PaymentProducer`: Publishes payment events to Kafka topics
   - Consumers:
     - `ChargingConsumer`: Processes payment charging events
     - `NotificationConsumer`: Sends notifications for payment state changes
     - `AnalyticsConsumer`: Tracks analytics and metrics
   - `PaymentInitiatedEvent`, `PaymentCompletedEvent`, `PaymentFailedEvent`, `PaymentChargedEvent`: Immutable event contracts
   - `KafkaConfig`: Centralized Kafka configuration with typed consumer factories
   - `KafkaTopics`: Constants for topic and consumer group names

### Key Design Patterns

- **Immutable Contracts**: Request/response objects implemented as Java Records (sealed, immutable)
- **Transactional Consistency**: `@Transactional` ensures atomicity across service operations
- **Two-Level Validation**:
  - Jakarta Bean Validation annotations on request objects
  - Custom `PaymentValidator` for complex business rules
- **Exception Translation**: Service layer throws custom exceptions, caught by `GlobalExceptionHandler`
- **Repository Pattern**: Spring Data JPA abstracts database operations
- **Strategic Indexing**: Optimized queries on `paymentId`, `userId`, and `status` fields

### Data Model

#### Payment Entity

```
Payment (Table: payments)
├── paymentId (String, UUID, Primary Key, indexed)
├── userId (String, indexed for user lookups)
├── amount (BigDecimal)
├── currency (String)
├── merchant (String)
├── description (String)
├── status (PaymentStatus enum: PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED)
├── createdAt (LocalDateTime, auto-populated)
└── updatedAt (LocalDateTime, auto-updated)
```

#### Payment Lifecycle

1. **PENDING**: Initial state after payment creation
2. **PROCESSING**: Payment is being processed
3. **COMPLETED**: Payment has been successfully completed
4. **FAILED**: Payment processing failed (terminal state)
5. **REFUNDED**: Payment has been refunded (terminal state)

### Directory Structure

```
src/main/java/com/payment/
├── PaymentCoreApplication.java         # Spring Boot entry point
├── controllers/
│   └── PaymentController.java          # REST endpoints (@PostMapping, @GetMapping)
├── config/
│   └── KafkaConfig.java                # Kafka configuration (producers, consumers, topics)
├── contracts/
│   ├── CreatePaymentRequest.java       # Request DTO (Java Record)
│   └── PaymentResponse.java            # Response DTO (Java Record)
├── events/
│   ├── PaymentInitiatedEvent.java      # Event: payment creation (Java Record)
│   ├── PaymentCompletedEvent.java      # Event: payment completion
│   ├── PaymentFailedEvent.java         # Event: payment failure
│   └── PaymentChargedEvent.java        # Event: charging operation
├── kafka/
│   ├── KafkaTopics.java                # Topic and consumer group constants
│   ├── PaymentProducer.java            # Publishes payment events to Kafka
│   ├── ChargingConsumer.java           # Consumes and processes charging events
│   ├── NotificationConsumer.java       # Consumes and sends notifications
│   └── AnalyticsConsumer.java          # Consumes and tracks analytics
├── models/
│   ├── Payment.java                    # JPA entity with @Entity annotation
│   └── PaymentStatus.java              # Status enum
├── errors/
│   ├── GlobalExceptionHandler.java     # @RestControllerAdvice for centralized error handling
│   ├── PaymentNotFoundException.java    # Custom exception for missing payments
│   └── InvalidPaymentException.java    # Custom exception for invalid operations
├── repositories/
│   └── PaymentRepository.java          # Spring Data JPA interface
└── services/
    ├── PaymentService.java             # @Service with @Transactional business logic
    └── PaymentValidator.java           # Validation rules

src/main/resources/
├── application.yml                      # Spring Boot configuration (YAML format)
└── data.sql                             # Database seed: 1000 payment records auto-loaded on startup

src/test/java/com/payment/              # 135 unit/integration tests (93% coverage)
├── PaymentServiceTest.java             # Unit tests for business logic
├── PaymentControllerTest.java          # Integration tests for REST endpoints
├── PaymentValidatorTest.java           # Validation logic tests
├── PaymentRepositoryTest.java          # Repository layer tests
├── PaymentIntegrationTest.java         # Full-stack integration tests with Testcontainers
├── kafka/
│   ├── ChargingConsumerTest.java       # Charging consumer unit tests
│   ├── NotificationConsumerTest.java   # Notification consumer unit tests
│   ├── AnalyticsConsumerTest.java      # Analytics consumer unit tests
│   ├── KafkaIntegrationTest.java       # Kafka integration tests
│   ├── KafkaConsumersIntegrationTest.java # Consumer flow integration tests
│   └── KafkaEndToEndTest.java          # End-to-end tests with Spring Boot context
└── events/
    └── PaymentEventTest.java           # Event class validation tests
```

## Configuration

### Database Setup

- **Automatic DDL**: `spring.jpa.hibernate.ddl-auto=create-drop` (schema is dropped and recreated on every application startup)
- **Seed Data**: `src/main/resources/data.sql` is automatically executed on startup, loading 1000 payment records for testing
- **PostgreSQL Driver**: Included in dependencies for runtime
- **Connection Pool**: HikariCP (default connection pool in Spring Boot)
- **Database Initialization**: Automatic table creation via Hibernate DDL followed by seed data population

### Kafka Configuration

- **Bootstrap Servers**: `kafka:9092` (Docker network) for production, overridden in test profiles
- **Topics**:
  - `payment-initiated`: Payment creation events (3 partitions)
  - `payment-charged`: Payment charging events (3 partitions)
  - `payment-completed`: Payment completion events (3 partitions)
  - `payment-failed`: Payment failure events (3 partitions)
- **Consumer Groups**:
  - `charging-service`: Handles payment charging
  - `notification-service`: Sends payment notifications
  - `analytics-service`: Tracks payment analytics
- **Serialization**: JSON serialization for events with type mapping for proper deserialization
- **Producer**: Acks all replicas, 3 retries, Snappy compression
- **Consumer**: Auto-commit disabled, earliest offset reset for new consumers

### Logging

- **Framework**: SLF4J with Logback backend
- **Log Levels**: Configured via `application.properties` and `application-docker.properties`
- **Usage Pattern**: Leverage `@Slf4j` annotation from Lombok for structured logging

## Package Structure Refactoring

The project has been reorganized for improved maintainability:

- **`com.payment.entity` → `com.payment.models`**: Data model classes (Payment, PaymentStatus) moved to `models` package
- **`com.payment.exceptions` → `com.payment.errors`**: Exception handling classes moved to `errors` package
  - `GlobalExceptionHandler.java`: Centralized exception handling
  - `PaymentNotFoundException.java`: Custom exception for missing payments
  - `InvalidPaymentException.java`: Custom exception for invalid operations

All imports have been updated across main and test code. The package structure now better reflects the purpose of each component.

## Common Development Tasks

### Adding a New Payment Operation

1. Create method in `PaymentService` with `@Transactional` annotation
2. Add validation logic to `PaymentValidator` if needed
3. Add corresponding endpoint to `PaymentController` with `@PostMapping` or `@GetMapping`
4. Define custom exception in `com.payment.errors` if introducing new error scenarios
5. Add exception handler in `GlobalExceptionHandler` for proper error response mapping
6. Write integration tests using Testcontainers (see test files in `src/test/java/com/payment` as reference)

### Modifying Database Schema

1. Update `Payment` entity in `src/main/java/com/payment/models/Payment.java`
2. Add corresponding property to `CreatePaymentRequest` and `PaymentResponse`
3. Update `PaymentValidator` if validation rules change
4. Database schema is recreated on startup with `ddl-auto=create-drop` (via `application.yml`)
5. Update seed data in `src/main/resources/data.sql` if necessary
6. Update integration tests to verify the schema changes

### Running Tests

```bash
# Run all tests
mvn test

# Run single test class
mvn test -Dtest=PaymentServiceTest

# Run specific test method
mvn test -Dtest=PaymentServiceTest#testCreatePayment

# Run with code coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Deployment

### Docker Deployment

```bash
# Build Docker image
docker build -t payment-core:latest .

# Run with Docker Compose (includes PostgreSQL)
docker-compose up -d

# Check application health
curl http://localhost:8080/health

# View application info
curl http://localhost:8080/info

# Stop services
docker-compose down
```

### Health & Monitoring

- **Health Endpoint**: `GET http://localhost:8080/health` - Returns application health status
- **Info Endpoint**: `GET http://localhost:8080/info` - Returns application metadata
- **Metrics**: Available via Spring Boot Actuator (configure in `application.properties`)

## Notes for Future Development

- **Lombok Configuration**: Uses `@Data`, `@RequiredArgsConstructor`, `@Slf4j` annotations; config in `lombok.config`
- **API Versioning**: Currently on `/api/v1/payments`; maintain this pattern for new endpoints
- **Transactional Boundaries**: Service layer methods are transactional; avoid nested `@Transactional` on repository
- **Error Responses**: Always throw custom exceptions; `GlobalExceptionHandler` maps them to structured JSON with timestamp, status, error, and message
- **Java 21 Features**: Leverage Records for immutable DTOs, Pattern Matching, Virtual Threads support
