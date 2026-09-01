# Backend

Spring Boot 4 (Java 21) service for Cuentas Claras, backed by PostgreSQL.

## Running locally

1. Have a PostgreSQL instance reachable (defaults assume a local install on `localhost:5432` with a `cuentas_claras` database).
2. Optionally override the connection via environment variables (defaults shown):

   ```
   DB_URL=jdbc:postgresql://localhost:5432/cuentas_claras
   DB_USER=postgres
   DB_PASS=admin
   ```

3. Start the app:

   ```
   ./mvnw spring-boot:run
   ```

4. Check the health endpoint:

   ```
   curl http://localhost:8080/actuator/health
   ```

   Expected response when the database is reachable: `{"status":"UP"}`.
