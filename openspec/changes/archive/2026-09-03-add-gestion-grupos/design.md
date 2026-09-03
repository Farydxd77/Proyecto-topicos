## Context

Ver `proposal.md` — Why. Estado actual relevante:

- Las entidades del modelo de grupos ya están mapeadas y **no las usa nadie**:
  `Grupo` (tabla `grupos`: `id`, `nombre`, `descripcion`, `creador_id` FK a
  `participantes`, hereda `created_at`/`updated_at` de `BaseEntity`),
  `GrupoParticipante` (tabla `grupo_participantes`, `@EmbeddedId` con
  `GrupoParticipanteId(grupoId, participanteId)`, `@MapsId` hacia `Grupo` y
  `Participante`, más `joined_at` con `@CreationTimestamp`). No existen
  `GrupoRepository` ni `GrupoParticipanteRepository`.
- `Grupo.creador` es `@ManyToOne(fetch = LAZY)` con `@JoinColumn(name =
  "creador_id", nullable = true)`. `Grupo` **no tiene** hoy ninguna colección de
  miembros.
- `Participante` es 1‑a‑1 con `Usuario` (`@OneToOne` LAZY, FK `usuario_id` única).
  `ParticipanteRepository.findByUsuarioId(Long)` ya existe.
- `PerfilService` ya resuelve el usuario autenticado leyendo el `username` de
  `SecurityContextHolder.getContext().getAuthentication()` y consultando
  `UsuarioRepository.findByUsername`. Es el patrón a seguir.
- `GlobalExceptionHandler` produce el formato de error estándar y hoy mapea
  `MethodArgumentNotValidException` → `400`, `UsernameAlreadyExistsException` →
  `409`, `BadCredentialsException` → `401` y `ResourceNotFoundException` → `404`.
  **No hay mapeo para `403` ni para conflictos genéricos.**
- `SecurityConfig` aplica `anyRequest().authenticated()` salvo `/api/auth/**` y
  `/actuator/health/**`; `JwtAuthenticationEntryPoint` ya devuelve el `401` con el
  formato estándar.
- `dto/response/ParticipanteDto(id, nombre, apellido, ci, username)` ya existe y no
  contiene contraseña.
- Ver modelo de datos completo y convenciones en CLAUDE.md.

## Goals / Non-Goals

**Goals:**

- Implementar los siete endpoints del spec delta reutilizando los patrones ya
  establecidos: controller delgado, `@Service` con constructor injection, Query
  Methods JPA sin `@Query`, errores centralizados en `GlobalExceptionHandler`.
- Concentrar en **un solo lugar** las dos reglas de autorización del dominio
  ("es miembro" y "es el creador"), para que gastos y balances las reutilicen tal
  cual cuando lleguen.
- Que los códigos `403`, `409` y `400` de negocio salgan con el mismo formato de
  error estándar que ya usan `401` y `404`.
- No cambiar el esquema de base de datos.

**Non-Goals (nivel diseño):**

- No se introduce un mapper genérico ni MapStruct; el mapeo entidad→DTO se hace a
  mano en `GrupoService` (igual que en `ParticipanteService`).
- No se usan `@PreAuthorize` ni expresiones SpEL de seguridad: la autorización es
  de dominio (depende de filas de `grupo_participantes`), no de roles.
- No se añade paginación ni `Pageable` a `GET /api/grupos`.
- No se optimiza con `@EntityGraph` ni join fetch; el volumen de esta fase no lo
  justifica.

## Decisions

### 1. Añadir `miembros` como `@OneToMany` en `Grupo`

Se agrega a `Grupo`:

```java
@OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
@Builder.Default
private List<GrupoParticipante> miembros = new ArrayList<>();
```

- **Por qué:** habilita las dos cosas que el diseño necesita y hoy no existen:
  (a) el Query Method `findByMiembrosParticipanteId(Long)` que resuelve
  `GET /api/grupos` en una sola consulta derivada, sin `@Query` manual (prohibido
  por CLAUDE.md); y (b) el borrado en cascada de `grupo_participantes` al eliminar
  el grupo, que el spec exige, mediante `cascade = ALL` + `orphanRemoval`.
  Es el **lado inverso** de una FK que ya existe (`grupo_participantes.grupo_id`):
  no añade ninguna columna ni tabla, por lo que `ddl-auto=update` no tiene nada que
  migrar.
- **Cuidado con Lombok:** `Grupo` usa `@SuperBuilder`; la colección lleva
  `@Builder.Default` para que el builder no la deje en `null`. No se usa `@Data`
  (prohibido por CLAUDE.md en entidades con relaciones).
- **Alternativas descartadas:**
  - Dejar `Grupo` sin colección y listar los grupos del usuario en dos pasos
    (`GrupoParticipanteRepository.findByParticipanteId` → extraer `grupoId` →
    `grupoRepository.findAllById`): funciona, pero duplica viajes a la base y
    obliga a borrar las membresías a mano antes de borrar el grupo, con riesgo de
    dejar filas huérfanas si falla a medias.
  - `@ManyToMany` entre `Grupo` y `Participante` con `@JoinTable`: perdería
    `joined_at` y la entidad `GrupoParticipante`, que el modelo de CLAUDE.md ya
    define y que gastos necesitará.

### 2. Tres excepciones de negocio nuevas, mapeadas en `GlobalExceptionHandler`

Se crean `ForbiddenOperationException` (→ `403`), `ConflictException` (→ `409`) y
`BadRequestException` (→ `400`), todas `RuntimeException` con constructor de
mensaje, y sus tres `@ExceptionHandler` en `GlobalExceptionHandler` reutilizando el
helper `standardBody(...)` ya existente.

- **Por qué:** el spec exige `403`, `409` y `400` con el **formato de error
  estándar**, y hoy no existe ninguna vía para producirlos desde un servicio. Son
  excepciones de dominio genéricas y reutilizables: gastos y balances necesitarán
  exactamente las mismas.
- **Por qué NO `org.springframework.security.access.AccessDeniedException`:** la
  lanza el servicio dentro del controlador, pero la traduce
  `ExceptionTranslationFilter` de Spring Security **antes** de llegar al
  `@RestControllerAdvice`, así que el cuerpo no tendría el formato estándar del
  proyecto. Habría que además configurar un `AccessDeniedHandler` en
  `SecurityConfig`, y el proposal declara no tocar `SecurityConfig`.
- **Alternativas descartadas:**
  - `@ResponseStatus(HttpStatus.FORBIDDEN)` sobre la excepción: devuelve el cuerpo
    por defecto de Spring, no el formato estándar.
  - `ResponseStatusException` desde el servicio: mezcla detalles HTTP en la capa de
    negocio y también produce un cuerpo distinto.
  - Reutilizar `UsernameAlreadyExistsException` para el `409` de "ya es miembro":
    su nombre miente sobre el caso; se prefiere `ConflictException` genérica.

### 3. `GrupoService` centraliza la resolución del solicitante y las dos guardas

`GrupoService` inyecta `GrupoRepository`, `GrupoParticipanteRepository`,
`ParticipanteRepository` y `UsuarioRepository`, y expone tres privados:

- `participanteActual()`: lee el `username` de `SecurityContextHolder`, resuelve
  `Usuario` y luego `ParticipanteRepository.findByUsuarioId`. Mismo patrón que
  `PerfilService.usuarioActual()`.
- `grupoDondeEsMiembro(Long grupoId, Participante solicitante)`: `findById` →
  `404` si no existe; `GrupoParticipanteRepository.findByGrupoIdAndParticipanteId`
  → `403` si no hay fila. Devuelve el `Grupo`.
- `grupoDondeEsCreador(Long grupoId, Participante solicitante)`: `findById` →
  `404`; compara `grupo.getCreador().getId()` con el id del solicitante → `403` si
  no coincide.

Cada método público llama a la guarda que le corresponde **antes** de tocar nada.

- **Por qué:** el orden `404` antes que `403` es el que pide el spec (grupo
  inexistente da `404` aunque el solicitante no sea miembro). Tener las guardas
  como métodos con nombre hace que cada endpoint sea una línea de intención
  (`grupoDondeEsCreador(id, yo)`) y que la regla no se pueda olvidar en un
  endpoint nuevo.
- **Nota sobre el orden 404/403 en editar y eliminar:** para el creador se usa
  directamente `grupoDondeEsCreador`, que **no** exige ser miembro primero; el
  creador siempre lo es por construcción, y un no miembro cae igualmente en `403`,
  que es lo que el spec pide para ese caso.
- **Alternativas descartadas:**
  - Un `GrupoSecurityService` aparte + `@PreAuthorize("@grupoSecurity.esMiembro(...)")`:
    la expresión SpEL oculta la regla en un string, se evalúa antes de saber si el
    grupo existe (rompe el `404` antes que `403`) y su fallo produce un cuerpo de
    error distinto (ver decisión 2).
  - Resolver el `Participante` en el controller y pasarlo al servicio: filtra
    detalles de autenticación al controller y repite el mismo bloque en siete
    endpoints.

### 4. Dos formas de salida: `GrupoResumenDto` para la lista, `GrupoResponse` para el detalle

- `GrupoResumenDto(Long id, String nombre, String descripcion, ParticipanteDto creador)`
  para `GET /api/grupos`.
- `GrupoResponse(Long id, String nombre, String descripcion, ParticipanteDto creador,
  List<ParticipanteDto> miembros)` para `POST`, `GET /{id}`, `PUT` y
  `POST /{id}/miembros`.

Ambos `record`, y ambos reutilizan `ParticipanteDto` para creador y miembros.

- **Por qué:** el spec pide la lista de miembros solo en el detalle y en las
  respuestas de creación/edición; incluirla en la lista de grupos obligaría a
  cargar todas las membresías de todos los grupos del usuario en cada llamada a
  `GET /api/grupos`. Reutilizar `ParticipanteDto` mantiene una única forma de
  "participante" en toda la API y garantiza que no se filtre la contraseña.
- **`POST /{id}/miembros` devuelve `GrupoResponse` completo** aunque el spec solo
  exija "la lista actualizada de miembros": es un superconjunto, evita un DTO más y
  le ahorra al cliente un `GET` posterior.
- **Alternativas descartadas:**
  - Un único DTO con `miembros` a veces `null`: contrato ambiguo para el cliente.
  - Devolver entidades JPA: arrastra relaciones LAZY y `Usuario.password`;
    prohibido por CLAUDE.md.

### 5. Todo método público del servicio es `@Transactional`; el mapeo ocurre dentro

Las lecturas son `@Transactional(readOnly = true)` y las escrituras
`@Transactional`. El mapeo entidad→DTO se hace **dentro** de la transacción.

- **Por qué:** `Grupo.creador`, `Grupo.miembros`, `GrupoParticipante.participante` y
  `Participante.usuario` son todas LAZY; construir `ParticipanteDto` navega hasta
  `usuario.getUsername()`. Sin transacción abierta se produce
  `LazyInitializationException`. Es el mismo motivo y la misma solución que en
  `ParticipanteService`.
- **Alternativa descartada:** poner esas relaciones en `EAGER` — efecto global que
  afectaría a auth y perfil.

### 6. Altas y bajas de miembros a través de la colección `miembros`

Agregar un miembro construye un `GrupoParticipante` (con su
`GrupoParticipanteId`), lo añade a `grupo.getMiembros()` y guarda el grupo; la
cascada persiste la fila. Quitar un miembro lo elimina de la colección y
`orphanRemoval` borra la fila. Eliminar el grupo es un solo
`grupoRepository.delete(grupo)`.

- **Por qué:** una sola forma de mutar la membresía, coherente con la cascada que
  ya se necesita para el borrado del grupo, y sin riesgo de que la colección en
  memoria quede desincronizada con la base dentro de la misma transacción (lo que
  haría que la respuesta devolviera una lista de miembros desactualizada).
- **Alternativa descartada:** `grupoParticipanteRepository.save(...)` /
  `.delete(...)` directos: más explícitos, pero dejan `grupo.getMiembros()` obsoleto
  en la misma transacción y obligan a recargar antes de mapear la respuesta.

### 7. Query Methods necesarios (sin `@Query`)

`GrupoRepository extends JpaRepository<Grupo, Long>`:

- `List<Grupo> findByMiembrosParticipanteId(Long participanteId)` — habilitado por
  la decisión 1.

`GrupoParticipanteRepository extends JpaRepository<GrupoParticipante, GrupoParticipanteId>`:

- `Optional<GrupoParticipante> findByGrupoIdAndParticipanteId(Long grupoId, Long participanteId)`
- `List<GrupoParticipante> findByGrupoId(Long grupoId)`

`findById` y `existsById` vienen de `JpaRepository`. `ParticipanteRepository` no
necesita métodos nuevos: `findById` y `findByUsuarioId` ya existen.

- **Por qué:** cubre exactamente los tres accesos que el servicio necesita, todos
  derivables del nombre. `findByGrupoIdAndParticipanteId` devuelve `Optional`
  (un resultado) y los otros dos `List`, según la convención de CLAUDE.md.

### 8. `grupos` como spec canónico nuevo

El delta usa `## ADDED Requirements` con `## Purpose`. Al archivar, OpenSpec
generará `openspec/specs/grupos/spec.md` en formato canónico, sustituyendo el
borrador manual actual. Mismo camino que siguió `gestion-general`.

### 9. Pruebas: integración con MockMvc, dos archivos

`src/test/java/com/cuentasclaras/backend/grupos/GrupoControllerTest.java` (crear,
listar, detalle, editar, eliminar) y `GrupoMiembrosControllerTest.java` (agregar y
quitar miembros). Ambos siguen el patrón ya usado en
`gestiongeneral/ParticipanteControllerTest`: `@SpringBootTest`, `@Transactional`,
`@TestPropertySource` con `jwt.secret` de prueba, `MockMvcBuilders` con
`springSecurity()`, y usuarios creados vía `POST /api/auth/register` con sufijo
`System.nanoTime()` para que no colisionen entre ejecuciones.

- **Por qué:** casi todas las reglas de esta capacidad son de autorización y solo
  se observan en el borde HTTP (`403` vs `404` vs `409`); un test unitario del
  servicio con mocks no verificaría los códigos ni el formato del cuerpo. Dividir
  en dos archivos evita un test de 600 líneas y separa los dos grupos de endpoints.
- **Alternativa descartada:** `@WebMvcTest` con `GrupoService` mockeado — no
  ejercita las guardas reales, que son justamente lo que hay que probar.

## Risks / Trade-offs

- **`403` en lugar de `404` para un no miembro revela que el grupo existe** →
  Mitigación: es lo que el spec exige explícitamente; el id es un entero
  secuencial y no expone datos del grupo. Si más adelante se quiere ocultar la
  existencia, se cambia a `404` en `grupoDondeEsMiembro` (un solo punto).
- **`cascade = ALL` en `Grupo.miembros` borra membresías al borrar el grupo, pero
  la tabla `gastos` tiene FK `grupo_id`** → borrar un grupo con gastos fallaría con
  violación de FK. Mitigación: hoy no existe ningún endpoint que cree gastos, así
  que el caso es inalcanzable; cuando llegue la capacidad de gastos habrá que
  decidir explícitamente (bloquear el borrado con `409` o cascadear) y ese cambio
  es local a `GrupoService`.
- **`Grupo.creador` es `nullable = true` en la entidad** → un grupo sin creador
  haría fallar `grupo.getCreador().getId()` con `NullPointerException`. Mitigación:
  todo grupo creado por esta capacidad tiene creador; las guardas tratan un
  `creador` nulo como "no eres el creador" (`403`) en vez de propagar el NPE. No se
  cambia la nulabilidad de la columna para no forzar una migración con
  `ddl-auto=update`.
- **N+1 al listar grupos y miembros** (`findByMiembrosParticipanteId` carga los
  grupos y luego cada `creador` LAZY se resuelve por separado) → Mitigación:
  volumen bajo en esta fase; si molesta, se añade `@EntityGraph` sin cambiar el
  contrato del endpoint.
- **`@Builder.Default` olvidado en `miembros`** dejaría la colección en `null` al
  construir por builder y produciría `NullPointerException` al agregar el creador
  → Mitigación: es un punto explícito en la tarea correspondiente y queda cubierto
  por el test de creación de grupo.
- **`ddl-auto=update`**: las tablas `grupos` y `grupo_participantes` ya se generan
  desde las entidades existentes y la colección nueva es solo el lado inverso de
  una FK existente; no hay cambios de esquema ni riesgo de migración.

## Migration Plan

No hay migración de datos ni de esquema: no se crean, renombran ni borran columnas
o tablas, y no hay contratos existentes que romper (todos los endpoints son
nuevos). El despliegue es el de siempre (`mvnw.cmd test` y arrancar la app). El
rollback es revertir el commit; como no se escribió nada en el esquema, no queda
estado que limpiar salvo las filas de `grupos`/`grupo_participantes` creadas
durante el uso.
