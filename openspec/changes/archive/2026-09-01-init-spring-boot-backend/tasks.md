## 1. Verify base project scaffolding

- [x] 1.1 Confirm `backend/pom.xml` builds with `mvnw -q -DskipTests package` and verify the build completes without errors
- [x] 1.2 Confirm `backend/src/main/resources/application.properties` has `DB_URL`, `DB_USER`, `DB_PASS` env-var overrides and `management.endpoints.web.exposure.include=health` set, adjusting values if missing

## 2. Package structure

- [x] 2.1 Create `com.cuentasclaras.backend.config` package and verify it compiles (add a package-info.java or first real config class from task 3)
- [x] 2.2 Create `com.cuentasclaras.backend.controller` package and verify it compiles
- [x] 2.3 Create `com.cuentasclaras.backend.service` package and verify it compiles
- [x] 2.4 Create `com.cuentasclaras.backend.repository` package and verify it compiles
- [x] 2.5 Create `com.cuentasclaras.backend.entity` package and verify it compiles
- [x] 2.6 Create `com.cuentasclaras.backend.dto` package and verify it compiles

## 3. Database connectivity

- [x] 3.1 Start a local PostgreSQL instance (e.g. via `docker run` or an already-installed service) matching the configured `cuentas_claras` database and verify the backend connects on startup (no datasource errors in logs)
- [x] 3.2 Run the app with `mvnw spring-boot:run` and verify Hibernate/JPA initializes against PostgreSQL without errors

## 4. Health check

- [x] 4.1 Start the app and call `GET /actuator/health` and verify it returns HTTP 200 with `{"status":"UP"}`
- [x] 4.2 Stop the local PostgreSQL instance, call `GET /actuator/health` again, and verify it reports a non-UP status, then restart PostgreSQL

## 5. Documentation and verification

- [x] 5.1 Add a short "Running the backend locally" section (README in `backend/` or root README) covering required env vars, how to start PostgreSQL, `mvnw spring-boot:run`, and the health check URL
- [x] 5.2 Run `mvnw test` and verify `BackendApplicationTests` (context load) passes
