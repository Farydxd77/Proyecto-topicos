## Context

Ver `proposal.md - Why` y `specs/auth/spec.md` para el contrato.

Estado actual:

- `seguridad-jwt` (ya archivado) dejó: `JwtUtil` (`generateToken(username)` →
  HS256, 24 h; `validateToken`; `getUsername`), `JwtAuthFilter`,
  `UserDetailsServiceImpl`, `JwtAuthenticationEntryPoint`, y `SecurityConfig`
  STATELESS con `/api/auth/**` y `/actuator/health/**` en `permitAll()`.
- `UsuarioRepository` existe (`findByUsername`). **No** existe
  `ParticipanteRepository`, ni el paquete `exception/`, ni ningún
  `@RestControllerAdvice`, ni un bean `PasswordEncoder`.
- Entidades (ver `CLAUDE.md - Modelo de datos`):
  - `Usuario`: `id`, `username VARCHAR(50) UNIQUE NOT NULL`,
    `password VARCHAR(255) NOT NULL`, timestamps de `BaseEntity`. Lombok
    `@Getter/@Setter/@SuperBuilder/@NoArgsConstructor/@AllArgsConstructor`.
  - `Participante`: `id`, `usuario_id BIGINT UNIQUE NOT NULL` (`@OneToOne` LAZY,
    sin cascade), `nombre`, `apellido`, `ci`, timestamps. Misma config Lombok.
  - Regla de `CLAUDE.md`: "Un usuario siempre tiene exactamente un participante
    (1 a 1)".
- `CLAUDE.md - Respuestas HTTP estándar`: 201 crea (recurso en body), 400
  validación, 401 sin/!token, 409 conflicto (username duplicado).
- `CLAUDE.md - Formato de error estándar`: JSON `timestamp/status/error/message/path`.
- `pom.xml`: `spring-boot-starter-validation` y `spring-boot-starter-security`
  (trae `spring-security-crypto` con `BCryptPasswordEncoder`). Jackson 3
  (`tools.jackson`). El slice `@AutoConfigureMockMvc` NO está en el classpath de
  test (igual que en `seguridad-jwt`).

## Goals / Non-Goals

**Goals:**

- Dos endpoints públicos que creen cuenta y emitan token, cumpliendo los códigos
  HTTP y el formato de error estándar.
- `Usuario` + `Participante` creados de forma atómica en el registro.
- Contraseña solo como hash BCrypt; nunca en ninguna respuesta.
- Login que no permita enumerar usuarios.

**Non-Goals (a nivel de diseño):**

- No exponer `AuthenticationManager` ni usar `DaoAuthenticationProvider` (el
  login compara el hash a mano con `PasswordEncoder.matches`).
- No `@EntityGraph`/DTO projections; los response DTO se arman en el service.
- No manejar en el `GlobalExceptionHandler` errores que aún no puede producir
  ningún controller de negocio (se añadirán con esos controllers).

## Decisions

### Decisión 1: `AuthService` transaccional arma `Usuario` + `Participante`

`AuthService.register(RegisterRequest)` anotado `@Transactional`:
1. `if (usuarioRepository.findByUsername(req.username()).isPresent()) throw new UsernameAlreadyExistsException(...)`.
2. `Usuario u = Usuario.builder().username(req.username()).password(passwordEncoder.encode(req.password())).build();`
   `usuarioRepository.save(u)`.
3. `Participante p = Participante.builder().usuario(u).nombre(...).apellido(...).ci(...).build();`
   `participanteRepository.save(p)`.
4. `String token = jwtUtil.generateToken(u.getUsername());`
5. devuelve `RegisterResponse(token, ParticipanteDto(p.getId(), nombre, apellido, ci, u.getUsername()))`.

- **Por qué**: `@Transactional` garantiza que un fallo tardío no deje un `Usuario`
  huérfano sin `Participante` (la regla 1‑a‑1 de `CLAUDE.md`). Se usa el
  `@SuperBuilder` que ya tienen las entidades. `save` explícito del `Participante`
  porque el `@OneToOne` no tiene `cascade`.
- **Alternativas descartadas**:
  - Añadir `cascade = PERSIST` al `@OneToOne` de `Participante`: cambia una
    entidad fuera del alcance de esta tarea y afecta a otros flujos. Rechazada.
  - Crear el `Participante` en un segundo endpoint: contradice el spec ("se crea
    automáticamente"). Rechazada.

### Decisión 2: Login con `PasswordEncoder.matches`, no `AuthenticationManager`

`AuthService.login(LoginRequest)`:
`usuarioRepository.findByUsername(req.username())`
`.filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))`
`.map(u -> new LoginResponse(jwtUtil.generateToken(u.getUsername()), new UsuarioDto(u.getId(), u.getUsername())))`
`.orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));`

- **Por qué**: no hace falta la maquinaria de `AuthenticationManager` para un
  único `UserDetailsService`; `matches` sobre el hash es suficiente y deja el
  `AuthenticationManager` para cuando haya más proveedores. El `.filter(...)`
  hace que "usuario no existe" y "password mala" tomen exactamente el mismo
  camino → mismo 401, sin enumeración (requisito del spec).
- **Alternativas descartadas**:
  - `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(...))`:
    exige exponer el bean y un `DaoAuthenticationProvider`; más piezas para el
    mismo resultado. Rechazada.
  - Mensajes distintos para 401 de login: viola "no revelar si el username
    existe". Rechazada.

### Decisión 3: `GlobalExceptionHandler` con `@RestControllerAdvice`

Un `@RestControllerAdvice` en `exception/` con `@ExceptionHandler` para:
- `MethodArgumentNotValidException` → 400; `message` = resumen y un mapa
  `campo → mensaje` construido desde `getBindingResult().getFieldErrors()`, dentro
  del mismo JSON estándar (campo extra `errors`).
- `UsernameAlreadyExistsException` (custom, no `@ResponseStatus`) → 409.
- `org.springframework.security.authentication.BadCredentialsException` → 401 con
  `message` genérico fijo ("Credenciales inválidas"), ignorando el mensaje real.
- Cuerpo construido con el mismo esquema que `JwtAuthenticationEntryPoint`
  (`timestamp` = `Instant.now()`, `status`, `error`, `message`, `path` =
  `request.getRequestURI()`), serializado por Jackson 3 automáticamente
  (devolviendo un `ResponseEntity<Map<String,Object>>` o un DTO `ApiError`).

- **Por qué**: centraliza el formato de error de `CLAUDE.md` para todos los
  controllers, no solo auth. El `AuthenticationEntryPoint` de `seguridad-jwt`
  sigue cubriendo los 401 que ocurren *antes* del `DispatcherServlet` (sin
  token); este advice cubre los que se lanzan *dentro* (login con password mala).
- **Alternativas descartadas**:
  - `@ResponseStatus` en cada excepción + `ErrorController`: no permite el cuerpo
    estándar con `path`/`timestamp` de forma uniforme. Rechazada.
  - Reutilizar `JwtAuthenticationEntryPoint` para el 401 de login: ese punto solo
    lo invoca Spring Security ante `AuthenticationException` no capturada en el
    filtro, no ante una excepción lanzada por el `AuthService`. Rechazada.

### Decisión 4: DTOs como `record` con Bean Validation

- `dto/request/RegisterRequest`: `record RegisterRequest(@NotBlank @Size(min=3,max=50) String username, @NotBlank @Size(min=8) String password, @NotBlank @Size(max=100) String nombre, @NotBlank @Size(max=100) String apellido, @NotBlank @Size(max=20) String ci)`.
- `dto/request/LoginRequest`: `record LoginRequest(@NotBlank String username, @NotBlank String password)`.
- `dto/response/RegisterResponse(String token, ParticipanteDto participante)` con
  `ParticipanteDto(Long id, String nombre, String apellido, String ci, String username)`.
- `dto/response/LoginResponse(String token, UsuarioDto usuario)` con
  `UsuarioDto(Long id, String username)`.
- El controller anota el `@RequestBody` con `@Valid`.

- **Por qué**: `record` = inmutable y sin Lombok; los response DTO no tienen
  campo de contraseña, así que es imposible filtrarla. `@Size(min=8)` /
  `@Size(min=3)` cubren los casos límite del spec; `@NotBlank` cubre
  vacío/ausente.
- **Alternativas descartadas**:
  - Serializar la entidad `Usuario`/`Participante` directamente con `@JsonIgnore`
    en `password`: frágil (un cambio de entidad puede reexponerla) y arrastra
    `created_at`/relaciones LAZY. Rechazada.
  - Validación manual en el service: duplica lo que Bean Validation + el
    `GlobalExceptionHandler` ya dan con el detalle por campo. Rechazada.

### Decisión 5: `PasswordEncoder` como bean en `SecurityConfig`

Se añade `@Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }`
a `SecurityConfig` (único `@Configuration` de seguridad existente).

- **Por qué**: BCrypt es lo que exige `CLAUDE.md`; ubicarlo junto al resto de la
  config de seguridad evita crear otra clase de configuración.
- **Alternativa descartada**: `@Configuration` nueva `PasswordConfig`: fragmenta
  la configuración sin motivo. Rechazada.

### Decisión 6: Verificación con `@SpringBootTest` + `MockMvc` a mano (patrón de `seguridad-jwt`)

`AuthIntegrationTest` (`@SpringBootTest` + `@Transactional`, `MockMvc` vía
`MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())`,
`@TestPropertySource` con `jwt.secret` largo). Cubre: register 201 (+ token
usable, + password no presente, + hash BCrypt en BD, + Participante creado),
register 409, register 400 (password corto y campos vacíos), login 200, login 401
(password mala y username inexistente, mismo mensaje).

- **Por qué**: mismo motivo que en `seguridad-jwt` — el slice `@AutoConfigureMockMvc`
  no está en el classpath de test en Spring Boot 4; `@Transactional` evita tocar
  datos reales; se necesita la BD para verificar el hash y el `Participante`.
- **Nombre `AuthIntegrationTest`** (no `AuthIT`) para que Surefire lo ejecute en
  `mvn test` (el proyecto no tiene Failsafe).

## Risks / Trade-offs

- **`passwordEncoder.matches` con hash "de prueba" mal formado en tests** →
  `matches` devuelve `false` sin lanzar; un test de login 200 debe registrar
  primero vía el endpoint (hash real), no insertar el `Usuario` a mano.
- **Timing attack en login** (`.filter` no ejecuta `matches` si el usuario no
  existe) → diferencia de tiempo teóricamente observable. Aceptado para el
  alcance actual (sin rate-limiting; el spec solo pide mismo *mensaje*).
- **`MethodArgumentNotValidException` cambia de forma entre versiones de Spring**
  → el handler usa `getBindingResult().getFieldErrors()`, API estable en Spring 6/7.
- **El `GlobalExceptionHandler` captura `BadCredentialsException` de Spring
  Security** → si en el futuro el `AuthenticationManager` la lanza en otro punto,
  el 401 saldría con cuerpo estándar; es el comportamiento deseado.
- **Registro no atómico si `participanteRepository.save` falla tras `usuario.save`**
  → mitigado por `@Transactional` (rollback de ambos).
- **`ddl-auto=update`**: las tablas ya existen; insertar filas de test bajo
  `@Transactional` hace rollback y no deja residuo.

## Migration Plan

1. `dto/request/{RegisterRequest,LoginRequest}.java`,
   `dto/response/{RegisterResponse,LoginResponse}.java` (+ DTOs anidados).
2. `repository/ParticipanteRepository.java`.
3. `exception/UsernameAlreadyExistsException.java` +
   `exception/GlobalExceptionHandler.java`.
4. `PasswordEncoder` bean en `SecurityConfig`.
5. `service/AuthService.java` (`register`, `login`, `@Transactional`).
6. `controller/AuthController.java` (`POST /register` 201, `POST /login` 200,
   `@Valid`).
7. `./mvnw clean compile`.
8. `AuthIntegrationTest`; `./mvnw clean test`.
9. Arranque manual: `curl` register → 201 con token; repetir → 409; login ok →
   200; login mal → 401; validación → 400.

**Rollback**: `git revert`. Sin cambios de esquema; las filas creadas en pruebas
manuales se pueden borrar por `username`.

## Open Questions

Ninguna.
