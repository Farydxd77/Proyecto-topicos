## Contexto

Los cinco defectos son independientes entre sí, pero comparten una raíz común: el
backend confía en que el camino feliz es el único camino. Se agrupan en un solo
cambio porque ninguno justifica su propio ciclo de propuesta y los cinco se
verifican con la misma corrida de tests.

El modelo de datos relevante (ver `CLAUDE.md`):

- `gastos.grupo_id` → `grupos.id`, `NOT NULL`
- `gasto_participantes.gasto_id` → `gastos.id`, parte de la PK compuesta
- `pagos.grupo_id` → `grupos.id`, `NOT NULL`
- `participantes.ci` `VARCHAR(20) NOT NULL`, **sin** restricción de unicidad
- `pagos.monto` `DECIMAL(10,2)`

## Decisión 1: el borrado en cascada se resuelve en el servicio

`GrupoService.eliminar` pasa a borrar, en orden, la división de cada gasto, los
gastos, los pagos y por último el grupo (cuyas membresías ya se van por la
cascada JPA existente sobre `miembros`).

**Alternativa descartada: `@OneToMany(cascade = ALL, orphanRemoval = true)` desde
`Grupo` hacia `gastos` y `pagos`.** Obligaría a cargar en memoria la colección
completa de gastos y pagos del grupo cada vez que se toca un `Grupo`, y `Grupo`
ya se serializa a `GrupoResponse` en cinco endpoints. El riesgo de que una
colección nueva se cuele en un `toResponse` y dispare N+1 consultas no compensa.

**Alternativa descartada: `ON DELETE CASCADE` en la FK.** Es la solución correcta
a nivel de base de datos, pero el proyecto corre con `ddl-auto=update`, que crea
claves foráneas pero no las altera una vez creadas. Las bases ya existentes en
las máquinas del equipo se quedarían sin la cascada y el bug seguiría vivo ahí.
Sin una herramienta de migración, no hay forma de garantizarlo.

**Alternativa descartada: prohibir borrar un grupo con gastos.** Devolver `409
Conflict` sería defendible, pero deja al usuario sin salida: para borrar el grupo
tendría que borrar sus gastos uno por uno, y la spec vigente de `grupos` ya
promete que el borrado funciona y que después el grupo devuelve `404`.

El borrado se hace con dos métodos nuevos de repositorio (`deleteByGrupoId` en
`GastoRepository` y en `PagoRepository`) más el `deleteByGastoId` que
`GastoParticipanteRepository` ya tiene. Todo dentro de la transacción que ya
abre `eliminar`, así que o se va todo o no se va nada.

## Decisión 2: `@Digits(integer = 8)` en lugar de agrandar la columna

`DECIMAL(10,2)` admite hasta `99999999.99`. La validación decía 10 dígitos
enteros; se baja a 8 para que coincida.

**Alternativa descartada: agrandar `pagos.monto` a `DECIMAL(20,2)`** (como
`gastos.monto`, que es `precision = 20, scale = 8`). Se descarta porque el monto
de un pago está siempre en USDT y `CLAUDE.md` fija la columna en `DECIMAL(10,2)`;
cambiarla obligaría a tocar el modelo de datos documentado y, con
`ddl-auto=update`, Hibernate no reduce ni amplía columnas ya creadas de forma
confiable. Alinear la validación a la columna es el arreglo de menor superficie.

`gastos.monto` no tiene este problema: `@Digits(integer = 12, fraction = 8)`
contra `precision = 20, scale = 8` calza exacto (20 − 8 = 12).

## Decisión 3: el catch-all no expone la excepción

El handler de `Exception` responde `500` con `message` fijo `"Ha ocurrido un error
inesperado"` y escribe `ex` completo en el log del servidor con nivel `ERROR`.

**Alternativa descartada: devolver `ex.getMessage()`.** Filtra nombres de tablas,
consultas SQL y rutas de clases al cliente. El mensaje de una excepción no
prevista no está redactado para un usuario final.

El orden de los handlers no depende de su posición en el archivo: Spring elige
siempre el más específico, así que el `Exception` genérico no le roba casos a los
handlers concretos. `ResourceNotFoundException` y compañía siguen ganando.

`AccessDeniedException` merece una nota: hoy la lanza Spring Security y la
resuelve su propio `ExceptionTranslationFilter`, **antes** de que la petición
llegue al `@RestControllerAdvice`. Como el proyecto no usa `@PreAuthorize` ni
reglas por rol (toda la autorización vive en los servicios y usa
`ForbiddenOperationException`), el handler queda como red de seguridad para el
día que se agregue una regla declarativa. Se registra igual, sin depender de él.

`NoResourceFoundException` (ruta inexistente, p. ej. `GET /api/inventado`) solo
llega al advice si Spring Boot está configurado para lanzarla; se agrega
`spring.mvc.problemdetails.enabled=false` implícito por omisión y se confía en el
comportamiento por defecto de Spring Boot 4, que sí la propaga.

## Decisión 4: `findByCi` devuelve `List`

`ParticipanteService.buscar` hoy hace `findByCi(ci).map(List::of).orElseGet(List::of)`.
Pasa a `findAllByCi(ci)` y se devuelve tal cual, coherente con las otras dos
búsquedas (`findByNombreContainingIgnoreCase`, `findByApellidoContainingIgnoreCase`),
que ya devuelven listas.

Se nombra `findAllByCi` y no `findByCi` para respetar la convención de
`CLAUDE.md`: `findBy{Campo}` → `Optional<T>`, `findAllBy{Campo}` → `List<T>`.

**Alternativa descartada: hacer `ci` UNIQUE.** El modelo permite CI repetidos a
propósito (dos personas pueden compartir CI entre países, y el proyecto no valida
CI contra ningún registro). Además, con datos ya cargados, agregar el índice único
fallaría en cualquier base que tenga duplicados.

## Riesgos

- El borrado en cascada es destructivo e irreversible: borrar un grupo ahora se
  lleva todo su historial de gastos y pagos. Ya era la intención de la spec, pero
  hasta hoy fallaba en vez de ejecutarse. Se cubre con tests explícitos.
- Cambiar `@Digits` de 10 a 8 es técnicamente restrictivo para un cliente que hoy
  mande un monto de 9 dígitos: antes recibía `500`, ahora recibe `400`. Ninguno de
  los dos es un caso de éxito, así que no rompe nada que funcionara.
