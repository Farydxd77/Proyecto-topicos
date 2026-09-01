# Refactor BaseEntity

## Objetivo
Eliminar la duplicación de los campos created_at y updated_at en las entidades
Usuario, Participante, Grupo y Gasto, extrayéndolos a una clase base BaseEntity
con @MappedSuperclass y @SuperBuilder. El campo id se mantiene declarado en cada
entidad individualmente.

## Comportamiento esperado

### Dado que existe la clase BaseEntity con @MappedSuperclass
Cuando una entidad hereda de BaseEntity,
Entonces hereda automáticamente created_at y updated_at sin necesidad de
declararlos nuevamente. El campo id (BIGINT IDENTITY) sigue declarándose en
cada entidad.

### Dado que las entidades usan @SuperBuilder
Cuando se construye una instancia con el patrón builder,
Entonces se pueden setear tanto los campos de BaseEntity como los propios
de la entidad en la misma cadena de builder.

### Dado que la aplicación arranca después del refactor
Cuando Hibernate inicializa,
Entonces las 6 tablas mantienen exactamente la misma estructura que antes
sin cambios en columnas ni tipos.

## Casos límite
- GrupoParticipante y GastoParticipante NO heredan de BaseEntity
  porque tienen PK compuesta y no tienen created_at ni updated_at
- joined_at en GrupoParticipante se mantiene como campo propio
- El refactor no cambia ninguna tabla en la BD

## Criterios de aceptación
- [x] Clase BaseEntity creada en entity/ con @MappedSuperclass, @SuperBuilder,
      @NoArgsConstructor, @AllArgsConstructor (además @Getter y @Setter)
- [x] BaseEntity tiene created_at y updated_at; el campo id se mantiene en cada
      entidad individualmente (@GeneratedValue IDENTITY), no sube a BaseEntity
- [x] Usuario, Participante, Grupo y Gasto heredan de BaseEntity
- [x] Esas 4 entidades usan @SuperBuilder (antes no tenían builder)
- [x] GrupoParticipante y GastoParticipante NO tocan — quedan igual
- [x] La app compila sin errores
- [x] mvnw test pasa
- [x] Las tablas en PostgreSQL mantienen la misma estructura

## Fuera de alcance
- No se crean repositorios ni servicios
- No se cambia ninguna lógica de negocio
- No se modifican GrupoParticipante ni GastoParticipante