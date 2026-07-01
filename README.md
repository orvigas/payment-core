# Payment Core System

[![Build Status](https://github.com/orvigas/payment-core/actions/workflows/ci.yml/badge.svg)](https://github.com/orvigas/payment-core/actions)
[![Code Coverage](https://codecov.io/gh/orvigas/payment-core/branch/main/graph/badge.svg)](https://codecov.io/gh/orvigas/payment-core)

A scalable, production-ready payment processing system built with Spring Boot 3, PostgreSQL, and Kafka.

## Features

- ✅ REST API for payment operations
- ✅ PostgreSQL database with proper indexing
- ✅ Comprehensive unit and integration testing
- ✅ Docker & Kubernetes ready
- ✅ CI/CD pipeline with GitHub Actions
- ✅ Error handling and validation
- ✅ Structured logging

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.1.0
- **Database:** PostgreSQL 15
- **Build:** Maven
- **Testing:** JUnit 5, Mockito, Testcontainers
- **Containerization:** Docker
- **Orchestration:** Kubernetes (planned for Month 3)

## Quick Start

### Prerequisites

- Java 17+
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
