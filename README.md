# System Design Portfolio

A growing collection of system design documents inspired by real-world applications.
Each design covers requirements, capacity estimation, architecture, and key tradeoffs —
structured the way you'd present in a FAANG system design interview.

---

## Designs

| # | System | Key Concepts | Status |
|---|--------|-------------|--------|
| 01 | [Rappi — Food Delivery Platform](./designs/rappi-food-delivery/) | Kafka, gRPC, Redis GEO, WebSockets, Microservices | ✅ Complete |
| 02 | Real-time Chat (WhatsApp-style) | WebSockets, fan-out, message persistence | 🔜 Coming soon |
| 03 | Music Streaming (Spotify-style) | CDN, media streaming, recommendations | 🔜 Coming soon |
| 04 | Ride Sharing (Uber-style) | Location matching, surge pricing, real-time | 🔜 Coming soon |
| 05 | E-commerce at Scale (Amazon-style) | Inventory, search, payments, warehousing | 🔜 Coming soon |

---

## How each design is structured

Every system follows the same structure — mirroring a real interview:

```
1. Requirements
   - Functional (what it does)
   - Non-functional (how well it does it)

2. Capacity Estimation
   - DAU, requests/sec, storage

3. High-Level Architecture
   - Components, technology choices, tradeoffs

4. Deep Dives
   - Critical flows (sequence diagrams)
   - Failure scenarios
   - Scaling bottlenecks

5. Diagrams
   - Architecture diagram (Excalidraw)
   - Sequence diagrams (Excalidraw)
```

---

## Tech stack recurring themes

| Technology | Used in |
|------------|---------|
| Apache Kafka (AWS MSK) | Event-driven communication between services |
| gRPC + Protocol Buffers | Inter-service communication |
| Redis (ElastiCache) | Caching, geospatial queries, WebSocket registry |
| PostgreSQL (RDS) | Per-service relational databases |
| Amazon EKS | Container orchestration |
| GraphQL | Flexible client-facing API layer |
| WebSockets | Real-time push to clients |

---

## About

These designs are built for learning and interview preparation,
inspired by the apps I use every day living in Mexico City.

Each one is designed to answer two interview questions:
- *"Draw the system"* → Architecture diagram
- *"Walk me through a request"* → Sequence diagram

> *"Sometimes doing less is more."* — start simple, evolve with real bottlenecks.
