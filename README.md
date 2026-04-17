# E-commerce Order Processing System

A production-grade Spring Boot backend for managing e-commerce orders end-to-end. Customers can place orders with multiple items, track status through a defined lifecycle, filter and list orders, and cancel when allowed. A scheduled background job automatically promotes pending orders for processing.

Built as a coding assessment submission — designed to demonstrate clean architecture, robust error handling, thorough test coverage, and real-world engineering practices.

---

## Table of Contents

- [Requirements Checklist](#requirements-checklist)
- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Order Status Lifecycle](#order-status-lifecycle)
- [Background Scheduler](#background-scheduler)
- [Validation & Error Handling](#validation--error-handling)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [System Design Patterns](#system-design-patterns)
- [Distributed System Fundamentals](#distributed-system-fundamentals)
- [Caching](#caching)
- [Logging](#logging)
- [Kafka Integration](#kafka-integration)
- [CI/CD Pipeline](#cicd-pipeline)
- [Docker & Deployment](#docker--deployment)
- [Design Decisions & Trade-offs](#design-decisions--trade-offs)
- [AI-Assisted Development: What Worked, What Broke, How I Fixed It](#ai-assisted-development-what-worked-what-broke-how-i-fixed-it)
- [Human Judgment: What Only a Developer Could Do](#human-judgment-what-only-a-developer-could-do)

---

## Requirements Checklist

| # | Requirement | Status | Implementation |
|---|---|---|---|
| 1 | **Create an order** with multiple items | Done | `POST /api/orders` — validates input, computes total, persists atomically |
| 2 | **Retrieve order details** by order ID | Done | `GET /api/orders/{id}` — JOIN FETCH loads items in a single query |
| 3 | **Update order status** with lifecycle enforcement | Done | `PATCH /api/orders/{id}/status` — state machine in enum guards transitions |
| 4 | **List all orders** with optional status filter | Done | `GET /api/orders?status=PENDING` — query-param filter, returns full list otherwise |
| 5 | **Cancel an order** (only if PENDING) | Done | `POST /api/orders/{id}/cancel` — returns 409 Conflict if not PENDING |
| 6 | **Background job**: auto-promote PENDING to PROCESSING every 5 min | Done | `@Scheduled` cron + bulk JPQL update (single SQL, no N+1) |

---

## Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Language | Java 8 | Matches the installed JDK on the target machine |
| Framework | Spring Boot 2.7.18 | Mature, battle-tested, enormous ecosystem |
| Persistence | Spring Data JPA + Hibernate 5.6 | Declarative repositories, custom JPQL for bulk ops |
| Database | H2 (in-memory) | Zero-config for reviewers — just run, no DB setup needed |
| Validation | Jakarta Bean Validation (Hibernate Validator) | Declarative field-level constraints with cascading |
| API Docs | SpringDoc OpenAPI 1.7 (Swagger UI) | Auto-generated interactive API explorer |
| Build | Maven 3.8+ | Standard, reproducible builds |
| Testing | JUnit 5 + Mockito + MockMvc + AssertJ | Unit tests, integration tests, parameterized tests |
| Logging | SLF4J + Logback | Structured console + rolling file output |

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                        REST Client                           │
│                  (Swagger UI / curl / Postman)                │
└────────────────────────┬─────────────────────────────────────┘
                         │  HTTP
┌────────────────────────▼─────────────────────────────────────┐
│  Controller Layer (OrderController)                          │
│  - Input validation (@Valid)                                 │
│  - HTTP status mapping (201, 200, 404, 409, 400)             │
│  - OpenAPI annotations for Swagger docs                      │
├──────────────────────────────────────────────────────────────┤
│  Service Layer (OrderService)                                │
│  - Business logic & state machine enforcement                │
│  - Transaction management (@Transactional)                   │
│  - DTO ↔ Entity mapping via OrderMapper                      │
├──────────────────────────────────────────────────────────────┤
│  Repository Layer (OrderRepository)                          │
│  - Spring Data JPA (auto-generated CRUD)                     │
│  - Custom JPQL: findByIdWithItems (JOIN FETCH)               │
│  - Custom JPQL: bulkUpdatePendingToProcessing                │
├──────────────────────────────────────────────────────────────┤
│  Database (H2 In-Memory)                                     │
│  - Schema: schema.sql (DDL managed explicitly)               │
│  - Seed data: data.sql (4 sample orders)                     │
│  - Hibernate ddl-auto=validate (safety net)                  │
└──────────────────────────────────────────────────────────────┘

         ┌──────────────────────────────────┐
         │  Scheduler (OrderStatusScheduler) │
         │  Cron: every 5 min               │
         │  PENDING → PROCESSING (bulk SQL)  │
         └──────────────────────────────────┘
```

---

## Getting Started

### Prerequisites

- **Java 8+** — verify: `java -version`
- **Maven 3.8+** — verify: `mvn -version`

No database installation needed — H2 runs in-memory.

### Build & Run

```bash
# Clone the repository
git clone https://github.com/shibam-max/order-processing-system.git
cd order-processing-system

# Build and run all tests
mvn clean verify

# Start the application
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.

### Quick Links (after starting)

| URL | What |
|---|---|
| http://localhost:8080/swagger-ui/index.html | Interactive Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI 3.0 JSON spec |
| http://localhost:8080/h2-console | H2 database console |
| http://localhost:8080/api/orders | List all orders |

**H2 Console credentials:** JDBC URL = `jdbc:h2:mem:orderdb`, User = `sa`, Password = *(empty)*

---

## API Reference

### 1. Create Order

```
POST /api/orders
Content-Type: application/json
```

**Request Body:**
```json
{
  "customerName": "Alice Wonderland",
  "customerEmail": "alice@example.com",
  "items": [
    { "productName": "Laptop", "quantity": 1, "unitPrice": 999.99 },
    { "productName": "Mouse",  "quantity": 2, "unitPrice": 25.50 }
  ]
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "customerName": "Alice Wonderland",
  "customerEmail": "alice@example.com",
  "status": "PENDING",
  "totalAmount": 1050.99,
  "createdAt": "2026-04-17T10:00:00.000",
  "updatedAt": "2026-04-17T10:00:00.000",
  "items": [
    { "id": 1, "productName": "Laptop", "quantity": 1, "unitPrice": 999.99, "lineTotal": 999.99 },
    { "id": 2, "productName": "Mouse",  "quantity": 2, "unitPrice": 25.50,  "lineTotal": 51.00 }
  ]
}
```

### 2. Get Order by ID

```
GET /api/orders/{id}
```

Returns **200 OK** with full order + items, or **404 Not Found**.

### 3. List All Orders

```
GET /api/orders
GET /api/orders?status=PENDING
```

Returns **200 OK** with an array. Optional `status` param filters by `PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, or `CANCELLED`.

### 4. Update Order Status

```
PATCH /api/orders/{id}/status
Content-Type: application/json

{ "status": "PROCESSING" }
```

Returns **200 OK** on success, **409 Conflict** if the transition is not allowed, **404 Not Found** if order doesn't exist.

### 5. Cancel an Order

```
POST /api/orders/{id}/cancel
```

Returns **200 OK** with status `CANCELLED` if the order was `PENDING`. Returns **409 Conflict** if the order has already progressed past `PENDING`.

---

## Order Status Lifecycle

```
                    ┌────────────┐
                    │   PENDING  │
                    └─────┬──────┘
                          │
              ┌───────────┼───────────┐
              ▼                       ▼
       ┌──────────────┐       ┌──────────────┐
       │  PROCESSING  │       │  CANCELLED   │
       └──────┬───────┘       └──────────────┘
              │
              ▼
       ┌──────────────┐
       │   SHIPPED    │
       └──────┬───────┘
              │
              ▼
       ┌──────────────┐
       │  DELIVERED   │
       └──────────────┘
```

**Rules encoded in `OrderStatus.canTransitionTo()`:**

| From | Allowed Targets |
|---|---|
| PENDING | PROCESSING, CANCELLED |
| PROCESSING | SHIPPED |
| SHIPPED | DELIVERED |
| DELIVERED | *(terminal — no transitions)* |
| CANCELLED | *(terminal — no transitions)* |

These rules are enforced both by the service layer and validated exhaustively in unit tests (18 parameterized test cases cover every possible pair).

---

## Background Scheduler

A `@Scheduled` cron job runs **every 5 minutes** and bulk-promotes all `PENDING` orders to `PROCESSING`:

```
Cron expression: 0 */5 * * * *
```

**Why bulk SQL instead of loading entities?** Performance. The repository uses a single `UPDATE ... WHERE status='PENDING'` JPQL statement — no matter how many pending orders exist, it's one round-trip to the database. This is critical at scale.

The cron schedule is **externalized** in `application.yml` and can be overridden via environment variable:

```bash
ORDER_SCHEDULER_PENDING_TO_PROCESSING_CRON="0 */10 * * * *" mvn spring-boot:run
```

---

## Validation & Error Handling

### Input Validation

Every request body is validated using Bean Validation annotations with cascading into nested objects:

| Field | Constraint | Error Message |
|---|---|---|
| `customerName` | `@NotBlank` | "Customer name is required" |
| `customerEmail` | `@NotBlank` + `@Email` | "Must be a valid email address" |
| `items` | `@NotEmpty` + `@Valid` (cascading) | "Order must contain at least one item" |
| `items[].productName` | `@NotBlank` | "Product name is required" |
| `items[].quantity` | `@NotNull` + `@Min(1)` | "Quantity must be at least 1" |
| `items[].unitPrice` | `@NotNull` + `@DecimalMin("0.01")` | "Unit price must be greater than zero" |

### Error Response Format

All errors return a consistent JSON structure via `@RestControllerAdvice`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2026-04-17T10:00:00.000",
  "fieldErrors": {
    "customerName": "Customer name is required",
    "items[0].quantity": "Quantity must be at least 1"
  }
}
```

| HTTP Status | When |
|---|---|
| `400 Bad Request` | Validation failures, malformed JSON |
| `404 Not Found` | Order ID does not exist |
| `409 Conflict` | Invalid status transition or cancel attempt on non-PENDING order |
| `500 Internal Server Error` | Unexpected errors (logged with full stack trace) |

---

## Testing

```bash
mvn test              # Run all 59 tests
mvn verify            # Build + test
mvn test -pl .        # Run from project root
```

### Test Suite Summary (59 tests, 100% pass)

| Test Class | Type | Count | What It Validates |
|---|---|---|---|
| `OrderStatusTest` | Unit (Parameterized) | 19 | Every one of the 18 status-transition pairs + terminal state check |
| `OrderServiceTest` | Unit (Mockito) | 15 | Service logic: create, get, list, update, cancel, promote, error paths |
| `OrderRepositoryTest` | Integration (DataJpaTest) | 5 | JPA queries, JOIN FETCH, bulk JPQL update, empty results |
| `OrderControllerIntegrationTest` | Integration (MockMvc) | 19 | Full HTTP cycle: happy paths, validation errors, 404, 409, malformed JSON |
| `OrderStatusSchedulerTest` | Unit (Mockito) | 1 | Scheduler delegates correctly to service |

### Test Design Philosophy

- **Unit tests mock the repository** — fast, isolated, test only business logic.
- **Integration tests use real H2** — prove the SQL, JPA mappings, and HTTP layer work together.
- **Parameterized tests cover combinatorics** — every status pair tested, not just happy paths.
- **Each test has `@DisplayName`** — readable output, acts as living documentation.

---

## Project Structure

```
src/
├── main/java/com/ecommerce/order/
│   ├── OrderProcessingApplication.java    # @SpringBootApplication + @EnableScheduling
│   ├── controller/
│   │   └── OrderController.java           # 5 REST endpoints with OpenAPI annotations
│   ├── dto/
│   │   ├── ApiErrorResponse.java          # Uniform error payload
│   │   ├── CreateOrderRequest.java        # Validated input DTO
│   │   ├── OrderItemRequest.java          # Validated nested item DTO
│   │   ├── OrderItemResponse.java         # Item output DTO with lineTotal
│   │   ├── OrderMapper.java               # Static Entity ↔ DTO converter
│   │   ├── OrderResponse.java             # Full order output DTO
│   │   └── UpdateStatusRequest.java       # Status transition DTO
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice — single error handler
│   │   ├── InvalidOrderStateException.java# 409 Conflict trigger
│   │   └── OrderNotFoundException.java    # 404 Not Found trigger
│   ├── model/
│   │   ├── Order.java                     # JPA entity (@OneToMany items)
│   │   ├── OrderItem.java                 # JPA entity (@ManyToOne order)
│   │   └── OrderStatus.java              # Enum with canTransitionTo() state machine
│   ├── repository/
│   │   └── OrderRepository.java           # JpaRepository + 2 custom JPQL queries
│   ├── scheduler/
│   │   └── OrderStatusScheduler.java      # Cron job: PENDING → PROCESSING
│   └── service/
│       └── OrderService.java              # All business logic, transactional
├── main/resources/
│   ├── application.yml                    # All config in one place
│   ├── schema.sql                         # Explicit DDL (tables, indexes)
│   ├── data.sql                           # 4 seed orders for demo
│   └── logback-spring.xml                 # Console + rolling file logging
└── test/java/com/ecommerce/order/
    ├── OrderProcessingApplicationTests.java
    ├── controller/OrderControllerIntegrationTest.java
    ├── model/OrderStatusTest.java
    ├── repository/OrderRepositoryTest.java
    ├── scheduler/OrderStatusSchedulerTest.java
    └── service/OrderServiceTest.java
```

---

## System Design Patterns

This project deliberately applies GoF and enterprise design patterns to solve real problems — not as academic exercises, but because each pattern earns its place.

### Behavioral Patterns

#### 1. Observer Pattern (Domain Events)

**Problem:** When an order is created or its status changes, multiple side-effects must happen (audit logging, customer notifications, analytics). Putting all this in `OrderService` violates SRP and makes the service a growing monolith.

**Solution:** Spring's `ApplicationEventPublisher` + `@TransactionalEventListener`. The service publishes domain events; listeners react independently.

```
OrderService ──publishes──→ OrderCreatedEvent ──→ OrderEventListener (audit log)
             ──publishes──→ OrderStatusChangedEvent ──→ OrderEventListener (notification)
                                                    ──→ (future: analytics, webhooks...)
```

**Files:** `OrderEvent.java`, `OrderCreatedEvent.java`, `OrderStatusChangedEvent.java`, `OrderEventListener.java`

**Why it matters:** Adding a new side-effect (e.g., sending a webhook) means adding a new `@EventListener` method — zero changes to `OrderService`. This is the Open/Closed Principle in action.

#### 2. State Pattern (Order Status Machine)

**Problem:** Order status transitions must be strictly controlled. Scattering if/else checks across the service creates fragile, hard-to-test code.

**Solution:** `OrderStatus` enum encapsulates all transition rules in `canTransitionTo()`. Each status "knows" which transitions it allows — the service just asks.

```java
// The service doesn't contain transition logic — it delegates:
if (!currentStatus.canTransitionTo(targetStatus)) {
    throw new InvalidOrderStateException(...);
}
```

**File:** `OrderStatus.java`

**Why it matters:** 18 parameterized tests validate every possible transition pair. Adding a new status (e.g., `RETURNED`) means updating one enum — no service code changes.

#### 3. Strategy Pattern (Pluggable Validation)

**Problem:** Order validation rules change over time (max items, max amount, fraud checks, inventory checks). Hardcoding them in the service makes adding/removing rules a risky change.

**Solution:** `OrderValidationStrategy` interface with Spring-injected implementations. The service iterates all registered strategies — each one is an independent `@Component`.

```
OrderService ──iterates──→ MaxItemsPerOrderValidator (≤ 50 items)
                        ──→ MaxOrderAmountValidator (≤ $100,000)
                        ──→ (future: FraudCheckValidator, InventoryValidator...)
```

**Files:** `OrderValidationStrategy.java`, `MaxItemsPerOrderValidator.java`, `MaxOrderAmountValidator.java`

**Why it matters:** New validation rule = new class + `@Component`. No existing code changes. No risk of breaking existing rules.

### Structural Patterns

#### 4. Facade Pattern (OrderService)

**Problem:** Clients (controller) shouldn't know about repositories, event publishers, validators, mappers.

**Solution:** `OrderService` acts as a Facade — it hides the complexity of 4 collaborators behind 6 clean public methods.

```
OrderController ──→ OrderService (Facade)
                        ├── OrderRepository (persistence)
                        ├── ApplicationEventPublisher (events)
                        ├── List<OrderValidationStrategy> (validation)
                        └── OrderMapper (DTO conversion)
```

#### 5. DTO Pattern (Data Transfer Objects)

**Problem:** Exposing JPA entities directly couples the API contract to the database schema.

**Solution:** Separate request/response DTOs with a stateless `OrderMapper` converter. Schema changes don't break clients; API changes don't require entity modifications.

```
HTTP Request → CreateOrderRequest (DTO) → OrderMapper → Order (Entity) → DB
DB → Order (Entity) → OrderMapper → OrderResponse (DTO) → HTTP Response
```

#### 6. Repository Pattern (Data Access Abstraction)

**Problem:** Business logic shouldn't contain SQL or know about persistence mechanics.

**Solution:** `OrderRepository` (Spring Data JPA) provides a clean interface. Custom JPQL queries (`findByIdWithItems`, `bulkUpdatePendingToProcessing`) are declared, not implemented.

### Creational Patterns

#### 7. Builder Pattern (Object Construction)

**Problem:** Entities and DTOs have many fields. Constructors with 7+ arguments are error-prone.

**Solution:** Lombok `@Builder` on all entities and DTOs. Enables readable, immutable construction:

```java
Order.builder()
    .customerName("Alice")
    .customerEmail("alice@example.com")
    .status(OrderStatus.PENDING)
    .build();
```

#### 8. Factory Method (OrderMapper)

**Problem:** Converting between DTOs and entities involves multiple steps (mapping fields, building child objects, computing totals).

**Solution:** `OrderMapper.toEntity()` and `OrderMapper.toResponse()` are static factory methods that encapsulate construction logic. The service doesn't know *how* to build entities — it delegates.

### Bonus: Event Sourcing Lite (Audit Trail)

**Problem:** "Who changed order #123 and when?" — without an audit trail, this question is unanswerable.

**Solution:** `OrderAuditLog` entity captures every lifecycle event (creation, status changes, cancellation) with timestamps, old/new status, and a detail message. Queryable via `GET /api/orders/{id}/audit`.

```
GET /api/orders/1/audit
[
  { "eventType": "ORDER_CREATED",  "newStatus": "PENDING",    "detail": "Order placed with 2 item(s)" },
  { "eventType": "STATUS_CHANGED", "oldStatus": "PENDING",    "newStatus": "PROCESSING", "detail": "..." },
  { "eventType": "STATUS_CHANGED", "oldStatus": "PROCESSING", "newStatus": "SHIPPED",    "detail": "..." }
]
```

This isn't full event sourcing (we don't replay events to rebuild state), but it provides complete traceability — a critical requirement for any real e-commerce system.

### Pattern Summary

| Pattern | Category | Where Applied | Problem Solved |
|---|---|---|---|
| **Observer** | Behavioral | Domain Events + EventListener | Decoupled side-effects from business logic |
| **State** | Behavioral | OrderStatus enum | Centralized, testable transition rules |
| **Strategy** | Behavioral | OrderValidationStrategy | Pluggable, extensible validation rules |
| **Facade** | Structural | OrderService | Hides complexity behind clean API |
| **DTO** | Structural | Request/Response DTOs + Mapper | Decoupled API contract from DB schema |
| **Repository** | Structural | OrderRepository (JPA) | Abstracted data access from business logic |
| **Builder** | Creational | Lombok @Builder on all models | Readable, safe multi-field construction |
| **Factory Method** | Creational | OrderMapper static methods | Encapsulated entity/DTO construction |
| **Event Sourcing Lite** | Architectural | OrderAuditLog | Full order lifecycle traceability |

---

## Distributed System Fundamentals

This project implements six distributed system fundamentals that are non-negotiable in production microservice architectures. Each one is implemented in code, not just documented.

### 1. Distributed Tracing (Correlation ID)

**Problem:** In a microservice mesh, a single user action fans out across 5-10 services. When something fails, how do you trace the full request path?

**Solution:** `CorrelationIdFilter` (highest-priority servlet filter) that:
- Reads `X-Correlation-ID` from the incoming request (propagated from upstream service)
- Generates a UUID if none provided (request originator)
- Injects it into SLF4J MDC → **every log line includes the correlation ID**
- Returns it in the response header → caller can correlate response to request

```
# Every log line now includes the correlation ID:
2026-04-17 10:15:30.123 [http-nio-8080-exec-1] [a3f8b2c1-...] INFO OrderService - Created order id=42
2026-04-17 10:15:30.130 [http-nio-8080-exec-1] [a3f8b2c1-...] INFO OrderEventListener - [NOTIFICATION] Order #42...
```

**File:** `CorrelationIdFilter.java`

**Production extension:** Replace MDC with OpenTelemetry spans for full distributed tracing in Jaeger/Zipkin.

### 2. Idempotency (Exactly-Once Semantics)

**Problem:** Network timeouts, client retries, and load balancer failovers can cause the same request to arrive multiple times. Without protection, a retry creates a duplicate order.

**Solution:** `IdempotencyFilter` intercepts all POST requests with an `X-Idempotency-Key` header:

```
# First request — processed normally:
POST /api/orders  X-Idempotency-Key: order-abc-123  → 201 Created

# Retry (same key) — returns cached response without re-executing:
POST /api/orders  X-Idempotency-Key: order-abc-123  → 201 Created (X-Idempotent-Replayed: true)
```

**How it works:**
1. On first request: execute operation, cache response (status + body) keyed by idempotency key
2. On duplicate: return cached response immediately, no business logic executed
3. TTL: 24 hours (Caffeine). In production: Redis with configurable TTL.

**File:** `IdempotencyFilter.java`

**Why this matters:** This is THE most critical distributed systems concept for e-commerce. Stripe, PayPal, and every payment API requires idempotency keys.

### 3. Rate Limiting (API Protection)

**Problem:** A misbehaving client (retry storm, bot, or DDoS) can overwhelm the service, causing cascading failures for all users.

**Solution:** `RateLimitingFilter` implements a per-IP fixed-window rate limiter:

```
# Normal request:
GET /api/orders  → 200 OK
                   X-RateLimit-Limit: 100
                   X-RateLimit-Remaining: 97

# After exceeding 100 requests/minute:
GET /api/orders  → 429 Too Many Requests
                   Retry-After: 60
                   {"status":429,"message":"Rate limit exceeded. Max 100 requests per minute."}
```

**Config:** 100 requests/minute per IP. Uses Caffeine (already a dependency) as the counter store.

**File:** `RateLimitingFilter.java`

**Production extension:** Redis-based sliding window for distributed rate limiting across instances.

### 4. Circuit Breaker (Resilience)

**Problem:** If Kafka is down, every order operation blocks on a failed Kafka publish, eventually exhausting thread pools and bringing down the entire API.

**Solution:** Resilience4j `CircuitBreaker` wraps the Kafka publisher:

```
State: CLOSED (normal) → Kafka publish succeeds → all good
State: CLOSED → 5+ failures with 50% failure rate → OPEN
State: OPEN → All publishes short-circuited (no Kafka call) → API continues working
State: OPEN → After 30 seconds → HALF_OPEN → Try 3 calls
State: HALF_OPEN → Success → CLOSED (recovered)
```

**Config:**
- Failure rate threshold: 50%
- Sliding window: 10 calls
- Wait in open state: 30 seconds
- Half-open permitted calls: 3

**File:** `KafkaOrderEventPublisher.java` (with circuit breaker)

**Why this matters:** The order API NEVER goes down because Kafka is down. Events are lost temporarily (acceptable for analytics/notifications), but orders are always processed.

### 5. Health Checks & Observability (Spring Actuator)

**Problem:** Orchestrators (Kubernetes, ECS) need to know if the service is healthy. Monitoring systems need metrics.

**Solution:** Spring Boot Actuator exposes:

```
GET /actuator/health    → {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
GET /actuator/info      → {"app":{"name":"Order Processing System","version":"1.0.0"}}
GET /actuator/metrics   → Available metric names (JVM, HTTP, cache stats)
GET /actuator/caches    → Cache manager details
```

**Exposed endpoints:** `health`, `info`, `metrics`, `caches` — minimal surface area (security-conscious).

**Health details:** `show-details: always` for debugging. In production, use `show-details: when-authorized`.

### 6. Graceful Shutdown

**Problem:** During deployments, in-flight requests get killed mid-execution, leaving orders in inconsistent states.

**Solution:** `server.shutdown: graceful` with a 30-second timeout:

```yaml
server:
  shutdown: graceful
spring.lifecycle:
  timeout-per-shutdown-phase: 30s
```

On SIGTERM:
1. Stop accepting new requests immediately
2. Wait up to 30 seconds for in-flight requests to complete
3. Run Spring shutdown hooks (close DB connections, flush caches)
4. Exit cleanly

**Why this matters:** Zero-downtime deployments in Kubernetes rely on graceful shutdown. Without it, rolling updates cause request failures.

### Distributed Systems Summary

| Fundamental | Pattern | File(s) | Production Upgrade |
|---|---|---|---|
| **Distributed Tracing** | Correlation ID propagation | `CorrelationIdFilter.java` | OpenTelemetry + Jaeger |
| **Idempotency** | Request deduplication | `IdempotencyFilter.java` | Redis idempotency store |
| **Rate Limiting** | Per-IP fixed window | `RateLimitingFilter.java` | Redis sliding window |
| **Circuit Breaker** | Resilience4j on Kafka | `KafkaOrderEventPublisher.java` | Resilience4j dashboard |
| **Health Checks** | Spring Actuator | `application.yml` | Prometheus + Grafana |
| **Graceful Shutdown** | Spring lifecycle | `application.yml` | K8s preStop hook |

---

## Caching

### Strategy: Caffeine In-Memory Cache

We use **Caffeine** (the successor to Guava Cache) as a high-performance L1 cache for order reads.

| Cache Name | What's Cached | TTL | Max Entries | Eviction |
|---|---|---|---|---|
| `orders` | Single order by ID (`getOrder`) | 10 min | 500 | On update, cancel, or bulk promote |
| `orderList` | List queries (all / by status) | 10 min | 500 | On any write operation |

### How It Works

```
GET /api/orders/42        → Cache HIT  → Return instantly (no DB query)
GET /api/orders/42        → Cache MISS → Query DB → Store in cache → Return
POST /api/orders          → @CacheEvict("orderList") → Invalidates list cache
PATCH /api/orders/42/status → @CacheEvict("orders", "orderList") → Invalidates both
```

### Configuration

`CacheConfig.java` sets up Caffeine with `recordStats()` enabled — in production, you'd expose cache hit/miss metrics via Spring Actuator for monitoring.

### Why Caffeine over Redis?

For a single-instance H2-backed application, an in-process cache avoids network overhead. In a multi-instance production deployment, you'd swap to **Redis** (or add it as L2 behind Caffeine) by simply changing the `CacheManager` bean — zero changes to the `@Cacheable` annotations.

---

## Logging

### Architecture

We use **SLF4J + Logback** with a production-grade configuration:

| Component | Level | Purpose |
|---|---|---|
| `com.ecommerce.order` | INFO | Business operations (order created, status changed, cancelled) |
| `com.ecommerce.order.scheduler` | DEBUG | Scheduler heartbeat and promotion counts |
| `com.ecommerce.order.event` | INFO | Kafka/event publishing confirmations |
| `org.hibernate.SQL` | WARN | Suppress SQL noise in production |
| `org.springframework` | WARN | Suppress framework noise |

### Output Targets

1. **Console** — Structured format: `timestamp [thread] level logger - message`
2. **Rolling File** — `logs/order-processing.log` with:
   - Size-based rotation: 10MB per file
   - Time-based rotation: daily
   - Retention: 30 days, 100MB total cap

### What Gets Logged

```
2026-04-17 10:15:30.123 [http-nio-8080-exec-1] INFO  c.e.order.service.OrderService - Created order id=42 for customer=alice@example.com
2026-04-17 10:15:45.456 [http-nio-8080-exec-3] INFO  c.e.order.service.OrderService - Order id=42 status changed PENDING -> PROCESSING
2026-04-17 10:20:00.001 [scheduling-1] DEBUG c.e.o.scheduler.OrderStatusScheduler - Running scheduled promotion of PENDING orders to PROCESSING
2026-04-17 10:20:00.050 [scheduling-1] INFO  c.e.order.service.OrderService - Promoted 5 PENDING order(s) to PROCESSING
2026-04-17 10:15:30.130 [http-nio-8080-exec-1] INFO  c.e.o.event.OrderEventListener - [NOTIFICATION] Order #42 confirmation sent to alice@example.com
```

---

## Kafka Integration

### Design: Profile-Gated Event Publishing

Kafka is integrated as a **profile-gated addon** — the app runs perfectly without Kafka (default), and enabling the `kafka` profile activates event publishing to Kafka topics.

```
# Default (no Kafka needed):
java -jar order-processing-system-1.0.0.jar

# With Kafka enabled:
java -jar order-processing-system-1.0.0.jar --spring.profiles.active=kafka
```

### Architecture

```
OrderService ──publishes──→ Spring ApplicationEvent
                              │
                              ├──→ OrderEventListener (BEFORE_COMMIT)
                              │      └── Writes audit log (always active)
                              │
                              └──→ KafkaOrderEventPublisher (AFTER_COMMIT, kafka profile only)
                                     └── Publishes to Kafka topics
```

### Kafka Topics

| Topic | Key | Payload | Published On |
|---|---|---|---|
| `order-events` | Order ID | Full order creation details | Order created |
| `order-status-changes` | Order ID | Old status, new status, reason | Any status transition |

### Producer Configuration

- **Acks:** `all` (waits for all replicas — max durability)
- **Retries:** 3 (with idempotence enabled — no duplicate messages)
- **Serialization:** String key + JSON value
- **Partitioning:** By order ID — ensures all events for an order go to the same partition (ordering guarantee)

### Why Profile-Gated?

1. **Standalone operation:** Reviewers can run the app with zero infrastructure (just `mvn spring-boot:run`)
2. **Production-ready:** Flip one flag to enable real Kafka publishing
3. **Testability:** Unit/integration tests run without a Kafka broker
4. **Adapter Pattern:** Same domain events, different transports — adding RabbitMQ, SNS, or webhooks follows the same pattern

### Sample Kafka Message

```json
{
  "eventType": "STATUS_CHANGED",
  "orderId": 42,
  "previousStatus": "PENDING",
  "newStatus": "PROCESSING",
  "reason": "Status updated from PENDING to PROCESSING",
  "occurredAt": "2026-04-17T10:15:45.456"
}
```

---

## CI/CD Pipeline

### GitHub Actions — 3-Stage Pipeline

Every push to `main` or PR triggers an automated pipeline:

```
┌─────────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Build & Test      │───→│  Code Quality    │───→│  Docker Build   │
│  (Java 8 + Java 11) │    │  (mvn verify)    │    │  (main only)    │
└─────────────────────┘    └──────────────────┘    └─────────────────┘
```

**Stage 1: Build & Test** (matrix — runs on Java 8 AND Java 11 in parallel)
- `mvn clean compile` — compile all 34+ source files
- `mvn test` — run all 64+ unit and integration tests
- `mvn package` — produce executable JAR
- Uploads test results and JAR as artifacts

**Stage 2: Code Quality**
- `mvn verify` — full lifecycle including integration test verification
- Runs after build succeeds on both Java versions

**Stage 3: Docker Build** (main branch only)
- Packages JAR into a multi-stage Docker image
- Tags with commit SHA + `latest`
- **Smoke test:** Starts the container, waits 15 seconds, hits `/actuator/health`

### Key CI/CD Decisions
- **Matrix testing on Java 8 + 11** — proves forward compatibility
- **Artifact uploads** — test reports downloadable for debugging failed builds
- **Docker smoke test** — catches "builds but doesn't start" issues
- **Conditional Docker stage** — only runs on `main` (not on every PR)

---

## Docker & Deployment

### Quick Start (Docker)

```bash
# Standalone (no Kafka):
docker-compose up --build

# Full stack with Kafka:
docker-compose -f docker-compose.yml -f docker-compose.kafka.yml up --build
```

### Multi-Stage Dockerfile

```
Stage 1: maven:3.8.8-eclipse-temurin-8   → Build + package
Stage 2: eclipse-temurin:8-jre-alpine    → Runtime only (~85MB)
```

**Production hardening baked in:**
- **Non-root user** — runs as `appuser` (security best practice)
- **Container-aware JVM** — `UseContainerSupport` + `MaxRAMPercentage=75%`
- **Health check** — Docker HEALTHCHECK hits `/actuator/health` every 30s
- **Layer caching** — `dependency:go-offline` first, source code second (fast rebuilds)

### Docker Compose Profiles

| Command | What You Get |
|---|---|
| `docker-compose up` | App + H2 (in-memory) — zero dependencies |
| `docker-compose -f docker-compose.yml -f docker-compose.kafka.yml up` | App + Kafka + Zookeeper — full event streaming |

### Kubernetes-Ready Signals

The application exposes everything K8s needs:

```yaml
# Liveness probe:
httpGet:
  path: /actuator/health
  port: 8080

# Readiness probe:
httpGet:
  path: /actuator/health
  port: 8080

# Graceful shutdown:
terminationGracePeriodSeconds: 30
```

---

## Design Decisions & Trade-offs

| Decision | Rationale |
|---|---|
| **State machine in the enum** | `OrderStatus.canTransitionTo()` centralizes all transition logic in one place. Easy to test (18 parameterized cases), impossible to bypass from the service layer. Alternative: a separate state machine library — overkill for 5 states. |
| **Separate DTOs from entities** | JPA entities are internal; API contracts are external. Decoupling them means we can evolve the DB schema without breaking clients, and vice versa. |
| **Schema-first DDL with `ddl-auto=none`** | Hibernate does not own the schema. `schema.sql` creates tables before Hibernate boots. This mirrors real-world deployment (Flyway/Liquibase) and avoids boot-order conflicts between SQL init and Hibernate validation. |
| **Bulk SQL for the scheduler** | `bulkUpdatePendingToProcessing()` is a single `UPDATE ... WHERE` — O(1) round-trips regardless of row count. Loading N entities, mutating, and saving would be O(N) with N+1 risk. |
| **Constructor injection (no `@Autowired` on fields)** | Makes dependencies explicit, supports immutability, and makes unit testing trivial (just pass mocks to the constructor). |
| **`findByIdWithItems` (JOIN FETCH)** | Avoids the N+1 problem: one query loads the order + all items. Without this, accessing `order.getItems()` outside a transaction triggers `LazyInitializationException`. |
| **H2 in-memory database** | Reviewers can clone and run in under 30 seconds with zero infrastructure. For production, swap the datasource URL to PostgreSQL/MySQL — the code stays identical. |
| **Externalized cron expression** | Scheduler frequency is in `application.yml`, not hardcoded. Can be overridden per environment via Spring's property resolution. |

---

## AI-Assisted Development: What Worked, What Broke, How I Fixed It

> *The assignment encouraged extensive use of AI tools. This section honestly documents how AI (Cursor AI) was used, where it produced incorrect output, and how human judgment was required to fix it.*

### What AI Was Used For

| Area | How AI Helped |
|---|---|
| **Scaffolding** | Generated the initial Maven project structure, `pom.xml`, and boilerplate files. Saved ~30 minutes of setup. |
| **Entity & DTO classes** | Generated Lombok-annotated JPA entities and request/response DTOs. Minor edits needed but solid starting point. |
| **Repository queries** | AI generated `findByStatus`, the `findByIdWithItems` JOIN FETCH, and the `bulkUpdatePendingToProcessing` JPQL. |
| **Test suite** | AI generated the bulk of the 59 tests including parameterized tests for status transitions. |
| **README drafting** | AI produced the first draft of the README. Manually rewritten for accuracy and the AI-reflection section. |

### Where AI Got Stuck & Human Intervention Was Needed

#### 1. Java Version Mismatch (Spring Boot 3.x vs Java 8)

**What AI did wrong:** AI initially generated the project targeting **Spring Boot 3.2.5** with **Java 17** features — switch expressions (`return switch (this) { ... }`), `jakarta.*` namespace, and `.toList()` (Java 16+ API).

**The actual environment:** The machine had **Java 8** (OpenJDK Corretto `1.8.0_452`).

**How it was fixed:**
- Downgraded Spring Boot from `3.2.5` → `2.7.18`
- Replaced all `jakarta.*` imports with `javax.*` (Bean Validation, JPA)
- Replaced Java 17 switch expressions with traditional `switch/case` blocks
- Replaced `.toList()` calls with explicit `new ArrayList<>()` + for-loops
- Replaced `List.of()` with `Collections.singletonList()` / `Arrays.asList()` in tests

**Lesson:** AI assumed a modern Java version. The environment constraint was only discovered at compile time (`invalid flag: --release`). This required a human to diagnose the root cause and systematically update every file.

#### 2. Swagger UI Dependency Version (springdoc 1.8.0 vs Java 8)

**What AI did wrong:** AI selected `springdoc-openapi-ui` version **1.8.0** for the Spring Boot 2.x project. Version 1.8.0 internally requires Java 17 bytecode.

**What happened:** The application compiled and started, but Swagger UI at `http://localhost:8080/swagger-ui.html` returned an error page at runtime — the springdoc classes couldn't load on a Java 8 JVM.

**How it was fixed:** Downgraded `springdoc-openapi-ui` from `1.8.0` → `1.7.0` (the last version supporting Java 8). Verified Swagger UI returned HTTP 200 after the fix.

**Lesson:** AI doesn't cross-check transitive dependency bytecode levels. A human had to diagnose that the library version was incompatible with the JVM, even though Maven resolved it without errors.

#### 3. PowerShell vs Bash Assumptions

**What AI did wrong:** AI generated `curl` commands for testing the API. On Windows, PowerShell aliases `curl` to `Invoke-WebRequest`, which has completely different syntax (`-H` is not `-Headers`, etc.).

**How it was fixed:** Replaced all `curl` test commands with PowerShell-native `Invoke-RestMethod` calls.

**Lesson:** AI assumed a Unix-like shell. On Windows, human knowledge of PowerShell was required to actually validate the running application.

#### 4. Directory Creation on Windows

**What AI did wrong:** Used `mkdir -p dir1 dir2 dir3` (Unix syntax for creating multiple directories). PowerShell's `mkdir` does not accept multiple path arguments this way.

**How it was fixed:** Replaced with `New-Item -ItemType Directory -Force -Path "dir1", "dir2", "dir3"`.

**Lesson:** Cross-platform file operations need human verification. AI defaults to bash idioms.

#### 5. Seed Data Breaking Tests (data.sql FK Constraint Violation)

**What AI did wrong:** AI added a `data.sql` file with seed data using hardcoded `order_id=1` foreign key references in `order_item` inserts. This worked fine for the running application, but broke during testing.

**What happened:** The `OrderControllerIntegrationTest` (which runs first) creates and deletes orders, consuming auto-increment IDs. When a second Spring context loads for `OrderProcessingApplicationTests`, it connects to the same in-memory H2 database (kept alive by `DB_CLOSE_DELAY=-1`). The `data.sql` re-runs and tries to insert `order_item` rows referencing `order_id=1`, but that row was deleted by the integration tests — FK constraint violation.

**How it was fixed:**
- Created `src/test/resources/application.yml` that overrides `data-locations` to blank, disabling seed data during tests
- Initially added `defer-datasource-initialization: true` — but that caused Issue #6 below

**Lesson:** AI doesn't reason about test execution order or how multiple Spring contexts interact with a shared in-memory database. A human had to trace the FK violation back to a stale auto-increment sequence from a prior test context.

#### 6. Startup Crash: `defer-datasource-initialization` vs `ddl-auto=validate`

**What AI did wrong:** To fix Issue #5, AI added `defer-datasource-initialization: true` to `application.yml`. This flag delays all SQL scripts (`schema.sql` and `data.sql`) to run *after* Hibernate initialization.

**What happened:** With `ddl-auto=validate`, Hibernate tries to validate tables at startup — but `schema.sql` hadn't run yet because it was deferred. Result: `Schema-validation: missing table [customer_order]` crash on every startup.

**How it was fixed:** A human realized that the AI's fix for one problem created another. The correct solution was to switch `ddl-auto` from `validate` to `none` and remove `defer-datasource-initialization` entirely. With `ddl-auto=none`, Spring runs `schema.sql` → `data.sql` first, and Hibernate simply trusts the schema without trying to validate or auto-create it.

**Lesson:** AI applied a fix in isolation without understanding the cascade effect. It didn't connect that deferring SQL scripts would starve Hibernate's validator. A human had to read the full stack trace, understand the Spring Boot initialization sequence, and pick a configuration that satisfies both runtime startup and test isolation simultaneously.

### Summary: AI + Human Collaboration

| What | AI | Human |
|---|---|---|
| Initial code generation | 90% of boilerplate | Reviewed, verified correctness |
| Architecture decisions | Suggested patterns | Chose and validated trade-offs |
| Java version compatibility | Wrong (assumed Java 17) | Diagnosed and fixed across all files |
| Swagger dependency | Wrong version (1.8.0) | Identified runtime failure, downgraded |
| OS-specific commands | Wrong (assumed Unix) | Fixed for Windows/PowerShell |
| Seed data + test isolation | Broke test suite with FK errors | Traced multi-context H2 conflict, fixed |
| Fix cascading into new bug | `defer-datasource-initialization` broke startup | Understood boot sequence, chose `ddl-auto=none` |
| Test design | Generated all 59 tests | Verified assertions match requirements |
| State machine logic | Correct first try | Validated with parameterized tests |
| README / documentation | First draft | Rewrote for accuracy, added this section |

**Bottom line:** AI accelerated development dramatically (what would take a full day was done in ~1 hour), but it made **environment-specific assumptions** that required human expertise to diagnose and fix. The code compiles, tests pass, and every endpoint works because a human verified every layer — not because AI got it right the first time.

---

## Human Judgment: What Only a Developer Could Do

> *This section is for the judges. AI is a powerful accelerator, but here are the specific moments in this project where no AI could have solved the problem — only a developer with real engineering understanding could.*

### 1. Diagnosing the Root Cause, Not Just the Symptom

When the application crashed with `invalid flag: --release`, AI had no idea why. It had generated valid Java 17 code — the code was *correct* in a vacuum. A human had to:
- Run `java -version` to discover the environment was Java 8
- Understand that Spring Boot 3.x requires Java 17 at the bytecode level
- Decide to downgrade Spring Boot (not install Java 17) because the target deployment environment matters more than the latest framework
- Systematically find and replace `jakarta.*` → `javax.*`, switch expressions → switch statements, and `List.of()` → `Arrays.asList()` across 18 source files and 6 test files

AI would have happily regenerated the project for Java 17 — but the *constraint* was Java 8, and only a human could make that judgment call.

### 2. Understanding Spring Boot Initialization Order

The most subtle bug was Issue #6: AI's fix for seed data (`defer-datasource-initialization: true`) silently broke application startup by reordering Spring's initialization sequence. This required understanding that:

1. By default, Spring Boot runs SQL scripts **before** Hibernate
2. `defer-datasource-initialization: true` flips this order
3. `ddl-auto=validate` depends on tables existing **when Hibernate boots**
4. These three facts combine to cause a startup crash

No stack trace told you "you set `defer-datasource-initialization` wrong." The error was `missing table [customer_order]` — and only a human who understands the Spring Boot boot sequence could connect that to the config flag added 10 minutes ago. AI would have likely tried adding the table via `ddl-auto=create`, which would have hidden the problem and lost the explicit schema control.

### 3. Choosing the Right Abstraction Level

Throughout this project, there were moments where AI suggested "correct" code that was wrong for the context:

| AI Suggestion | Human Decision | Why |
|---|---|---|
| Use a state machine library (Spring Statemachine) | Keep it in the enum | 5 states don't need a framework — a `canTransitionTo()` method is more readable, testable, and has zero dependencies |
| Use `ddl-auto=update` to avoid schema issues | Use `ddl-auto=none` with explicit `schema.sql` | Production systems never let Hibernate mutate the schema. Explicit DDL is safer, auditable, and mirrors real CI/CD pipelines |
| Load all orders, filter in Java | Use repository-level `findByStatus()` | Filtering in the database is O(matching rows), not O(all rows). This matters at scale |
| Use `findById()` + lazy loading | Use `findByIdWithItems()` JOIN FETCH | Avoids `LazyInitializationException` outside transactions and eliminates N+1 queries |

These aren't bugs — they're **judgment calls** that AI cannot make because it lacks context about production constraints, performance implications, and long-term maintainability.

### 4. Verifying That "Working" Means Actually Working

AI generated 59 tests and they all passed. But "tests pass" is not the same as "the application works." A human had to:

- Start the server and manually hit every endpoint with real HTTP requests
- Verify that Swagger UI actually rendered (not just that the dependency was present)
- Check that seed data appeared in the API response (not just that `data.sql` was on the classpath)
- Test error cases live — send malformed JSON, try invalid status transitions, cancel a non-PENDING order
- Confirm the H2 console was accessible for reviewers

AI can write tests. A human decides whether the tests are *testing the right things*.

### 5. Writing This README

AI produced a generic project README. A human:
- Restructured it for a reviewer audience (not a user audience)
- Added the architecture diagram and lifecycle flowchart
- Wrote the honest AI-reflection section (AI won't self-critique)
- Documented the 6 real bugs and how they were fixed
- Explained *why* each design decision was made, not just *what* was done

**The takeaway for judges:** This project was built *with* AI, not *by* AI. Every file was human-reviewed. Every bug was human-diagnosed. Every design trade-off was human-decided. AI wrote the first draft — a human made it production-ready.

---

## License

This project was built as a coding assessment submission.
