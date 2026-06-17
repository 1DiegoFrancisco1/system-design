# 01 — Rappi Food Delivery Platform

> Rappi is the dominant food delivery super-app in Latin America.
> This design covers the core food delivery flow at scale — think DoorDash or Uber Eats for LATAM.

---

## Requirements

### Functional (MVP)
- **Customer:** browse restaurants → place order → pay → track in real time
- **Restaurant:** receive orders → accept/reject → update status
- **Driver:** get assigned → update location → confirm delivery

### Non-Functional
| Constraint | Decision | Reasoning |
|------------|----------|-----------|
| Availability | 99.99% | Revenue-critical — downtime = lost orders |
| Consistency | Eventual (most flows) | Menu staleness is okay; order status must be reliable |
| Latency | < 500ms order placement, location updates every 5s | Good UX without over-engineering |
| Durability | Zero order loss | Lost order = lost money + angry customer |

---

## Capacity Estimation

| Metric | Value | Reasoning |
|--------|-------|-----------|
| DAU | 1,000,000 | Given |
| Orders/day | 100,000 | ~10% of DAU place an order |
| Peak orders/sec | ~50 | Concentrated in 2 lunch/dinner peaks |
| Read/write ratio | 20:1 | Menu browsing >> order placement |
| Driver location updates/sec | ~2,000 | 10,000 active drivers × every 5s |

**Key insight:** the system is read-heavy overall, but has a high-frequency write spike from driver location updates. The hardest engineering problem isn't placing orders — it's handling 2,000 location writes/sec while staying available.

---

## Architecture

### Stack
| Layer | Technology | Why |
|-------|-----------|-----|
| Client | React + GraphQL | 3 different clients (customer, restaurant, driver) each need different data shapes |
| API Gateway | AWS API Gateway | Auth, rate limiting, GraphQL → gRPC fan-out |
| Auth | AWS Cognito + JWT | Managed auth, zero custom code |
| Inter-service | gRPC + Protocol Buffers | Typed contracts, binary performance, streaming support |
| Messaging | Apache Kafka (AWS MSK) | Fan-out to multiple consumers, event replay, 2K writes/sec |
| Cache | Redis (ElastiCache) | Sub-ms reads, GEOADD for driver location, WebSocket registry |
| Database | PostgreSQL (RDS) | Per-service databases, ACID compliance |
| Orchestration | Docker → Amazon EKS | Independent scaling per service, self-healing |
| Images | Amazon ECR + S3 | Docker image storage, static assets |

### Services
| Service | Responsibility | Database |
|---------|---------------|----------|
| Order Service | Place & manage orders, idempotency | PostgreSQL |
| Restaurant Service | Menus, availability, cache-aside | PostgreSQL + Redis |
| Driver Service | Location tracking, assignment | Redis GEO + PostgreSQL |
| Notification Service | Push notifications, WebSocket | Redis (registry) |
| User Service | Auth, profiles (absorbed by Order at Stage 1) | PostgreSQL |

### Kafka Topics
| Topic | Publisher | Consumers |
|-------|-----------|-----------|
| `order.placed` | Order Service | Payment, Notification, Restaurant |
| `order.confirmed` | Order Service | Notification, Driver |
| `order.ready` | Restaurant Service | Driver, Notification |
| `driver.location` | Driver Service | Notification (WebSocket push) |
| `payment.done` | Payment Consumer | Order Service, Notification |

---

## Key Design Decisions & Tradeoffs

### Why Kafka over RabbitMQ or SQS?
- **Fan-out:** one `order.placed` event consumed independently by Payment, Notification, and Restaurant consumers
- **Replay:** if Analytics consumer crashes, Kafka holds events — consumer catches up on recovery
- **Volume:** 2,000 driver location writes/sec — Kafka handles this natively
- RabbitMQ would work for lower-volume flows; SQS is single-consumer only

### Why gRPC over REST between services?
- **Typed contracts** via `.proto` files — breaking changes caught at compile time, not in production
- **Binary protocol** — 5-10x smaller than JSON, matters at 2K location updates/sec
- **Native streaming** — Driver Service streams location updates without a separate WebSocket layer
- Tradeoff: harder to debug (binary, not human-readable) — use `grpcurl` for testing

### Why Redis GEO for driver location?
- `GEOADD` overwrites current position — sub-millisecond writes
- `GEORADIUS` finds all drivers within Xkm in one command — no complex SQL
- Postgres + PostGIS would work but adds latency at 2K writes/sec
- Location history written to Postgres **asynchronously** (not on hot path)

### Idempotency on Order Placement
- Client generates UUID when customer taps confirm
- Redis stores UUID for 24 hours
- Double-tap? Same UUID → return cached response → one order created

### Timeout Strategy (Restaurant Acceptance)
- 5 minute window for restaurant to accept/reject
- On timeout: **auto-cancel + refund** (Option A for MVP)
- Option B (reassign to nearby restaurant) is a future improvement

---

## Diagrams

| Diagram | File | Description |
|---------|------|-------------|
| High-level architecture | [`diagrams/rappi-architecture.excalidraw`](./diagrams/rappi-architecture.excalidraw) | Full system components and connections |
| Order placement sequence | [`diagrams/rappi-order-sequence.excalidraw`](./diagrams/rappi-order-sequence.excalidraw) | Step-by-step order placement with alt flows |

---

## Evolution Path

### Stage 1 (Docker Compose — local)
- 3 services: Order, Restaurant, Driver
- Kafka + Redis + PostgreSQL in Docker
- Simple Kafka consumer for notifications (logs only)
- No auth — hardcode user ID

### Stage 2 (Single EC2)
- Deploy Docker Compose to EC2
- Add Cognito auth
- Real push notifications
- Extract Notification Service

### Stage 3 (EKS)
- Migrate to Kubernetes
- HPA autoscaling per service
- MSK for managed Kafka
- Full 5-service architecture

---

## What I learned

- Event-driven architecture with Kafka — fan-out, partitioning, at-least-once delivery
- gRPC inter-service communication with Protocol Buffers
- Redis as a geospatial store (GEOADD, GEORADIUS)
- WebSocket registry pattern for real-time push at scale
- Idempotency keys to prevent duplicate orders
- Timeout strategies for distributed workflows
- When NOT to use microservices (monolith-first principle)
- Docker Compose → EKS evolution path
