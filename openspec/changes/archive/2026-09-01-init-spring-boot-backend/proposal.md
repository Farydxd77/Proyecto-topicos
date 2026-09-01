## Why

This change establishes the base Spring Boot application in backend/ — build tooling, PostgreSQL connectivity, a clear package structure, and a working health check — so future features have a place to land.

## What Changes

- Confirm and finalize the Maven-based Spring Boot 4 project in `backend/` (Java 21), building on the scaffolding already generated there.
- Configure PostgreSQL as the runtime datasource, with connection settings overridable via environment variables (`DB_URL`, `DB_USER`, `DB_PASS`).
- Establish a package structure under `com.cuentasclaras.backend` (e.g. `config`, `controller`, `service`, `repository`, `domain`/`model`, `dto`) so future capabilities have a consistent place to live.
- Expose a working health check endpoint via Spring Boot Actuator (`/actuator/health`) confirming the app and its dependencies (including the database) are reachable.
- Add a root-level README or backend-local notes on how to run the service locally (env vars, `mvnw spring-boot:run`, expected health check URL).

## Capabilities

### New Capabilities
- `backend-bootstrap`: The base Spring Boot backend application — startup, configuration, package layout, and the health check endpoint that verifies the service (and its database connection) is up.

### Modified Capabilities
(none — this is the first backend capability introduced)

## Impact

- Affected code: everything under `backend/` (pom.xml, `application.properties`, `com.cuentasclaras.backend` package structure).
- New runtime dependency: a reachable PostgreSQL instance (local or containerized) for the datasource and for `/actuator/health` to report `UP` with DB details.
- No impact to the existing frontend (still mock-data only); this change only stands up the backend independently.
- Project scope expands beyond the original frontend-only description in `openspec/specs/project.md` — that doc will need a follow-up update once the backend direction is confirmed, but is out of scope for this change.
