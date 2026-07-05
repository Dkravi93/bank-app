# Bank App — Spring Boot Banking REST API

A small, self-contained banking backend: user registration/login (JWT), account
creation, deposits, and transfers between accounts — built to be genuinely
understood end-to-end rather than assembled from a tutorial.

## Why this project exists

This was built specifically to have real, defensible Spring Boot + Java experience
to speak to in interviews — not to pad a resume with a name-dropped framework.
Every design decision below is something I can walk through and justify.

## Stack

- Java 17, Spring Boot 3.3
- Spring Web, Spring Data JPA, Spring Security
- PostgreSQL (data), Redis (wired in, for future rate-limiting/caching)
- JWT (jjwt) for stateless auth
- Docker + Docker Compose
- JUnit 5 for tests, including a concurrency test (see below)

## The interesting engineering problem: safe money transfers

The core risk in any banking API is a **race condition on account balances** —
two concurrent transfers reading a stale balance and both succeeding when only
one should have. This project handles that with two layered mechanisms:

1. **Pessimistic row locking** (`SELECT ... FOR UPDATE` via
   `@Lock(LockModeType.PESSIMISTIC_WRITE)` in `AccountRepository`) inside the
   transfer's DB transaction, so a second transfer touching the same account
   blocks until the first one commits.
2. **Fixed lock ordering** — when transferring between account A and account B,
   we always lock whichever account number sorts first, regardless of transfer
   direction. This prevents a classic deadlock: transfer A→B and transfer B→A
   running at the same instant, each holding one lock and waiting on the other.
3. **Optimistic version check** (`@Version` on `Account`) as a second line of
   defense in case anything slips past the row lock.
4. **Idempotency keys** — every deposit/transfer request carries a client-supplied
   key; a retried request (timeout + retry, double-submit) is detected and
   rejected rather than double-processing the money movement.

`TransactionServiceConcurrencyTest` fires 20 concurrent transfer requests
against an account that can only afford 10 of them, and asserts the balance
never goes negative and never over-credits the destination. This is the test
I'd actually run in front of an interviewer.

## Architecture

```
controller/   → REST endpoints (thin, no business logic)
service/      → business logic, transaction boundaries (@Transactional)
repository/   → Spring Data JPA interfaces
model/        → JPA entities (User, Account, Transaction)
dto/          → request/response records, kept separate from entities
security/     → JWT generation/validation, Spring Security UserDetailsService
exception/    → custom exceptions + @RestControllerAdvice global handler
config/       → SecurityConfig (filter chain, stateless sessions)
```

Entities are never returned directly from controllers for user-facing account
data (see `AccountResponse` DTO) to avoid leaking internal fields and to keep
API shape decoupled from the DB schema.

## Running locally

```bash
docker compose up --build
```

This starts the app on `:8080`, Postgres, and Redis. Swagger UI is at
`http://localhost:8080/swagger-ui.html`.

### Without Docker

You need Postgres and Redis running locally, then:

```bash
mvn spring-boot:run
```

Config defaults (overridable via env vars) are in
`src/main/resources/application.yml`.

## API quick reference

| Method | Endpoint                                  | Auth |
|--------|--------------------------------------------|------|
| POST   | `/api/auth/register`                       | none |
| POST   | `/api/auth/login`                          | none |
| POST   | `/api/accounts`                            | JWT  |
| GET    | `/api/accounts`                             | JWT  |
| GET    | `/api/accounts/{accountNumber}`             | JWT  |
| POST   | `/api/accounts/{accountNumber}/deposit`     | JWT  |
| POST   | `/api/accounts/{accountNumber}/transfer`    | JWT  |
| GET    | `/api/accounts/{accountNumber}/transactions`| JWT  |

## What I'd build next

- Flyway migrations instead of `hibernate.ddl-auto: update` (fine for a
  portfolio project, not something I'd ship to production as-is)
- Rate limiting on `/transfer` using Redis (already wired into the stack)
- Scheduled interest accrual job for savings accounts
- Refresh tokens instead of a single long-lived JWT

## Honest scope note

This is a focused portfolio project (single service, not microservices) built
to demonstrate real Spring Boot fundamentals — REST API design, JPA/transaction
management, Spring Security + JWT, and concurrency-safe financial writes. It is
not a claim of production banking system experience.
