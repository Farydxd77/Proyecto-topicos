## Why

Tras el refactor `refactor-base-entity`, las entidades raíz (`Usuario`,
`Participante`, `Grupo`, `Gasto`) ya usan Lombok, pero las clases de PK compuesta
(`GrupoParticipanteId`, `GastoParticipanteId`) y las entidades de join que las
usan (`GrupoParticipante`, `GastoParticipante`) siguen con getters/setters,
constructores y `equals`/`hashCode` escritos a mano. Esto rompe la consistencia
del paquete `entity/` y va contra la convención de Lombok de `CLAUDE.md`.

## What Changes

- `GrupoParticipanteId` y `GastoParticipanteId` (clases `@Embeddable`):
  - Se añaden `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`
    y `@EqualsAndHashCode`.
  - Se eliminan los getters/setters, los dos constructores y los métodos
    `equals`/`hashCode` escritos a mano.
  - Siguen implementando `Serializable` (obligatorio para una PK compuesta JPA).
  - NO usan `@Builder` ni `@SuperBuilder`.
- `GrupoParticipante` y `GastoParticipante` (entidades `@EmbeddedId`):
  - Se añaden `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`.
  - Se eliminan todos los getters/setters escritos a mano.
  - NO llevan `@EqualsAndHashCode` (la igualdad la aporta la clave `@EmbeddedId`).
  - NO heredan de `BaseEntity` ni usan `@Builder` / `@SuperBuilder`.
  - `GrupoParticipante` mantiene `joinedAt` (`@CreationTimestamp`, columna
    `joined_at`) como campo propio; `GastoParticipante` mantiene `montoAdeudado`.
  - Se conserva el inicializador `private XId id = new XId();` del campo
    `@EmbeddedId` y las relaciones `@ManyToOne` + `@MapsId`.
- Cambio puramente estructural: no se toca ninguna tabla, columna, tipo,
  constraint ni lógica de negocio.

## Capabilities

### New Capabilities
Ninguna. Es un refactor estructural sin cambios de comportamiento; el cambio
declara `skip_specs: true` en su `.openspec.yaml`.

### Modified Capabilities
Ninguna. La capability `entidades` ya especifica el comportamiento de las
membresías y los repartos de gasto (clave compuesta por par, fecha de alta,
monto adeudado), y ese comportamiento no cambia.

## Impact

- **Código modificado**: `GrupoParticipanteId.java`, `GastoParticipanteId.java`,
  `GrupoParticipante.java`, `GastoParticipante.java` (todas en
  `backend/src/main/java/com/cuentasclaras/backend/entity/`).
- **Sin cambios**: `BaseEntity.java`, `Usuario.java`, `Participante.java`,
  `Grupo.java`, `Gasto.java`.
- **API pública**: los getters/setters existentes se mantienen con la misma
  firma, ahora generados por Lombok. `GrupoParticipanteId` / `GastoParticipanteId`
  ganan un `@AllArgsConstructor` equivalente al constructor `(x, participante)`
  actual; `equals`/`hashCode` pasan a generarlos Lombok con los mismos campos.
- **Base de datos**: sin impacto. Hibernate (`ddl-auto=update`) genera
  `grupo_participantes` y `gasto_participantes` con la misma estructura.
- **Dependencias**: ninguna nueva; Lombok ya está en el `pom.xml`.
- **Tests**: `./mvnw test` debe seguir pasando sin cambios funcionales.

## Non-Goals

- No se modifican `BaseEntity` ni las 4 entidades raíz.
- No se cambia lógica de negocio ni el esquema de la base de datos.
- No se introduce `@Builder` / `@SuperBuilder` en estas 4 clases.
- No se añade `@EqualsAndHashCode` a las entidades de join.
- No se cambia la estrategia de PK compuesta (`@EmbeddedId` + `@MapsId`).
- No se toca el `frontend/`.
