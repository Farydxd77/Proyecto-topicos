## Why

El backend ya resuelve autenticación, perfil, consulta de usuarios/participantes y
administración de grupos, pero un grupo todavía no sirve para nada: no hay forma de
registrar los gastos que se reparten entre sus miembros, que es el núcleo del
producto. Las entidades `Gasto`, `GastoParticipante` y `GastoParticipanteId` ya
están mapeadas pero no las usa nadie (no hay repositorios, servicio ni endpoints).
Existe un borrador de requisitos escrito a mano en `openspec/specs/gastos/spec.md`;
esta tarea lo formaliza e implementa.

## What Changes

- Nuevo endpoint `POST /api/grupos/{id}/gastos`: cualquier miembro registra un
  gasto (`descripcion`, `monto`, `pagadorId`, `fecha`). El monto se divide
  equitativamente entre **todos los miembros del grupo en ese momento**; el pagador
  absorbe los centavos sobrantes del redondeo. Devuelve `201` con el gasto y su
  división.
- Nuevo endpoint `GET /api/grupos/{id}/gastos`: devuelve `200` con los gastos del
  grupo ordenados por `fecha` descendente; `[]` si no hay gastos.
- Nuevo endpoint `GET /api/grupos/{id}/gastos/{gastoId}`: devuelve `200` con los
  datos del gasto y su división (lista de `participante` + `montoAdeudado`).
- Nuevo endpoint `PUT /api/grupos/{id}/gastos/{gastoId}`: cualquier miembro
  actualiza `descripcion`, `monto`, `pagadorId` y `fecha` (reemplazo completo) y el
  sistema **recalcula** la división entre los miembros actuales del grupo. Devuelve
  `200` con el gasto actualizado.
- Nuevo endpoint `DELETE /api/grupos/{id}/gastos/{gastoId}`: cualquier miembro
  elimina el gasto y sus filas en `gasto_participantes`. Devuelve `204`.
- Nuevo `GastoRepository` y nuevo `GastoParticipanteRepository` con Query Methods
  JPA (incluido un `deleteByGastoId` derivado).
- En `GrupoParticipanteRepository` no hacen falta métodos nuevos:
  `findByGrupoId` y `findByGrupoIdAndParticipanteId` ya existen.
- Nuevo `GastoService` con la verificación de que el usuario es miembro del grupo (reimplementada localmente sobre
  los repositorios existentes) y el cálculo de la división equitativa.
- Nuevo `GastoController` bajo `/api/grupos/{id}/gastos`.
- Nuevos DTOs de entrada (`RegistrarGastoRequest`, `ActualizarGastoRequest`) y de
  salida (`GastoResponse`, `GastoResumenDto`, `GastoParticipanteDto`).
- Se reutilizan las excepciones `ForbiddenOperationException` (→ `403`),
  `BadRequestException` (→ `400`) y `ResourceNotFoundException` (→ `404`), ya
  mapeadas al formato de error estándar en `GlobalExceptionHandler`. No se crean
  excepciones nuevas.
- Todos los endpoints requieren JWT válido; sin cambios en `SecurityConfig`
  (`anyRequest().authenticated()` ya cubre `/api/grupos/**`).
- **Corrección incidental en `grupos`** (fuera del alcance original, aprobada
  durante la implementación): `GrupoService.quitarMiembro` no borraba la fila de
  `grupo_participantes` de la base de datos (`orphanRemoval` sobre el bag no
  programa el `DELETE` cuando la entidad ya está gestionada y se pasa por
  `save()`/`merge()`). Se cambia por un borrado explícito vía
  `GrupoParticipanteRepository.delete(...)`. Destapado por el test preexistente
  `GrupoMiembrosControllerTest.quitarMiembro_creadorQuitaAOtro_devuelve204YDejaDeVerElGrupo`,
  que fallaba en `main` antes de esta tarea.

## Non-Goals

- No se implementan balances, deudas netas ni liquidación entre participantes (es
  la capacidad `balances`, tarea siguiente).
- No se implementan pagos reales ni transferencias entre participantes.
- La división es **siempre equitativa**: no se puede personalizar el reparto por
  participante ni excluir a un miembro de un gasto.
- `PUT` es un reemplazo completo del gasto (los cuatro campos son obligatorios); no
  hay actualización parcial tipo `PATCH`.
- No se restringe quién edita o elimina un gasto más allá de ser miembro del grupo:
  un miembro puede editar o borrar un gasto pagado por otro.
- No hay paginación, filtros por fecha/pagador ni límite de gastos por grupo.
- No se modifica ninguna entidad JPA ni el esquema de base de datos.
- No se toca `frontend/` (los gastos siguen fuera de alcance según CLAUDE.md; el
  frontend continúa contra MSW).
- No se añaden anotaciones `@PreAuthorize` ni se toca `SecurityConfig`.

## Capabilities

### New Capabilities

- `gastos`: registro y gestión de los gastos de un grupo por parte de sus
  miembros — registrar un gasto con reparto equitativo automático entre todos los
  miembros del momento, listar los gastos del grupo por fecha, ver el detalle de un
  gasto con su división, editar un gasto recalculando la división, y eliminar un
  gasto con su división. El pagador absorbe el centavo sobrante del redondeo y la
  suma de lo adeudado siempre iguala el monto del gasto.

### Modified Capabilities

<!-- Ninguna: no cambian los requisitos de capacidades existentes. -->

## Impact

- **Código nuevo**:
  - `repository/GastoRepository`, `repository/GastoParticipanteRepository`.
  - `service/GastoService`.
  - `controller/GastoController`.
  - `dto/request/RegistrarGastoRequest`, `dto/request/ActualizarGastoRequest`.
  - `dto/response/GastoResponse`, `dto/response/GastoResumenDto`,
    `dto/response/GastoParticipanteDto`.
  - `test/.../gastos/GastoControllerTest`, `test/.../gastos/GastoDivisionTest`.
- **Código modificado**:
  - `service/GrupoService.java`: corrección incidental en `quitarMiembro` (ver
    _What Changes_). Único archivo existente que se toca; no cambia el contrato de
    ningún endpoint de `grupos`.
  - Se reutilizan `GrupoRepository`, `GrupoParticipanteRepository`,
    `ParticipanteRepository`, las tres excepciones de negocio,
    `GlobalExceptionHandler` y `dto/response/ParticipanteDto` tal cual.
- **APIs**: nuevas rutas bajo `/api/grupos/{id}/gastos/**`, protegidas por JWT sin
  tocar `SecurityConfig`.
- **Base de datos**: sin cambios de esquema. Las tablas `gastos` y
  `gasto_participantes` ya las genera Hibernate (`ddl-auto=update`) a partir de las
  entidades existentes.
- **Dependencias**: ninguna nueva.
- **Specs**: al archivar el cambio, el delta reemplaza el borrador manual de
  `openspec/specs/gastos/spec.md` por un spec en formato canónico.
