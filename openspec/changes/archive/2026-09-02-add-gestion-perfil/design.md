## Context

Ver `proposal.md` — Why. Estado actual relevante:

- `Usuario` (tabla `usuarios`) y `Participante` (tabla `participantes`) ya existen,
  con relación 1‑a‑1 vía `Participante.usuario` (`@OneToOne`, FK `usuario_id`
  única). Ver modelo de datos en CLAUDE.md.
- El token JWT lleva el `username` como subject (`JwtUtil.getUsername`) y
  `JwtAuthFilter` deja un `UserDetails` en el `SecurityContext`. El username es
  único, por lo que sirve para resolver al usuario autenticado.
- `AuthService.register` ya crea `Usuario` + `Participante` juntos y usa
  `PasswordEncoder` (BCrypt, bean en `SecurityConfig`).
- `GlobalExceptionHandler` ya produce el formato de error estándar y maneja
  `MethodArgumentNotValidException` (400), `UsernameAlreadyExistsException` (409) y
  `BadCredentialsException` (401). No hay handler de 404 todavía.
- `ParticipanteRepository` está vacío; `UsuarioRepository` tiene `findByUsername`.
- `SecurityConfig` protege todo salvo `/api/auth/**` y `/actuator/health/**`, así
  que `/api/perfil/**` queda autenticado sin tocar configuración.

## Goals / Non-Goals

**Goals:**

- Exponer `GET/PUT /api/perfil/me`, `PUT /api/perfil/me/username` y
  `PUT /api/perfil/me/password` operando siempre sobre el usuario del token.
- Reutilizar patrones existentes: controller delgado, `@Service` con constructor
  injection, DTOs `record`, validación con anotaciones Jakarta, errores vía
  `GlobalExceptionHandler`.
- No filtrar nunca la contraseña en ninguna respuesta.

**Non-Goals:**

- No se implementa `DELETE /api/perfil/me` ni baja lógica.
- No se permite cambiar el `ci` (decisión del usuario; alineado con CLAUDE.md).
- No se pide la contraseña actual para confirmar cambios de username/password
  (queda para una iteración futura de endurecimiento).
- No se invalidan tokens ya emitidos tras cambiar username o password.
- Sin roles ni gestión de perfiles ajenos.

## Decisions

### 1. Resolver el usuario autenticado por `username` desde el `SecurityContext`

`PerfilService` obtiene el `username` con
`SecurityContextHolder.getContext().getAuthentication().getName()` y carga el
`Usuario` con `usuarioRepository.findByUsername(...)`. 

- **Por qué:** es lo que ya hay en el token y en el filtro JWT; no requiere cambios
  en `JwtUtil` ni en el filtro.
- **Alternativas descartadas:**
  - Inyectar `@AuthenticationPrincipal` en el controller y pasarlo al service:
    acopla el service a Spring Security MVC; se prefiere que el controller quede
    trivial y el service resuelva identidad.
  - Meter el `userId` en el JWT: cambio transversal en emisión/validación de
    tokens, innecesario para esta tarea.

### 2. Un único `PerfilService` con métodos `@Transactional`

Métodos: `obtenerMiPerfil()`, `actualizarMiPerfil(ActualizarPerfilRequest)`,
`cambiarUsername(CambiarUsernameRequest)`, `cambiarPassword(CambiarPasswordRequest)`.
Cada mutación es `@Transactional`; la lectura `@Transactional(readOnly = true)`
(el `@OneToOne` LAZY a `Usuario` se navega desde `Participante`, y a la inversa se
carga el participante por query explícita).

- **Por qué:** el spec agrupa todo bajo la capacidad "perfil"; mantener una sola
  clase evita dispersión.
- **Alternativa descartada:** reutilizar `AuthService` — mezcla responsabilidades
  de autenticación con gestión de cuenta.

### 3. Nuevo query method `ParticipanteRepository.findByUsuarioId(Long)`

Devuelve `Optional<Participante>`. Sigue la convención del proyecto (Query Methods,
`Optional` para un resultado, sin `@Query`). El nombre `findByUsuarioId` resuelve
la propiedad anidada `usuario.id`.

- **Alternativa descartada:** `findByUsuario(Usuario)` — obliga a tener la entidad
  `Usuario` en mano; `usuarioId` es más directo y coincide con el texto del spec
  de referencia (`findByUsuarioId`).

### 4. `PerfilResponse` como DTO de salida único para GET y los PUT que devuelven perfil

`record PerfilResponse(Long id, Long usuarioId, String username, String nombre,
String apellido, String ci, Instant createdAt)`. `createdAt` sale de
`Participante` (hereda `BaseEntity`). No se añade ningún campo derivado de
`password`.

- **Por qué:** una sola forma de respuesta simplifica el cliente y el test
  "nunca incluye contraseña".
- **Alternativa descartada:** reutilizar `ParticipanteDto` — no tiene `usuarioId`
  ni `createdAt` y se usa en las respuestas de auth; cambiarlo tendría efecto
  colateral.

### 5. DTOs de entrada con validación Jakarta; el 400 lo produce el handler existente

- `ActualizarPerfilRequest(@NotBlank @Size(max=100) String nombre,
  @NotBlank @Size(max=100) String apellido)` — sin campo `ci` (se ignora aunque el
  cliente lo mande, por no estar en el record).
- `CambiarUsernameRequest(@NotBlank @Size(min=3, max=50) String username)`.
- `CambiarPasswordRequest(@NotBlank @Size(min=8) String password)`.

Coincide con las restricciones de `RegisterRequest`. El
`MethodArgumentNotValidException` ya se traduce a 400 con formato estándar.

### 6. Conflicto de username: reutilizar `UsernameAlreadyExistsException` (409)

`cambiarUsername` comprueba con `usuarioRepository.findByUsername(nuevo)`: si
existe y su `id` != el del usuario actual, lanza `UsernameAlreadyExistsException`
(ya mapeada a 409). Si el nuevo username es igual al actual, no-op con 200.

- **Alternativa descartada:** una excepción nueva — ya existe uná con el mapeo y el
  mensaje correctos.

### 7. Nueva `ResourceNotFoundException` + handler 404

`RuntimeException` simple en `exception/`. Handler nuevo en
`GlobalExceptionHandler` que devuelve 404 con `standardBody`. Se usa cuando el
usuario del token no tiene participante vinculado (caso defensivo; en la práctica
`register` siempre crea ambos).

- **Por qué:** el proyecto aún no tiene 404 y otros endpoints futuros (grupos,
  gastos) lo necesitarán; se introduce aquí de forma mínima y reutilizable.

### 8. El caso "403 Forbidden" del spec de referencia se cubre por diseño, no por código

Todos los endpoints son `/me` y resuelven identidad solo desde el token: no hay
parámetro para apuntar a otra cuenta, así que el acceso cruzado es imposible por
construcción. El spec delta lo recoge como requisito de "Aislamiento entre cuentas"
en vez de un 403 alcanzable. Una petición sin token válido da 401 (entry point
JWT existente).

## Risks / Trade-offs

- **No se pide la contraseña actual para cambiar username/password** → riesgo si un
  token es robado. Mitigación: fuera de alcance explícito; documentado como mejora
  futura (pedir `passwordActual` y/o rotar el secreto/versión del token).
- **Tokens siguen válidos tras cambiar el username** → el `subject` del token viejo
  deja de resolver a un usuario y esas peticiones darán 401 al no encontrar el
  `UserDetails`. Trade-off aceptable: el usuario vuelve a iniciar sesión. Se
  documenta; no se añade lista de revocación.
- **`ci` inmutable vía API** pero editable directamente en BD → aceptable en esta
  fase; si luego se permite corregir el `ci`, será un cambio aditivo (nuevo campo
  en `ActualizarPerfilRequest` + escenario en el spec).
- **`ddl-auto=update`** no crea columnas nuevas (no hay ninguna), así que no hay
  riesgo de migración.
