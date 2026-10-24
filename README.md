# SwiftRoute

A high-throughput, headless URL shortening and click-analytics REST API built with Spring Boot, Redis, and PostgreSQL.

---

## Overview

SwiftRoute is engineered to handle massive read-to-write traffic ratios common in URL shorteners. It provides sub-millisecond redirection via multi-tier Redis caching, non-blocking click-tracking through Redis queues/streams, and persistent relational data storage[cite: 11, 13, 20].

## Key Features

* **Sub-Millisecond Redirection:** `302 Found` redirects powered by a Redis Cache-Aside layer[cite: 11, 20].
* **Base62 URL Encoding:** Collision-resistant short codes using 64-bit unique sequences with custom alias support.
* **Asynchronous Click Analytics:** Non-blocking tracking offloaded to Redis and batch-inserted into PostgreSQL[cite: 13, 20].
* **Distributed Rate Limiting:** Redis-backed sliding-window rate limiting to protect endpoints against abuse[cite: 14, 20].
* **Stateless Security:** JWT-based authentication, token revocation denylists, and role-based access control[cite: 6, 7, 20].
* **Abuse Protection:** Input sanitization, loopback address filtering (SSRF protection), and parameterized SQL[cite: 15, 20].

---

## Tech Stack

* **Language & Framework:** Java 21, Spring Boot 3.x (Spring Web, Spring Security, Spring Data JPA, Actuator)
* **Primary Database:** PostgreSQL 16+[cite: 8, 20]
* **Cache & Message Broker:** Redis 7+[cite: 8, 11, 13, 20]
* **Connection Pooling & Migrations:** HikariCP, Flyway[cite: 10, 20]

---

## Getting Started

### Prerequisites

* Java 21 JDK or higher
* Docker & Docker Compose (or local installations of PostgreSQL and Redis)
* Maven 3.9+ (or use the project wrapper `./mvnw`)