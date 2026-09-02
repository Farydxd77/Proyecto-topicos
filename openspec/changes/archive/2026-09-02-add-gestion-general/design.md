## Context

Ver `proposal.md` — Why. Estado actual relevante:

- `Usuario` (tabla `usuarios`: `id`, `username` único, `password` BCrypt) y
  `Participante` (tabla `participantes`: `id`, `usuario_id` único, `nombre`,
  `apellido`, `ci`) ya existen, con relación 1‑a‑1 vía
  `Participante.usuario` (`@OneToOne(fetch = LAZY)`, FK `usuario_id`). Ver modelo
  de datos completo en CLAUDE.md.
- `UsuarioRepository` tiene `findByUsername`. `ParticipanteRepository` tiene
  `findByUsuarioId(Long)` (añadido por el cambio `add-gestion-perfil`).
- Ya existen los DTOs de salida `UsuarioDto(id, username)` y
  `ParticipanteDto(id, nombre, apellido, ci, username)`; ninguno lleva contraseña.
  Se usan hoy en las respuestas de `/api/auth`.
- `GlobalExceptionHandler` produce el formato de error estándar y ya mapea
  `ResourceNotFoundException` a `404` y la ausencia de token a `401`
  (`JwtAuthenticationEntryPoint`).
- `SecurityConfig` protege todo salvo `/api/auth/**` y `/actuator/health/**`, así
  que `/api/usuarios/**` y `/api/participantes/**` quedan autenticados sin tocar
  configuración.
- El borrador `openspec/specs/gestion-general/spec.md` (escrito a mano, formato no
  canónico) es la fuente de requisitos de esta tarea.

## Goals / Non-Goals

**Goals:**

- Exponer los `GET` de solo lectura de `/api/usuarios` y `/api/participantes`
  descritos en el spec delta, reutilizando los patrones del proyecto: controller
  delgado, `@Service` con constructor injection, Query Methods JPA, errores vía
  `GlobalExceptionHandler`.
- No filtrar nunca la contraseña: se logra reutilizando DTOs que no la contienen.
- Dejar el filtrado por query param simple y predecible (un solo criterio por
  petición).

**Non-Goals (nivel diseño):**

- No se introduce una capa de mapeo/mapper genérica; el mapeo entidad→DTO se hace
  a mano en cada service (2‑4 campos).
- No se añade paginación ni `Pageable` aunque `findAll()` lo permitiría.
- No se crea una excepción nueva: se reutiliza `ResourceNotFoundException`.
- No se toca `SecurityConfig` ni se añaden anotaciones `@PreAuthorize`.

## Decisions

### 1. Dos controllers y dos services, uno por recurso

`UsuarioController` (`/api/usuarios`) + `UsuarioService`, y
`ParticipanteController` (`/api/participantes`) + `ParticipanteService`.
`ParticipanteService` inyecta además `UsuarioRepository` para resolver
`GET /api/usuarios/{id}/participante` (verificar primero que el usuario existe
para distinguir el `404` de "usuario no existe" del de "usuario sin
participante"... ambos son `404`, pero deja el mensaje correcto).

- **Por qué:** coincide con la estructura de paquetes de CLAUDE.md y con el estilo
  de `AuthController`/`AuthService`. El endpoint anidado
  `/api/usuarios/{id}/participante` vive en `UsuarioController` por prefijo de
  ruta, pero delega en `ParticipanteService`.
- **Alternativas descartadas:**
  - Un único `GestionGeneralController`/`Service`: mezcla dos recursos REST
    distintos y crece mal cuando lleguen `POST`/`PUT`.
  - Poner `/api/usuarios/{id}/participante` en `ParticipanteController` con
    `@GetMapping` de ruta absoluta: rompe la coherencia de `@RequestMapping`.

### 2. Reutilizar `UsuarioDto` y `ParticipanteDto` sin modificarlos

Las respuestas usan los `record` existentes. `UsuarioDto` = (`id`, `username`).
`ParticipanteDto` = (`id`, `nombre`, `apellido`, `ci`, `username`), donde
`username` sale de `participante.getUsuario().getUsername()`.

- **Por qué:** ya existen, ya excluyen la contraseña, y su forma cubre exactamente
  lo que pide el spec. Reutilizar evita DTOs casi idénticos y mantiene una sola
  forma de "usuario" y "participante" en toda la API.
- **Alternativas descartadas:**
  - `UsuarioResponse`/`ParticipanteResponse` nuevos con `usuarioId`, `createdAt`,
    etc.: el spec de referencia no pide esos campos aquí; añadirlos sería alcance
    extra. Si más adelante hacen falta, es un cambio aditivo.
  - Devolver entidades JPA directamente: arrastra `password` y relaciones LAZY;
    prohibido por CLAUDE.md (seguridad) y frágil en serialización.

### 3. Mapeo entidad→DTO dentro de métodos `@Transactional(readOnly = true)`

Cada método de servicio que devuelve `ParticipanteDto` navega el `@OneToOne` LAZY
`usuario` para leer `username`; se ejecuta con `@Transactional(readOnly = true)`
para que la sesión de Hibernate siga abierta al acceder a la relación.

- **Por qué:** evita `LazyInitializationException` sin cambiar el `fetch` de la
  entidad (que afectaría a otros flujos como auth).
- **Alternativa descartada:** cambiar `Participante.usuario` a `EAGER` — efecto
  global no deseado. Otra: `@EntityGraph`/join fetch — innecesario para el volumen
  de esta fase.

### 4. Filtros por `@RequestParam(required = false)` con precedencia fija

`GET /api/usuarios` recibe `username` opcional. `GET /api/participantes` recibe
`ci`, `nombre`, `apellido` opcionales. El controller/servicio elige **un** criterio
con precedencia `ci` > `nombre` > `apellido`; si no viene ninguno, devuelve todos.

- **Por qué:** el spec de referencia enumera un query method por criterio, sin
  ninguno combinado. Precedencia fija hace el comportamiento determinista y
  testeable sin `Specification`/Criteria API.
- **Alternativas descartadas:**
  - Filtros combinados (AND) con `Specification` o `@Query`: prohibido `@Query`
    manual por CLAUDE.md, y `Specification` es sobre-ingeniería aquí.
  - Endpoints separados (`/api/participantes/buscar?...`): multiplica rutas para lo
    mismo.
  - `400` si se envía más de un filtro: más estricto de lo que pide el spec y peor
    DX; se prefiere ignorar los de menor precedencia.

### 5. Endpoints de colección siempre devuelven array; los de recurso, objeto o 404

`GET /api/usuarios[?username=]` y `GET /api/participantes[?...]` devuelven siempre
un array JSON (vacío `[]` si no hay resultados), incluida la búsqueda por `ci`.
`GET /api/usuarios/{id}`, `GET /api/participantes/{id}` y
`GET /api/usuarios/{id}/participante` devuelven el objeto o `404`
(`ResourceNotFoundException`).

- **Por qué:** el spec de referencia dice explícitamente "búsqueda sin resultados
  → 200 con []" y "lista vacía → 200 con []"; una colección con forma estable
  (siempre array) es más simple de consumir que una que a veces es objeto y a
  veces `404`. El `{id}` sí es un recurso único: `404` es la semántica correcta.
- **Alternativa descartada:** que `?ci=` y `?username=` devuelvan un objeto único
  o `404` — entra en conflicto con la regla de "[]" y complica el cliente.

### 6. Nuevos Query Methods, sin `@Query`

- `UsuarioRepository`: `List<Usuario> findByUsernameContainingIgnoreCase(String)`.
- `ParticipanteRepository`:
  `List<Participante> findByNombreContainingIgnoreCase(String)`,
  `List<Participante> findByApellidoContainingIgnoreCase(String)`,
  `Optional<Participante> findByCi(String)`.

`findByCi` devuelve `Optional` siguiendo el spec de referencia y la convención
"un resultado → Optional"; el `ci` se trata como único en la práctica (aunque el
esquema no ponga UNIQUE). El servicio convierte ese `Optional` en lista de 0 o 1
elementos para la respuesta de `?ci=`.

- **Alternativa descartada:** `List<Participante> findAllByCi(String)` — más
  defensivo ante `ci` duplicados, pero se aparta del texto del spec de referencia;
  si aparecen `ci` duplicados reales se cambia a `findAllByCi` (cambio aislado).

### 7. Seguridad: sin cambios de configuración

`/api/usuarios/**` y `/api/participantes/**` caen bajo
`anyRequest().authenticated()`. Sin token → `401` por el entry point JWT
existente. No se añaden roles ni `@PreAuthorize` (fuera de alcance por proposal).

### 8. `gestion-general` como spec canónico nuevo

El delta usa `## ADDED Requirements`. Al archivar, OpenSpec generará
`openspec/specs/gestion-general/spec.md` en formato canónico, sustituyendo el
borrador manual actual.

## Risks / Trade-offs

- **`findByCi` lanza `IncorrectResultSizeDataAccessException` si hay >1 fila con el
  mismo `ci`** (el esquema no fuerza UNIQUE) → Mitigación: se asume `ci` único en
  la práctica en esta fase; si se detecta lo contrario, migrar a `findAllByCi`
  (cambio local en repo + servicio, sin tocar el contrato del endpoint).
- **`findAll()` sin paginación** puede devolver listas grandes a futuro →
  Mitigación: aceptable en esta fase (volumen bajo); añadir `Pageable` será
  aditivo y no rompe el contrato de "array".
- **Endpoints abiertos a cualquier autenticado** exponen el directorio completo de
  usuarios/participantes (sin contraseñas) → Mitigación: es una decisión explícita
  del proposal; cuando existan roles admin se restringe con `@PreAuthorize` sin
  cambiar la forma de las respuestas.
- **Precedencia de filtros poco descubrible** para el cliente → Mitigación:
  documentada en el spec delta ("Se envían varios parámetros de filtro"); el caso
  normal es enviar uno solo.
- **`ddl-auto=update`**: no hay columnas ni tablas nuevas, sin riesgo de
  migración.
