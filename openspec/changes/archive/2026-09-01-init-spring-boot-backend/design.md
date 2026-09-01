## Context

`backend/` already contains a Maven-generated Spring Boot 4 (Java 21) skeleton: `pom.xml` with web, security, data-jpa, validation, actuator, PostgreSQL driver, Lombok, and JJWT dependencies already declared; `application.properties` already points at PostgreSQL via `DB_URL`/`DB_USER`/`DB_PASS` env vars and already exposes `management.endpoints.web.exposure.include=health`. Only `BackendApplication.java` and a matching test class exist under `com.cuentasclaras.backend` — no sub-packages yet. See proposal.md - Why/What Changes for motivation.

## Goals / Non-Goals

**Goals:**
- Land a conventional, predictable package layout under `com.cuentasclaras.backend` that later capabilities (auth, accounts, etc.) can drop into without restructuring.
- Make `/actuator/health` reflect real DB connectivity (not just `status: UP` with no checks).
- Keep local run instructions simple (env vars + `mvnw`).

**Non-Goals:**
- No business domain models, controllers, or JWT auth logic yet — this change is infrastructure only.
- No database schema/migration tooling (e.g. Flyway/Liquibase) decision — deferred to when the first real entity is added.
- No CI/CD or containerization (Dockerfile, docker-compose) — deferred; local Postgres is assumed to be provided by the developer.

## Decisions

- **Package layout**: use `com.cuentasclaras.backend.{config,controller,service,repository,entity,dto}`, one package per architectural layer, flat (no per-feature subpackages yet). Alternative considered: feature-based packaging (e.g. `backend.auth`, `backend.accounts`) — rejected for now because no feature exists yet; layer-based is simpler to bootstrap and can be migrated to feature packages later if the codebase grows large enough to warrant it.
- **Health check**: rely on Spring Boot Actuator's built-in `DataSourceHealthIndicator` (auto-configured once a datasource + `spring-boot-starter-actuator` are present) rather than a custom `/health` controller. `management.endpoint.health.show-details` stays `never` in this change (already set) to avoid leaking DB details publicly; per-environment tuning is left for later.
- **No packages created empty just to exist**: only create a package once it has content justifying it (e.g. `config` gets a real `@Configuration` class if one is needed for Actuator/DB wiring; otherwise the package structure requirement is satisfied by seeding each package with the first real class the health-check work needs, plus a placeholder is avoided — package directories are created as part of adding the corresponding class in tasks.md).

## Risks / Trade-offs

- [No local PostgreSQL available] → document required local setup (or a docker run command) in backend README so `/actuator/health` can be verified end-to-end during this change.
- [Layer-based packages could get crowded as the app grows] → acceptable now; revisit packaging strategy once a second real capability is added.
