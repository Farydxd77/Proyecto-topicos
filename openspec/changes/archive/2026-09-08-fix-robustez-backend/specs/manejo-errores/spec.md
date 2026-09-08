## ADDED Requirements

### Requirement: Formato de error uniforme en toda la API

Toda respuesta con código de estado `4xx` o `5xx` de cualquier endpoint bajo `/api`
SHALL tener un cuerpo JSON con exactamente estos cinco campos: `timestamp` (instante
UTC en formato ISO-8601), `status` (el código HTTP como número), `error` (la frase
de estado HTTP), `message` (texto en español dirigido a una persona) y `path` (la
ruta solicitada). Esto SHALL valer también para los fallos que no origina el código
de la aplicación sino la infraestructura web (cuerpo ilegible, tipo de parámetro
incorrecto, método no permitido, ruta inexistente, excepción imprevista). Una
respuesta de error MUST NOT usar el cuerpo por defecto del framework.

#### Scenario: Un error de negocio usa el formato estándar

- **WHEN** una petición autenticada provoca un error de negocio conocido (por
  ejemplo consultar un recurso inexistente)
- **THEN** el cuerpo de la respuesta contiene `timestamp`, `status`, `error`,
  `message` y `path`
- **AND** `status` coincide con el código HTTP de la respuesta y `path` con la ruta
  solicitada

#### Scenario: Un error de infraestructura usa el mismo formato

- **WHEN** una petición falla por una causa ajena a la lógica de negocio (cuerpo
  ilegible, método no permitido, ruta inexistente)
- **THEN** el cuerpo de la respuesta tiene la misma forma que el de un error de
  negocio

### Requirement: Cuerpo de petición ilegible

Cuando el cuerpo de una petición no se puede deserializar —JSON malformado, cuerpo
vacío en un endpoint que exige cuerpo, o un valor cuyo tipo no corresponde al campo
(por ejemplo una `fecha` que no es una fecha válida o un `monto` que no es un
número)— el sistema SHALL responder `400 Bad Request` con el formato de error
estándar y un `message` que indique que el cuerpo de la petición es inválido. El
sistema MUST NOT exponer la traza de la excepción ni el detalle interno del parser.

#### Scenario: JSON malformado

- **WHEN** un usuario autenticado envía una petición `POST` o `PUT` con un cuerpo
  que no es JSON válido
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar

#### Scenario: Fecha con formato inválido

- **WHEN** un usuario autenticado envía un cuerpo cuyo campo de fecha no respeta el
  formato `YYYY-MM-DD`
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar

#### Scenario: Campo numérico con un valor no numérico

- **WHEN** un usuario autenticado envía un cuerpo cuyo campo de monto contiene texto
  en lugar de un número
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar

### Requirement: Parámetro de ruta con tipo incorrecto

Cuando un parámetro de ruta declarado como numérico recibe un valor que no se puede
convertir (por ejemplo `GET /api/grupos/abc`), el sistema SHALL responder `400 Bad
Request` con el formato de error estándar y un `message` que nombre el parámetro
ofensor.

#### Scenario: Identificador de ruta no numérico

- **WHEN** un usuario autenticado envía una petición a una ruta cuyo identificador
  numérico contiene texto
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar
- **AND** el `message` menciona el nombre del parámetro que no se pudo convertir

### Requirement: Método HTTP no permitido

Cuando una ruta existente recibe un método HTTP que no admite, el sistema SHALL
responder `405 Method Not Allowed` con el formato de error estándar.

#### Scenario: Método no soportado sobre una ruta existente

- **WHEN** un usuario autenticado envía un método HTTP que la ruta no expone
- **THEN** el sistema responde `405 Method Not Allowed` con el formato de error
  estándar

### Requirement: Ruta inexistente

Cuando la ruta solicitada bajo `/api` no corresponde a ningún endpoint, el sistema
SHALL responder `404 Not Found` con el formato de error estándar.

#### Scenario: Ruta que no existe

- **WHEN** un usuario autenticado envía una petición a una ruta bajo `/api` que no
  está expuesta
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

### Requirement: Violación de integridad de datos

Cuando una operación de escritura viola una restricción de la base de datos que la
validación de la aplicación no anticipó (unicidad, clave foránea, restricción
`CHECK`), el sistema SHALL responder `409 Conflict` con el formato de error estándar
y un `message` genérico. El sistema MUST NOT exponer el nombre de la tabla, de la
restricción ni la sentencia SQL.

#### Scenario: Restricción de base de datos violada

- **WHEN** una operación de escritura viola una restricción de integridad no
  contemplada por la validación previa
- **THEN** el sistema responde `409 Conflict` con el formato de error estándar
- **AND** el `message` no contiene nombres de tablas, restricciones ni SQL

### Requirement: Excepción imprevista

Cuando se produce cualquier excepción que ningún handler específico contempla, el
sistema SHALL responder `500 Internal Server Error` con el formato de error estándar
y un `message` genérico fijo. El sistema SHALL registrar la excepción completa, con
su traza, en el log del servidor con nivel `ERROR`. La respuesta MUST NOT incluir el
mensaje de la excepción, su tipo, su traza, ni ninguna ruta de clase o de archivo.

#### Scenario: Fallo no contemplado

- **WHEN** una petición provoca una excepción que ningún handler específico maneja
- **THEN** el sistema responde `500 Internal Server Error` con el formato de error
  estándar
- **AND** el `message` es un texto genérico que no revela detalles internos
- **AND** la excepción completa queda registrada en el log del servidor

#### Scenario: Los handlers específicos tienen precedencia

- **WHEN** una petición provoca una excepción para la que existe un handler
  específico
- **THEN** el sistema responde con el código de ese handler y no con `500`

### Requirement: Acceso denegado por una regla declarativa

Cuando la autorización declarativa de Spring Security rechaza una petición
autenticada, el sistema SHALL responder `403 Forbidden` con el formato de error
estándar, igual que cuando la rechaza una regla de negocio del servicio.

#### Scenario: Rechazo por regla declarativa

- **WHEN** una petición con un token válido es rechazada por una regla de
  autorización declarativa
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar
