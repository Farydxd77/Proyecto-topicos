## Why

El backend tiene cinco defectos de robustez que hoy se manifiestan como respuestas
`500 Internal Server Error` con un cuerpo que no respeta el formato de error
estándar, o como documentación que contradice al código:

1. `DELETE /api/grupos/{id}` sobre un grupo que tiene gastos o pagos falla. La
   entidad `Grupo` solo cascadea `miembros`; `gastos.grupo_id` y `pagos.grupo_id`
   son claves foráneas `NOT NULL` que bloquean el borrado de la fila del grupo.
2. `RegistrarPagoRequest` y `ActualizarPagoRequest` validan el monto con
   `@Digits(integer = 10, fraction = 2)`, pero la columna `pagos.monto` es
   `DECIMAL(10,2)`, que admite 8 dígitos enteros. Un monto de 9 o 10 dígitos
   enteros pasa la validación y revienta al insertar.
3. `GlobalExceptionHandler` no tiene un handler de último recurso ni maneja las
   excepciones de infraestructura más comunes (JSON malformado, tipo de parámetro
   incorrecto en la ruta, violación de integridad, ruta inexistente,
   `AccessDeniedException`). Esos casos salen con el cuerpo por defecto de Spring
   Boot, no con el formato que `CLAUDE.md` promete para *cualquier* error.
4. `ParticipanteRepository.findByCi` devuelve `Optional<Participante>` sobre la
   columna `participantes.ci`, que no es `UNIQUE`. Dos participantes con el mismo
   CI hacen que `GET /api/participantes?ci=X` lance
   `IncorrectResultSizeDataAccessException`.
5. `CLAUDE.md` sigue listando en *Fuera de alcance* el registro de que una
   transferencia de la liquidación ya se pagó, que es exactamente lo que la
   capacidad `pagos` implementó.

## What Changes

- `DELETE /api/grupos/{id}` pasa a eliminar en cascada los gastos del grupo (con
  su división en `gasto_participantes`) y los pagos del grupo, antes de borrar la
  fila del grupo. Sigue respondiendo `204 No Content`.
- Los dos request DTO de pagos pasan a `@Digits(integer = 8, fraction = 2)`, que
  es lo que la columna `DECIMAL(10,2)` admite de verdad. Un monto mayor devuelve
  `400 Bad Request` con el formato estándar en lugar de `500`.
- Nueva capacidad `manejo-errores` que fija por contrato lo que hoy solo vive en
  `CLAUDE.md`: **toda** respuesta de error de la API usa el mismo JSON
  (`timestamp`, `status`, `error`, `message`, `path`), incluidas las que hoy
  produce Spring por su cuenta. Se agregan handlers para
  `HttpMessageNotReadableException` (400), `MethodArgumentTypeMismatchException`
  (400), `HttpRequestMethodNotSupportedException` (405),
  `DataIntegrityViolationException` (409), `AccessDeniedException` (403),
  `NoResourceFoundException` (404) y un catch-all de `Exception` (500) que
  registra la traza en el log del servidor pero **no** la expone en la respuesta.
- `GET /api/participantes?ci=X` pasa a devolver todos los participantes con ese
  CI en lugar de fallar cuando hay más de uno. `findByCi` devuelve
  `List<Participante>`.
- `CLAUDE.md` deja de contradecirse: se quita la línea de *Fuera de alcance* sobre
  el registro de pagos, se agrega `pagos` a la descripción de la fase actual y a
  la estructura de paquetes del backend.

## Capabilities

### New Capabilities

- `manejo-errores`: contrato del formato de error uniforme de la API y del mapeo
  de cada familia de fallos (validación, negocio, infraestructura, imprevisto) al
  código HTTP correspondiente, incluida la garantía de que una excepción no
  prevista nunca filtra detalles internos al cliente.

### Modified Capabilities

- `grupos`: el borrado de un grupo pasa a arrastrar sus gastos y sus pagos, no
  solo sus membresías.
- `pagos`: el límite superior del monto queda alineado con lo que la columna
  admite, y superarlo es un `400`, no un `500`.
- `gestion-general`: la búsqueda de participantes por CI devuelve una lista y
  admite CI repetidos.

## Impact

- **Backend modificado**: `service/GrupoService` (borrado en cascada),
  `repository/GastoRepository` y `repository/PagoRepository` (borrado por grupo),
  `repository/ParticipanteRepository` y `service/ParticipanteService`
  (`findByCi` → `List`), `dto/request/RegistrarPagoRequest` y
  `dto/request/ActualizarPagoRequest` (`@Digits`),
  `exception/GlobalExceptionHandler` (7 handlers nuevos).
- **Base de datos**: sin cambios de esquema. El borrado en cascada se resuelve en
  la capa de servicio, no con `ON DELETE CASCADE`, para no depender de que
  `ddl-auto=update` altere claves foráneas ya creadas.
- **Contrato de la API**: ninguna respuesta exitosa cambia. Cambian códigos de
  error que hoy son `500` y pasan a ser `400`, `403`, `404`, `405` o `409`.
- **Documentación**: `CLAUDE.md`.
- **Sin cambios de frontend**: el cliente ya maneja el formato de error estándar
  vía `ApiError` en `src/api/client.ts`.

## Non-Goals

- No se agrega `ON DELETE CASCADE` en el esquema ni se introduce una herramienta
  de migración (Flyway/Liquibase): el proyecto sigue con `ddl-auto=update`.
- No se hace `ci` una columna `UNIQUE`: el modelo de datos admite CI repetidos a
  propósito y este cambio solo deja de romperse cuando ocurren.
- No se agrega un identificador de correlación ni un `traceId` al cuerpo de error.
- No se cambia el formato de error de validación de campos, que ya incluye el mapa
  `errors` además de los cinco campos estándar.
- No se toca la capacidad `balances`: un grupo borrado deja de existir junto con
  todo su historial, no se recalcula nada.
- No se resuelve el borrado de un participante o un usuario (no existe ese
  endpoint y sigue fuera de alcance).
