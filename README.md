## API Rate Limiter

## Overview

This project is a proof-of-concept rate-limiting middleware for a B2B SaaS API.

The main goal is to prevent one client from using a large amount of API capacity and affecting other clients. The limiter supports different limits for client tiers and different limits for read and write endpoints.

The implementation is intentionally in-memory because that was a constraint of the exercise. The README also documents what changes would be needed if the application were running across multiple instances.

## Section A — Architecture & Trade-offs

I used a token bucket because it gives me a simple way to control the request rate while still allowing some short bursts. Each client/endpoint combination has a bucket with a configured capacity. Tokens are added back over time, and each request uses one token. If there are no tokens available, the middleware returns a 429 Too Many Requests response with a Retry-After header.

The limits are kept in application.yml, so I can change the standard and premium limits without changing the middleware code. I also separate read and write traffic. GET requests use the read limit, while POST, PUT, PATCH, and DELETE use the stricter write limit. The client ID and endpoint type are used when finding the bucket, so one client's traffic does not affect another client's bucket, and read traffic does not consume the write limit.

I implemented the limiter as a Spring OncePerRequestFilter. I chose this because I wanted the rate-limit check to happen before the controller and avoid adding rate-limit code to every route.

The biggest limitation is that the state is only in memory. This makes the proof of concept simple and fast, but each application instance has its own buckets. Restarting the application also clears all the buckets. If the application is running on multiple instances, the same client could get a separate quota on each instance.

I also added a maximum number of tracked client keys so the in-memory map does not grow forever. The eviction logic is intentionally simple for this proof of concept. With more time, I would add better TTL-based cleanup, metrics, and more detailed tests around concurrent requests and bucket eviction.

If this moved to production with multiple instances, I would keep the middleware and configuration model but move the bucket state into a shared store such as Redis.

## Section B — Production Readiness Plan

## B1 — Failure Modes & Scaling Plan

The first failure mode is a process restart. Since the buckets are stored only in memory, restarting the application clears them and every client starts with a fresh quota. For this proof of concept, I think that is acceptable because persistent storage is not allowed. The downside is that a client could temporarily get more requests after a restart than it normally would.

The bigger issue is horizontal scaling. If I run three application instances, each instance has its own copy of the buckets. A client sending requests across all three instances could therefore use more than its configured limit. The middleware would still work on each instance, but the limit would not be a global limit for that organization. I would solve this by moving the bucket state into a shared store such as Redis and making the token updates atomic.

Memory growth is another issue. Every new client/endpoint combination can create state in memory. If there are a large number of unique client IDs, an unbounded map could keep growing and put pressure on the JVM heap. I added a maximum tracked-client limit for the proof of concept. In production, I would use bounded entries with expiration/TTL instead.

The first things I would monitor are the number of active buckets, evictions, rate-limit rejections, and JVM memory usage. If those numbers start growing unexpectedly, that would give us an early signal that the in-memory approach is becoming a problem.

## B2 — Plausible but Incorrect AI Output

One example I can think of is if I asked an AI coding assistant to implement the in-memory tracking for the rate limiter. It could suggest using a ConcurrentHashMap with the client ID as the key. At first, that looks correct because it is thread-safe and gives me a quick way to find each client's bucket.

The issue is that if I just keep adding new clients to that map, nothing removes them. If the API keeps getting requests from new client IDs, the map can keep growing and eventually cause memory problems. So even though the rate limiter would work functionally, it would not fully meet the requirement around memory growth.

I would catch this by looking beyond the normal happy-path tests. I would ask what happens if a very large number of unique clients make requests over time. I would also check whether entries in the map ever get removed or whether there is some maximum size.

In my implementation, I added a configurable limit on the number of tracked clients instead of allowing the map to grow forever. I also documented that this is a simplified solution for the proof of concept. If I were taking this to production, I would probably use a bounded cache with TTL/expiration and add metrics around the number of tracked clients, evictions, rejected requests, and memory usage.

The main thing I took away is that I don't want to accept AI-generated code just because it compiles or passes the basic tests. I need to check whether it actually handles the constraints and failure cases in the problem.

## Section C — AI Usage Log

## Interaction 1 — Initial Design

What I asked:
I asked the AI how I could design the rate limiter using Spring Boot while keeping the state in memory.

What it gave me:
It suggested using a Spring filter with a token bucket and keeping separate limits for reads and writes.

What I kept:
I kept the filter and token bucket approach because it fit the middleware requirement and gave me a way to support bursts instead of just counting requests in fixed time windows.

What I changed:
I made the configuration and client tiers fit the requirements of this specific task instead of using the example values from the AI.

## Interaction 2 — Memory and Concurrency

What I asked:
After I had the basic limiter working, I asked the AI to look for problems with the in-memory state.

What it gave me:
It pointed out that a concurrent map would be safe for concurrent access, but an unrestricted map could keep growing as new client IDs appeared.

What I changed:
I added a configurable maximum for tracked clients. I kept the implementation simple because this is a proof of concept, but I documented that production would need better expiration/eviction.

Why:
The functional tests could pass while the memory problem was still there, so I wanted to handle that requirement explicitly.

## Interaction 3 — Scaling and Failure Modes

What I asked:
I asked what would happen if the application restarted or if we ran multiple instances.

What it gave me:
It pointed out that restarting the process would clear the buckets and that each server instance would have its own rate-limit state.

What I kept:
I kept those limitations in the README because they are important consequences of the in-memory constraint.

What I would change for production:
I would move the bucket state to a shared store such as Redis so all instances use the same state. I would also add metrics and better expiration for old entries.

## Running the Project

The application can be run as a normal Spring Boot application.

The rate-limit configuration is kept outside the middleware logic, so the configured client tiers and endpoint limits can be adjusted without changing the implementation.

## Run the API manually

mvn clean test

mvn clean test

## Test the API manually

## GET
curl -i \
-H "X-Client-Id: org-123" \
-H "X-Client-Tier: standard" \
http://localhost:8080/api/customers

## POST
curl -i \
-X POST \
-H "Content-Type: application/json" \
-H "X-Client-Id: org-123" \
-H "X-Client-Tier: standard" \
-d '{"name":"Harini"}' \
http://localhost:8080/api/customers

## GET
curl -i \
-H "X-Client-Id: premium-org" \
-H "X-Client-Tier: premium" \
http://localhost:8080/api/customers

## Production Follow-up

This implementation intentionally stays within the exercise constraints.

If I were taking it further, my next steps would be:

Move rate-limit state to Redis so the limit is shared across application instances.

Use atomic operations for token updates so concurrent requests cannot bypass the limit.

Add TTL-based cleanup and better eviction instead of relying on the proof-of-concept memory limit.

Add metrics for rejected requests, active buckets, evictions, and memory usage.

Add more load and concurrency tests, especially around bursts and simultaneous requests.

Add monitoring/alerts so a sudden increase in 429 responses or memory usage is visible.

The main trade-off of the current implementation is simplicity versus distributed consistency. For this proof of concept, keeping the state in memory makes the implementation easy to understand and avoids external infrastructure. For a production system with multiple instances, a shared rate-limit store would be necessary.

