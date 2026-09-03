## Why

El backend ya resuelve autenticación, perfil y consulta de usuarios/participantes,
pero no existe todavía ninguna forma de agrupar personas: sin grupos no hay dónde
registrar gastos ni calcular balances, que son el núcleo del producto. Las
entidades `Grupo`, `GrupoParticipante` y `GrupoParticipanteId` ya están mapeadas
pero no las usa nadie (no hay repositorios, servicios ni endpoints). Además existe
un borrador de requisitos escrito a mano en `openspec/specs/grupos/spec.md`; esta
tarea lo formaliza e implementa.

## What Changes

- Nuevo endpoint `POST /api/grupos`: crea un grupo con el participante del usuario
  autenticado como creador y lo agrega automáticamente como primer miembro.
  Devuelve `201` con los datos del grupo y su lista de miembros.
- Nuevo endpoint `GET /api/grupos`: devuelve `200` con los grupos donde el usuario
  autenticado es miembro; `[]` si no pertenece a ninguno.
- Nuevo endpoint `GET /api/grupos/{id}`: devuelve `200` con `nombre`,
  `descripcion`, `creador` y lista de miembros si el usuario es miembro; `403` si
  no lo es; `404` si el grupo no existe.
- Nuevo endpoint `PUT /api/grupos/{id}`: solo el creador actualiza `nombre` y/o
  `descripcion`. Devuelve `200`; `403` si el solicitante es miembro pero no creador.
- Nuevo endpoint `DELETE /api/grupos/{id}`: solo el creador elimina el grupo y sus
  filas en `grupo_participantes`. Devuelve `204`; `403` si no es el creador.
- Nuevo endpoint `POST /api/grupos/{id}/miembros`: solo el creador agrega un
  participante por `participanteId`. Devuelve `201` con la lista actualizada de
  miembros; `409` si el participante ya es miembro; `404` si el participante no
  existe.
- Nuevo endpoint `DELETE /api/grupos/{id}/miembros/{participanteId}`: solo el
  creador quita a un miembro. Devuelve `204`; `400` si el `participanteId` es el
  del creador (el creador no puede quitarse a sí mismo).
- Nuevos repositorios `GrupoRepository` y `GrupoParticipanteRepository` con Query
  Methods JPA.
- Nuevo `GrupoService` con las reglas de membresía y de autoría (creador).
- Nuevo `GrupoController` bajo `/api/grupos`.
- Nuevos DTOs de entrada (`CrearGrupoRequest`, `ActualizarGrupoRequest`,
  `AgregarMiembroRequest`) y de salida (`GrupoResponse`, `GrupoResumenDto`).
- Nuevas excepciones `ForbiddenOperationException` (→ `403`),
  `ConflictException` (→ `409`) y `BadRequestException` (→ `400`), mapeadas al
  formato de error estándar en `GlobalExceptionHandler`.
- Se agrega a `Grupo` la colección `miembros` (`@OneToMany` a `GrupoParticipante`)
  para permitir el Query Method `findByMiembrosParticipanteId` y la cascada de
  borrado. Sin cambios de esquema en base de datos.
- Todos los endpoints requieren JWT válido; sin cambios en `SecurityConfig`
  (`anyRequest().authenticated()` ya los cubre).

## Non-Goals

- No se implementan gastos, balances ni liquidación (tareas siguientes).
- No se implementan roles dentro del grupo: solo la distinción creador vs. miembro.
- No se implementa transferencia del rol de creador a otro miembro.
- Un miembro no puede abandonar el grupo por su cuenta; solo el creador lo quita.
- No se agregan miembros en lote ni por `username`/`ci` (solo por `participanteId`).
- No hay paginación, ordenamiento ni búsqueda de grupos.
- No se toca `frontend/`: las pantallas de grupos siguen fuera de alcance según
  CLAUDE.md, y el frontend continúa contra MSW.
- No se toca `SecurityConfig` ni se añaden anotaciones `@PreAuthorize`.

## Capabilities

### New Capabilities

- `grupos`: creación y administración de grupos de viaje por parte de usuarios
  autenticados — crear un grupo, listar los grupos propios, ver el detalle con sus
  miembros, editar y eliminar el grupo, y agregar o quitar miembros. Solo el
  creador administra; el resto de miembros solo consulta.

### Modified Capabilities

<!-- Ninguna: no cambian los requisitos de capacidades existentes. -->

## Impact

- **Código nuevo**:
  - `repository/GrupoRepository`, `repository/GrupoParticipanteRepository`.
  - `service/GrupoService`.
  - `controller/GrupoController`.
  - `dto/request/CrearGrupoRequest`, `dto/request/ActualizarGrupoRequest`,
    `dto/request/AgregarMiembroRequest`.
  - `dto/response/GrupoResponse`, `dto/response/GrupoResumenDto`.
  - `exception/ForbiddenOperationException`, `exception/ConflictException`,
    `exception/BadRequestException`.
  - `test/.../grupos/GrupoControllerTest`, `test/.../grupos/GrupoMiembrosControllerTest`.
- **Código modificado**:
  - `entity/Grupo`: nueva colección `miembros`
    (`@OneToMany(mappedBy = "grupo", cascade = ALL, orphanRemoval = true)`).
  - `exception/GlobalExceptionHandler`: tres handlers nuevos (`403`, `409`, `400`).
- **DTOs reutilizados**: `dto/response/ParticipanteDto` para representar creador y
  miembros; no expone contraseña.
- **APIs**: nueva ruta base `/api/grupos/**`, protegida por JWT sin tocar
  `SecurityConfig`.
- **Base de datos**: sin cambios de esquema. Las tablas `grupos` y
  `grupo_participantes` ya las genera Hibernate (`ddl-auto=update`) a partir de las
  entidades existentes; la colección `miembros` es solo el lado inverso de una FK
  que ya existe.
- **Dependencias**: ninguna nueva.
- **Specs**: al archivar el cambio, el delta reemplaza el borrador manual de
  `openspec/specs/grupos/spec.md` por un spec en formato canónico.
