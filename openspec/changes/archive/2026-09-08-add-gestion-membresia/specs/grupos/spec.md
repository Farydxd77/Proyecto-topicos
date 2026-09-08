## ADDED Requirements

### Requirement: Transferir el rol de creador

El sistema SHALL exponer `PUT /api/grupos/{id}/creador` reservado al creador actual
del grupo. El cuerpo SHALL incluir `participanteId` (obligatorio), que SHALL
corresponder a un miembro actual del grupo distinto del creador. Ante datos válidos
el sistema SHALL fijar ese participante como nuevo creador y SHALL responder `200
OK` con el grupo completo actualizado. El creador saliente SHALL seguir siendo
miembro del grupo, perdiendo únicamente los privilegios de creador. La operación
SHALL ser idempotente en su efecto: el estado resultante depende solo del
`participanteId` recibido.

El sistema SHALL responder `404 Not Found` cuando el grupo no existe, `403
Forbidden` cuando el solicitante no es el creador (sea miembro o no), y `400 Bad
Request` cuando el `participanteId` no es miembro del grupo o es el creador actual.

#### Scenario: El creador transfiere el rol a otro miembro

- **WHEN** el creador envía `PUT /api/grupos/{id}/creador` con el `participanteId`
  de otro miembro del grupo
- **THEN** el sistema responde `200 OK` con el grupo, cuyo `creador` es ahora ese
  participante
- **AND** el creador saliente sigue apareciendo en los `miembros` del grupo

#### Scenario: El nuevo creador puede ejercer los privilegios

- **WHEN** tras la transferencia el nuevo creador envía `PUT /api/grupos/{id}` con
  un nombre distinto
- **THEN** el sistema responde `200 OK` con el grupo actualizado

#### Scenario: El creador saliente pierde los privilegios

- **WHEN** tras la transferencia el creador saliente envía `PUT /api/grupos/{id}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar

#### Scenario: El destinatario no es miembro del grupo

- **WHEN** el creador envía `PUT /api/grupos/{id}/creador` con el `participanteId`
  de alguien que no pertenece al grupo
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar
- **AND** el creador del grupo no cambia

#### Scenario: El destinatario es el creador actual

- **WHEN** el creador envía `PUT /api/grupos/{id}/creador` con su propio
  `participanteId`
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar

#### Scenario: Un miembro no creador intenta transferir el rol

- **WHEN** un miembro que no es el creador envía `PUT /api/grupos/{id}/creador`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar
- **AND** el creador del grupo no cambia

#### Scenario: El grupo no existe

- **WHEN** un usuario autenticado envía `PUT /api/grupos/{id}/creador` con un `id`
  inexistente
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

## MODIFIED Requirements

### Requirement: Quitar un miembro del grupo

El sistema SHALL exponer `DELETE /api/grupos/{id}/miembros/{participanteId}`, que
cubre dos operaciones según quién la solicite:

- **Expulsar a otro**: cuando el `participanteId` es distinto del solicitante, la
  operación SHALL estar reservada al creador del grupo.
- **Abandonar el grupo**: cuando el `participanteId` es el del propio solicitante,
  cualquier miembro PUEDE ejecutarla, excepto el creador.

En ambos casos, si el participante es miembro del grupo, el sistema SHALL eliminar
su membresía y SHALL responder `204 No Content` sin cuerpo. Quitar a un miembro NO
SHALL modificar los gastos ni los pagos ya registrados: su división y sus montos
quedan intactos, y el participante sigue apareciendo en los balances del grupo
mientras conserve saldo.

El sistema SHALL responder `400 Bad Request` cuando el `participanteId` corresponde
al creador del grupo, porque el creador no puede quitarse a sí mismo ni ser
expulsado: primero debe transferir el rol, o eliminar el grupo si es su único
miembro. El sistema SHALL responder `403 Forbidden` cuando el solicitante no es
miembro del grupo, y también cuando un miembro no creador intenta quitar a otro
participante. El sistema SHALL responder `404 Not Found` cuando el grupo no existe o
cuando el participante indicado no es miembro del grupo.

#### Scenario: El creador quita a un miembro

- **WHEN** el creador envía `DELETE /api/grupos/{id}/miembros/{participanteId}`
  con el id de un miembro distinto de sí mismo
- **THEN** el sistema responde `204 No Content` sin cuerpo
- **AND** ese participante ya no aparece en los miembros del grupo
- **AND** ese participante deja de ver el grupo en su `GET /api/grupos`

#### Scenario: Un miembro intenta abandonar el grupo por su cuenta

- **WHEN** un miembro que no es el creador envía
  `DELETE /api/grupos/{id}/miembros/{participanteId}` con su propio id
- **THEN** el sistema responde `204 No Content` sin cuerpo
- **AND** deja de aparecer en los miembros del grupo
- **AND** el grupo deja de aparecer en su `GET /api/grupos`

#### Scenario: Quien abandona conserva su saldo en los balances

- **WHEN** un miembro con saldo distinto de cero abandona el grupo
- **THEN** sigue apareciendo en `GET /api/grupos/{id}/balances` con su mismo
  `balance`
- **AND** la suma de todos los balances del grupo sigue siendo exactamente `0.00`

#### Scenario: El creador intenta quitarse a sí mismo

- **WHEN** el creador envía `DELETE /api/grupos/{id}/miembros/{participanteId}`
  con su propio `participanteId`
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar
- **AND** la membresía del grupo no cambia

#### Scenario: El creador puede salir después de transferir el rol

- **WHEN** el creador transfiere el rol a otro miembro y luego envía `DELETE
  /api/grupos/{id}/miembros/{participanteId}` con su propio id
- **THEN** el sistema responde `204 No Content` sin cuerpo
- **AND** deja de aparecer en los miembros del grupo

#### Scenario: El participante no es miembro del grupo

- **WHEN** el creador envía `DELETE /api/grupos/{id}/miembros/{participanteId}`
  con el id de un participante que no pertenece al grupo
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: Un miembro no creador intenta quitar a otro

- **WHEN** un miembro que no es el creador envía
  `DELETE /api/grupos/{id}/miembros/{participanteId}` con el id de un tercero
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar
- **AND** la membresía del grupo no cambia

#### Scenario: Quien no es miembro intenta quitar a alguien

- **WHEN** un usuario autenticado que no pertenece al grupo envía
  `DELETE /api/grupos/{id}/miembros/{participanteId}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar
