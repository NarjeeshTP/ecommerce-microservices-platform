# Payment Service

Payment processing service with simulated payment gateway, redirect flow, and idempotent callback handling.

## ✅ Current Status (Dec 10, 2025)

**Version:** 0.0.1-SNAPSHOT  
**Port:** 8085  
**Status:** Week 7 Implementation Ready

### Features Implemented
- ✅ **Simulated Payment Gateway** - Mock external payment provider
- ✅ **Redirect Flow** - Initiate payment with redirect URL
- ✅ **Callback Endpoint** - Handle gateway callbacks (webhooks)
- ✅ **Idempotent Callbacks** - Prevent duplicate payment processing
- ✅ **Signature Verification** - Validate callback authenticity (stub)
- ✅ **Order Integration** - Update order status on payment success
- ✅ **Payment Tracking** - Full transaction history
- ✅ **Database Persistence** - PostgreSQL with Flyway migrations

---

## Features

### Core Functionality
- ✅ **Initiate Payment** - Create payment session with redirect URL
- ✅ **Process Redirect** - User redirected to simulated gateway page
- ✅ **Handle Callback** - Receive webhook from gateway (async)
- ✅ **Verify Signature** - Validate callback authenticity
- ✅ **Update Order** - Notify Order Service on success/failure
- ✅ **Idempotency** - Safe callback retries
- ✅ **Payment History** - Query transactions by order/user

### Technical Features
- ✅ **Simulated Gateway** - No real payment processor needed
- ✅ **HMAC Signature** - Request signing and verification
- ✅ **Idempotency Keys** - Transaction_id for callbacks
- ✅ **WebClient Integration** - Non-blocking Order Service calls
- ✅ **State Machine** - Payment status transitions
- ✅ **Database Migrations** - Flyway for schema versioning

---

## Quick Start

### Prerequisites
- Docker Desktop running
- Java 17+
- Maven 3.6+
- PostgreSQL (port 5434)

### 1. Start PostgreSQL

```bash
# Start PostgreSQL for Payment Service
docker run -d --name paymentdb \
  -e POSTGRES_DB=paymentdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5434:5432 \
  postgres:15
```

### 2. Build & Run

```bash
# Navigate to payment-service
cd services/payment-service

# Build
mvn clean package -Dmaven.test.skip=true

# Run
java -jar target/payment-service-0.0.1-SNAPSHOT.jar
```

### 3. Verify

```bash
# Health check
curl http://localhost:8085/actuator/health | jq

# Expected: {"status":"UP"}
```

---

## Payment Flow

### Complete Payment Flow Diagram

```
┌─────────┐                                                      ┌──────────┐
│  User   │                                                      │  Order   │
│ (Browser)│                                                     │ Service  │
└────┬────┘                                                      └────┬─────┘
     │                                                                │
     │  1. Create Order                                              │
     │────────────────────────────────────────────────────────────>  │
     │                                                                │
     │  2. Order Created (orderId: 123, amount: $99.99)              │
     │  <────────────────────────────────────────────────────────────│
     │                                                                │
┌────┴────┐                                                      ┌────┴─────┐
│  User   │                                                      │ Payment  │
│         │                                                      │ Service  │
└────┬────┘                                                      └────┬─────┘
     │                                                                │
     │  3. Initiate Payment (orderId: 123)                           │
     │────────────────────────────────────────────────────────────>  │
     │                                                                │
     │                                    4. Create Payment Record    │
     │                                       (status: INITIATED)      │
     │                                                                │
     │  5. Redirect URL                                              │
     │  <────────────────────────────────────────────────────────────│
     │    (http://localhost:8085/api/payments/redirect/abc-123)     │
     │                                                                │
     │  6. User clicks, browser redirects                            │
     │────────────────────────────────────────────────────────────>  │
     │                                                                │
┌────┴────┐                                                      ┌────┴─────┐
│ Simulated│                                                     │ Payment  │
│ Gateway  │                                                     │ Service  │
│   Page   │                                                     └────┬─────┘
└────┬────┘                                                           │
     │                                                                │
     │  7. Show payment form (card details)                          │
     │    [Pay $99.99] [Cancel]                                      │
     │                                                                │
     │  8. User clicks "Pay"                                         │
     │────────────────────────────────────────────────────────────>  │
     │                                                                │
     │                               9. Update Payment (PROCESSING)  │
     │                                                                │
     │  10. Async Callback (webhook)                                 │
     │  <────────────────────────────────────────────────────────────│
     │    POST /callback                                             │
     │    {                                                           │
     │      "transaction_id": "abc-123",                             │
     │      "status": "SUCCESS",                                     │
     │      "signature": "hmac-sha256..."                            │
     │    }                                                           │
     │                                                                │
     │                               11. Verify Signature ✓           │
     │                               12. Check Idempotency ✓          │
     │                               13. Update Payment (COMPLETED)   │
     │                                                                │
     │                           14. Notify Order Service             │
     │                               (Payment Successful)             │
     │                                                          ┌────┴─────┐
     │                                                          │  Order   │
     │                                                          │ Service  │
     │                                                          └────┬─────┘
     │                                                               │
     │                           15. Update Order                    │
     │                               (status: PAYMENT_CONFIRMED)     │
     │                                                                │
     │  16. Redirect to success page                                 │
     │  <────────────────────────────────────────────────────────────│
     │    (http://yourapp.com/order/123/success)                    │
     │                                                                │
```

### Flow Steps Explained

**Step 1-2: Order Creation**
- User creates order in Order Service
- Order status: CREATED → PAYMENT_PENDING

**Step 3-5: Payment Initiation**
- Frontend calls Payment Service with orderId
- Payment Service creates payment record (status: INITIATED)
- Returns redirect URL to frontend

**Step 6-8: User Redirect**
- User's browser redirects to payment gateway page
- Simulated page shows payment form
- User enters card details and clicks "Pay"

**Step 9-10: Payment Processing**
- Gateway processes payment (simulated)
- Gateway sends async callback (webhook) to Payment Service

**Step 11-13: Callback Handling**
- Payment Service verifies HMAC signature
- Checks idempotency (transaction_id already processed?)
- Updates payment status (COMPLETED or FAILED)

**Step 14-15: Order Update**
- Payment Service calls Order Service
- Order status: PAYMENT_PENDING → PAYMENT_CONFIRMED

**Step 16: User Redirect**
- User redirected to success/failure page

---

## Payment States

### State Machine

```
INITIATED
   ↓
PROCESSING
   ↓
COMPLETED (terminal) ✅
   or
FAILED (terminal) ❌
   or
CANCELLED (terminal) ⚠️
```

### State Transitions

```java
INITIATED → PROCESSING  // User on gateway page
PROCESSING → COMPLETED  // Payment successful
PROCESSING → FAILED     // Payment declined
PROCESSING → CANCELLED  // User cancelled
INITIATED → EXPIRED     // Session timeout
```

---

## Idempotency

### What It Does
Prevents duplicate payment processing when gateway retries callback.

### How It Works

```bash
# First callback
POST /api/payments/callback
Body: {
  "transaction_id": "txn-123",
  "status": "SUCCESS"
}
Response: 200 OK (payment processed)

# Retry (gateway sends again)
POST /api/payments/callback
Body: {
  "transaction_id": "txn-123",  # Same transaction_id
  "status": "SUCCESS"
}
Response: 200 OK (idempotent, no duplicate charge)
```

### Implementation

```java
// Check if transaction already processed
Payment existing = paymentRepository
    .findByTransactionId(transactionId);

if (existing != null && existing.getStatus() == PaymentStatus.COMPLETED) {
    log.info("Callback already processed: {}", transactionId);
    return existing; // Idempotent response
}

// Process payment...
```

### Benefits
- ✅ Safe to retry callbacks
- ✅ No duplicate charges
- ✅ Network failure resilient
- ✅ Gateway-side retries handled

---

## Signature Verification

### Purpose
Verify callback authenticity (prevent fraud).

### HMAC Signature Generation

```java
// Gateway generates signature
String payload = transactionId + status + amount + timestamp;
String signature = HMAC_SHA256(payload, SECRET_KEY);

// Send to Payment Service
POST /callback
Body: {
  "transaction_id": "txn-123",
  "status": "SUCCESS",
  "amount": 99.99,
  "timestamp": 1702123456,
  "signature": "a3f5d8c..."  # HMAC signature
}
```

### Verification in Payment Service

```java
public boolean verifySignature(CallbackRequest request, String receivedSignature) {
    // Reconstruct payload
    String payload = request.getTransactionId() + 
                     request.getStatus() + 
                     request.getAmount() + 
                     request.getTimestamp();
    
    // Calculate expected signature
    String expectedSignature = generateHMAC(payload, SECRET_KEY);
    
    // Compare (constant-time comparison)
    return MessageDigest.isEqual(
        expectedSignature.getBytes(),
        receivedSignature.getBytes()
    );
}
```

### Security Benefits
- ✅ Prevents callback spoofing
- ✅ Validates gateway authenticity
- ✅ Detects tampered payloads
- ✅ Replay attack protection (timestamp)

---

## API Endpoints

### 1. Initiate Payment

```bash
POST /api/payments/initiate
Headers:
  Content-Type: application/json

Body:
{
  "orderId": "order-uuid",
  "amount": 99.99,
  "currency": "USD",
  "returnUrl": "http://yourapp.com/order/success"
}

Response: 201 Created
{
  "paymentId": "payment-uuid",
  "transactionId": "txn-123",
  "status": "INITIATED",
  "redirectUrl": "http://localhost:8085/api/payments/redirect/payment-uuid",
  "expiresAt": "2025-12-10T10:30:00Z"
}
```

**What happens:**
1. Payment record created (status: INITIATED)
2. Redirect URL generated
3. Frontend redirects user to this URL

### 2. Payment Redirect (User)

```bash
GET /api/payments/redirect/{paymentId}

Response: 200 OK (HTML page)
<!DOCTYPE html>
<html>
<body>
  <h1>Simulated Payment Gateway</h1>
  <p>Amount: $99.99</p>
  <form method="POST" action="/api/payments/process/{paymentId}">
    <input name="cardNumber" placeholder="4111 1111 1111 1111" />
    <input name="cvv" placeholder="123" />
    <button type="submit">Pay Now</button>
    <button type="button" onclick="cancel()">Cancel</button>
  </form>
</body>
</html>
```

**What user sees:**
- Payment form with card input
- Amount to be charged
- Pay/Cancel buttons

### 3. Process Payment (Simulated)

```bash
POST /api/payments/process/{paymentId}
Headers:
  Content-Type: application/x-www-form-urlencoded

Body:
cardNumber=4111111111111111&cvv=123

Response: 302 Redirect
Location: http://yourapp.com/order/success?payment_id=payment-uuid&status=SUCCESS
```

**What happens:**
1. Validate card (simulated - always succeeds if card starts with "4")
2. Update payment status: INITIATED → PROCESSING
3. Trigger async callback (simulated webhook)
4. Redirect user to return URL

### 4. Payment Callback (Gateway → Service)

```bash
POST /api/payments/callback
Headers:
  Content-Type: application/json
  X-Gateway-Signature: hmac-sha256-signature

Body:
{
  "transaction_id": "txn-123",
  "status": "SUCCESS",
  "amount": 99.99,
  "currency": "USD",
  "timestamp": 1702123456,
  "gateway_reference": "gw-ref-789"
}

Response: 200 OK
{
  "received": true,
  "transaction_id": "txn-123",
  "status": "PROCESSED"
}
```

**What happens:**
1. Verify HMAC signature ✓
2. Check idempotency (transaction_id) ✓
3. Update payment: PROCESSING → COMPLETED
4. Call Order Service to update order
5. Return 200 OK (important for gateway retry logic)

### 5. Get Payment Status

```bash
GET /api/payments/{paymentId}

Response: 200 OK
{
  "paymentId": "payment-uuid",
  "orderId": "order-uuid",
  "transactionId": "txn-123",
  "status": "COMPLETED",
  "amount": 99.99,
  "currency": "USD",
  "gatewayReference": "gw-ref-789",
  "createdAt": "2025-12-10T10:00:00Z",
  "completedAt": "2025-12-10T10:05:00Z"
}
```

### 6. Get Order Payments

```bash
GET /api/payments/order/{orderId}

Response: 200 OK
[
  {
    "paymentId": "payment-1",
    "status": "FAILED",
    "amount": 99.99,
    "createdAt": "2025-12-10T09:00:00Z"
  },
  {
    "paymentId": "payment-2",
    "status": "COMPLETED",
    "amount": 99.99,
    "createdAt": "2025-12-10T10:00:00Z"
  }
]
```

---

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5434
DB_NAME=paymentdb
DB_USER=postgres
DB_PASSWORD=postgres

# Order Service
ORDER_SERVICE_URL=http://localhost:8084

# Payment Gateway (Simulated)
GATEWAY_API_KEY=test-api-key-12345
GATEWAY_SECRET_KEY=test-secret-key-67890
GATEWAY_BASE_URL=http://localhost:8085/simulate
GATEWAY_CALLBACK_URL=http://localhost:8085/api/payments/callback
GATEWAY_REDIRECT_URL=http://localhost:8085/api/payments/redirect/{paymentId}

# Payment Settings
PAYMENT_MIN_AMOUNT=1.00
PAYMENT_MAX_AMOUNT=100000.00
PAYMENT_IDEMPOTENCY_TTL_HOURS=24

# Server
SERVER_PORT=8085
```

---

## Project Structure

```
payment-service/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/payment/
│   │   │   ├── PaymentServiceApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── PaymentController.java
│   │   │   │   └── PaymentCallbackController.java
│   │   │   ├── service/
│   │   │   │   ├── PaymentService.java
│   │   │   │   ├── PaymentGatewayService.java (Simulated)
│   │   │   │   └── SignatureVerificationService.java
│   │   │   ├── repository/
│   │   │   │   └── PaymentRepository.java
│   │   │   ├── entity/
│   │   │   │   ├── Payment.java
│   │   │   │   └── PaymentStatus.java
│   │   │   ├── dto/
│   │   │   │   ├── InitiatePaymentRequest.java
│   │   │   │   ├── PaymentResponse.java
│   │   │   │   └── PaymentCallbackRequest.java
│   │   │   ├── client/
│   │   │   │   └── OrderServiceClient.java
│   │   │   ├── exception/
│   │   │   │   ├── PaymentNotFoundException.java
│   │   │   │   └── InvalidSignatureException.java
│   │   │   └── config/
│   │   │       ├── WebClientConfig.java
│   │   │       └── PaymentGatewayConfig.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── templates/
│   │       │   └── payment-gateway.html (Simulated page)
│   │       └── db/migration/
│   │           ├── V1__create_payments_table.sql
│   │           └── V2__add_indexes.sql
│   └── test/
│       └── java/com/ecommerce/payment/
│           ├── service/
│           │   └── PaymentServiceTest.java
│           └── integration/
│               └── PaymentIntegrationTest.java
├── Dockerfile
├── pom.xml
└── README.md (this file)
```

---

## Week 7 Learning Summary

### 1. Payment Gateway Integration
**What:** External payment processor (Stripe, PayPal, etc.)
**Why:** Handle actual money transactions
**Simulated:** No real gateway needed for testing

### 2. Redirect Flow
**What:** User redirected to gateway page → Pays → Redirected back
**Why:** PCI compliance (gateway handles card data)
**Flow:**
```
Your App → Gateway Page → Payment → Your App
```

### 3. Callback (Webhook) Handling
**What:** Gateway sends async notification of payment result
**Why:** User might close browser before redirect
**Critical:** Handle duplicates (idempotency)

### 4. Signature Verification
**What:** HMAC signature validates callback authenticity
**Why:** Prevent fraudulent callbacks
**Method:** HMAC-SHA256 with shared secret

### 5. Idempotent Callbacks
**Problem:**
```java
// Without idempotency
callback1(); // Charge customer $99.99 ✓
callback2(); // Charge customer $99.99 again! ❌ (double charge)
```

**Solution:**
```java
// With idempotency
callback1(); // Charge customer $99.99 ✓
callback2(); // Already processed, skip ✓ (safe)
```

### 6. Order Integration
**What:** Update Order Service on payment success
**Method:** WebClient (non-blocking HTTP call)
**Result:** Order status: PAYMENT_PENDING → PAYMENT_CONFIRMED

---

## Database Schema

### payments
```sql
id UUID PRIMARY KEY
transaction_id VARCHAR(255) UNIQUE  -- Idempotency key
order_id UUID NOT NULL
amount DECIMAL(19,2) NOT NULL
currency VARCHAR(3) DEFAULT 'USD'
status VARCHAR(50) NOT NULL  -- State machine
gateway_reference VARCHAR(255)  -- Gateway's transaction ID
redirect_url TEXT
return_url TEXT
created_at TIMESTAMP
updated_at TIMESTAMP
completed_at TIMESTAMP
failed_at TIMESTAMP
failure_reason TEXT
```

**Indexes:**
- transaction_id (unique, for idempotency)
- order_id (query payments by order)
- status (query pending/completed payments)

---

## Testing

### Unit Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=PaymentServiceTest
```

### Integration Tests

```bash
# Uses Testcontainers (PostgreSQL)
mvn test -Dtest=PaymentIntegrationTest
```

### Manual Testing

#### 1. Complete Payment Flow

```bash
# Step 1: Initiate payment
curl -X POST http://localhost:8085/api/payments/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-123",
    "amount": 99.99,
    "currency": "USD",
    "returnUrl": "http://localhost:3000/success"
  }' | jq

# Response:
{
  "paymentId": "payment-abc",
  "redirectUrl": "http://localhost:8085/api/payments/redirect/payment-abc"
}

# Step 2: Open redirect URL in browser
open "http://localhost:8085/api/payments/redirect/payment-abc"

# Step 3: Fill form and click "Pay"
# (Simulated gateway will process)

# Step 4: Check payment status
curl http://localhost:8085/api/payments/payment-abc | jq

# Expected:
{
  "paymentId": "payment-abc",
  "status": "COMPLETED",
  "amount": 99.99
}
```

#### 2. Simulate Callback (Webhook)

```bash
# Generate signature (in production, gateway does this)
PAYLOAD="txn-123SUCCESScopy99.991702123456"
SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "test-secret-key-67890" | cut -d' ' -f2)

# Send callback
curl -X POST http://localhost:8085/api/payments/callback \
  -H "Content-Type: application/json" \
  -H "X-Gateway-Signature: $SIGNATURE" \
  -d '{
    "transaction_id": "txn-123",
    "status": "SUCCESS",
    "amount": 99.99,
    "currency": "USD",
    "timestamp": 1702123456
  }' | jq

# Response:
{
  "received": true,
  "transaction_id": "txn-123",
  "status": "PROCESSED"
}
```

#### 3. Test Idempotency

```bash
# Send same callback twice
curl -X POST http://localhost:8085/api/payments/callback \
  -H "Content-Type: application/json" \
  -H "X-Gateway-Signature: $SIGNATURE" \
  -d '{
    "transaction_id": "txn-123",
    "status": "SUCCESS",
    "amount": 99.99
  }' | jq

# First call: Processes payment
# Second call: Returns same response (idempotent, no duplicate)
```

---

## Troubleshooting

### Database Connection Failed
```bash
# Check PostgreSQL
docker ps | grep paymentdb

# Start if not running
docker start paymentdb
```

### Signature Verification Failed
```bash
# Error: "Invalid signature"
# Check: SECRET_KEY matches between gateway and service

# Debug: Log payload and signature
# src/main/java/...SignatureVerificationService.java
log.debug("Expected signature: {}", expectedSignature);
log.debug("Received signature: {}", receivedSignature);
```

### Callback Not Received
```bash
# Check callback URL is accessible
curl http://localhost:8085/api/payments/callback

# For production:
# 1. Callback URL must be public (use ngrok for local testing)
# 2. Gateway must be configured with correct URL
# 3. Firewall rules must allow gateway IP
```

### Order Update Failed
```bash
# Check Order Service is running
curl http://localhost:8084/actuator/health

# Check logs
tail -f logs/payment-service.log | grep OrderServiceClient
```

---

## Monitoring

### Actuator Endpoints

```bash
# Health
curl http://localhost:8085/actuator/health

# Metrics
curl http://localhost:8085/actuator/metrics

# Prometheus
curl http://localhost:8085/actuator/prometheus
```

### Database Monitoring

```sql
-- Payments by status
SELECT status, COUNT(*), SUM(amount) 
FROM payments 
GROUP BY status;

-- Failed payments
SELECT * FROM payments 
WHERE status = 'FAILED' 
ORDER BY created_at DESC;

-- Pending payments (potential issues)
SELECT * FROM payments 
WHERE status = 'PROCESSING' 
  AND created_at < NOW() - INTERVAL '1 hour';
```

---

## Security Best Practices

### 1. Signature Verification
```java
✅ Always verify callback signatures
✅ Use constant-time comparison (prevent timing attacks)
✅ Include timestamp in signature (prevent replay)
❌ Never skip verification in production
```

### 2. HTTPS Only
```yaml
✅ Production: HTTPS required for callbacks
✅ Use certificate pinning for gateway calls
❌ Never use HTTP in production
```

### 3. Secrets Management
```bash
✅ Store API keys in environment variables
✅ Use Vault/AWS Secrets Manager in production
❌ Never commit secrets to Git
```

### 4. PCI Compliance
```
✅ Gateway handles card data (not your service)
✅ Never store full card numbers
✅ Only store last 4 digits (if needed)
❌ Never log card data
```

---

## Next Steps (Week 8+)

- [ ] Real payment gateway integration (Stripe/PayPal)
- [ ] Refund processing
- [ ] Partial refunds
- [ ] Payment retries for failed transactions
- [ ] 3D Secure (SCA) support
- [ ] Multiple payment methods (cards, wallets, bank transfer)
- [ ] Fraud detection integration
- [ ] Payment reconciliation reports
- [ ] Webhook retry logic (exponential backoff)
- [ ] Payment analytics dashboard

---

## API Documentation

- **Swagger UI**: http://localhost:8085/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8085/api-docs

---

## Build Commands

```bash
# Clean build
mvn clean package

# Skip tests
mvn clean package -Dmaven.test.skip=true

# Run tests
mvn test

# Coverage report
mvn jacoco:report

# Run locally
mvn spring-boot:run

# Build Docker image
docker build -t payment-service:latest .
```

---

## Dependencies

- Spring Boot 3.1.6
- Spring Data JPA
- Spring WebFlux (WebClient)
- PostgreSQL Driver
- Flyway Migration
- Lombok
- Testcontainers

---

## Comparison: Cart vs Order vs Payment

| Feature | Cart | Order | Payment |
|---------|------|-------|---------|
| **Port** | 8083 | 8084 | 8085 |
| **Database** | cartdb (5432) | orderdb (5433) | paymentdb (5434) |
| **Main Pattern** | Session mgmt | Outbox + State | Redirect + Callback |
| **External Calls** | Catalog, Pricing | Cart, Inventory | Order Service |
| **Idempotency** | Session-based | Key-based | Transaction-based |
| **State Machine** | Simple | 6 states | 5 states |
| **Key Feature** | WebClient | Event publishing | Gateway simulation |

---

**Last Updated:** December 10, 2025  
**Version:** 0.0.1-SNAPSHOT  
**Status:** Week 7 Complete ✅

