## Purpose

Permitir que un usuario autenticado cree grupos de viaje, consulte los grupos a
los que pertenece con su lista de miembros, y —cuando es el creador del grupo—
edite sus datos, lo elimine y administre quiénes lo integran. La capacidad
establece la frontera de visibilidad de todo lo que vendrá después: un usuario
solo ve y opera sobre los grupos de los que es miembro.

## ADDED Requirements

### Requirement: Crear un grupo

El sistema SHALL exponer `POST /api/grupos` para usuarios autenticados con token
JWT válido. El cuerpo SHALL aceptar `nombre` (obligatorio, no vacío, máximo 100
caracteres) y `descripcion` (opcional). Cuando los datos son válidos, el sistema
SHALL crear el grupo con el participante del usuario autenticado como creador,
SHALL agregarlo automáticamente como primer miembro del grupo, y SHALL responder
`201 Created` con `id`, `nombre`, `descripcion`, el creador y la lista de
miembros. Si `nombre` falta, está vacío o solo contiene espacios, el sistema SHALL
responder `400 Bad Request` con el formato de error estándar y no SHALL crear
ningún grupo.

#### Scenario: Creación con datos válidos

- **WHEN** un usuario autenticado envía `POST /api/grupos` con
  `{"nombre": "Viaje a Uyuni", "descripcion": "Enero 2026"}`
- **THEN** el sistema responde `201 Created` con el grupo creado (`id`, `nombre`,
  `descripcion`, `creador`, `miembros`)
- **AND** el `creador` corresponde al participante del usuario autenticado
- **AND** la lista `miembros` contiene exactamente un elemento: ese mismo creador

#### Scenario: Creación sin descripción

- **WHEN** un usuario autenticado envía `POST /api/grupos` con solo
  `{"nombre": "Cumpleaños"}`
- **THEN** el sistema responde `201 Created` con `descripcion` nula
- **AND** el creador queda agregado como único miembro

#### Scenario: Nombre vacío

- **WHEN** un usuario autenticado envía `POST /api/grupos` con `nombre` ausente,
  vacío o compuesto solo de espacios
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar
- **AND** no se crea ningún grupo

#### Scenario: Petición sin token

- **WHEN** se envía `POST /api/grupos` sin cabecera `Authorization` o con un token
  inválido o expirado
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar

### Requirement: Listar los grupos propios

El sistema SHALL exponer `GET /api/grupos` que devuelve `200 OK` con un array
JSON de los grupos en los que el usuario autenticado es miembro, sea creador o
no. Cada elemento SHALL incluir al menos `id`, `nombre`, `descripcion` y el
creador del grupo. El sistema MUST NOT incluir grupos en los que el usuario no es
miembro. Si el usuario no pertenece a ningún grupo, el sistema SHALL responder
`200 OK` con un array vacío `[]`.

#### Scenario: El usuario pertenece a varios grupos

- **WHEN** un usuario autenticado que es miembro de dos grupos envía
  `GET /api/grupos`
- **THEN** el sistema responde `200 OK` con un array de esos dos grupos
- **AND** cada elemento incluye `id`, `nombre`, `descripcion` y `creador`

#### Scenario: El usuario no pertenece a ningún grupo

- **WHEN** un usuario autenticado que no es miembro de ningún grupo envía
  `GET /api/grupos`
- **THEN** el sistema responde `200 OK` con `[]`

#### Scenario: Existen grupos ajenos

- **WHEN** un usuario autenticado envía `GET /api/grupos` y existen grupos creados
  por otros usuarios en los que él no es miembro
- **THEN** la respuesta no incluye ninguno de esos grupos ajenos

#### Scenario: Petición sin token

- **WHEN** se envía `GET /api/grupos` sin token válido
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar

### Requirement: Ver el detalle de un grupo

El sistema SHALL exponer `GET /api/grupos/{id}` que, cuando el grupo existe y el
usuario autenticado es miembro, devuelve `200 OK` con `id`, `nombre`,
`descripcion`, el creador y la lista completa de miembros. Cada miembro y el
creador SHALL identificarse con `id`, `nombre`, `apellido`, `ci` y `username`, y
la respuesta MUST NOT incluir contraseñas ni sus hashes. Si el grupo no existe, el
sistema SHALL responder `404 Not Found`. Si el grupo existe pero el usuario
autenticado no es miembro, el sistema SHALL responder `403 Forbidden`. Ambos
errores SHALL usar el formato de error estándar.

#### Scenario: Un miembro consulta el grupo

- **WHEN** un usuario autenticado que es miembro del grupo envía
  `GET /api/grupos/{id}`
- **THEN** el sistema responde `200 OK` con `nombre`, `descripcion`, `creador` y
  `miembros`
- **AND** la lista `miembros` incluye a todos los participantes del grupo,
  incluido el creador
- **AND** ningún elemento de la respuesta contiene contraseña ni su hash

#### Scenario: Un no miembro consulta el grupo

- **WHEN** un usuario autenticado que no es miembro del grupo envía
  `GET /api/grupos/{id}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar

#### Scenario: El grupo no existe

- **WHEN** un usuario autenticado envía `GET /api/grupos/{id}` con un `id`
  inexistente
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: Petición sin token

- **WHEN** se envía `GET /api/grupos/{id}` sin token válido
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar

### Requirement: Editar un grupo

El sistema SHALL exponer `PUT /api/grupos/{id}` que permite actualizar `nombre`
y/o `descripcion` del grupo. La operación SHALL estar reservada al creador del
grupo. Cuando el solicitante es el creador y los datos son válidos, el sistema
SHALL responder `200 OK` con el grupo actualizado y su lista de miembros. El
sistema SHALL responder `403 Forbidden` cuando el solicitante es miembro pero no
creador, y también cuando no es miembro. El sistema SHALL responder `404 Not
Found` cuando el grupo no existe, y `400 Bad Request` cuando `nombre` está vacío o
solo contiene espacios. La edición MUST NOT alterar la lista de miembros ni el
creador del grupo.

#### Scenario: El creador edita el grupo

- **WHEN** el creador envía `PUT /api/grupos/{id}` con un `nombre` y una
  `descripcion` válidos
- **THEN** el sistema responde `200 OK` con los valores actualizados
- **AND** el creador y la lista de miembros permanecen sin cambios

#### Scenario: Un miembro no creador intenta editar

- **WHEN** un miembro que no es el creador envía `PUT /api/grupos/{id}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar
- **AND** el grupo no se modifica

#### Scenario: Un no miembro intenta editar

- **WHEN** un usuario autenticado que no es miembro del grupo envía
  `PUT /api/grupos/{id}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar

#### Scenario: Nombre vacío

- **WHEN** el creador envía `PUT /api/grupos/{id}` con `nombre` ausente, vacío o
  solo con espacios
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar
- **AND** el grupo no se modifica

#### Scenario: El grupo no existe

- **WHEN** un usuario autenticado envía `PUT /api/grupos/{id}` con un `id`
  inexistente
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

### Requirement: Eliminar un grupo

El sistema SHALL exponer `DELETE /api/grupos/{id}` reservado al creador del grupo.
Cuando el solicitante es el creador, el sistema SHALL eliminar el grupo junto con
todos sus registros de membresía y SHALL responder `204 No Content` sin cuerpo.
Tras la eliminación, el grupo MUST NOT aparecer en `GET /api/grupos` de ninguno de
sus antiguos miembros y `GET /api/grupos/{id}` SHALL responder `404 Not Found`. El
sistema SHALL responder `403 Forbidden` cuando el solicitante es miembro pero no
creador, o cuando no es miembro, y `404 Not Found` cuando el grupo no existe.

#### Scenario: El creador elimina el grupo

- **WHEN** el creador envía `DELETE /api/grupos/{id}`
- **THEN** el sistema responde `204 No Content` sin cuerpo
- **AND** se eliminan también todos los registros de membresía del grupo
- **AND** una consulta posterior a `GET /api/grupos/{id}` responde `404 Not Found`

#### Scenario: Un miembro no creador intenta eliminar

- **WHEN** un miembro que no es el creador envía `DELETE /api/grupos/{id}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar
- **AND** el grupo sigue existiendo

#### Scenario: El grupo no existe

- **WHEN** un usuario autenticado envía `DELETE /api/grupos/{id}` con un `id`
  inexistente
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

### Requirement: Agregar un miembro al grupo

El sistema SHALL exponer `POST /api/grupos/{id}/miembros` reservado al creador del
grupo. El cuerpo SHALL aceptar `participanteId` (obligatorio). Cuando el
participante existe y aún no pertenece al grupo, el sistema SHALL agregarlo y
SHALL responder `201 Created` con la lista actualizada de miembros del grupo. El
sistema SHALL responder `409 Conflict` cuando el participante ya es miembro,
`404 Not Found` cuando el grupo o el participante no existen, `400 Bad Request`
cuando falta `participanteId`, y `403 Forbidden` cuando el solicitante no es el
creador del grupo. Todos los errores SHALL usar el formato de error estándar.

#### Scenario: El creador agrega un participante nuevo

- **WHEN** el creador envía `POST /api/grupos/{id}/miembros` con el
  `participanteId` de alguien que aún no es miembro
- **THEN** el sistema responde `201 Created` con la lista actualizada de miembros
- **AND** la lista incluye al participante agregado
- **AND** ese participante ve el grupo en su `GET /api/grupos`

#### Scenario: El participante ya es miembro

- **WHEN** el creador envía `POST /api/grupos/{id}/miembros` con el
  `participanteId` de alguien que ya pertenece al grupo
- **THEN** el sistema responde `409 Conflict` con el formato de error estándar
- **AND** la membresía del grupo no cambia

#### Scenario: El participante no existe

- **WHEN** el creador envía `POST /api/grupos/{id}/miembros` con un
  `participanteId` inexistente
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: Falta el participanteId

- **WHEN** el creador envía `POST /api/grupos/{id}/miembros` sin `participanteId`
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar

#### Scenario: Un miembro no creador intenta agregar

- **WHEN** un miembro que no es el creador envía `POST /api/grupos/{id}/miembros`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar
- **AND** la membresía del grupo no cambia

### Requirement: Quitar un miembro del grupo

El sistema SHALL exponer `DELETE /api/grupos/{id}/miembros/{participanteId}`
reservado al creador del grupo. Cuando el participante es miembro del grupo y no
es el creador, el sistema SHALL eliminar su membresía y SHALL responder `204 No
Content` sin cuerpo. El sistema SHALL responder `400 Bad Request` cuando el
`participanteId` corresponde al creador del grupo, porque el creador no puede
quitarse a sí mismo. El sistema SHALL responder `404 Not Found` cuando el grupo no
existe o cuando el participante indicado no es miembro del grupo, y
`403 Forbidden` cuando el solicitante no es el creador.

#### Scenario: El creador quita a un miembro

- **WHEN** el creador envía `DELETE /api/grupos/{id}/miembros/{participanteId}`
  con el id de un miembro distinto de sí mismo
- **THEN** el sistema responde `204 No Content` sin cuerpo
- **AND** ese participante ya no aparece en los miembros del grupo
- **AND** ese participante deja de ver el grupo en su `GET /api/grupos`

#### Scenario: El creador intenta quitarse a sí mismo

- **WHEN** el creador envía `DELETE /api/grupos/{id}/miembros/{participanteId}`
  con su propio `participanteId`
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar
- **AND** la membresía del grupo no cambia

#### Scenario: El participante no es miembro del grupo

- **WHEN** el creador envía `DELETE /api/grupos/{id}/miembros/{participanteId}`
  con el id de un participante que no pertenece al grupo
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: Un miembro no creador intenta quitar a otro

- **WHEN** un miembro que no es el creador envía
  `DELETE /api/grupos/{id}/miembros/{participanteId}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar
- **AND** la membresía del grupo no cambia

#### Scenario: Un miembro intenta abandonar el grupo por su cuenta

- **WHEN** un miembro que no es el creador envía
  `DELETE /api/grupos/{id}/miembros/{participanteId}` con su propio id
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar
- **AND** sigue siendo miembro del grupo
