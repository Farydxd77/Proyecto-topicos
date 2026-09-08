## MODIFIED Requirements

### Requirement: Registrar un pago entre participantes

El sistema SHALL exponer `POST /api/grupos/{grupoId}/pagos` que registra un pago en
USDT hecho por el participante autenticado (el pagador) a otro miembro del grupo (el
receptor). El cuerpo SHALL incluir `receptorId` (obligatorio), `monto` (obligatorio,
mayor que 0, hasta 8 enteros y 2 decimales, interpretado siempre en USDT), `fecha`
(obligatoria) y `txId` (opcional, hasta 100 caracteres; hash de transacción
blockchain que se guarda solo como referencia y NO se verifica). El límite de 8
dígitos enteros SHALL coincidir con lo que admite la columna `pagos.monto`
(`DECIMAL(10,2)`), de modo que un monto fuera de rango se rechace con `400 Bad
Request` y nunca produzca un error del servidor. El pagador SHALL ser siempre el
participante resuelto del token JWT: el cliente no lo envía y no puede registrar un
pago a nombre de otro. Ante datos válidos el sistema SHALL responder `201 Created`
con el pago creado, incluyendo su `id`, el grupo, el pagador, el receptor, el
`monto`, la `fecha` y el `txId`. Tras registrarlo, `GET
/api/grupos/{grupoId}/balances` y `GET /api/grupos/{grupoId}/liquidacion` SHALL
reflejar el pago.

#### Scenario: El pagador registra un pago válido

- **WHEN** un miembro autenticado envía `POST /api/grupos/{grupoId}/pagos` con un
  `receptorId` que es otro miembro del grupo, `monto` `150.00`, una `fecha` válida y
  sin `txId`
- **THEN** el sistema responde `201 Created` con el pago, cuyo pagador es el
  participante del token y cuyo `monto` es `150.00` en USDT
- **AND** el pago aparece luego en `GET /api/grupos/{grupoId}/pagos`

#### Scenario: El pago incluye un txId opcional

- **WHEN** el pagador envía `POST /api/grupos/{grupoId}/pagos` con un `txId` de
  hasta 100 caracteres
- **THEN** el sistema responde `201 Created` y el `txId` queda guardado tal cual en
  el pago, sin verificarlo contra ninguna blockchain

#### Scenario: Monto menor o igual a cero

- **WHEN** el pagador envía `POST /api/grupos/{grupoId}/pagos` con `monto` `0` o
  negativo
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar y
  no se registra ningún pago

#### Scenario: Monto en el límite superior admitido

- **WHEN** el pagador envía `POST /api/grupos/{grupoId}/pagos` con `monto`
  `99999999.99` (8 dígitos enteros, el máximo que admite la columna)
- **THEN** el sistema responde `201 Created` y el pago queda registrado con ese
  monto

#### Scenario: Monto con más dígitos enteros de los que admite la columna

- **WHEN** el pagador envía `POST /api/grupos/{grupoId}/pagos` con `monto`
  `100000000.00` (9 dígitos enteros)
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar
- **AND** no se registra ningún pago

#### Scenario: El pagador y el receptor son la misma persona

- **WHEN** el pagador envía `POST /api/grupos/{grupoId}/pagos` con un `receptorId`
  igual a su propio participante
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar

#### Scenario: El receptor no es miembro del grupo

- **WHEN** el pagador envía `POST /api/grupos/{grupoId}/pagos` con un `receptorId`
  que no pertenece al grupo (o no existe)
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar

### Requirement: Editar un pago propio

El sistema SHALL exponer `PUT /api/grupos/{grupoId}/pagos/{pagoId}` que actualiza un
pago existente del grupo. Solo el participante que registró el pago (su pagador)
PUEDE editarlo. El cuerpo SHALL aceptar los mismos campos que el alta —`receptorId`,
`monto`, `fecha` y `txId`— y el sistema SHALL revalidarlos con las mismas reglas
(monto mayor que 0 y de hasta 8 dígitos enteros y 2 decimales; receptor miembro del
grupo; receptor distinto del pagador; `txId` opcional de hasta 100 caracteres). El
límite de 8 dígitos enteros SHALL coincidir con lo que admite la columna
`pagos.monto` (`DECIMAL(10,2)`), de modo que un monto fuera de rango se rechace con
`400 Bad Request` y nunca produzca un error del servidor. El grupo y el pagador del
pago NO cambian. Ante datos válidos el sistema SHALL responder `200 OK` con el pago
actualizado, y `GET /api/grupos/{grupoId}/balances` y `GET
/api/grupos/{grupoId}/liquidacion` SHALL reflejar los nuevos valores.

#### Scenario: El pagador edita el monto de su pago

- **WHEN** el participante que registró el pago envía `PUT
  /api/grupos/{grupoId}/pagos/{pagoId}` con un `monto` distinto y válido
- **THEN** el sistema responde `200 OK` con el pago actualizado y los balances del
  grupo reflejan el nuevo `monto`

#### Scenario: El pagador cambia el receptor del pago

- **WHEN** el participante que registró el pago envía `PUT
  /api/grupos/{grupoId}/pagos/{pagoId}` con un `receptorId` distinto que también es
  miembro del grupo y distinto del pagador
- **THEN** el sistema responde `200 OK` con el pago actualizado

#### Scenario: Edición con datos inválidos

- **WHEN** el participante que registró el pago envía `PUT
  /api/grupos/{grupoId}/pagos/{pagoId}` con `monto` menor o igual a `0`, con un
  `receptorId` que no es miembro del grupo, o con un `receptorId` igual al pagador
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar y
  el pago no se modifica

#### Scenario: Edición con un monto fuera del rango de la columna

- **WHEN** quien registró el pago envía `PUT /api/grupos/{grupoId}/pagos/{pagoId}`
  con `monto` `100000000.00` (9 dígitos enteros)
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar
- **AND** el pago no cambia

#### Scenario: El pago a editar no existe

- **WHEN** el participante envía `PUT /api/grupos/{grupoId}/pagos/{pagoId}` con un
  `pagoId` inexistente o que pertenece a otro grupo
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar
