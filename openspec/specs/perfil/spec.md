# perfil Specification

## Purpose

Permitir que un usuario autenticado consulte y edite su propia cuenta: sus datos
de acceso (username y password) y su información personal vinculada (nombre y
apellido del participante). Todas las operaciones actúan sobre el usuario del
token JWT y nunca sobre la cuenta de otro usuario.

## Requirements

### Requirement: Consultar el perfil propio

El sistema SHALL exponer `GET /api/perfil/me` que, para un usuario autenticado con
token JWT válido, devuelve 200 OK con los datos de su usuario y de su participante
vinculado: `id` (del participante), `usuarioId`, `username`, `nombre`, `apellido`,
`ci` y `createdAt`. La respuesta MUST NOT incluir la contraseña ni su hash bajo
ninguna circunstancia.

#### Scenario: Usuario autenticado consulta su perfil

- **WHEN** un usuario autenticado envía `GET /api/perfil/me` con un token JWT válido
- **THEN** el sistema responde 200 OK con `id`, `usuarioId`, `username`, `nombre`,
  `apellido`, `ci` y `createdAt`
- **AND** el cuerpo de la respuesta no contiene ningún campo de contraseña

#### Scenario: Petición sin token

- **WHEN** se envía `GET /api/perfil/me` sin cabecera `Authorization` o con un token
  inválido o expirado
- **THEN** el sistema responde 401 Unauthorized con el formato de error estándar

#### Scenario: Usuario del token sin participante vinculado

- **WHEN** un usuario autenticado envía `GET /api/perfil/me` pero no existe un
  participante vinculado a su `usuarioId`
- **THEN** el sistema responde 404 Not Found con el formato de error estándar

### Requirement: Actualizar datos personales del perfil propio

El sistema SHALL exponer `PUT /api/perfil/me` que actualiza el `nombre` y el
`apellido` del participante vinculado al usuario autenticado y devuelve 200 OK con
el perfil actualizado (misma forma que `GET /api/perfil/me`). El campo `ci` NO es
editable: si se envía en el cuerpo, el sistema SHALL ignorarlo y conservar el `ci`
registrado. `nombre` y `apellido` MUST ser cadenas no vacías de hasta 100
caracteres.

#### Scenario: Actualización válida de nombre y apellido

- **WHEN** un usuario autenticado envía `PUT /api/perfil/me` con `nombre` y
  `apellido` no vacíos
- **THEN** el sistema persiste los nuevos valores en el participante vinculado
- **AND** responde 200 OK con el perfil actualizado sin campos de contraseña

#### Scenario: El CI enviado en el cuerpo se ignora

- **WHEN** un usuario autenticado envía `PUT /api/perfil/me` incluyendo un `ci`
  distinto al registrado
- **THEN** el sistema responde 200 OK y el `ci` del participante permanece sin
  cambios

#### Scenario: Nombre o apellido vacíos

- **WHEN** un usuario autenticado envía `PUT /api/perfil/me` con `nombre` o
  `apellido` vacío, en blanco o ausente
- **THEN** el sistema responde 400 Bad Request con el formato de error estándar y no
  modifica el participante

#### Scenario: Petición sin token

- **WHEN** se envía `PUT /api/perfil/me` sin token válido
- **THEN** el sistema responde 401 Unauthorized con el formato de error estándar

### Requirement: Cambiar el username propio

El sistema SHALL exponer `PUT /api/perfil/me/username` que cambia el `username` del
usuario autenticado. El nuevo `username` MUST tener entre 3 y 50 caracteres. Si el
nuevo `username` ya existe en la base de datos (y no es el actual del propio
usuario), el sistema SHALL responder 409 Conflict. En caso de éxito devuelve 200 OK
con el perfil actualizado.

#### Scenario: Cambio a un username disponible

- **WHEN** un usuario autenticado envía `PUT /api/perfil/me/username` con un
  `username` de 3 a 50 caracteres que no existe en la base de datos
- **THEN** el sistema actualiza el `username` del usuario
- **AND** responde 200 OK con el perfil actualizado

#### Scenario: Cambio a un username ya en uso

- **WHEN** un usuario autenticado envía `PUT /api/perfil/me/username` con un
  `username` que ya pertenece a otro usuario
- **THEN** el sistema responde 409 Conflict con el formato de error estándar y no
  modifica el usuario

#### Scenario: Username demasiado corto o vacío

- **WHEN** un usuario autenticado envía `PUT /api/perfil/me/username` con un
  `username` de menos de 3 caracteres, vacío o ausente
- **THEN** el sistema responde 400 Bad Request con el formato de error estándar

#### Scenario: Cambio al mismo username actual

- **WHEN** un usuario autenticado envía `PUT /api/perfil/me/username` con su
  `username` actual
- **THEN** el sistema responde 200 OK sin reportar conflicto

### Requirement: Cambiar la contraseña propia

El sistema SHALL exponer `PUT /api/perfil/me/password` que cambia la contraseña del
usuario autenticado. La nueva contraseña MUST tener al menos 8 caracteres. La
contraseña se almacena hasheada con BCrypt y nunca en texto plano. En caso de éxito
devuelve 200 OK.

#### Scenario: Cambio de contraseña válido

- **WHEN** un usuario autenticado envía `PUT /api/perfil/me/password` con una
  contraseña de 8 o más caracteres
- **THEN** el sistema almacena el hash BCrypt de la nueva contraseña
- **AND** responde 200 OK
- **AND** la respuesta no contiene la contraseña ni su hash

#### Scenario: Contraseña demasiado corta

- **WHEN** un usuario autenticado envía `PUT /api/perfil/me/password` con una
  contraseña de menos de 8 caracteres, vacía o ausente
- **THEN** el sistema responde 400 Bad Request con el formato de error estándar y no
  modifica la contraseña

#### Scenario: Petición sin token

- **WHEN** se envía `PUT /api/perfil/me/password` sin token válido
- **THEN** el sistema responde 401 Unauthorized con el formato de error estándar

### Requirement: Aislamiento entre cuentas de usuario

Todos los endpoints bajo `/api/perfil/me` SHALL operar exclusivamente sobre el
usuario identificado por el token JWT. El sistema MUST NOT aceptar un identificador
de usuario o de participante en la ruta, la query o el cuerpo para actuar sobre la
cuenta de otro usuario.

#### Scenario: No existe forma de apuntar a otra cuenta

- **WHEN** un usuario autenticado usa cualquier endpoint `/api/perfil/me`
- **THEN** el sistema resuelve el usuario y el participante únicamente a partir del
  token JWT
- **AND** cualquier identificador ajeno presente en el cuerpo se ignora
