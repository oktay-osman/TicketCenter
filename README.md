<div align="center">

<img src="src/main/resources/images/logo.png" alt="TicketCenter" width="220"/>

# TicketCenter

**A desktop ticket distribution system for event organizers, distributors, and administrators — built with JavaFX and Spring Boot.**

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.8-4FC08D?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-4169E1?logo=postgresql&logoColor=white)
![License](https://img.shields.io/badge/status-active-success)

</div>

---

## Overview

TicketCenter models a real-world event ticketing business: **organizers** publish events and hand off ticket sales to one or more **distributors**, who sell tickets through a purchase form and report back on inventory, revenue, and buyer details. An **administrator** oversees accounts, distributor assignments, and system-wide reporting.

The system is built for **concurrent use** — multiple distributors can sell tickets for the same event at the same time without overselling, thanks to row-level locking and optimistic concurrency control at the database layer.

## Features

**Accounts & Profiles**
- Admin-managed creation of organizer and distributor accounts
- Role-based dashboards and access control (Admin / Organizer / Distributor)
- Editable organizer and distributor profiles, including commission rate
- Organizer-submitted distributor ratings, averaged across events

**Events & Ticketing**
- Event creation with category, capacity, seat types, per-type pricing, and a per-person ticket limit
- Multi-distributor assignment per event
- Distributor sale form capturing buyer details and seat selection
- Inventory-safe ticket sales — pessimistic row locks plus `@Version` optimistic locking prevent overselling under concurrent access

**Reports**
- Distributor report: tickets sold by event category, revenue, rating — filterable by an arbitrary date range
- Event report: date, status, location, tickets sold, revenue — available to organizers (their own events) and admins (all events), filterable by date range
- Access rules enforced: organizers and distributors only ever see their own events

**Notifications**
- Distributors are notified when assigned to a new event
- Organizers receive a periodic digest of ticket sales (hourly, via a scheduled job)
- Organizers and distributors are alerted about upcoming events with unsold tickets

## Architecture

A JavaFX desktop client backed by a Spring-managed service layer and Spring Data JPA, following a standard layered structure:

```
controller/   Spring-managed FXML controllers (UI event handling)
service/      Business logic (@Service beans)
repository/   Spring Data JPA repositories
model/        JPA entities
util/         SpringContext, SessionManager
```

FXML views live in `src/main/resources/fxml/` and are loaded with `SpringContext::getBean` as the controller factory, so every controller is a full Spring bean with constructor-injected services.

## Getting Started

### Prerequisites
- Java 17
- PostgreSQL running locally, with a `ticket_center` database

### Configure the database
Connection settings live in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ticket_center
    username: postgres
    password: password
```

Schema is created and kept in sync automatically via `hibernate.ddl-auto: update` — no manual migrations needed.

### Run the app

```bash
./mvnw javafx:run
```

### Other useful commands

```bash
./mvnw clean package          # build a distributable JAR
./mvnw test -Dtest=ClassName  # run a specific test class
```

## Tech Stack

| Layer | Technology |
|---|---|
| UI | JavaFX 17 + FXML |
| Application | Spring Boot 3.1.5 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Auth | BCrypt password hashing |
| Build | Maven |
