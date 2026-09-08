# bajas Specification

## Purpose
TBD - created by archiving change 2026-09-08-add-baja-con-deuda. Update Purpose after archive.

## Requirements

### Requirement: Registrar la baja de un participante con saldo pendiente

Cuando un participante deja de ser miembro de un grupo —porque el creador lo quita o
porque abandona por su cuenta— y su balance en ese grupo es distinto de `0.00`, el
sistema SHALL registrar una baja con el `participante`, el `saldo` que tenía al
momento de salir y el estado `PENDIENTE`. El `saldo` SHALL congelarse en ese valor y
NO SHALL recalcularse después.

Cuando el balance del participante que sale es exactamente `0.00`, el sistema MUST
NOT registrar ninguna baja: no hay nada que decidir.

El registro de la baja NO SHALL alterar por sí solo ningún balance: mientras la baja
esté `PENDIENTE`, el saldo del que se fue sigue figurando tal cual en los balances
del grupo.

#### Scenario: Sale un miembro que debe dinero

- **WHEN** el creador quita del grupo a un miembro cuyo balance es `-300.00`
- **THEN** el sistema registra una baja de ese participante con `saldo` `-300.00` y
  estado `PENDIENTE`
- **AND** los balances del grupo no cambian por el solo hecho de registrarla

#### Scenario: Sale un miembro al que le deben dinero

- **WHEN** un miembro con balance `+300.00` abandona el grupo por su cuenta
- **THEN** el sistema registra una baja con `saldo` `+300.00` y estado `PENDIENTE`

#### Scenario: Sale un miembro sin saldo

- **WHEN** sale del grupo un miembro cuyo balance es exactamente `0.00`
- **THEN** el sistema no registra ninguna baja

#### Scenario: El saldo queda congelado

- **WHEN** después de registrada una baja se modifica un gasto en el que participaba
  el que se fue
- **THEN** el `saldo` guardado en la baja no cambia

### Requirement: Consultar las bajas de un grupo

El sistema SHALL exponer `GET /api/grupos/{id}/bajas` que, para un miembro
autenticado del grupo, devuelve `200 OK` con la lista de bajas del grupo. Cada
entrada SHALL incluir su `id`, el `participante` que se fue, el `saldo` congelado, el
`estado` (`PENDIENTE`, `ASUMIDA` o `NO_ASUMIDA`), la `fecha` y, cuando el estado es
`ASUMIDA`, el `reparto`: quién absorbió cuánto. La lista SHALL estar vacía si el
grupo no tiene bajas.

#### Scenario: Un grupo con bajas

- **WHEN** un miembro autenticado envía `GET /api/grupos/{id}/bajas` en un grupo con
  bajas registradas
- **THEN** el sistema responde `200 OK` con una entrada por baja, cada una con su
  estado

#### Scenario: Una baja asumida trae su reparto

- **WHEN** se consulta una baja en estado `ASUMIDA`
- **THEN** su `reparto` indica qué participante absorbió qué monto

#### Scenario: Un grupo sin bajas

- **WHEN** un miembro consulta las bajas de un grupo del que no salió nadie con saldo
- **THEN** el sistema responde `200 OK` con una lista vacía

#### Scenario: El usuario no es miembro del grupo

- **WHEN** un usuario autenticado que no es miembro envía `GET /api/grupos/{id}/bajas`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar

#### Scenario: Petición sin token

- **WHEN** se envía `GET /api/grupos/{id}/bajas` sin un token JWT válido
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar

### Requirement: Resolver una baja pendiente

El sistema SHALL exponer `PUT /api/grupos/{id}/bajas/{bajaId}` reservado al creador
del grupo, con el cuerpo `{ "asumir": true | false }` donde `asumir` es obligatorio.

Cuando `asumir` es `true`, el sistema SHALL pasar la baja a `ASUMIDA` y SHALL repartir
el saldo en partes iguales entre los miembros actuales del grupo, congelando ese
reparto. Cuando `asumir` es `false`, el sistema SHALL pasar la baja a `NO_ASUMIDA` sin
generar ningún reparto y sin alterar ningún balance.

En ambos casos el sistema SHALL responder `200 OK` con la baja actualizada.

El sistema SHALL responder `404 Not Found` cuando el grupo no existe, o cuando la
baja no existe o pertenece a otro grupo; `403 Forbidden` cuando el solicitante no es
el creador del grupo; `409 Conflict` cuando la baja ya fue resuelta; y `400 Bad
Request` cuando se pide asumirla y no hay ningún miembro actual que pueda absorberla.

#### Scenario: El creador acepta asumir la deuda

- **WHEN** el creador envía `PUT /api/grupos/{id}/bajas/{bajaId}` con `asumir` en
  `true` sobre una baja pendiente de `-300.00`, y quedan 2 miembros en el grupo
- **THEN** el sistema responde `200 OK` con la baja en estado `ASUMIDA`
- **AND** el `reparto` asigna `-150.00` a cada uno de los 2 miembros

#### Scenario: El creador decide no asumir la deuda

- **WHEN** el creador envía `PUT /api/grupos/{id}/bajas/{bajaId}` con `asumir` en
  `false`
- **THEN** el sistema responde `200 OK` con la baja en estado `NO_ASUMIDA`
- **AND** la baja no tiene reparto
- **AND** ningún balance del grupo cambia

#### Scenario: Un miembro no creador intenta resolverla

- **WHEN** un miembro que no es el creador envía `PUT /api/grupos/{id}/bajas/{bajaId}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar
- **AND** la baja sigue `PENDIENTE`

#### Scenario: La baja ya fue resuelta

- **WHEN** el creador envía `PUT /api/grupos/{id}/bajas/{bajaId}` sobre una baja que
  ya está en `ASUMIDA` o en `NO_ASUMIDA`
- **THEN** el sistema responde `409 Conflict` con el formato de error estándar
- **AND** la baja conserva su estado y su reparto

#### Scenario: La baja no existe o es de otro grupo

- **WHEN** el creador envía `PUT /api/grupos/{id}/bajas/{bajaId}` con un `bajaId`
  inexistente o que pertenece a otro grupo
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: Cuerpo sin el campo asumir

- **WHEN** el creador envía la petición sin `asumir`
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar

### Requirement: El reparto de una baja asumida queda congelado

El reparto de una baja `ASUMIDA` SHALL calcularse una sola vez, con los miembros
actuales al momento de resolverla, y NO SHALL recalcularse ante cambios posteriores
de la composición del grupo. Un miembro que entra después de la decisión MUST NOT
absorber nada de esa baja; un miembro que sale después SHALL conservar la parte que
absorbió.

#### Scenario: Entra un miembro después de asumir la deuda

- **WHEN** tras asumir una baja se agrega un miembro nuevo al grupo
- **THEN** el reparto de esa baja no cambia
- **AND** el miembro nuevo no absorbe nada de ella

#### Scenario: El reparto de una deuda no divisible en partes exactas

- **WHEN** se asume una baja de `-100.00` entre 3 miembros
- **THEN** la suma de los montos del reparto es exactamente `-100.00`
