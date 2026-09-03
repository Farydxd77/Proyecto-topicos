# gestion-general Specification

## Purpose

Permitir que cualquier usuario autenticado consulte de solo lectura el directorio
de usuarios y de participantes del sistema: listarlos completos, obtenerlos por
identificador y filtrarlos por campos simples. Ninguna respuesta expone la
contraseña ni su hash. La capacidad queda preparada para restringirse por roles de
administrador en el futuro sin cambiar su contrato de datos.

## Requirements

### Requirement: Listar todos los usuarios

El sistema SHALL exponer `GET /api/usuarios` que, para un usuario autenticado con
token JWT válido, devuelve `200 OK` con un array JSON de todos los usuarios. Cada
elemento SHALL incluir `id` y `username`. La respuesta MUST NOT incluir la
contraseña ni su hash bajo ninguna circunstancia. Si no hay usuarios, el sistema
SHALL devolver `200 OK` con un array vacío `[]`.

#### Scenario: Usuario autenticado lista los usuarios

- **WHEN** un usuario autenticado envía `GET /api/usuarios` con un token JWT válido
- **THEN** el sistema responde `200 OK` con un array JSON donde cada elemento tiene
  `id` y `username`
- **AND** ningún elemento contiene un campo de contraseña ni su hash

#### Scenario: No hay usuarios registrados

- **WHEN** un usuario autenticado envía `GET /api/usuarios` y no existe ningún
  usuario
- **THEN** el sistema responde `200 OK` con `[]`

#### Scenario: Petición sin token

- **WHEN** se envía `GET /api/usuarios` sin cabecera `Authorization` o con un token
  inválido o expirado
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar

### Requirement: Buscar usuarios por username

El sistema SHALL aceptar el parámetro de query opcional `username` en
`GET /api/usuarios`. Cuando está presente, el sistema SHALL devolver `200 OK` con
un array JSON de los usuarios cuyo `username` contiene el texto indicado, sin
distinguir mayúsculas de minúsculas. Si ningún usuario coincide, el sistema SHALL
devolver `200 OK` con `[]`. La respuesta MUST NOT incluir la contraseña.

#### Scenario: Existen usuarios que coinciden

- **WHEN** un usuario autenticado envía `GET /api/usuarios?username={texto}` y al
  menos un usuario contiene ese texto en su `username`
- **THEN** el sistema responde `200 OK` con un array de esos usuarios (`id`,
  `username`), sin contraseña

#### Scenario: La búsqueda no distingue mayúsculas

- **WHEN** un usuario autenticado busca con un `username` en distinto caso al
  almacenado (por ejemplo `ANA` frente a `ana`)
- **THEN** el sistema incluye en el resultado los usuarios que coinciden ignorando
  mayúsculas y minúsculas

#### Scenario: Ningún usuario coincide

- **WHEN** un usuario autenticado envía `GET /api/usuarios?username={texto}` y
  ningún `username` contiene ese texto
- **THEN** el sistema responde `200 OK` con `[]`

### Requirement: Obtener un usuario por id

El sistema SHALL exponer `GET /api/usuarios/{id}` que devuelve `200 OK` con `id` y
`username` del usuario indicado. La respuesta MUST NOT incluir la contraseña. Si no
existe un usuario con ese `id`, el sistema SHALL devolver `404 Not Found` con el
formato de error estándar (`timestamp`, `status`, `error`, `message`, `path`).

#### Scenario: El usuario existe

- **WHEN** un usuario autenticado envía `GET /api/usuarios/{id}` y existe un usuario
  con ese `id`
- **THEN** el sistema responde `200 OK` con `id` y `username`
- **AND** la respuesta no contiene ningún campo de contraseña

#### Scenario: El usuario no existe

- **WHEN** un usuario autenticado envía `GET /api/usuarios/{id}` y no existe un
  usuario con ese `id`
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

### Requirement: Obtener el participante vinculado a un usuario

El sistema SHALL exponer `GET /api/usuarios/{id}/participante` que devuelve
`200 OK` con los datos del participante vinculado al usuario indicado (`id`,
`nombre`, `apellido`, `ci`, `username`). Si no existe un usuario con ese `id`, o el
usuario existe pero no tiene un participante vinculado, el sistema SHALL devolver
`404 Not Found` con el formato de error estándar.

#### Scenario: El usuario existe y tiene participante

- **WHEN** un usuario autenticado envía `GET /api/usuarios/{id}/participante` y ese
  usuario tiene un participante vinculado
- **THEN** el sistema responde `200 OK` con `id`, `nombre`, `apellido`, `ci` y
  `username` del participante

#### Scenario: El usuario no existe

- **WHEN** un usuario autenticado envía `GET /api/usuarios/{id}/participante` y no
  existe un usuario con ese `id`
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: El usuario existe pero no tiene participante vinculado

- **WHEN** un usuario autenticado envía `GET /api/usuarios/{id}/participante` para
  un usuario sin participante vinculado
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

### Requirement: Listar todos los participantes

El sistema SHALL exponer `GET /api/participantes` que, para un usuario autenticado
con token JWT válido, devuelve `200 OK` con un array JSON de todos los
participantes. Cada elemento SHALL incluir `id`, `nombre`, `apellido`, `ci` y
`username` (el `username` del usuario vinculado). Si no hay participantes, el
sistema SHALL devolver `200 OK` con `[]`.

#### Scenario: Usuario autenticado lista los participantes

- **WHEN** un usuario autenticado envía `GET /api/participantes` con un token JWT
  válido
- **THEN** el sistema responde `200 OK` con un array JSON donde cada elemento tiene
  `id`, `nombre`, `apellido`, `ci` y `username`

#### Scenario: No hay participantes registrados

- **WHEN** un usuario autenticado envía `GET /api/participantes` y no existe ningún
  participante
- **THEN** el sistema responde `200 OK` con `[]`

#### Scenario: Petición sin token

- **WHEN** se envía `GET /api/participantes` sin token válido
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar

### Requirement: Buscar participantes por nombre, apellido o CI

El sistema SHALL aceptar en `GET /api/participantes` los parámetros de query
opcionales `nombre`, `apellido` y `ci`. `nombre` y `apellido` SHALL filtrar por
coincidencia parcial sin distinguir mayúsculas; `ci` SHALL filtrar por coincidencia
exacta. En todos los casos el sistema SHALL devolver `200 OK` con un array JSON de
los participantes que coinciden, o `[]` si no hay coincidencias. Cuando se envía
más de uno de estos parámetros, el sistema SHALL aplicar exactamente uno con la
precedencia `ci` > `nombre` > `apellido` e ignorar los demás.

#### Scenario: Búsqueda por nombre parcial

- **WHEN** un usuario autenticado envía `GET /api/participantes?nombre={texto}` y al
  menos un participante contiene ese texto en su `nombre` (ignorando mayúsculas)
- **THEN** el sistema responde `200 OK` con el array de participantes que coinciden

#### Scenario: Búsqueda por apellido parcial

- **WHEN** un usuario autenticado envía `GET /api/participantes?apellido={texto}` y
  al menos un participante contiene ese texto en su `apellido` (ignorando
  mayúsculas)
- **THEN** el sistema responde `200 OK` con el array de participantes que coinciden

#### Scenario: Búsqueda por CI exacto

- **WHEN** un usuario autenticado envía `GET /api/participantes?ci={texto}` y existe
  un participante con ese `ci`
- **THEN** el sistema responde `200 OK` con un array que contiene ese participante

#### Scenario: Búsqueda sin coincidencias

- **WHEN** un usuario autenticado filtra por `nombre`, `apellido` o `ci` y ningún
  participante coincide
- **THEN** el sistema responde `200 OK` con `[]`

#### Scenario: Se envían varios parámetros de filtro

- **WHEN** un usuario autenticado envía `GET /api/participantes` con `ci` y también
  `nombre` y/o `apellido`
- **THEN** el sistema aplica solo el filtro por `ci` e ignora `nombre` y `apellido`

### Requirement: Obtener un participante por id

El sistema SHALL exponer `GET /api/participantes/{id}` que devuelve `200 OK` con
`id`, `nombre`, `apellido`, `ci` y `username` del participante indicado. Si no
existe un participante con ese `id`, el sistema SHALL devolver `404 Not Found` con
el formato de error estándar.

#### Scenario: El participante existe

- **WHEN** un usuario autenticado envía `GET /api/participantes/{id}` y existe un
  participante con ese `id`
- **THEN** el sistema responde `200 OK` con `id`, `nombre`, `apellido`, `ci` y
  `username`

#### Scenario: El participante no existe

- **WHEN** un usuario autenticado envía `GET /api/participantes/{id}` y no existe un
  participante con ese `id`
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

### Requirement: Ninguna respuesta expone la contraseña

Ningún endpoint de esta capacidad SHALL incluir la contraseña del usuario ni su
hash BCrypt en la respuesta, ni en las representaciones de usuario ni en las de
participante, ni en listas ni en recursos individuales.

#### Scenario: Las representaciones de usuario no llevan contraseña

- **WHEN** un usuario autenticado obtiene cualquier respuesta de `/api/usuarios` o
  `/api/usuarios/{id}`
- **THEN** el cuerpo no contiene ningún campo `password` ni su hash

#### Scenario: Las representaciones de participante no llevan contraseña

- **WHEN** un usuario autenticado obtiene cualquier respuesta de
  `/api/participantes`, `/api/participantes/{id}` o
  `/api/usuarios/{id}/participante`
- **THEN** el cuerpo no contiene ningún campo `password` ni su hash

### Requirement: Acceso restringido a usuarios autenticados

Todos los endpoints bajo `/api/usuarios/**` y `/api/participantes/**` SHALL exigir
un token JWT válido. Una petición sin token, con token inválido o expirado SHALL
recibir `401 Unauthorized` con el formato de error estándar. No se aplican
restricciones adicionales por rol o por pertenencia a grupos en esta fase.

#### Scenario: Petición sin token a cualquier endpoint de la capacidad

- **WHEN** se envía cualquier `GET` a `/api/usuarios/**` o `/api/participantes/**`
  sin un token JWT válido
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar

#### Scenario: Cualquier usuario autenticado puede consultar

- **WHEN** un usuario autenticado cualquiera consulta estos endpoints
- **THEN** el sistema responde con los datos solicitados sin comprobar rol ni
  pertenencia a grupos
