## Context

Ver `proposal.md - Why` para la motivación.

Estado actual (revisado en `backend/src/main/java/com/cuentasclaras/backend/entity/`):

- `Usuario`, `Participante`, `Grupo`, `Gasto`: cada una declara su propio `id`
  (`@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id")`), `createdAt`
  (`@CreationTimestamp`) y `updatedAt` (`@UpdateTimestamp`), más getters/setters
  escritos a mano. **Hoy no usan Lombok** y **no tienen `@Builder`**.
- `GrupoParticipante`, `GastoParticipante`: PK compuesta con `@EmbeddedId`
  (`GrupoParticipanteId`, `GastoParticipanteId`), sin `id` simple y sin
  `created_at` / `updated_at`. `GrupoParticipante` tiene `joinedAt`
  (`@CreationTimestamp`, columna `joined_at`).

Restricciones:

- Modelo de datos fijo (ver `CLAUDE.md - Modelo de datos`): PKs `BIGINT`
  autoincremental (`IDENTITY`), todas las tablas de entidades raíz con
  `created_at` y `updated_at` `TIMESTAMP NOT NULL`.
- Hibernate con `ddl-auto=update`: el esquema se deriva del modelo de entidades.
  El refactor **no debe** alterar nombres de columnas, tipos ni constraints.
- `CLAUDE.md - Lombok en entidades`: usar `@Getter @Setter @NoArgsConstructor
  @AllArgsConstructor @Builder`; nunca `@Data`; para herencia usar
  `@SuperBuilder` en lugar de `@Builder`.
- Todos los artefactos OpenSpec en español.

## Goals / Non-Goals

**Goals:**

- Definir una única `BaseEntity` (`@MappedSuperclass`) que concentre `createdAt`
  y `updatedAt` con exactamente el mismo mapeo que hoy. El campo `id` se mantiene
  en cada entidad individualmente.
- Que `Usuario`, `Participante`, `Grupo` y `Gasto` hereden esos campos y adopten
  Lombok con `@SuperBuilder`, sin getters/setters a mano.
- Cero cambios en el esquema generado por Hibernate (mismas 6 tablas, mismas
  columnas, tipos y constraints).

**Non-Goals (a nivel de diseño):**

- No se introduce una jerarquía de auditoría más rica (autor, versión, soft
  delete) ni JPA Auditing (`@EntityListeners(AuditingEntityListener)`).
- No se unifican las entidades de join bajo `BaseEntity`.
- No se migra a otra estrategia de generación de PK (`SEQUENCE`, `TABLE`).
- No se añade `equals`/`hashCode` basados en `id` (se puede evaluar aparte).

## Decisions

### Decisión 1: `@MappedSuperclass` en vez de `@Entity` + `@Inheritance`

`BaseEntity` se anota con `@MappedSuperclass` y **no** con `@Entity`.

- **Por qué**: `@MappedSuperclass` hace que las columnas heredadas (`created_at`,
  `updated_at`) se mapeen en la tabla de cada subclase, sin crear
  tabla propia ni columna discriminadora. El esquema resultante es idéntico al
  actual. El campo `id` se declara en cada entidad individualmente.
- **Alternativas descartadas**:
  - `@Inheritance(strategy = SINGLE_TABLE/JOINED/TABLE_PER_CLASS)`: todas
    requieren que la base sea `@Entity`, crean discriminador o tablas extra y
    cambian el esquema. Rechazado.
  - Interfaz + `default methods`: no puede aportar estado ni mapeo JPA. Rechazado.

### Decisión 2: `id` se mantiene en cada entidad

El campo `id` NO sube a `BaseEntity` — cada entidad lo declara individualmente.

- **Por qué**: cada entidad puede tener necesidades distintas para su PK en el
  futuro. Mantenerlo en cada clase es más explícito y flexible.
- **Alternativas descartadas**: subir `id` a `BaseEntity` junto con los
  timestamps — rechazado por decisión explícita del usuario.

### Decisión 3: `@SuperBuilder` + `@NoArgsConstructor` + `@AllArgsConstructor` en base y subclases

`BaseEntity` y las 4 subclases llevan `@Getter @Setter @SuperBuilder
@NoArgsConstructor @AllArgsConstructor`.

- **Por qué**:
  - `@SuperBuilder` es obligatorio por `CLAUDE.md` cuando hay herencia; permite
    encadenar en el builder tanto campos de `BaseEntity` como de la subclase.
    `@Builder` normal no ve los campos de la superclase.
  - `@SuperBuilder` en Lombok exige que **todas** las clases de la jerarquía
    (incluida la base) lo tengan; por eso también va en `BaseEntity`.
  - `@NoArgsConstructor`: JPA requiere constructor sin argumentos.
  - `@AllArgsConstructor`: coherencia con la convención de `CLAUDE.md` y evita el
    error de Lombok cuando `@SuperBuilder` y `@NoArgsConstructor` conviven sin un
    constructor completo generado.
- **Alternativas descartadas**:
  - `@Data`: prohibido por `CLAUDE.md` (genera `equals`/`hashCode`/`toString` que
    causan `StackOverflow` y carga perezosa no deseada con relaciones).
  - Mantener getters/setters a mano y solo añadir `@SuperBuilder`: el usuario
    pidió explícitamente "adoptar Lombok completo". Rechazado.

### Decisión 4: Mapeo de columnas idéntico, verbatim

En `BaseEntity` los `@Column` se copian tal cual están hoy:

- `createdAt`: `@Column(name = "created_at", nullable = false, updatable = false)` + `@CreationTimestamp`
- `updatedAt`: `@Column(name = "updated_at", nullable = false)` + `@UpdateTimestamp`

- **Por qué**: garantiza que Hibernate genere exactamente el mismo DDL. Los
  timestamps siguen gestionados por Hibernate (`@CreationTimestamp` /
  `@UpdateTimestamp`), no por `@PrePersist`/`@PreUpdate` manuales ni por JPA
  Auditing, para no cambiar comportamiento.
- **Alternativa descartada**: cambiar a `@PrePersist`/`@PreUpdate` con
  `Instant`/`LocalDateTime.now()`. Cambia semántica (zona horaria, precisión) sin
  necesidad. Rechazado.

### Decisión 5: `GrupoParticipante` y `GastoParticipante` intactos

No heredan de `BaseEntity` y no se modifican en absoluto.

- **Por qué**: usan `@EmbeddedId` (PK compuesta), no tienen `id` simple ni
  `created_at`/`updated_at`. Forzarlas bajo `BaseEntity` rompería su PK y
  añadiría columnas inexistentes. `joinedAt` sigue como campo propio de
  `GrupoParticipante`.

### Decisión 6: Ubicación y visibilidad

`BaseEntity` vive en `com.cuentasclaras.backend.entity` (mismo paquete que las
entidades), clase `public abstract class BaseEntity`.

- **Por qué**: `abstract` porque nunca se instancia directamente; mismo paquete
  para no ampliar la superficie de paquetes y seguir la estructura de `CLAUDE.md`.

## Risks / Trade-offs

- **Lombok `@SuperBuilder` mal aplicado (falta en la base o en una subclase)** →
  Falla de compilación clara. Mitigación: la lista de tareas exige `@SuperBuilder`
  en las 5 clases y `./mvnw clean compile` como checkpoint.
- **Hibernate detecta un cambio de esquema por diferencia sutil de `@Column`** →
  columnas duplicadas o `alter table` inesperado con `ddl-auto=update`.
  Mitigación: copiar los `@Column` verbatim (Decisión 4) y verificar el DDL
  generado en el log de arranque (`hibernate.hbm2ddl` / `show-sql`) contra el
  modelo de `CLAUDE.md`.
- **Código cliente que construía entidades con `new Usuario()` + setters** → sigue
  funcionando porque `@NoArgsConstructor` + `@Setter` se mantienen. El builder es
  aditivo.
- **`@AllArgsConstructor` en la subclase genera un constructor con orden de
  parámetros distinto al histórico** → hoy no existe ningún `new Usuario(...)` con
  argumentos (constructores implícitos), así que no hay ruptura. Riesgo bajo.
- **Interacción con JSON / serialización (Jackson) por nuevos setters** → los DTOs
  de respuesta son clases aparte (`dto/response/`), las entidades no se serializan
  directamente; impacto nulo.
- **Tests existentes** → `./mvnw test` debe pasar sin cambios. Si algún test hacía
  reflexión sobre campos declarados en la clase concreta, habría que ajustarlo;
  no se detectan tests así en el repo actual.

## Migration Plan

1. Crear `BaseEntity.java` con `@MappedSuperclass` + Lombok + solo `createdAt` y `updatedAt` (sin `id`).
2. Refactorizar `Usuario`, `Participante`, `Grupo`, `Gasto`: `extends BaseEntity`,
   borrar `createdAt`/`updatedAt` y sus accesores, añadir anotaciones Lombok
   de clase, limpiar imports huérfanos. El `id` se mantiene en cada entidad.
3. `./mvnw clean compile` → debe compilar.
4. `./mvnw test` → debe pasar.
5. Arrancar la app contra PostgreSQL y revisar en el log que Hibernate no emite
   `alter table` sobre `created_at` ni `updated_at`.

**Rollback**: `git revert` del commit del cambio. No hay migración de datos ni de
esquema, así que el rollback es inmediato y sin efectos en la BD.

## Open Questions

Ninguna.
