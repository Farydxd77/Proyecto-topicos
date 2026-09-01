## Context

Ver `proposal.md - Why` para la motivación y `specs/seguridad-jwt/spec.md` para
el contrato de comportamiento.

Estado actual:

- `config/SecurityConfig.java`: un `SecurityFilterChain` con
  `authorizeHttpRequests` que solo abre `/actuator/health/**` y protege el resto
  con `httpBasic` + el usuario en memoria autogenerado por Spring Boot.
- No hay paquete `security/` con clases (solo la estructura). No hay
  `UsuarioRepository`. No hay `exception/GlobalExceptionHandler`.
- `application.properties` ya define `jwt.secret=${JWT_SECRET:clave-secreta-local-solo-para-desarrollo-no-usar-en-prod}`
  y `jwt.expiration=86400000` (24 h en ms).
- `pom.xml` ya trae `jjwt-api/impl/jackson 0.12.6` y `spring-security-test`.
- Entidad `Usuario` (tabla `usuarios`, ver `CLAUDE.md - Modelo de datos`):
  `id`, `username VARCHAR(50) UNIQUE NOT NULL`, `password VARCHAR(255) NOT NULL`
  (BCrypt), timestamps heredados de `BaseEntity`.
- `CLAUDE.md - Seguridad`: todos los endpoints salvo `/api/auth/**` requieren JWT;
  expiración 24 h; token en `Authorization: Bearer {token}`.
- `CLAUDE.md - Formato de error estándar`: JSON con `timestamp`, `status`,
  `error`, `message`, `path` para cualquier error.

## Goals / Non-Goals

**Goals:**

- Cadena de filtros stateless que valide un JWT por request y pueble el
  `SecurityContext`.
- Rutas públicas (`/api/auth/**`, `/actuator/health/**`) y todo lo demás
  protegido, con 401 en formato estándar cuando falta un token válido.
- `JwtUtil` con emisión/validación probables de forma unitaria.
- Verificación por test de integración `MockMvc` de los escenarios del spec.

**Non-Goals (a nivel de diseño):**

- No exponer `AuthenticationManager` ni `PasswordEncoder` (los introduce la tarea
  de `/api/auth/register` + `/api/auth/login`).
- No `GlobalExceptionHandler` para controllers (esta tarea solo cubre el 401 de
  la capa de seguridad, que ocurre antes del `DispatcherServlet`).
- No roles ni `GrantedAuthority` reales (lista de authorities vacía).
- No caché de `UserDetails` ni de claves.

## Decisions

### Decisión 1: `JwtUtil` con la API 0.12.x de jjwt y clave HMAC derivada del secreto

`JwtUtil` lee `jwt.secret` y `jwt.expiration` con `@Value`. Deriva la clave con
`Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))` y usa
`Jwts.builder().subject(username).issuedAt(...).expiration(...).signWith(key).compact()`
para emitir, y `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`
para validar/leer el `subject`.

- **Por qué**: es la API no-deprecada de jjwt 0.12.6 (la del `pom.xml`).
  `parseSignedClaims` lanza `JwtException` (incluye `ExpiredJwtException`,
  `SignatureException`, `MalformedJwtException`); `validateToken` las captura y
  devuelve `false`, cumpliendo "no lanza excepción al llamador".
- **Requisito operativo**: HS256 exige una clave ≥ 256 bits. El secreto por
  defecto de `application.properties` tiene 52 caracteres (>32 bytes), suficiente.
  Se documenta en `tasks.md` que el `jwt.secret` de test debe ser igual de largo.
- **Alternativas descartadas**:
  - API estática `Jwts.parserBuilder()` / `setSigningKey` (0.11.x): deprecada en
    0.12.x. Rechazada.
  - Firmar con `SignatureAlgorithm.HS512`: requiere clave ≥ 512 bits; el secreto
    actual no llega. HS256 es suficiente para el alcance. Rechazada.
  - Guardar la clave como `Base64`/`byte[]` en config: más fricción operativa sin
    beneficio ahora. Rechazada.

### Decisión 2: `JwtAuthFilter extends OncePerRequestFilter`, tolerante a fallo

El filtro: si no hay header `Authorization` o no empieza por `Bearer `, llama a
`filterChain.doFilter` sin autenticar (no escribe respuesta). Si hay token, lo
valida con `JwtUtil`; si es válido, obtiene el `username`, carga
`UserDetails` con `UserDetailsServiceImpl`, crea un
`UsernamePasswordAuthenticationToken(userDetails, null, authorities)` con
`details` de la request y lo coloca en el `SecurityContext`. Cualquier problema
(token inválido, usuario inexistente → `UsernameNotFoundException`) se traga y se
deja pasar la cadena **sin** autenticación.

- **Por qué**: separar detección de fallo (aquí) de la respuesta 401 (en el
  `AuthenticationEntryPoint`) mantiene un único punto que construye el cuerpo de
  error estándar y hace que "usuario inexistente" y "sin token" produzcan el
  mismo 401, como pide el spec.
- **Alternativas descartadas**:
  - Que el filtro escriba el 401 directamente: duplica la lógica del cuerpo de
    error y complica el caso `/api/auth/**` (que no debe romperse por un token
    basura). Rechazada.
  - `GenericFilterBean` en vez de `OncePerRequestFilter`: no garantiza ejecución
    única por request en forwards/errores. Rechazada.

### Decisión 3: `UserDetailsServiceImpl` + `UsuarioRepository` mínimo

`UsuarioRepository extends JpaRepository<Usuario, Long>` con
`Optional<Usuario> findByUsername(String username)`. `UserDetailsServiceImpl`
implementa `UserDetailsService.loadUserByUsername`, y devuelve
`org.springframework.security.core.userdetails.User(username, passwordHash,
Collections.emptyList())`; si no hay usuario, lanza `UsernameNotFoundException`.

- **Por qué**: `UserDetailsService` es el punto de extensión estándar que
  consumirá tanto este filtro como el futuro `DaoAuthenticationProvider` del
  login. El repositorio es la dependencia mínima imprescindible; crear solo
  `findByUsername` no invade el alcance de "servicios de negocio".
- **Alternativas descartadas**:
  - Consultar con `EntityManager`/JPQL a mano en el service: reinventa Spring
    Data sin motivo. Rechazada.
  - Un `UserDetails` custom que envuelva `Usuario`: útil cuando se necesita el
    `id` en el contexto; hoy no se necesita y añade superficie. Se puede migrar
    después. Rechazada por ahora.

### Decisión 4: `SecurityConfig` stateless con entry point custom

`SecurityFilterChain`:
`csrf.disable()`,
`sessionManagement(s -> s.sessionCreationPolicy(STATELESS))`,
`authorizeHttpRequests(a -> a.requestMatchers("/api/auth/**",
"/actuator/health/**").permitAll().anyRequest().authenticated())`,
`exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))`,
`addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`.
Se elimina `httpBasic`.

`JwtAuthenticationEntryPoint implements AuthenticationEntryPoint` escribe
`status 401`, `contentType application/json`, y un cuerpo serializado con Jackson:
`{ timestamp: Instant.now().toString(), status: 401, error: "Unauthorized",
message: authException.getMessage() ó "No autenticado", path: request.getRequestURI() }`.

- **Por qué**: cumple literalmente el criterio "401 con formato de error
  estándar". `STATELESS` + sin CSRF es la configuración canónica para APIs con
  JWT. `permitAll` sobre `/actuator/health/**` conserva el comportamiento que
  `backend-bootstrap` ya especifica.
- **Alternativas descartadas**:
  - Mantener `httpBasic` en paralelo: dos mecanismos de auth, ambigüedad en los
    tests y en el 401. Rechazada (de ahí el **BREAKING** del proposal).
  - Reusar el futuro `GlobalExceptionHandler`: no aplica, el
    `AuthenticationEntryPoint` actúa fuera del `DispatcherServlet`. Rechazada.
  - `@PreAuthorize` a nivel de método: no cubre "todo lo no listado protegido" de
    forma central. Rechazada.

### Decisión 5: Verificación con `@SpringBootTest` + `MockMvc` construido a mano

Un test `SeguridadJwtIntegrationTest` con `@SpringBootTest` (webEnvironment MOCK) y
`@Transactional` (rollback automático, no toca datos reales), que construye el
`MockMvc` con `MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())`,
guarda un `Usuario` real vía repositorio con `ddl-auto=update`, pide
`/actuator/health` (200), pide una ruta protegida inexistente sin token
(401 + comprueba el JSON), y con `JwtUtil.generateToken(username)` comprueba que
la misma ruta ya no da 401.

- **Por qué**:
  - `@WebMvcTest` no carga JPA ni el repositorio, y los escenarios
    "usuario existe/no existe" necesitan la BD.
  - En Spring Boot 4 el *slice* `@AutoConfigureMockMvc` vive en
    `spring-boot-webmvc-test`, que NO viene con `spring-boot-starter-test`.
    Para no añadir dependencias, se arma el `MockMvc` a mano desde el
    `WebApplicationContext` y se le aplica `springSecurity()` de
    `spring-security-test` (sí presente).
  - `@Transactional` en lugar de `ddl-auto=create-drop`: verifica los mismos
    escenarios sin arriesgar los datos de la BD de desarrollo compartida.
  - La ruta protegida puede no existir como controller: Spring Security responde
    401 antes de enrutar, así que sirve igual para la aserción.
- **Alternativas descartadas**:
  - Añadir `spring-boot-webmvc-test` al `pom.xml` para usar
    `@AutoConfigureMockMvc`: introduce una dependencia nueva que el proposal
    decía evitar. Rechazada.
  - `create-drop` sobre la BD de desarrollo: destruye datos. Rechazada.
  - Solo test unitario de `JwtUtil`: no cubre la cadena de filtros ni el formato
    del 401. Se hace además, pero no basta.
  - `MockMvc` con `@MockBean UsuarioRepository`: pierde la verificación real de
    `findByUsername`. Rechazada.

## Risks / Trade-offs

- **`jwt.secret` de test demasiado corto → `WeakKeyException` al arrancar el test**
  → fallo ruidoso. Mitigación: fijar en el test un secreto ≥ 32 bytes
  (`application-test.properties` o `@TestPropertySource`).
- **Jackson 3 en Spring Boot 4** → el `ObjectMapper` autoconfigurado es
  `tools.jackson.databind.ObjectMapper` (no `com.fasterxml.jackson.databind`).
  `JwtAuthenticationEntryPoint` importa el paquete `tools.jackson`.
- **La ruta protegida de prueba no existe como endpoint** → sin JWT devuelve 401
  (seguridad antes de enrutar); con JWT válido devuelve 404, no 401. El test
  asserta "distinto de 401", no "200". Riesgo: si alguien lee el criterio como
  "200" habría que añadir un controller dummy. Se documenta la interpretación.
- **`UsernameNotFoundException` se traga en el filtro** → un token de un usuario
  borrado produce 401 genérico, sin pista de "usuario no existe". Aceptado: es lo
  que pide el spec y evita filtrar información.
- **`csrf.disable()`** → correcto para una API stateless con `Authorization`
  header; no hay formularios ni cookies de sesión. Documentado.
- **Cambio BREAKING de HTTP Basic a Bearer** → cualquier cliente/manual actual
  deja de autenticar. Aceptado: no hay clientes productivos aún (fase backend).
- **Reloj/expiración**: jjwt aplica `allowedClockSkew` 0 por defecto; un token en
  el límite exacto podría fallar. Irrelevante para 24 h de validez.

## Migration Plan

1. `repository/UsuarioRepository.java` con `findByUsername`.
2. `security/JwtUtil.java` (emisión + validación + `getUsername`).
3. `security/UserDetailsServiceImpl.java`.
4. `security/JwtAuthenticationEntryPoint.java` (cuerpo 401 estándar).
5. `security/JwtAuthFilter.java`.
6. Reescribir `config/SecurityConfig.java` (stateless, rutas, filtro, entry point).
7. `./mvnw clean compile`.
8. Test unitario `JwtUtilTest` + integración `SeguridadJwtIntegrationTest`; `./mvnw clean test`.
9. Arranque manual contra PostgreSQL: `GET /actuator/health` → 200; `GET
   /api/grupos` sin token → 401 con JSON estándar.

**Rollback**: `git revert` del commit. Sin cambios de esquema ni de datos; vuelve
la configuración HTTP Basic anterior.

## Open Questions

Ninguna.
