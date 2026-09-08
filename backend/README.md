# Backend

Servicio Spring Boot 4 (Java 21) de Cuentas Claras, sobre PostgreSQL.

## Correr en local

1. Levantar PostgreSQL: desde la raíz del repo, `docker compose up -d`.
2. Opcionalmente, pisar la conexión con variables de entorno (se muestran los
   valores por defecto, que son los del `docker-compose.yml`):

   ```
   DB_URL=jdbc:postgresql://localhost:5432/cuentas_claras
   DB_USER=postgres
   DB_PASS=admin
   ```

3. Arrancar:

   ```
   ./mvnw spring-boot:run
   ```

4. Comprobar el endpoint de salud:

   ```
   curl http://localhost:8080/actuator/health
   ```

   Respuesta esperada con la base accesible: `{"status":"UP"}`.

## Tests

**Los tests NO necesitan Docker ni PostgreSQL.** La fase de test corre sobre H2 en
memoria, en modo de compatibilidad PostgreSQL:

```
./mvnw test
```

La configuración está en `src/test/resources/application.properties`. Se llama igual
que el de `src/main/resources` a propósito: el classpath de test tiene precedencia,
así que **ninguna clase de test necesita anotarse** para usar la base en memoria, y
no hay forma de olvidarse una anotación y terminar escribiendo en la base de
desarrollo. `infraestructura/ConfiguracionDeTestTest` falla si eso llegara a
cambiar.

## Imagen de contenedor

```
docker build -t cuentas-claras-backend .
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/cuentas_claras \
  -e DB_USER=postgres \
  -e DB_PASS=admin \
  -e JWT_SECRET=cambiar-en-produccion-por-un-secreto-largo \
  cuentas-claras-backend
```

La imagen es multi-etapa: compila con Maven y ejecuta solo el `.jar` sobre un JRE 21,
con un usuario sin privilegios. No lleva credenciales ni direcciones incrustadas:
todo llega por variables de entorno.

## Configuración

| Propiedad                 | Variable            | Por defecto | Para qué                                                        |
|---------------------------|---------------------|-------------|-----------------------------------------------------------------|
| `spring.datasource.url`   | `DB_URL`            | local       | Conexión a PostgreSQL                                            |
| `jwt.secret`              | `JWT_SECRET`        | de desarrollo | Firma de los JWT. **Cambiar en producción**                    |
| `cors.allowed-origins`    | —                   | vacío       | Orígenes del frontend, separados por coma. Vacío = sin CORS      |
| `criptoya.cache-ttl`      | —                   | `60s`       | Cuánto se reutiliza una cotización sin volver a consultar        |
| `criptoya.cache-max-stale`| —                   | `24h`       | Antigüedad tolerada de una cotización cuando CriptoYa está caído |
