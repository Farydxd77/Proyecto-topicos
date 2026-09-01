## 1. Crear BaseEntity

- [x] 1.1 Crear `backend/src/main/java/com/cuentasclaras/backend/entity/BaseEntity.java`
      como `public abstract class BaseEntity` con `@MappedSuperclass` y Lombok de
      clase `@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor`
      (nunca `@Data`). Campos: `LocalDateTime createdAt` (`@CreationTimestamp`,
      `@Column(name = "created_at", nullable = false, updatable = false)`);
      `LocalDateTime updatedAt` (`@UpdateTimestamp`,
      `@Column(name = "updated_at", nullable = false)`). El campo `id` NO va
      en BaseEntity — se mantiene en cada entidad individualmente. Verificar con
      `./mvnw clean compile` que compila.

## 2. Refactorizar entidades raíz

- [x] 2.1 `Usuario.java`: `extends BaseEntity`; borrar campos `createdAt`,
      `updatedAt` y todos sus getters/setters a mano; añadir Lombok de clase
      `@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor`;
      conservar `@Entity @Table(name = "usuarios")`, el campo `id` con su
      `@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id")` y los
      campos `username` / `password` con sus `@Column` actuales; eliminar
      imports huérfanos (`CreationTimestamp`, `UpdateTimestamp` si ya no se
      usan). Verificar con `./mvnw clean compile`.
- [x] 2.2 `Participante.java`: `extends BaseEntity`; borrar `createdAt`/
      `updatedAt` y accesores a mano; añadir Lombok de clase; conservar
      `@Entity @Table(name = "participantes")`, el campo `id`, la relación
      `@OneToOne` a `Usuario` y los campos `nombre` / `apellido` / `ci`;
      limpiar imports. Verificar con `./mvnw clean compile`.
- [x] 2.3 `Grupo.java`: `extends BaseEntity`; borrar `createdAt`/`updatedAt`
      y accesores a mano; añadir Lombok de clase; conservar
      `@Entity @Table(name = "grupos")`, el campo `id`, la relación
      `@ManyToOne` a `creador` y los campos `nombre` / `descripcion`;
      limpiar imports. Verificar con `./mvnw clean compile`.
- [x] 2.4 `Gasto.java`: `extends BaseEntity`; borrar `createdAt`/`updatedAt`
      y accesores a mano; añadir Lombok de clase; conservar
      `@Entity @Table(name = "gastos")`, el campo `id`,
      `@Check(constraints = "monto > 0")`, las relaciones `@ManyToOne` a
      `grupo` y `pagador` y los campos `descripcion` / `monto` / `fecha`;
      limpiar imports. Verificar con `./mvnw clean compile`.

## 3. Verificar entidades de join sin cambios

- [x] 3.1 Confirmar que `GrupoParticipante.java`, `GastoParticipante.java`,
      `GrupoParticipanteId.java` y `GastoParticipanteId.java` NO fueron
      modificados (`git diff --stat` no debe listarlos) y siguen sin heredar de
      `BaseEntity`.

## 4. Verificación integral

- [x] 4.1 Ejecutar `./mvnw clean test` y verificar que compila y todos los tests
      pasan sin cambios funcionales.
- [x] 4.2 Arrancar la app contra PostgreSQL (`./mvnw spring-boot:run`) con
      `spring.jpa.show-sql=true` y verificar en el log que Hibernate NO emite
      `alter table` sobre las columnas `created_at` ni `updated_at` de
      `usuarios`, `participantes`, `grupos` ni `gastos`; el campo `id` sigue
      declarado en cada entidad; las 6 tablas mantienen la estructura
      documentada en `CLAUDE.md - Modelo de datos`.s`.
- [x] 4.3 Repasar los criterios de aceptación de
      `openspec/specs/refactor-base-entity/spec.md` y marcarlos todos como
      cumplidos.
