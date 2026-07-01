# Payment Core API - Insomnia Collection

A complete REST API testing suite for the Payment Core application using Insomnia.

## Overview

This Insomnia collection provides a comprehensive set of requests to test all endpoints of the Payment Core API. It includes:

- **Dynamic environments** for local, Docker, staging, and production
- **Pre-configured requests** for all payment operations
- **Automated tests** with assertions for each request
- **Environment variables** for easy configuration switching
- **Test coverage** for success and error scenarios

## Installation

### Import into Insomnia

1. **Open Insomnia** application
2. Click **Import** (top menu bar)
3. Select **From File**
4. Navigate to and select `Insomnia-Payment-Core.json`
5. Click **Open** to import the collection

The collection will be imported with:
- All request folders and organization
- Complete request configurations
- Built-in test scripts
- All environment configurations

## Environments

### 1. Local Development
**Base URL:** `http://localhost:8080`

Use this environment when running the payment core locally with Maven:
```bash
mvn spring-boot:run
```

### 2. Docker Compose
**Base URL:** `http://localhost:8080`

Use this environment when running with Docker Compose:
```bash
docker-compose up -d
```

### 3. Staging
**Base URL:** `https://staging.payment-core.example.com`

For testing against staging environment (update URL as needed)

### 4. Production
**Base URL:** `https://api.payment-core.example.com`

For production testing (use with caution!)

## Collection Structure

```
Payment Core API
├── Health & System
│   ├── Health Check (GET /health)
│   └── Application Info (GET /info)
└── Payments
    ├── Create Payment
    │   ├── Success (POST /api/v1/payments)
    │   ├── USD Currency (POST /api/v1/payments)
    │   └── Invalid Amount (POST /api/v1/payments)
    ├── Retrieve Payment
    │   ├── Success (GET /api/v1/payments/{id})
    │   └── Not Found (GET /api/v1/payments/{id})
    └── Payment Operations
        ├── Confirm Payment (POST /api/v1/payments/{id}/confirm)
        ├── Confirm Not Found (POST /api/v1/payments/{id}/confirm)
        ├── Refund Payment (POST /api/v1/payments/{id}/refund)
        └── Refund Not Found (POST /api/v1/payments/{id}/refund)
```

## API Endpoints

### Health & Monitoring

#### Health Check
- **Method:** GET
- **URL:** `/health`
- **Description:** Check application health status
- **Expected Response:** 200 OK with health status

#### Application Info
- **Method:** GET
- **URL:** `/info`
- **Description:** Get application information and metadata
- **Expected Response:** 200 OK with app info

### Payment Operations

#### Create Payment
- **Method:** POST
- **URL:** `/api/v1/payments`
- **Request Body:**
  ```json
  {
    "userId": "user_123",
    "amount": 5000.00,
    "currency": "MXN",
    "merchant": "jersey-mikes",
    "description": "Payment description"
  }
  ```
- **Success Response:** 201 Created
- **Error Response:** 400 Bad Request (invalid amount, missing fields)

**Example Requests Included:**
- Success case with MXN currency
- USD currency variant
- Invalid request with negative amount

#### Get Payment
- **Method:** GET
- **URL:** `/api/v1/payments/{paymentId}`
- **Path Variables:** `{{ payment_id }}` (automatically set after create)
- **Success Response:** 200 OK with payment details
- **Error Response:** 404 Not Found

#### Confirm Payment
- **Method:** POST
- **URL:** `/api/v1/payments/{paymentId}/confirm`
- **Path Variables:** `{{ payment_id }}`
- **Success Response:** 200 OK with confirmation
- **Error Response:** 404 Not Found

#### Refund Payment
- **Method:** POST
- **URL:** `/api/v1/payments/{paymentId}/refund`
- **Path Variables:** `{{ payment_id }}`
- **Success Response:** 200 OK with refund details
- **Error Response:** 404 Not Found

## Using Environment Variables

### Dynamic Variables

The collection uses environment variables for dynamic request configuration:

- **`{{ base_url }}`** - Base URL of the API (changes per environment)
- **`{{ payment_id }}`** - Payment ID (automatically populated after create request)

### Switching Environments

To switch between environments:

1. Click the **Environment Selector** (dropdown) at the top
2. Select the desired environment (Local, Docker, Staging, Production)
3. All requests will use the selected environment's variables

### Setting Custom Values

To override environment variables:

1. Click the **Environment Selector**
2. Click **Edit** next to the active environment
3. Modify the variable values as needed
4. Click **Done**

## Test Scripts

Each request includes automated tests that run after the response is received. Tests verify:

- **Status codes** (200, 201, 400, 404, etc.)
- **Response structure** (required fields present)
- **Data validation** (values match expectations)
- **Response times** (acceptable performance)
- **Dynamic variable capture** (auto-save payment_id from create)

### View Test Results

1. Send a request
2. Click the **Tests** tab in the response view
3. Review test results (pass/fail)

## Testing Workflow

### Happy Path Testing

Follow this workflow to test a complete payment lifecycle:

1. **Create Payment** → Get payment_id from response
2. **Get Payment** → Verify payment was created
3. **Confirm Payment** → Transition to confirmed state
4. **Get Payment** → Verify confirmed status
5. **Refund Payment** → Initiate refund
6. **Get Payment** → Verify refund status

### Error Scenario Testing

Test error handling:

1. **Create Payment - Invalid** → Test validation
2. **Get Payment - Not Found** → Test missing payment
3. **Confirm Not Found** → Test operation on missing payment
4. **Refund Not Found** → Test refund on missing payment

## Running All Tests

To run all tests in the collection:

1. Click the **Play** button (►) next to the collection name
2. Select the environment
3. Click **Run** to execute all requests sequentially
4. View results in the test report

## Environment Configuration

### Adding a New Environment

1. Click **Environment Selector** at top
2. Click **Manage Environments**
3. Click **Create** to add new environment
4. Set environment name and base URL
5. Configure `payment_id` variable
6. Click **Done**

### Modifying Base URL

Each environment contains a `base_url` variable. To use a different server:

1. Select the environment
2. Click **Edit**
3. Update `base_url` to your server
4. Click **Done**

## Authentication (Future)

When authentication is required:

1. Add Bearer token to environment variables:
   ```
   auth_token = "your-token-here"
   ```

2. Add Authorization header to requests:
   ```
   Authorization: Bearer {{ auth_token }}
   ```

## Tips & Tricks

### Auto-capture Payment ID

The "Create Payment - Success" request automatically captures the payment ID from the response and saves it to the `payment_id` environment variable. This enables subsequent requests to use:

```
{{ payment_id }}
```

### Reset Payment ID

To reset the payment ID between test runs:

1. Click **Environment Selector**
2. Click **Edit** on the active environment
3. Clear the `payment_id` value
4. Click **Done**

### Organize Requests

Group related requests by:
- Operation type (Create, Read, Update, Delete)
- Entity (Payments, Users, etc.)
- Status codes (Success, Errors)

### Add Custom Requests

To add new requests:

1. Right-click a folder
2. Select **New Request**
3. Configure method, URL, headers, body
4. Add test scripts as needed
5. Save with Ctrl+S (Mac: Cmd+S)

## Performance Baseline

Expected response times (local development):

- GET requests: < 200ms
- POST requests: < 500ms
- Complex operations: < 2000ms

The collection includes performance assertions that fail if these thresholds are exceeded.

## Troubleshooting

### "Cannot GET /api/v1/payments"

**Issue:** Application not running
**Solution:** Start the application:
```bash
mvn spring-boot:run
# or
docker-compose up
```

### 404 Payment Not Found

**Issue:** Payment ID doesn't exist in database
**Solution:** 
- Create a new payment first
- Ensure you're using the correct environment
- Check if the database is properly initialized

### CORS or Network Errors

**Issue:** Cannot connect to API
**Solution:**
- Verify base URL in environment
- Check application is running on correct port
- Disable VPN if using one
- Check firewall settings

### Tests Failing

**Issue:** Automated tests show failures
**Solution:**
- Check response structure matches test expectations
- Verify request parameters are valid
- Review test script for recent changes
- Check application logs for errors

## Collection Version

- **Version:** 1.0.0
- **Last Updated:** July 2026
- **Payment Core Version:** 1.0.0
- **Spring Boot:** 3.5.0

## Support

For issues with:
- **API endpoints:** Check application logs
- **Insomnia:** Refer to [Insomnia documentation](https://docs.insomnia.rest/)
- **Collection:** Review this guide or check repository issues

---

**Happy Testing!** 🧪
