# Lombok en clases @Embeddable y entidades @EmbeddedId

## Objetivo
Agregar Lombok a las clases de PK compuesta (GrupoParticipanteId y GastoParticipanteId)
y a las entidades que las usan (GrupoParticipante y GastoParticipante),
eliminando getters/setters escritos a mano.

## Comportamiento esperado

### Dado que GrupoParticipanteId y GastoParticipanteId son clases @Embeddable
Cuando se agregan las anotaciones Lombok,
Entonces tienen @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
e implementan Serializable, sin getters/setters escritos a mano.

### Dado que GrupoParticipante y GastoParticipante usan @EmbeddedId
Cuando se agregan las anotaciones Lombok,
Entonces tienen @Getter @Setter @NoArgsConstructor @AllArgsConstructor
sin getters/setters escritos a mano.

### Dado que la aplicación arranca después del cambio
Cuando Hibernate inicializa,
Entonces las tablas grupo_participantes y gasto_participantes mantienen
exactamente la misma estructura que antes.

## Casos límite
- GrupoParticipanteId y GastoParticipanteId NO usan @SuperBuilder ni @Builder
- GrupoParticipante y GastoParticipante NO heredan de BaseEntity
- @EqualsAndHashCode solo va en las clases @Embeddable, no en las entidades
- Las clases @Embeddable deben implementar Serializable obligatoriamente

## Criterios de aceptación
- [x] GrupoParticipanteId tiene @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode e implementa Serializable
- [x] GastoParticipanteId tiene @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode e implementa Serializable
- [x] GrupoParticipante tiene @Getter @Setter @NoArgsConstructor @AllArgsConstructor
- [x] GastoParticipante tiene @Getter @Setter @NoArgsConstructor @AllArgsConstructor
- [x] Ninguna de las 4 clases tiene getters/setters escritos a mano
- [x] La app compila sin errores
- [x] mvnw test pasa
- [x] Las tablas grupo_participantes y gasto_participantes mantienen la misma estructura

## Fuera de alcance
- No se modifican otras entidades
- No se cambia ninguna lógica de negocio
- No se cambia el esquema de la base de datos