# Interview Talking Points — Rappi Food Delivery

Quick reference for system design interviews. These are the answers
to the questions interviewers actually ask.

---

## "Why did you choose Kafka over RabbitMQ?"

> "RabbitMQ was a serious contender. For a pure task queue it might actually
> be simpler. We chose Kafka for three specific reasons: fan-out to independent
> consumers, event replay on failure recovery, and the volume of driver location
> updates at 2,000 writes/sec. If this were a simpler system with one consumer
> per event and moderate volume, RabbitMQ would be the right call."

---

## "Why gRPC and not REST between services?"

> "REST would be simpler to debug — you can curl it, inspect it in plain text.
> gRPC is binary so you need specific tooling. We chose gRPC because inter-service
> communication at location-update scale specifically justifies the performance gain,
> and the typed proto contracts catch breaking changes at compile time, not in production."

---

## "Are you overengineering this?"

> "For a true MVP I'd simplify — Docker Compose instead of EKS, REST between
> services instead of gRPC, Postgres PostGIS for location instead of Redis GEO.
> But I'm designing for the scale this system needs to reach, and I'd introduce
> complexity incrementally as bottlenecks appear."

---

## "What's the hardest problem in this system?"

> "Driver location tracking. 10,000 active drivers sending GPS pings every 5 seconds
> is 2,000 writes/sec — that's the hottest write path in the whole system. The solution
> is Redis GEOADD for current position (sub-millisecond writes), Kafka partitioned by
> order_id for ordered event delivery, and a WebSocket registry in Redis so any
> Notification Consumer pod can find the right customer connection."

---

## "What happens if a service goes down?"

> "Because we're event-driven with Kafka, each service fails independently.
> If the Notification Service crashes, orders still get placed and paid —
> Payment and Restaurant consumers keep running. When Notification recovers,
> it resumes from its last Kafka offset and catches up on missed events.
> No manual intervention, no lost messages."

---

## "Why microservices and not a monolith?"

> "A monolith would actually be the right call for a true MVP — faster to build,
> simpler to operate. The specific reason we went microservices here is that
> each service has a fundamentally different scaling profile: Driver Service
> needs to handle 2K writes/sec, Restaurant Service is read-heavy with caching,
> Order Service needs strong consistency. Scaling them independently justifies
> the operational complexity."
