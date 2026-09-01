## Purpose

Provides the base runnable backend service — application startup, database connectivity, and a health check — that future backend capabilities build on top of.

## ADDED Requirements

### Requirement: Application starts and connects to PostgreSQL
The backend SHALL start as a Spring Boot application and SHALL establish a connection to a PostgreSQL database using connection settings that can be overridden by environment variables without code changes.

#### Scenario: Successful startup with database available
- **WHEN** the backend is started with a reachable PostgreSQL instance and valid credentials
- **THEN** the application starts successfully and establishes a datasource connection to that database

#### Scenario: Connection settings overridden by environment
- **WHEN** the `DB_URL`, `DB_USER`, or `DB_PASS` environment variables are set to non-default values
- **THEN** the backend connects using those values instead of the built-in defaults

### Requirement: Health check endpoint reports service status
The backend SHALL expose an HTTP health check endpoint that reports whether the application and its database connection are healthy.

#### Scenario: Healthy service
- **WHEN** a client sends a GET request to the health check endpoint while the database is reachable
- **THEN** the endpoint responds with HTTP 200 and a status indicating the service is up

#### Scenario: Database unavailable
- **WHEN** a client sends a GET request to the health check endpoint while the database is unreachable
- **THEN** the endpoint responds with a non-200 status indicating the service is degraded or down

### Requirement: Consistent package structure for backend code
The backend codebase SHALL be organized into a consistent set of packages (such as configuration, web/controller, service, repository, and domain/model layers) under the base package, so that future capabilities have a predictable place to be added.

#### Scenario: New capability follows existing structure
- **WHEN** a developer adds a new backend capability
- **THEN** its code is placed into the corresponding existing package (controller, service, repository, domain, etc.) rather than requiring a new top-level layout
