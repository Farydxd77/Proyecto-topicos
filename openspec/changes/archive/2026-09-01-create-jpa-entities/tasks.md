## 1. Composite key classes

- [x] 1.1 Create `GrupoParticipanteId` (`@Embeddable`, grupoId + participanteId) in `com.cuentasclaras.backend.entity` and verify `mvnw -q -DskipTests compile` succeeds
- [x] 1.2 Create `GastoParticipanteId` (`@Embeddable`, gastoId + participanteId) in `com.cuentasclaras.backend.entity` and verify `mvnw -q -DskipTests compile` succeeds

## 2. Entities

- [x] 2.1 Create `Usuario` entity (id, username, password, created_at, updated_at) and verify `mvnw -q -DskipTests compile` succeeds
- [x] 2.2 Create `Participante` entity (id, nombre, apellido, ci, `@OneToOne` unique usuario reference, created_at, updated_at) and verify `mvnw -q -DskipTests compile` succeeds
- [x] 2.3 Create `Grupo` entity (id, nombre, descripcion, optional `@ManyToOne` creador, created_at, updated_at) and verify `mvnw -q -DskipTests compile` succeeds
- [x] 2.4 Create `GrupoParticipante` entity (`@EmbeddedId` + `@MapsId` grupo/participante, joined_at) and verify `mvnw -q -DskipTests compile` succeeds
- [x] 2.5 Create `Gasto` entity (id, descripcion, monto with `@Check` > 0, optional `@ManyToOne` pagador, `@ManyToOne` grupo, fecha, created_at, updated_at) and verify `mvnw -q -DskipTests compile` succeeds
- [x] 2.6 Create `GastoParticipante` entity (`@EmbeddedId` + `@MapsId` gasto/participante, monto_adeudado) and verify `mvnw -q -DskipTests compile` succeeds

## 3. Schema verification

- [x] 3.1 Start the app against PostgreSQL and verify Hibernate (`ddl-auto=update`) creates the 6 tables (usuarios, participantes, grupos, grupo_participantes, gastos, gasto_participantes) with no startup errors
- [x] 3.2 Inspect the generated schema (e.g. `\d` per table in psql) and verify column names/types and the `monto > 0` check constraint match `openspec/specs/data-model.md`
- [x] 3.3 Run `mvnw test` and verify the existing `BackendApplicationTests` (context load) still passes with the new entities present
