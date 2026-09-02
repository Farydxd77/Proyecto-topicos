## Why

Hoy, tras registrarse e iniciar sesión, un usuario autenticado solo puede operar
sobre su propia cuenta (`/api/perfil/me`). No existe forma de listar ni consultar
usuarios y participantes del sistema, algo que la fase 2 necesita para armar
selectores de miembros de grupo, búsquedas y futuras pantallas de administración.
Ya hay un borrador de requisitos escrito a mano en
`openspec/specs/gestion-general/spec.md`; esta tarea lo formaliza e implementa.

## What Changes

- Nuevo endpoint `GET /api/usuarios`: devuelve la lista de todos los usuarios
  (`id`, `username`), nunca la contraseña.
- Nuevo endpoint `GET /api/usuarios?username={texto}`: filtra la lista de usuarios
  por coincidencia parcial de `username` (case-insensitive). Sin coincidencias
  devuelve `200` con `[]`.
- Nuevo endpoint `GET /api/usuarios/{id}`: devuelve un usuario por id, o `404` con
  el formato de error estándar si no existe.
- Nuevo endpoint `GET /api/usuarios/{id}/participante`: devuelve el participante
  vinculado a ese usuario, o `404` si el usuario no existe o no tiene participante.
- Nuevo endpoint `GET /api/participantes`: devuelve la lista de todos los
  participantes (`id`, `nombre`, `apellido`, `ci`, `username`).
- Nuevo endpoint `GET /api/participantes?nombre={texto}` y
  `?apellido={texto}`: filtran por coincidencia parcial case-insensitive.
- Nuevo endpoint `GET /api/participantes?ci={texto}`: filtra por `ci` exacto.
- Nuevo endpoint `GET /api/participantes/{id}`: devuelve un participante por id, o
  `404` si no existe.
- Nuevos `UsuarioService` y `ParticipanteService` de solo lectura.
- Nuevos `UsuarioController` y `ParticipanteController`.
- Nuevos query methods en `UsuarioRepository` y `ParticipanteRepository`.
- Todos los endpoints requieren JWT válido (ya cubierto por
  `anyRequest().authenticated()` en `SecurityConfig`; sin cambios de config).

## Non-Goals

- No se implementan roles ni permisos de administrador (los endpoints quedan
  abiertos a cualquier usuario autenticado, listos para restringir después).
- No se crean, editan ni eliminan usuarios/participantes desde estos endpoints
  (crear es `POST /api/auth/register`; editar el propio es `PUT /api/perfil/me`).
- No hay paginación, ordenamiento ni filtros combinados avanzados.
- No se filtra la visibilidad por "grupos propios del usuario" todavía.
- Sin cambios en `frontend/`.

## Capabilities

### New Capabilities

- `gestion-general`: consulta de solo lectura de usuarios y participantes para
  cualquier usuario autenticado — listar todos, buscar por id, y filtrar por
  `username` (usuarios) o por `nombre` / `apellido` / `ci` (participantes), más el
  participante vinculado a un usuario. Nunca expone contraseñas.

### Modified Capabilities

<!-- Ninguna: no cambian requisitos de capacidades existentes. -->

## Impact

- **Código nuevo**: `controller/UsuarioController`, `controller/ParticipanteController`,
  `service/UsuarioService`, `service/ParticipanteService`.
- **Código modificado**:
  - `repository/UsuarioRepository`: agregar
    `findByUsernameContainingIgnoreCase(String) -> List<Usuario>`.
  - `repository/ParticipanteRepository`: agregar
    `findByNombreContainingIgnoreCase(String) -> List<Participante>`,
    `findByApellidoContainingIgnoreCase(String) -> List<Participante>`,
    `findByCi(String) -> Optional<Participante>`.
- **DTOs**: se reutilizan `dto/response/UsuarioDto` (`id`, `username`) y
  `dto/response/ParticipanteDto` (`id`, `nombre`, `apellido`, `ci`, `username`)
  sin modificarlos; ninguno incluye contraseña.
- **Excepciones**: se reutiliza `ResourceNotFoundException` (ya mapeada a `404`
  con formato estándar en `GlobalExceptionHandler`).
- **APIs**: nuevas rutas base `/api/usuarios/**` y `/api/participantes/**`,
  protegidas por JWT sin tocar `SecurityConfig`.
- **Base de datos**: sin cambios de esquema (usa `usuarios` y `participantes`).
- **Dependencias**: ninguna nueva.
- **Specs**: esta tarea reemplaza el borrador manual de
  `openspec/specs/gestion-general/spec.md` por un spec en formato canónico al
  archivar el cambio.
