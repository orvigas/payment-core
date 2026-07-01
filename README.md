# Payment Core System

[![Build Status](https://github.com/orvigas/payment-core/actions/workflows/ci.yml/badge.svg)](https://github.com/orvigas/payment-core/actions)
[![Code Coverage](https://codecov.io/gh/orvigas/payment-core/branch/main/graph/badge.svg)](https://codecov.io/gh/orvigas/payment-core)

A scalable, production-ready payment processing system built with Spring Boot 3, PostgreSQL, and modern Java features. This system provides RESTful APIs for payment operations with comprehensive error handling, validation, and transactional integrity.

## Features

- ✅ RESTful API for payment operations (Create, Retrieve, Confirm, Refund)
- ✅ Immutable data contracts using Java Records
- ✅ PostgreSQL database with strategic indexing for optimal query performance
- ✅ Comprehensive unit and integration testing with JUnit 5 and Mockito
- ✅ Testcontainers for isolated database testing
- ✅ Docker & Docker Compose support for local development
- ✅ Global exception handling with structured error responses
- ✅ Request validation with Jakarta Bean Validation
- ✅ Transactional consistency with Spring `@Transactional`
- ✅ Structured logging with SLF4J
- ✅ Complete Javadoc documentation for all classes
- ✅ Spring Boot Actuator for health checks and monitoring

## Tech Stack

### Core Framework
- **Language:** Java 23 (latest LTS features: Records, Pattern Matching, Virtual Threads support)
- **Framework:** Spring Boot 3.5.0
- **Spring Version:** 6.2.7
- **Build Tool:** Apache Maven 3.13.0
- **JPA Provider:** Hibernate ORM 6.6.15

### Database & Data Access
- **Database:** PostgreSQL 15 (with Alpine Linux in Docker)
- **Connection Pool:** HikariCP (included in Spring Boot)
- **ORM:** Spring Data JPA with Hibernate
- **Migrations:** Automatic DDL with `spring.jpa.hibernate.ddl-auto`

### Testing & Quality
- **Unit Testing:** JUnit 5 (Jupiter)
- **Mocking:** Mockito 5.2.0 with inline agent support
- **Integration Testing:** Testcontainers for PostgreSQL
- **Code Coverage:** JaCoCo
- **Build Plugin:** Maven Compiler Plugin 3.13.0

### API & Validation
- **REST Framework:** Spring Web MVC
- **Validation Framework:** Jakarta Bean Validation (Jakarta.validation)
- **JSON Processing:** Jackson (included in Spring Boot)
- **Logging:** SLF4J with Logback

### DevOps & Containerization
- **Containerization:** Docker with Dockerfile
- **Container Orchestration:** Docker Compose
- **Health Checks:** Spring Boot Actuator
- **Monitoring:** Actuator endpoints (/health, /info)

## Quick Start

### Prerequisites

- Java 23+
- Maven 3.8+
- Docker & Docker Compose

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

# Run with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Architecture
