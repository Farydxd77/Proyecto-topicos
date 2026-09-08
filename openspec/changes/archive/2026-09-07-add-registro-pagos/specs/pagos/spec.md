## Purpose

Permitir que los participantes de un grupo registren en USDT los pagos que hacen
entre sí para saldar sus deudas, y que esos pagos se reflejen en los balances y en
la liquidación mínima del grupo. Es una capacidad de escritura sobre datos propios:
cada participante registra, edita y borra únicamente sus propios pagos.

## ADDED Requirements

### Requirement: Registrar un pago entre participantes

El sistema SHALL exponer `POST /api/grupos/{grupoId}/pagos` que registra un pago en
USDT hecho por el participante autenticado (el pagador) a otro miembro del grupo (el
receptor). El cuerpo SHALL incluir `receptorId` (obligatorio), `monto` (obligatorio,
mayor que 0, hasta 10 enteros y 2 decimales, interpretado siempre en USDT), `fecha`
(obligatoria) y `txId` (opcional, hasta 100 caracteres; hash de transacción
blockchain que se guarda solo como referencia y NO se verifica). El pagador SHALL
ser siempre el participante resuelto del token JWT: el cliente no lo envía y no puede
registrar un pago a nombre de otro. Ante datos válidos el sistema SHALL responder
`201 Created` con el pago creado, incluyendo su `id`, el grupo, el pagador, el
receptor, el `monto`, la `fecha` y el `txId`. Tras registrarlo, `GET
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

#### Scenario: El pagador y el receptor son la misma persona

- **WHEN** el pagador envía `POST /api/grupos/{grupoId}/pagos` con un `receptorId`
  igual a su propio participante
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar

#### Scenario: El receptor no es miembro del grupo

- **WHEN** el pagador envía `POST /api/grupos/{grupoId}/pagos` con un `receptorId`
  que no pertenece al grupo (o no existe)
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar

### Requirement: Listar los pagos de un grupo

El sistema SHALL exponer `GET /api/grupos/{grupoId}/pagos` que, para cualquier
miembro autenticado del grupo, devuelve `200 OK` con la lista de pagos del grupo
ordenada por `fecha` descendente. Cada elemento SHALL incluir el `id`, el pagador,
el receptor, el `monto` en USDT, la `fecha` y el `txId`. Si el grupo no tiene pagos
registrados el sistema SHALL responder `200 OK` con `[]`.

#### Scenario: Miembro lista los pagos del grupo

- **WHEN** un miembro autenticado envía `GET /api/grupos/{grupoId}/pagos` para un
  grupo con varios pagos
- **THEN** el sistema responde `200 OK` con todos los pagos del grupo ordenados por
  `fecha` de más reciente a más antigua

#### Scenario: Grupo sin pagos

- **WHEN** un miembro autenticado envía `GET /api/grupos/{grupoId}/pagos` para un
  grupo sin pagos
- **THEN** el sistema responde `200 OK` con `[]`

### Requirement: Consultar el detalle de un pago

El sistema SHALL exponer `GET /api/grupos/{grupoId}/pagos/{pagoId}` que, para
cualquier miembro autenticado del grupo, devuelve `200 OK` con los datos del pago
cuando el pago existe y pertenece a ese grupo. Si el pago no existe, o existe pero
pertenece a otro grupo, el sistema SHALL responder `404 Not Found` con el formato de
error estándar.

#### Scenario: Miembro consulta un pago del grupo

- **WHEN** un miembro autenticado envía `GET /api/grupos/{grupoId}/pagos/{pagoId}`
  para un pago que pertenece al grupo
- **THEN** el sistema responde `200 OK` con el `id`, el pagador, el receptor, el
  `monto`, la `fecha` y el `txId` del pago

#### Scenario: El pago no pertenece al grupo indicado

- **WHEN** un miembro autenticado envía `GET /api/grupos/{grupoId}/pagos/{pagoId}`
  con un `pagoId` que existe pero pertenece a otro grupo
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: El pago no existe

- **WHEN** un miembro autenticado envía `GET /api/grupos/{grupoId}/pagos/{pagoId}`
  con un `pagoId` inexistente
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

### Requirement: Editar un pago propio

El sistema SHALL exponer `PUT /api/grupos/{grupoId}/pagos/{pagoId}` que actualiza un
pago existente del grupo. Solo el participante que registró el pago (su pagador)
PUEDE editarlo. El cuerpo SHALL aceptar los mismos campos que el alta —`receptorId`,
`monto`, `fecha` y `txId`— y el sistema SHALL revalidarlos con las mismas reglas
(monto mayor que 0; receptor miembro del grupo; receptor distinto del pagador; `txId`
opcional de hasta 100 caracteres). El grupo y el pagador del pago NO cambian. Ante
datos válidos el sistema SHALL responder `200 OK` con el pago actualizado, y `GET
/api/grupos/{grupoId}/balances` y `GET /api/grupos/{grupoId}/liquidacion` SHALL
reflejar los nuevos valores.

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

#### Scenario: El pago a editar no existe

- **WHEN** el participante envía `PUT /api/grupos/{grupoId}/pagos/{pagoId}` con un
  `pagoId` inexistente o que pertenece a otro grupo
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

### Requirement: Eliminar un pago propio

El sistema SHALL exponer `DELETE /api/grupos/{grupoId}/pagos/{pagoId}` que elimina
un pago existente del grupo. Solo el participante que registró el pago (su pagador)
PUEDE eliminarlo. Ante una eliminación exitosa el sistema SHALL responder `204 No
Content` sin cuerpo, y el pago SHALL dejar de contar en `GET
/api/grupos/{grupoId}/balances` y `GET /api/grupos/{grupoId}/liquidacion`.

#### Scenario: El pagador elimina su pago

- **WHEN** el participante que registró el pago envía `DELETE
  /api/grupos/{grupoId}/pagos/{pagoId}`
- **THEN** el sistema responde `204 No Content` y el pago ya no aparece en `GET
  /api/grupos/{grupoId}/pagos` ni influye en los balances

#### Scenario: El pago a eliminar no existe

- **WHEN** el participante envía `DELETE /api/grupos/{grupoId}/pagos/{pagoId}` con un
  `pagoId` inexistente o que pertenece a otro grupo
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

### Requirement: Acceso restringido y resolución de identidad

Todos los endpoints de pagos SHALL exigir un token JWT válido y SHALL resolver al
participante solicitante a partir del token. El sistema SHALL comprobar la
existencia del grupo antes que la membresía: un `grupoId` inexistente devuelve `404
Not Found` aunque el solicitante no sea miembro. Un grupo existente con un
solicitante que no es miembro devuelve `403 Forbidden`. En `PUT` y `DELETE`, un
solicitante que es miembro del grupo pero NO es el pagador que registró el pago
SHALL recibir `403 Forbidden`. Todos los errores SHALL usar el formato de error
estándar.

#### Scenario: Petición sin token

- **WHEN** se envía cualquier endpoint de `/api/grupos/{grupoId}/pagos` sin un token
  JWT válido
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar

#### Scenario: Usuario no miembro del grupo

- **WHEN** un usuario autenticado que no pertenece al grupo envía cualquier endpoint
  de `/api/grupos/{grupoId}/pagos` sobre un grupo que sí existe
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar

#### Scenario: Grupo inexistente se comprueba antes que la membresía

- **WHEN** un usuario autenticado envía cualquier endpoint de
  `/api/grupos/{grupoId}/pagos` sobre un `grupoId` que no existe
- **THEN** el sistema responde `404 Not Found`, no `403 Forbidden`

#### Scenario: Un miembro que no es el pagador intenta editar o eliminar

- **WHEN** un miembro del grupo que no registró el pago envía `PUT` o `DELETE` sobre
  `/api/grupos/{grupoId}/pagos/{pagoId}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar y el
  pago no se modifica ni se elimina

### Requirement: Los pagos se reflejan en los balances del grupo

Cada pago registrado SHALL afectar el `balance` de su pagador y de su receptor en
`GET /api/grupos/{grupoId}/balances` y en la liquidación de `GET
/api/grupos/{grupoId}/liquidacion`. Un pago del deudor al acreedor SHALL acercar
ambos balances a cero: el `monto` del pago se suma al `balance` del pagador y se
resta del `balance` del receptor. La suma de todos los `balance` del grupo SHALL
seguir siendo exactamente `0.00` tras registrar, editar o eliminar cualquier
cantidad de pagos. Un participante que no es miembro actual del grupo pero figura
como pagador o receptor de algún pago SHALL aparecer en la respuesta de balances.

#### Scenario: Un pago salda parte de una deuda

- **WHEN** en un grupo con balances `Ana = +600.00`, `Beto = -200.00`,
  `Carla = -200.00`, `Diego = -200.00` (escenario Samaipata) Beto registra un pago
  de `200.00` a Ana
- **THEN** `GET /api/grupos/{grupoId}/balances` devuelve `Ana = +400.00`,
  `Beto = 0.00`, `Carla = -200.00`, `Diego = -200.00`
- **AND** `GET /api/grupos/{grupoId}/liquidacion` ya no incluye ninguna
  transferencia de Beto

#### Scenario: La suma de balances sigue cerrando en cero con pagos

- **WHEN** un grupo tiene una combinación cualquiera de gastos y de pagos
  registrados
- **THEN** la suma de todos los `balance` de `GET /api/grupos/{grupoId}/balances` es
  exactamente `0.00`

#### Scenario: Eliminar un pago revierte su efecto en los balances

- **WHEN** se elimina un pago previamente registrado
- **THEN** los balances del grupo vuelven a ser los que había antes de registrar ese
  pago
