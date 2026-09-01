## Context

The data model these entities implement is fully specified in `CLAUDE.md` (## Modelo de datos) and `openspec/specs/data-model.md`: 6 tables (`usuarios`, `participantes`, `grupos`, `grupo_participantes`, `gastos`, `gasto_participantes`), BIGINT identity PKs, `created_at`/`updated_at` on the 4 "root" tables, and two join tables with composite PKs. See proposal.md - Why for motivation. Only `com.cuentasclaras.backend.entity` (currently empty aside from `package-info.java`) is affected.

## Goals / Non-Goals

**Goals:**
- Map every table, column, type, and constraint from the data model onto a corresponding JPA entity, one Java class per table, in `com.cuentasclaras.backend.entity`.
- Let Hibernate's `ddl-auto=update` generate a schema that matches the data model exactly, including the `monto > 0` check and the composite keys.

**Non-Goals:**
- No decision here about repository/service-layer query patterns (fetch joins, projections) — deferred to the change that adds repositories.
- No decision about switching to Flyway/Liquibase — out of scope while `ddl-auto=update` is the project's chosen approach (CLAUDE.md).

## Decisions

- **Entity class names**: plain domain names with no `Entity` suffix (`Usuario`, `Participante`, `Grupo`, `GrupoParticipante`, `Gasto`, `GastoParticipante`), per explicit user confirmation for this change. Alternative considered: `UsuarioEntity`-style suffixes (CLAUDE.md's own class-naming example) — not used here since the user explicitly chose plain names for this change.
- **Composite keys** (`GrupoParticipante`, `GastoParticipante`): use `@EmbeddedId` with a small `@Embeddable` id class per join entity, combined with `@MapsId` on the two `@ManyToOne` associations. Alternative considered: `@IdClass` — rejected because it duplicates the FK fields as primitive-typed shadow fields on the entity, whereas `@EmbeddedId` + `@MapsId` keeps a single source of truth (the `@ManyToOne` associations) and gives the composite key proper `equals`/`hashCode` for free.
- **created_at/updated_at**: use Hibernate's `@CreationTimestamp` / `@UpdateTimestamp` (not manual `@PrePersist`/`@PreUpdate` callbacks) on `Usuario`, `Participante`, `Grupo`, and `Gasto` — the four tables that have both columns per the data model (`grupo_participantes` only has `joined_at`; `gasto_participantes` has neither). Alternative considered: a shared `@MappedSuperclass` with the two timestamp fields — rejected for now as an unnecessary abstraction for 4 entities; revisit if more auditable entities are added later.
- **`monto > 0` constraint**: enforce at the database level via Hibernate's `@org.hibernate.annotations.Check(constraints = "monto > 0")` on `Gasto`, so `ddl-auto=update` emits the same CHECK constraint the data model specifies — without adding Bean Validation (out of scope per proposal.md - Non-Goals). Alternative considered: enforcing only via `@Positive` Bean Validation — rejected because validation logic is explicitly out of scope for this change, and the spec's "non-positive amount rejected" scenario needs to hold even without a validation layer.
- **Relationship fetch type**: `FetchType.LAZY` on every `@ManyToOne`/`@OneToOne` association (`Participante.usuario`, `Grupo.creador`, `Gasto.grupo`, `Gasto.pagador`, and the `@MapsId` associations on the join entities). Alternative considered: JPA's default `EAGER` for `@ManyToOne`/`@OneToOne` — rejected to avoid unintentionally loading large object graphs once repositories/services start querying these entities.
- **Table/column naming**: explicit `@Table(name = "...")` / `@Column(name = "...")` on every mapping, matching the exact snake_case names in the data model, rather than relying on Spring Boot's implicit naming strategy to derive them. Alternative considered: rely on the default `CamelCaseToUnderscoresNamingStrategy` — rejected because explicit names make the mapping to `CLAUDE.md`'s data model directly verifiable without checking naming-strategy behavior.
- **Nullable creator/pagador FKs**: `Grupo.creador` and `Gasto.pagador` are mapped as optional (`nullable = true`) associations, matching the data model — neither column is marked `NOT NULL` there — even though `entidades/spec.md`'s "Casos límite" implies a pagador is expected in practice; enforcing that is business logic, out of scope here.

## Risks / Trade-offs

- [`@EmbeddedId` + `@MapsId` adds a small id class per join entity] → accepted; it's the standard, well-documented JPA idiom for composite FK-based keys and keeps the entities themselves simple.
- [`@Check` is a Hibernate-specific annotation, not portable JPA] → acceptable; the project already commits to Hibernate-specific `ddl-auto=update` instead of a portable migration tool.
- [`FetchType.LAZY` everywhere means the upcoming repository/service change must fetch associations explicitly when needed] → acceptable; flagged for that change rather than solved here, since no queries exist yet.
