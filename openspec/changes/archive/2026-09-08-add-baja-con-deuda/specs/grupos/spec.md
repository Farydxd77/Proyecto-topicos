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

Cuando el participante que sale tiene un `balance` distinto de `0.00`, el sistema
SHALL registrar además una **baja pendiente de decisión** con ese saldo congelado,
según la capacidad `bajas`. Cuando su balance es exactamente `0.00`, el sistema MUST
NOT registrar ninguna baja. El registro de la baja NO SHALL alterar por sí solo
ningún balance ni impedir la salida.

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

#### Scenario: Salir con saldo deja una baja pendiente

- **WHEN** un miembro con `balance` distinto de `0.00` sale del grupo, sea por su
  cuenta o quitado por el creador
- **THEN** queda registrada una baja `PENDIENTE` con ese saldo
- **AND** aparece en `GET /api/grupos/{id}/bajas`

#### Scenario: Salir sin saldo no deja ninguna baja

- **WHEN** sale del grupo un miembro cuyo `balance` es exactamente `0.00`
- **THEN** no se registra ninguna baja
- **AND** `GET /api/grupos/{id}/bajas` no lo incluye

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
