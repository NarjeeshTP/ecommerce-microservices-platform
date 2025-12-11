# Notification Service

Event-driven notification service that sends emails and SMS based on order and payment events.

## ✅ Current Status (Dec 11, 2025)

**Version:** 0.0.1-SNAPSHOT  
**Port:** 8087  
**Status:** Week 9 Implementation Ready

### Features Implemented
- ✅ **Event-Driven** - Kafka consumers for OrderConfirmed, PaymentCompleted
- ✅ **Email Notifications** - Thymeleaf templates, SMTP integration
- ✅ **SMS Notifications** - Twilio integration (mock mode)
- ✅ **Mock Mode** - Test without real email/SMS providers
- ✅ **Notification History** - Track all sent notifications
- ✅ **Retry Logic** - Automatic retry on failures
- ✅ **Template Engine** - Dynamic email templates
- ✅ **Database Persistence** - PostgreSQL with Flyway migrations

---

## Features

### Core Functionality
- ✅ **Order Confirmed Email** - Sent when order is created
- ✅ **Payment Success Email** - Sent when payment completes
- ✅ **Payment Failed Email** - Sent when payment fails
- ✅ **Order Shipped SMS** - Sent when order ships (future)
- ✅ **Notification History** - Query sent notifications
- ✅ **Resend Notifications** - Retry failed notifications

### Technical Features
- ✅ **Kafka Consumers** - Listen to order-events, payment-events
- ✅ **SMTP Integration** - Gmail, SendGrid, AWS SES compatible
- ✅ **Twilio SMS** - SMS via Twilio API
- ✅ **Mock Mode** - Test without external services
- ✅ **Template Engine** - Thymeleaf for HTML emails
- ✅ **Retry Mechanism** - Exponential backoff (3 attempts)
- ✅ **Idempotency** - Prevent duplicate notifications

---

## Quick Start

### Prerequisites
- Docker Desktop running
- Java 17+
- Maven 3.6+
- PostgreSQL (port 5436)
- Kafka (port 9092)

### 1. Start PostgreSQL

```bash
docker run -d --name notificationdb \
  -e POSTGRES_DB=notificationdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5436:5432 \
  postgres:15
```

### 2. Build & Run

```bash
cd services/notification-service
mvn clean package -Dmaven.test.skip=true
java -jar target/notification-service-0.0.1-SNAPSHOT.jar
```

### 3. Verify

```bash
curl http://localhost:8087/actuator/health | jq
# Expected: {"status":"UP"}
```

---

## Event-Driven Architecture

### Flow Diagram

```
┌─────────────┐
│ Order Service│
└──────┬──────┘
       │
       │ 1. OrderConfirmed event
       │
       ↓
┌──────────────────────────────────┐
│  Kafka (order-events topic)      │
└──────┬───────────────────────────┘
       │
       │ 2. Event consumed
       │
       ↓
┌──────────────────────────────────┐
│  Notification Service            │
│  - OrderEventListener            │
└──────┬───────────────────────────┘
       │
       │ 3. Send notification
       │
       ↓
┌──────────────────────────────────┐
│  Email Provider (SMTP)           │
│  - Gmail, SendGrid, SES          │
│                                  │
│  OR                              │
│                                  │
│  SMS Provider (Twilio)           │
└──────────────────────────────────┘

       ↓
┌──────────────────────────────────┐
│  User receives notification      │
└──────────────────────────────────┘
```

### Event Examples

#### OrderConfirmed Event

```json
{
  "eventType": "OrderConfirmed",
  "orderId": "order-123",
  "orderNumber": "ORD-20251211-001",
  "userId": "user-456",
  "userEmail": "customer@example.com",
  "userPhone": "+1234567890",
  "totalAmount": 299.99,
  "currency": "USD",
  "items": [
    {
      "productId": "LAPTOP-001",
      "productName": "MacBook Pro",
      "quantity": 1,
      "price": 299.99
    }
  ],
  "timestamp": "2025-12-11T10:00:00Z"
}
```

#### PaymentCompleted Event

```json
{
  "eventType": "PaymentCompleted",
  "paymentId": "pay-789",
  "orderId": "order-123",
  "orderNumber": "ORD-20251211-001",
  "userId": "user-456",
  "userEmail": "customer@example.com",
  "amount": 299.99,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD",
  "timestamp": "2025-12-11T10:05:00Z"
}
```

---

## Notification Types

### 1. Order Confirmed Email

**Trigger:** OrderConfirmed event from Order Service  
**Template:** `order-confirmed.html`  
**Subject:** "Order Confirmed - {orderNumber}"

**Content:**
- Order number
- Order date
- Item list
- Total amount
- Estimated delivery
- Track order link

**Example:**
```html
Hi John,

Your order #ORD-20251211-001 has been confirmed!

Order Details:
- MacBook Pro x1 - $299.99

Total: $299.99

Estimated Delivery: Dec 15, 2025

Track your order: https://ecommerce.com/orders/order-123

Thanks for shopping with us!
```

### 2. Payment Completed Email

**Trigger:** PaymentCompleted event from Payment Service  
**Template:** `payment-completed.html`  
**Subject:** "Payment Received - {orderNumber}"

**Content:**
- Payment confirmation number
- Amount paid
- Payment method
- Invoice link

**Example:**
```html
Hi John,

We've received your payment!

Payment Details:
- Order: #ORD-20251211-001
- Amount: $299.99
- Method: Visa ending in 4242

Download Invoice: https://ecommerce.com/invoices/inv-123

Your order will be shipped soon!
```

### 3. Payment Failed Email

**Trigger:** PaymentFailed event from Payment Service  
**Template:** `payment-failed.html`  
**Subject:** "Payment Failed - {orderNumber}"

**Content:**
- Failure reason
- Retry payment link
- Support contact

### 4. Order Shipped SMS (Future)

**Trigger:** OrderShipped event from Inventory Service  
**Content:** "Your order #ORD-20251211-001 has shipped! Track: https://ecommerce.com/track/xyz"

---

## API Endpoints

### 1. Get Notification History

```bash
GET /api/notifications/user/{userId}

Response: 200 OK
[
  {
    "id": "notif-123",
    "userId": "user-456",
    "type": "ORDER_CONFIRMED",
    "channel": "EMAIL",
    "recipient": "customer@example.com",
    "status": "SENT",
    "sentAt": "2025-12-11T10:00:05Z",
    "subject": "Order Confirmed - ORD-20251211-001"
  },
  {
    "id": "notif-124",
    "userId": "user-456",
    "type": "PAYMENT_COMPLETED",
    "channel": "EMAIL",
    "recipient": "customer@example.com",
    "status": "SENT",
    "sentAt": "2025-12-11T10:05:10Z",
    "subject": "Payment Received - ORD-20251211-001"
  }
]
```

### 2. Get Notification by ID

```bash
GET /api/notifications/{notificationId}

Response: 200 OK
{
  "id": "notif-123",
  "userId": "user-456",
  "type": "ORDER_CONFIRMED",
  "channel": "EMAIL",
  "recipient": "customer@example.com",
  "status": "SENT",
  "sentAt": "2025-12-11T10:00:05Z",
  "subject": "Order Confirmed - ORD-20251211-001",
  "retryCount": 0,
  "error": null
}
```

### 3. Resend Failed Notification

```bash
POST /api/notifications/{notificationId}/resend

Response: 200 OK
{
  "id": "notif-125",
  "status": "SENT",
  "sentAt": "2025-12-11T10:15:00Z"
}
```

### 4. Send Manual Notification

```bash
POST /api/notifications/send
Headers:
  Content-Type: application/json

Body:
{
  "userId": "user-456",
  "email": "customer@example.com",
  "type": "ORDER_CONFIRMED",
  "data": {
    "orderNumber": "ORD-20251211-001",
    "totalAmount": 299.99
  }
}

Response: 201 Created
{
  "id": "notif-126",
  "status": "SENT"
}
```

---

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5436
DB_NAME=notificationdb
DB_USER=postgres
DB_PASSWORD=postgres

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Email (Gmail example)
EMAIL_ENABLED=false  # Set true for real emails
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
EMAIL_FROM=noreply@ecommerce.com

# SMS (Twilio)
SMS_ENABLED=false  # Set true for real SMS
SMS_PROVIDER=twilio
TWILIO_ACCOUNT_SID=your-account-sid
TWILIO_AUTH_TOKEN=your-auth-token
TWILIO_FROM_NUMBER=+1234567890

# Server
SERVER_PORT=8087
```

### Mock Mode (Default)

```yaml
notification:
  email:
    enabled: false
    mock: true  # Logs email instead of sending
  sms:
    enabled: false
    mock: true  # Logs SMS instead of sending
```

**Mock Output:**
```
2025-12-11 10:00:05 [kafka-listener] INFO  EmailService - 
📧 MOCK EMAIL SENT
To: customer@example.com
Subject: Order Confirmed - ORD-20251211-001
Body: Hi John, Your order has been confirmed...
```

---

## Email Templates

### Template Structure

```
resources/
└── templates/
    ├── order-confirmed.html
    ├── payment-completed.html
    ├── payment-failed.html
    └── order-shipped.html
```

### Example: order-confirmed.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Order Confirmed</title>
</head>
<body>
    <h1>Hi <span th:text="${customerName}">Customer</span>,</h1>
    
    <p>Your order <strong th:text="${orderNumber}">ORD-123</strong> has been confirmed!</p>
    
    <h2>Order Details:</h2>
    <table>
        <tr th:each="item : ${items}">
            <td th:text="${item.productName}">Product</td>
            <td th:text="${item.quantity}">1</td>
            <td th:text="${item.price}">$99.99</td>
        </tr>
    </table>
    
    <p><strong>Total:</strong> $<span th:text="${totalAmount}">299.99</span></p>
    
    <p>Estimated Delivery: <span th:text="${estimatedDelivery}">Dec 15, 2025</span></p>
    
    <a th:href="@{https://ecommerce.com/orders/{orderId}(orderId=${orderId})}">
        Track Your Order
    </a>
    
    <p>Thanks for shopping with us!</p>
</body>
</html>
```

---

## Database Schema

### notifications

```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    order_id UUID,
    type VARCHAR(50) NOT NULL,  -- ORDER_CONFIRMED, PAYMENT_COMPLETED, etc.
    channel VARCHAR(20) NOT NULL,  -- EMAIL, SMS
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    body TEXT,
    status VARCHAR(50) NOT NULL,  -- PENDING, SENT, FAILED
    retry_count INT DEFAULT 0,
    error_message TEXT,
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_order ON notifications(order_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);
```

---

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn test -Dtest=NotificationIntegrationTest
```

### Manual Testing

#### 1. Test Mock Email

```bash
# Publish OrderConfirmed event to Kafka
kafka-console-producer --broker-list localhost:9092 --topic order-events

# Paste JSON:
{
  "eventType": "OrderConfirmed",
  "orderId": "order-123",
  "orderNumber": "ORD-TEST-001",
  "userId": "user-456",
  "userEmail": "test@example.com",
  "totalAmount": 99.99
}

# Check logs for mock email
tail -f logs/notification-service.log | grep "MOCK EMAIL SENT"
```

#### 2. Test Real Email (Gmail)

```bash
# Set environment variables
export EMAIL_ENABLED=true
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password  # Generate from Google Account settings

# Restart service
java -jar target/notification-service-0.0.1-SNAPSHOT.jar

# Publish event (same as above)
```

#### 3. Check Notification History

```bash
curl http://localhost:8087/api/notifications/user/user-456 | jq
```

---

## Troubleshooting

### Email Not Sending

```bash
# Check mail configuration
curl http://localhost:8087/actuator/health | jq '.components.mail'

# Enable mail debug logging
logging:
  level:
    org.springframework.mail: DEBUG
```

**Common Issues:**
- Gmail: Enable "Less secure app access" or use App Password
- Port blocked: Try port 465 (SSL) instead of 587 (TLS)
- Authentication failed: Check username/password

### Kafka Consumer Not Receiving Events

```bash
# Check consumer group
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group notification-service \
  --describe

# Check topic has events
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning
```

### Database Connection Failed

```bash
docker ps | grep notificationdb
docker start notificationdb
```

---

## Week 9 Learning Summary

### 1. Event-Driven Notifications

**What:** Asynchronous notifications triggered by events  
**Why:** Decoupling - Order Service doesn't need to know about notifications  
**How:** Kafka events → Consumer → Send notification

### 2. Multi-Channel Notifications

**Email:**
- SMTP protocol
- HTML templates (Thymeleaf)
- Attachments (invoices, receipts)

**SMS:**
- Twilio API
- Short messages (160 chars)
- Delivery confirmations

### 3. Mock Mode Pattern

**Development:**
```java
if (emailConfig.isMock()) {
    log.info("📧 MOCK EMAIL: {}", email);
} else {
    mailSender.send(email);
}
```

**Benefits:**
- Test without real services
- No costs during development
- Faster testing

### 4. Retry Logic

**Problem:** Email server temporarily down

**Solution:**
```java
@Retryable(
    value = {MailSendException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public void sendEmail(Email email) {
    mailSender.send(email);
}
```

**Retry Behavior:**
```
Attempt 1: Fails → Wait 1s
Attempt 2: Fails → Wait 2s
Attempt 3: Success ✅
```

### 5. Idempotency

**Problem:** Kafka retries → Duplicate emails

**Solution:**
```java
// Check if already sent
Notification existing = repo.findByOrderIdAndType(orderId, type);
if (existing != null && existing.getStatus() == SENT) {
    log.info("Notification already sent: {}", orderId);
    return;
}

// Send and save
sendEmail(email);
repo.save(notification);
```

---

## Project Structure

```
notification-service/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/notification/
│   │   │   ├── NotificationServiceApplication.java
│   │   │   ├── controller/
│   │   │   │   └── NotificationController.java
│   │   │   ├── service/
│   │   │   │   ├── NotificationService.java
│   │   │   │   ├── EmailService.java
│   │   │   │   └── SmsService.java
│   │   │   ├── listener/
│   │   │   │   ├── OrderEventListener.java
│   │   │   │   └── PaymentEventListener.java
│   │   │   ├── repository/
│   │   │   │   └── NotificationRepository.java
│   │   │   ├── entity/
│   │   │   │   ├── Notification.java
│   │   │   │   ├── NotificationType.java
│   │   │   │   └── NotificationStatus.java
│   │   │   ├── dto/
│   │   │   │   ├── NotificationResponse.java
│   │   │   │   └── SendNotificationRequest.java
│   │   │   └── config/
│   │   │       ├── KafkaConfig.java
│   │   │       └── MailConfig.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── templates/
│   │       │   ├── order-confirmed.html
│   │       │   ├── payment-completed.html
│   │       │   └── payment-failed.html
│   │       └── db/migration/
│   │           └── V1__create_notifications_table.sql
│   └── test/
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Next Steps (Week 10+)

- [ ] Slack/Discord notifications
- [ ] Push notifications (mobile)
- [ ] Notification preferences (user settings)
- [ ] Unsubscribe links
- [ ] Batch notifications (digest emails)
- [ ] Priority queue (urgent notifications first)
- [ ] A/B testing templates
- [ ] Analytics (open rate, click rate)
- [ ] Multi-language support
- [ ] Rich media (images, videos)

---

## API Documentation

- **Swagger UI**: http://localhost:8087/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8087/api-docs

---

## Build Commands

```bash
# Clean build
mvn clean package

# Skip tests
mvn clean package -Dmaven.test.skip=true

# Run tests
mvn test

# Run locally
mvn spring-boot:run

# Build Docker image
docker build -t notification-service:latest .
```

---

## Dependencies

- Spring Boot 3.1.6
- Spring Kafka
- Spring Mail
- Thymeleaf
- PostgreSQL Driver
- Flyway Migration
- Testcontainers

---

**Last Updated:** December 11, 2025  
**Version:** 0.0.1-SNAPSHOT  
**Status:** Week 9 (Notification) Complete ✅

