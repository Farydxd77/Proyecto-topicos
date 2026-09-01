## 1. Refactorizar clases @Embeddable (PK compuesta)

- [x] 1.1 `GrupoParticipanteId.java`: añadir anotaciones de clase
      `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode`;
      borrar el constructor vacío, el constructor `(grupoId, participanteId)`,
      los getters/setters y los métodos `equals`/`hashCode` a mano; conservar
      `@Embeddable`, `implements Serializable` y los campos `Long grupoId` /
      `Long participanteId`; quitar `import java.util.Objects`. NO añadir
      `@Builder`/`@SuperBuilder`. Verificar con `./mvnw clean compile`.
- [x] 1.2 `GastoParticipanteId.java`: mismo tratamiento que 1.1 con los campos
      `Long gastoId` / `Long participanteId`; conservar `@Embeddable`,
      `implements Serializable`; quitar `import java.util.Objects`. Verificar con
      `./mvnw clean compile`.

## 2. Refactorizar entidades @EmbeddedId

- [x] 2.1 `GrupoParticipante.java`: añadir anotaciones de clase
      `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`; borrar todos los
      getters/setters a mano (incluido `getJoinedAt`); conservar
      `@Entity @Table(name = "grupo_participantes")`, `@EmbeddedId private
      GrupoParticipanteId id = new GrupoParticipanteId();`, las dos relaciones
      `@ManyToOne(fetch = LAZY)` con `@MapsId` + `@JoinColumn` y el campo
      `joinedAt` (`@CreationTimestamp`, `@Column(name = "joined_at", nullable =
      false, updatable = false)`). NO `@EqualsAndHashCode`, NO herencia de
      `BaseEntity`, NO `@Builder`. Verificar con `./mvnw clean compile`.
- [x] 2.2 `GastoParticipante.java`: añadir anotaciones de clase
      `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`; borrar todos los
      getters/setters a mano; conservar
      `@Entity @Table(name = "gasto_participantes")`, `@EmbeddedId private
      GastoParticipanteId id = new GastoParticipanteId();`, las dos relaciones
      `@ManyToOne(fetch = LAZY)` con `@MapsId` + `@JoinColumn` y el campo
      `montoAdeudado` (`@Column(name = "monto_adeudado", nullable = false,
      precision = 10, scale = 2)`). NO `@EqualsAndHashCode`, NO herencia, NO
      `@Builder`. Verificar con `./mvnw clean compile`.

## 3. Verificación integral

- [x] 3.1 Confirmar con `git diff --stat` que solo cambiaron esos 4 archivos y
      que `BaseEntity.java`, `Usuario.java`, `Participante.java`, `Grupo.java` y
      `Gasto.java` NO aparecen.
- [x] 3.2 Ejecutar `./mvnw clean test` y verificar que compila y todos los tests
      pasan sin cambios funcionales.
- [x] 3.3 Arrancar la app contra PostgreSQL (`./mvnw spring-boot:run`) con
      `spring.jpa.show-sql=true` y verificar en el log que Hibernate NO emite
      `alter table` sobre `grupo_participantes` ni `gasto_participantes`; las PKs
      compuestas y las columnas `joined_at` / `monto_adeudado` mantienen la
      estructura documentada en `CLAUDE.md - Modelo de datos`.
- [x] 3.4 Repasar los criterios de aceptación de
      `openspec/specs/lombok-embeddable/spec.md` y marcarlos todos como
      cumplidos.
