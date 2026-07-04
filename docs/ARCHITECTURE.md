# Architecture

Payment Core is a Spring Boot 3.5 / Java 21 payment processing service. It exposes a small REST API, persists state in PostgreSQL, and processes payments asynchronously through Kafka events. This document describes how the pieces fit together and why they are shaped the way they are.

Related documents: [SECURITY.md](SECURITY.md), [DEPLOYMENT.md](DEPLOYMENT.md), [PERFORMANCE.md](PERFORMANCE.md).

## System Overview

```text
Client
  |  HTTPS + JWT
  v
PaymentController / AuthController        (com.payment.controllers)
  |
JwtAuthenticationFilter -> SecurityContext (com.payment.security)
  |
PaymentService + PaymentValidator          (com.payment.services)
  |                        \
PaymentRepository           PaymentProducer -> Kafka topics
  |                                              |
PostgreSQL                        ChargingConsumer / NotificationConsumer / AnalyticsConsumer
```

The synchronous path (create, get, confirm, refund) is a classic layered design: controller, service, repository. The asynchronous path fans payment state changes out to independent consumers over Kafka, so charging, notifications, and analytics can evolve and scale independently of the API.

## Layers

### Controllers (`com.payment.controllers`)

- `PaymentController` serves `/api/v1/payments`: create, get by ID, confirm, refund. Every method takes the authenticated `Authentication` principal and passes its name (the JWT subject) to the service as the payment owner or requester — never a client-supplied value — so ownership can be enforced in one place. Payment creation is guarded by the `payment-creation` rate limiter with a fallback that translates `RequestNotPermitted` into a 429. The fallback deliberately matches only that exception so real failures (for example a database error) still surface as errors instead of a false 429.
- `AuthController` serves `/api/v1/auth`: login and token refresh. Login delegates to Spring Security's `AuthenticationManager` rather than comparing password hashes itself.

Controllers hold no business logic. They validate input via Jakarta Bean Validation, call the service, and map results to HTTP responses. Every endpoint carries OpenAPI annotations; Swagger UI is available at `/swagger-ui.html`.

### Security (`com.payment.security`, `com.payment.config`)

Stateless JWT authentication. `JwtAuthenticationFilter` runs once per request, extracts the Bearer token, validates it through `JwtTokenProvider`, and populates the `SecurityContext`. There are no HTTP sessions and CSRF is disabled, which is safe only because authentication is header-based. See [SECURITY.md](SECURITY.md) for the full flow, endpoint matrix, and known gaps.

### Services (`com.payment.services`)

`PaymentService` owns the payment lifecycle. Each public method is `@Transactional` so a payment row and its side effects commit or roll back together. `PaymentValidator` holds business-rule validation that goes beyond what bean-validation annotations can express.

`createPayment` persists the payment in `PENDING`, records metrics, and publishes a `PaymentInitiatedEvent`. Everything after that point happens asynchronously via Kafka.

### Repositories (`com.payment.repositories`)

Spring Data JPA interfaces (`PaymentRepository`, `UserRepository`). No custom SQL beyond derived queries; the schema itself is owned by Flyway (see below).

### Error handling (`com.payment.errors`)

Services throw domain exceptions (`PaymentNotFoundException`, `InvalidPaymentException`, `InvalidCredentialsException`, `RateLimitExceededException`). `GlobalExceptionHandler` translates them into structured JSON error responses with consistent status codes, so controllers never build error payloads by hand and internal details never leak into responses.

## Data Model

### Payment

| Column | Notes |
|---|---|
| `paymentId` | UUID string, primary key |
| `userId` | Owner; indexed, references `User.userId` |
| `amount`, `currency`, `merchant`, `description` | Payment details |
| `status` | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `REFUNDED` |
| `createdAt`, `updatedAt`, `completedAt` | Timestamps |

Indexes exist on `paymentId` (PK), `userId` (fetch a user's payments), and `status` (filter by state). These match the three real query patterns; nothing else is indexed to keep writes cheap.

### User

Users live in the `users` table (migration V007) with a serial internal `id` and a separate UUID `userId`. The UUID is what appears in JWT subjects and in `Payment.userId`; the serial key never leaves the database, so identifiers exposed to clients are not guessable or enumerable. `User` implements `UserDetails` directly, which avoids an adapter class between JPA and Spring Security. Roles (`USER`, `ADMIN`) are stored in a `user_roles` join table and fetched eagerly because they are needed on every authentication.

### Payment lifecycle

```text
PENDING -> PROCESSING -> COMPLETED -> REFUNDED
                     \-> FAILED
```

- `confirmPayment` only accepts `PENDING` payments; `refundPayment` only accepts `COMPLETED` ones. Invalid transitions throw `InvalidPaymentException` (HTTP 400).
- Every read or transition first checks that the caller owns the payment, throwing `PaymentAccessDeniedException` (HTTP 403) otherwise. See [SECURITY.md](SECURITY.md) for the full ownership model.
- `FAILED` and `REFUNDED` are terminal.
- The charging consumer also drives `PENDING -> PROCESSING -> COMPLETED/FAILED` asynchronously when it processes a `PaymentInitiatedEvent`.

## Event-Driven Processing

### Topics and consumers

| Topic | Producer | Consumers (group) |
|---|---|---|
| `payment-initiated` | `PaymentService` via `PaymentProducer` | `ChargingConsumer` (charging-service), `AnalyticsConsumer` (analytics-service) |
| `payment-charged` | `ChargingConsumer` | `NotificationConsumer` (notification-service), `AnalyticsConsumer` |
| `payment-completed` | `PaymentProducer` | `AnalyticsConsumer` |
| `payment-failed` | `PaymentProducer` | `AnalyticsConsumer` |

Topic and group names are constants in `KafkaTopics` so producers and listeners cannot drift apart. Topics are auto-created with 3 partitions and 1 replica; listener containers run with concurrency 3 to match the partition count.

### Flow for a new payment

1. `POST /api/v1/payments` persists the payment as `PENDING` and publishes `PaymentInitiatedEvent`.
2. `ChargingConsumer` picks it up, marks the payment `PROCESSING`, calls the charge processor (currently simulated with a 90% success rate; a real integration would call Stripe, Adyen, or similar), sets `COMPLETED` or `FAILED`, and publishes `PaymentChargedEvent`.
3. `NotificationConsumer` reacts to the charge result and would send the customer email in a real deployment.
4. `AnalyticsConsumer` observes all events for metrics.

### Serialization

Events are immutable Java Records serialized as JSON (`JsonSerializer` on the producer, typed `JsonDeserializer` factories per event class on the consumer side). Typed consumer factories exist so listeners receive concrete event types instead of `Object` plus manual casting; each new event type gets its own factory in `KafkaConfig`.

## Database Schema Management

Flyway owns the schema; Hibernate runs with `ddl-auto: validate` and only checks that entity mappings match what the migrations created, failing fast on drift instead of silently altering tables. Migrations V001 through V008 live in `src/main/resources/db/migration/` and are never edited after they have run — Flyway tracks checksums, so a modified migration breaks startup. Schema changes always mean a new `Vnnn__description.sql` file.

For local development, `app.database.reset-on-startup` (default `true`) makes Flyway clean and re-migrate on every boot so the local schema can never drift from what is checked in. The operational implications of this flag are covered in [DEPLOYMENT.md](DEPLOYMENT.md).

## Key Design Decisions

- **Records as API contracts and events.** `CreatePaymentRequest`, `PaymentResponse`, `LoginRequest`, `LoginResponse`, and all Kafka events are records. Immutability removes a whole class of aliasing bugs in request handling and event publishing.
- **Two-level validation.** Bean Validation annotations catch structural problems at the controller boundary; `PaymentValidator` holds business rules in the service layer where they can use repository state.
- **Exception translation instead of inline error handling.** One `@RestControllerAdvice` produces every error response, which keeps the error format consistent and keeps stack traces out of client responses.
- **Constructor injection everywhere** (via Lombok `@RequiredArgsConstructor`), which keeps dependencies explicit and classes testable without a Spring context.
- **Resilience4j at the integration edges.** Circuit breakers, retries, and time limiters wrap the charger, notifications, Kafka publishing, and database calls. Thresholds and the reasoning behind them are documented in [PERFORMANCE.md](PERFORMANCE.md).

## Observability

`CustomMetrics` registers Micrometer counters (`payment.created`, `payment.completed`, `payment.failed`, `charge.success`, `charge.failure`), timers with P50/P95/P99 percentiles (`payment.processing.duration`, `charge.processing.duration`), and gauges (`payment.active`, `circuitbreaker.open`). Metrics are exported at `/actuator/prometheus`; traces are shipped over the Zipkin protocol to `otel-collector`, which forwards them to Jaeger unchanged while also deriving Service Performance Monitor metrics from them (see [DEPLOYMENT.md](DEPLOYMENT.md#service-performance-monitor)). New operations are expected to add their own counters and timers in `CustomMetrics` rather than logging numbers.

A single trace now spans the whole payment flow, HTTP request through every Kafka hop: `KafkaTemplate` and the listener container factory in `KafkaConfig` both have observation explicitly enabled and bound to the app's `ObservationRegistry`, since they are built as manual `@Bean`s rather than through Spring Boot's Kafka autoconfiguration, which is what normally wires tracing in without any extra code. `%X{traceId}`/`%X{spanId}` in `logging.pattern.console` make the same correlation visible directly in log output, including inside `@KafkaListener` methods.
