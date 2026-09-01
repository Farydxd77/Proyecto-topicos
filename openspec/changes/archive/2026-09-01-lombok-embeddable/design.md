## Context

Ver `proposal.md - Why` para la motivación.

Estado actual (revisado en `backend/src/main/java/com/cuentasclaras/backend/entity/`):

- `GrupoParticipanteId` / `GastoParticipanteId`: clases `@Embeddable` que
  `implements Serializable`. Campos `Long` (`grupoId`/`gastoId`,
  `participanteId`). Tienen a mano: constructor vacío, constructor
  `(x, participante)`, getters, setters, y `equals`/`hashCode` sobre ambos campos
  (`instanceof` pattern + `Objects.equals` / `Objects.hash`).
- `GrupoParticipante` / `GastoParticipante`: entidades `@Entity` con
  `@EmbeddedId private XId id = new XId();`, dos `@ManyToOne(fetch = LAZY)` con
  `@MapsId(...)` + `@JoinColumn`, y un campo propio
  (`joinedAt` con `@CreationTimestamp` / `montoAdeudado` `DECIMAL(10,2)`).
  Getters/setters a mano; `joinedAt` solo tiene getter. No usan Lombok, no
  heredan de `BaseEntity`.

Restricciones (ver `CLAUDE.md`):

- Modelo de datos fijo: `grupo_participantes` y `gasto_participantes` con PK
  compuesta `(grupo_id, participante_id)` / `(gasto_id, participante_id)`;
  `joined_at TIMESTAMP NOT NULL`; `monto_adeudado DECIMAL(10,2) NOT NULL`.
- Hibernate `ddl-auto=update`: el esquema se deriva de las entidades; el cambio
  **no debe** alterar columnas, tipos ni constraints.
- `CLAUDE.md - Lombok en entidades`: `@Getter @Setter @NoArgsConstructor
  @AllArgsConstructor`; nunca `@Data`; `@SuperBuilder` solo cuando hay herencia.
- Todos los artefactos OpenSpec en español.
- Precedente: el change `refactor-base-entity` ya aplicó Lombok a las 4 entidades
  raíz con el mismo criterio.

## Goals / Non-Goals

**Goals:**

- Sustituir el código repetitivo (constructores, getters, setters,
  `equals`/`hashCode`) de las 4 clases por anotaciones Lombok.
- Mantener la clave compuesta funcional: `@EqualsAndHashCode` en las
  `@Embeddable` (JPA compara PKs por valor), `Serializable` intacto.
- Cero cambios en el esquema generado por Hibernate.

**Non-Goals (a nivel de diseño):**

- No unificar las `@Embeddable` bajo una clase base común.
- No añadir `@Builder`/`@SuperBuilder` (clave compuesta + `@MapsId` hacen el
  builder confuso y no aporta valor aquí).
- No cambiar `fetch`, `@MapsId`, nombres de `@JoinColumn` ni el inicializador
  `= new XId()`.
- No tocar `equals`/`hashCode` de las entidades (JPA usa la PC de `@EmbeddedId`).

## Decisions

### Decisión 1: `@EqualsAndHashCode` solo en las clases `@Embeddable`

`GrupoParticipanteId` y `GastoParticipanteId` llevan `@EqualsAndHashCode`; las
entidades `GrupoParticipante` y `GastoParticipante` **no**.

- **Por qué**: JPA exige que la clase de una PK compuesta implemente
  `equals`/`hashCode` por valor para identificar filas y gestionar el caché de
  primer nivel. Hoy están a mano sobre `(grupoId, participanteId)` /
  `(gastoId, participanteId)`; `@EqualsAndHashCode` (por defecto, todos los
  campos no estáticos) genera exactamente lo mismo. En las entidades, en cambio,
  `@EqualsAndHashCode` de Lombok incluiría las relaciones `@ManyToOne` LAZY y
  podría disparar carga perezosa o `StackOverflow` (riesgo citado en
  `CLAUDE.md`); la identidad de la entidad ya la aporta el `@EmbeddedId`.
- **Alternativas descartadas**:
  - `@EqualsAndHashCode` también en las entidades: riesgo de recursión/lazy-load,
    sin beneficio. Rechazado.
  - `@Data` en las `@Embeddable`: incluye `@ToString` y `@RequiredArgsConstructor`
    y está prohibido por `CLAUDE.md`. Rechazado.
  - Dejar `equals`/`hashCode` a mano y solo anotar getters/setters: deja código
    repetitivo que el cambio busca eliminar. Rechazado.

### Decisión 2: `@NoArgsConstructor` + `@AllArgsConstructor` en las 4 clases

- **Por qué**:
  - `@NoArgsConstructor`: JPA/Hibernate necesita constructor sin argumentos tanto
    en la `@Embeddable` como en la `@Entity`. En las entidades, además, conserva
    el inicializador de campo `id = new XId()` (Lombok no lo pisa).
  - `@AllArgsConstructor`: en las `@Embeddable` reemplaza el constructor
    `(x, participante)` existente con firma equivalente (mismo orden de campos).
    En las entidades da un constructor completo; hoy no hay ningún
    `new GrupoParticipante(...)` con argumentos, así que no rompe nada, y
    mantiene la coherencia con la convención de `CLAUDE.md`.
- **Alternativas descartadas**:
  - Solo `@NoArgsConstructor` en las entidades: se aparta de la convención del
    proyecto sin motivo. Rechazado.
  - `@RequiredArgsConstructor`: no hay campos `final`, no aplica. Rechazado.

### Decisión 3: `@Getter` + `@Setter` a nivel de clase, sin accesores a mano

Las 4 clases usan `@Getter @Setter` de clase y se borran todos los
getters/setters manuales.

- **Por qué**: `joinedAt` hoy es "solo getter"; añadirle setter con `@Setter` de
  clase es inocuo (Hibernate lo puebla vía `@CreationTimestamp` al persistir; el
  setter extra no cambia el DDL ni el comportamiento) y evita anotar campo por
  campo. Coherente con cómo quedaron las entidades raíz en `refactor-base-entity`.
- **Alternativa descartada**: `@Getter` de clase + `@Setter` campo a campo para
  preservar el "solo lectura" de `joinedAt`. Más ruido, beneficio marginal.
  Rechazado.

### Decisión 4: Sin `@Builder` / `@SuperBuilder`

- **Por qué**: `CLAUDE.md` pide `@SuperBuilder` *solo* cuando hay herencia; estas
  clases no heredan de nada. Un `@Builder` sobre una entidad con `@EmbeddedId`
  inicializado y `@MapsId` induce a construir el objeto en un estado
  inconsistente (id sin sincronizar con las relaciones). El `spec.md` de
  referencia lo excluye explícitamente.
- **Alternativa descartada**: `@Builder` en las `@Embeddable` (donde sí sería
  inocuo) — se descarta por consistencia: el `spec.md` dice "ninguna de las 4".

### Decisión 5: `Serializable` se mantiene explícito

Las `@Embeddable` siguen con `implements Serializable` y el `import java.io`.

- **Por qué**: requisito de JPA para clases de PK compuesta; Lombok no lo aporta.
  `@EqualsAndHashCode` no implica `Serializable`.

## Risks / Trade-offs

- **`@EqualsAndHashCode` con configuración distinta a la actual (p. ej. `callSuper`)**
  → PKs que no comparan igual y filas duplicadas o `merge` roto. Mitigación: usar
  `@EqualsAndHashCode` sin parámetros (todos los campos, `callSuper=false`), que
  equivale al código actual; cubrir con `./mvnw test` y arranque real.
- **`@Setter` de clase expone `setJoinedAt` / `setId`** → código cliente podría
  mutar campos gestionados. Mitigación: riesgo bajo (no hay servicios aún);
  `joined_at` lo sigue fijando `@CreationTimestamp` en el `INSERT`.
- **`@AllArgsConstructor` en la entidad cambia la forma de construcción** → hoy
  no existe ninguna llamada con argumentos; `grep` de `new GrupoParticipante(` /
  `new GastoParticipante(` debe salir vacío antes de aplicar.
- **Hibernate emite `alter table` por una diferencia sutil de mapeo** →
  columnas/constraints inesperados. Mitigación: no se toca ningún `@Column`,
  `@JoinColumn`, `@MapsId` ni `@Embeddable`; verificar el log de arranque
  (`show-sql`, `ddl-auto=update`) sin `alter table` sobre
  `grupo_participantes` / `gasto_participantes`.
- **Lombok no procesa `@Embeddable`** → falso riesgo: Lombok opera sobre
  cualquier clase Java; `@Embeddable` es una anotación JPA ortogonal.

## Migration Plan

1. Refactorizar `GrupoParticipanteId` y `GastoParticipanteId`: añadir
   `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode`,
   borrar constructores/getters/setters/`equals`/`hashCode` a mano, conservar
   `implements Serializable`. Quitar imports huérfanos (`java.util.Objects`).
2. Refactorizar `GrupoParticipante` y `GastoParticipante`: añadir
   `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`, borrar
   getters/setters a mano, conservar `@EmbeddedId`, `= new XId()`, `@ManyToOne`,
   `@MapsId`, `@JoinColumn` y el campo propio.
3. `./mvnw clean compile` → debe compilar.
4. `./mvnw clean test` → debe pasar.
5. Arrancar contra PostgreSQL y revisar que Hibernate no emite `alter table`
   sobre las dos tablas de join.

**Rollback**: `git revert` del commit. Sin migración de datos ni de esquema, el
rollback es inmediato.

## Open Questions

Ninguna.
