## Why

Las entidades `Usuario`, `Participante`, `Grupo` y `Gasto` repiten el mismo bloque
de código: la clave primaria `id` (`@Id @GeneratedValue(IDENTITY)`), el campo
`created_at` (`@CreationTimestamp`), el campo `updated_at` (`@UpdateTimestamp`) y
sus getters. Además tienen getters/setters escritos a mano en lugar de Lombok, en
contra de la convención de `CLAUDE.md`. Esta duplicación hace que cualquier ajuste
en el manejo de identificadores o timestamps haya que replicarlo en cuatro
archivos y facilita que se introduzcan inconsistencias.

## What Changes

- Se crea `BaseEntity` en `entity/` con `@MappedSuperclass`, anotada con Lombok
  `@Getter`, `@Setter`, `@SuperBuilder`, `@NoArgsConstructor` y
  `@AllArgsConstructor`.
- `BaseEntity` concentra los campos comunes: `createdAt`
  (`@CreationTimestamp`, `@Column(name = "created_at", nullable = false,
  updatable = false)`) y `updatedAt` (`@UpdateTimestamp`, `@Column(name =
  "updated_at", nullable = false)`).
- `Usuario`, `Participante`, `Grupo` y `Gasto` pasan a `extends BaseEntity`,
  eliminan sus campos `createdAt` / `updatedAt` y todos sus
  getters/setters a mano. El campo `id` se mantiene en cada entidad individualmente.
- Esas 4 entidades adoptan Lombok: `@Getter`, `@Setter`, `@SuperBuilder`,
  `@NoArgsConstructor`, `@AllArgsConstructor` (usando `@SuperBuilder` por la
  herencia, según `CLAUDE.md`). No se usa `@Data`.
- `GrupoParticipante` y `GastoParticipante` **no se tocan**: tienen PK compuesta
  (`@EmbeddedId`) y no tienen `created_at` / `updated_at`. `joined_at` sigue
  siendo campo propio de `GrupoParticipante`.
- El refactor es puramente estructural: no cambia ninguna tabla, columna, tipo,
  constraint ni lógica de negocio en PostgreSQL.

## Capabilities

### New Capabilities
Ninguna. Es un refactor estructural sin cambios de comportamiento; el cambio
declara `skip_specs: true` en su `.openspec.yaml`.

### Modified Capabilities
Ninguna. La capability `entidades` ya especifica que las entidades tienen
identificador autogenerado y timestamps de creación/actualización, y ese
comportamiento no cambia.

## Impact

- **Código nuevo**: `backend/src/main/java/com/cuentasclaras/backend/entity/BaseEntity.java`.
- **Código modificado**: `Usuario.java`, `Participante.java`, `Grupo.java`,
  `Gasto.java` (herencia + Lombok, se borra código duplicado).
- **Sin cambios**: `GrupoParticipante.java`, `GastoParticipante.java`,
  `GrupoParticipanteId.java`, `GastoParticipanteId.java`.
- **API pública**: los getters (`getCreatedAt()`, `getUpdatedAt()`) se
  mantienen con la misma firma, ahora generados por Lombok en `BaseEntity`.
  `getId()` sigue en cada entidad individualmente generado por Lombok.
  Aparecen nuevos setters (`setCreatedAt`, `setUpdatedAt`) y métodos
  builder que antes no existían.
- **Base de datos**: sin impacto. Hibernate (`ddl-auto=update`) genera las 6
  tablas con exactamente la misma estructura.
- **Dependencias**: ninguna nueva; Lombok ya está en el `pom.xml`.
- **Tests**: `./mvnw test` debe seguir pasando sin cambios funcionales.

## Non-Goals

- No se crean repositorios ni servicios.
- No se modifica lógica de negocio.
- No se tocan `GrupoParticipante` ni `GastoParticipante`.
- No se cambia el esquema de la base de datos ni se introduce Flyway/Liquibase.
- No se añaden campos de auditoría nuevos (autor, versión, borrado lógico).
- No se toca el `frontend/`.
