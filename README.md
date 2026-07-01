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

## Endpoint Flow Walkthrough

This section explains what happens when each endpoint receives a request, from the client all the way through the system and back.

### 1. Create Payment: POST /api/v1/payments

**Request:**
```json
{
  "userId": "user123",
  "amount": 5000.00,
  "currency": "MXN",
  "merchant": "jersey-mikes",
  "description": "Order #123"
}
```

**Step-by-Step Flow:**

1. **Client sends HTTP POST request** → REST endpoint receives the request with JSON payload
2. **PaymentController.createPayment()** → Spring deserializes JSON into CreatePaymentRequest record
3. **Request Validation** → Jakarta Bean Validation annotations are checked automatically
4. **PaymentValidator.validateCreatePaymentRequest()** → Custom business rule validation runs (user ID format, positive amount, valid currency)
5. **Payment entity created** → New Payment object instantiated with PENDING status, UUID generated for paymentId
6. **PaymentRepository.save()** → Entity persisted to PostgreSQL database (INSERT query)
7. **PaymentProducer.publishPaymentInitiated()** → PaymentInitiatedEvent created and published to Kafka topic `payment-initiated`
8. **Kafka Consumers receive event**:
   - ChargingConsumer: Processes payment charging logic asynchronously
   - NotificationConsumer: Prepares notification messages
   - AnalyticsConsumer: Records analytics data
9. **PaymentResponse created** → Payment entity mapped to response DTO (Record)
10. **HTTP 201 Created response** → Client receives payment details with new paymentId

**Expected Result:**
- Status: 201 Created
- Response body contains: paymentId, userId, amount, currency, merchant, status (PENDING), timestamps
- Database: New row inserted in payments table
- Kafka: Event published to 3 topics via producers

---

### 2. Get Payment: GET /api/v1/payments/{paymentId}

**Request:**
```
GET /api/v1/payments/550e8400-e29b-41d4-a716-446655440000
```

**Step-by-Step Flow:**

1. **Client sends HTTP GET request** → Spring extracts paymentId from URL path
2. **PaymentController.getPayment()** → Path variable captured and passed to service layer
3. **PaymentService.getPayment()** → Service method marked as read-only transactional
4. **PaymentRepository.findByPaymentId()** → JPA query executed against PostgreSQL (SELECT query using paymentId index)
5. **Database lookup** → PostgreSQL uses index on paymentId column for fast lookup
6. **Entity found or not found**:
   - Found: Payment object returned from query
   - Not found: PaymentNotFoundException thrown
7. **Exception handling (if not found)** → GlobalExceptionHandler catches exception and creates error response
8. **PaymentResponse mapping** → Payment entity converted to response DTO
9. **HTTP 200 OK response** → Client receives current payment details

**Expected Result:**
- Status: 200 OK
- Response body contains: current payment state (all fields including status, amount, merchant, timestamps)
- Database: Single indexed SELECT query (fast)
- If not found: Status 404 Not Found with error message

---

### 3. Confirm Payment: POST /api/v1/payments/{paymentId}/confirm

**Request:**
```
POST /api/v1/payments/550e8400-e29b-41d4-a716-446655440000/confirm
```

**Step-by-Step Flow:**

1. **Client sends HTTP POST request** → Spring extracts paymentId from URL path
2. **PaymentController.confirmPayment()** → Path variable passed to service layer
3. **PaymentService.confirmPayment()** → Service method marked as transactional (atomic operation)
4. **PaymentRepository.findByPaymentId()** → Look up payment by ID
5. **Status validation** → Service checks payment status is PENDING (required for confirmation)
   - If not PENDING: InvalidPaymentException thrown
6. **Status transition: PENDING → PROCESSING** → Service updates status field
7. **PaymentRepository.save()** → First save to database with PROCESSING status (UPDATE query)
8. **Status transition: PROCESSING → COMPLETED** → Service updates status field
9. **Timestamp update** → CompletedAt field set to current timestamp
10. **PaymentRepository.save()** → Second save to database with COMPLETED status (UPDATE query)
11. **Transaction committed** → Both updates committed atomically to PostgreSQL
12. **PaymentResponse mapping** → Updated Payment entity converted to response DTO
13. **HTTP 200 OK response** → Client receives updated payment with COMPLETED status

**Expected Result:**
- Status: 200 OK
- Response body contains: payment with status = COMPLETED, completedAt timestamp populated
- Database: Two atomic UPDATE queries; payment.status changed from PENDING to PROCESSING to COMPLETED
- If payment not PENDING: Status 400 Bad Request with "Only PENDING payments can be confirmed"
- Transaction ensures atomicity: both updates succeed or both fail

---

### 4. Refund Payment: POST /api/v1/payments/{paymentId}/refund

**Request:**
```
POST /api/v1/payments/550e8400-e29b-41d4-a716-446655440000/refund
```

**Step-by-Step Flow:**

1. **Client sends HTTP POST request** → Spring extracts paymentId from URL path
2. **PaymentController.refundPayment()** → Path variable passed to service layer
3. **PaymentService.refundPayment()** → Service method marked as transactional
4. **PaymentRepository.findByPaymentId()** → Look up payment by ID
5. **Status validation** → Service checks payment status is COMPLETED (required for refund)
   - If not COMPLETED: InvalidPaymentException thrown
6. **Status transition: COMPLETED → REFUNDED** → Service updates status field
7. **PaymentRepository.save()** → Save to database with REFUNDED status (UPDATE query)
8. **Transaction committed** → Update committed to PostgreSQL
9. **PaymentResponse mapping** → Updated Payment entity converted to response DTO
10. **HTTP 200 OK response** → Client receives updated payment with REFUNDED status

**Expected Result:**
- Status: 200 OK
- Response body contains: payment with status = REFUNDED
- Database: Single atomic UPDATE query; payment.status changed from COMPLETED to REFUNDED
- If payment not COMPLETED: Status 400 Bad Request with "Only COMPLETED payments can be refunded"
- Terminal state: REFUNDED status cannot be changed further

---

## Error Handling Across All Endpoints

**GlobalExceptionHandler** catches all exceptions and returns structured responses:

```json
{
  "timestamp": "2026-07-01T12:34:56.789Z",
  "status": 400,
  "error": "InvalidPaymentException",
  "message": "Only PENDING payments can be confirmed"
}
```

**Common Error Scenarios:**

1. **Validation Errors** (400)
   - Missing required fields
   - Negative amount
   - Empty userId or merchant

2. **Not Found Errors** (404)
   - paymentId does not exist in database

3. **Business Rule Violations** (400)
   - Trying to confirm non-PENDING payment
   - Trying to refund non-COMPLETED payment

4. **System Errors** (500)
   - Database connection failures
   - Kafka producer failures (logged but request completes)

---

## Testing

```bash
# Run all tests
mvn test

# Run with coverage report
mvn clean test jacoco:report

# View coverage report (opens in browser)
open target/site/jacoco/index.html
```

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

### Architecture Diagrams

#### System Architecture

```
CLIENT (REST API)
       |
       v
┌──────────────────────────────────────┐
│   PAYMENT CORE (Spring Boot 3.5)     │
│  ┌────────────────────────────────┐  │
│  │ REST Controller Layer          │  │
│  │ /api/v1/payments               │  │
│  │ - POST (Create)                │  │
│  │ - GET (Retrieve)               │  │
│  │ - POST confirm (Confirm)       │  │
│  │ - POST refund (Refund)         │  │
│  └────────────────────────────────┘  │
│              |                        │
│              v                        │
│  ┌────────────────────────────────┐  │
│  │ Service Layer                  │  │
│  │ - PaymentService               │  │
│  │ - PaymentValidator             │  │
│  │ - Event Publishing             │  │
│  └────────────────────────────────┘  │
│         |                  |          │
│         v                  v          │
│   ┌─────────────┐    ┌────────────┐  │
│   │ Repository  │    │   Kafka    │  │
│   │ (JPA)       │    │  Producer  │  │
│   └─────────────┘    └────────────┘  │
└──────────────────────────────────────┘
       |                     |
       v                     v
   DATABASE            KAFKA BROKER
   (PostgreSQL)        (Message Queue)
       |                     |
       |              ┌──────┼──────┐
       |              |      |      |
       v              v      v      v
   Payments      Charging Notification Analytics
   Table         Consumer  Consumer   Consumer
```

#### Application Architecture (Layered)

```
┌─────────────────────────────────────┐
│   REST API Layer                    │
│   PaymentController                 │
│   - Endpoints: /api/v1/payments     │
└────────────────┬────────────────────┘
                 |
┌────────────────v────────────────────┐
│   Service Layer                     │
│   - PaymentService (Business Logic) │
│   - PaymentValidator (Validation)   │
│   - PaymentProducer (Event Pub)     │
└────┬──────────────────────┬─────────┘
     |                      |
┌────v──────────┐   ┌──────v──────────┐
│ Repository    │   │ Kafka Producer  │
│ Layer         │   │ - Publishes:    │
│ - JPA         │   │   * Initiated   │
│ - Queries     │   │   * Charged     │
└────┬──────────┘   │   * Completed   │
     |              │   * Failed      │
┌────v──────────┐   └──────┬──────────┘
│ Data Access   │          |
│ PostgreSQL    │   ┌──────v──────────┐
│ - Payments    │   │ Kafka Topics    │
│ - Indexes     │   │ - 4 Topics      │
└───────────────┘   │ - 3 Partitions  │
                    │ - 3 Consumers   │
                    └─────────────────┘
```

#### Component Diagram

```
┌─────────────────────────────────────────────────┐
│         PAYMENT CORE COMPONENTS                 │
├─────────────────────────────────────────────────┤
│                                                 │
│  PaymentController                              │
│  ├─ createPayment()                             │
│  ├─ getPayment()                                │
│  ├─ confirmPayment()                            │
│  └─ refundPayment()                             │
│         |                                       │
│         v                                       │
│  PaymentService                                 │
│  ├─ Business Logic                              │
│  ├─ Transactions                                │
│  └─ Event Publishing                            │
│         |                                       │
│  ┌──────┴──────┬──────────┐                    │
│  |             |          |                    │
│  v             v          v                    │
│  PaymentValidator  PaymentRepository  Kafka    │
│  - Validation      - JPA Queries     Producer  │
│  - Business Rules  - Database Access - Events  │
│                                                 │
│  Models/Contracts:                              │
│  - Payment (Entity)                             │
│  - PaymentStatus (Enum)                         │
│  - CreatePaymentRequest (Record)                │
│  - PaymentResponse (Record)                     │
│                                                 │
│  Kafka Events:                                  │
│  - PaymentInitiatedEvent                        │
│  - PaymentChargedEvent                          │
│  - PaymentCompletedEvent                        │
│  - PaymentFailedEvent                           │
│                                                 │
│  Kafka Consumers:                               │
│  - ChargingConsumer                             │
│  - NotificationConsumer                         │
│  - AnalyticsConsumer                            │
└─────────────────────────────────────────────────┘
```

#### Deployment Diagram

```
┌─────────────────────────────────────────────┐
│      DOCKER COMPOSE ENVIRONMENT             │
├─────────────────────────────────────────────┤
│                                             │
│  Host (Local Development)                   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ payment-core Container              │   │
│  │ Image: payment-core:latest          │   │
│  │ Port: 8080                          │   │
│  │ Java 21, Spring Boot 3.5.0          │   │
│  │ REST API: /api/v1/payments          │   │
│  │ Health: /health, /info              │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ postgres Container                  │   │
│  │ Image: postgres:15-alpine           │   │
│  │ Port: 5432                          │   │
│  │ Database: payment_db                │   │
│  │ Seed: 1000 payment records          │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ kafka Container                     │   │
│  │ Image: confluentinc/cp-kafka        │   │
│  │ Port: 9092                          │   │
│  │ Topics: 4                           │   │
│  │ Partitions: 3, Replicas: 1         │   │
│  │ Groups: charging, notification,     │   │
│  │         analytics                   │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ zookeeper Container                 │   │
│  │ Image: confluentinc/cp-zookeeper    │   │
│  │ Port: 2181                          │   │
│  │ Manages: Kafka coordination          │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  Docker Network: payment-network            │
└─────────────────────────────────────────────┘

Data Flow:
  API Request → Spring Boot App → PostgreSQL
  Spring Boot → Kafka Producer → Kafka Broker
  Kafka → Consumers (Charging, Notification, Analytics)
```

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
└── application.yml                      # Spring Boot configuration (YAML format)

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
- **PostgreSQL Driver**: Included in dependencies for runtime
- **Connection Pool**: HikariCP (default connection pool in Spring Boot)

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
5. Update integration tests to verify the schema changes

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
