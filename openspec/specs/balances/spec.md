# balances Specification

## Purpose

Permitir que los miembros de un grupo consulten el estado de cuentas: el balance
neto de cada participante (lo que pagó en gastos menos lo que le corresponde adeudar
según las divisiones, más lo que pagó y menos lo que recibió en los pagos
registrados) y la lista mínima de transferencias que lleva a todos los balances a
cero. La suma de todos los balances es siempre exactamente cero. Es una capacidad de
solo lectura: no registra pagos ni modifica gastos.

## Requirements

### Requirement: Consultar el balance de cada participante de un grupo

El sistema SHALL exponer `GET /api/grupos/{id}/balances` que, para un miembro
autenticado del grupo, devuelve `200 OK` con una entrada por cada participante con
actividad en el grupo: cada miembro actual del grupo y, además, cualquier
participante que figure como pagador o en la división de algún gasto registrado del
grupo, como pagador o receptor de algún pago registrado del grupo, o como sujeto o
absorbente de alguna baja del grupo. Cada entrada SHALL incluir la identificación del
participante, su `balance` como número con 2 decimales y el booleano
`esMiembroActual`, que SHALL ser `true` cuando el participante pertenece hoy al grupo
y `false` cuando aparece solo por su actividad pasada. El `balance` SHALL calcularse
como `balance = (suma de los montos en USDT de los gastos que pagó) - (suma de los
montoAdeudado de ese participante en todos los gastos del grupo) + (suma de los
montos de los pagos que ese participante realizó) - (suma de los montos de los pagos
que ese participante recibió) + (lo condonado por las bajas asumidas de las que ese
participante es el sujeto) - (lo absorbido por ese participante en las bajas
asumidas del grupo)`. Los montos de gastos, pagos y bajas entran en el cálculo en
USDT. Un `balance` positivo significa que al participante le deben dinero; uno
negativo, que debe dinero. Un pago del deudor al acreedor acerca ambos balances a
cero. El valor de `esMiembroActual` NO SHALL influir en el cálculo del `balance` ni
en qué participantes se incluyen.

Solo las bajas en estado `ASUMIDA` SHALL afectar el cálculo. Una baja `PENDIENTE` o
`NO_ASUMIDA` MUST NOT alterar ningún `balance`.

#### Scenario: Miembro consulta los balances del grupo

- **WHEN** un miembro autenticado envía `GET /api/grupos/{id}/balances`
- **THEN** el sistema responde `200 OK` con una entrada por participante con
  actividad, cada una con su `balance` a 2 decimales
- **AND** un participante que solo pagó gastos tiene `balance` positivo y uno que
  solo adeuda tiene `balance` negativo

#### Scenario: Cada entrada indica si el participante sigue en el grupo

- **WHEN** un miembro autenticado envía `GET /api/grupos/{id}/balances`
- **THEN** cada entrada incluye `esMiembroActual`
- **AND** las entradas de los miembros actuales del grupo lo traen en `true`

#### Scenario: Escenario Samaipata

- **WHEN** en un grupo de 4 miembros (Ana, Beto, Carla, Diego) Ana registra un
  único gasto de `800.00` pagado por ella y repartido equitativamente entre los 4
- **THEN** `GET /api/grupos/{id}/balances` devuelve `Ana = +600.00`,
  `Beto = -200.00`, `Carla = -200.00`, `Diego = -200.00`

#### Scenario: Un pago descuenta la deuda en los balances

- **WHEN** partiendo de los balances `Ana = +600.00`, `Beto = -200.00`,
  `Carla = -200.00`, `Diego = -200.00`, Beto registra un pago de `200.00` a Ana
- **THEN** `GET /api/grupos/{id}/balances` devuelve `Ana = +400.00`, `Beto = 0.00`,
  `Carla = -200.00`, `Diego = -200.00`

#### Scenario: Participante que salió del grupo pero tiene un pago figura en los balances

- **WHEN** un participante que ya no es miembro actual del grupo figura como pagador
  o receptor de un pago registrado del grupo
- **THEN** ese participante aparece en la respuesta de `GET /api/grupos/{id}/balances`
  con su `balance` calculado
- **AND** su entrada trae `esMiembroActual` en `false`

#### Scenario: Un participante que salió debiendo conserva su deuda

- **WHEN** un miembro con `balance` negativo abandona el grupo o es quitado por el
  creador
- **THEN** sigue apareciendo en `GET /api/grupos/{id}/balances` con el mismo
  `balance` y con `esMiembroActual` en `false`
- **AND** la suma de todos los balances sigue siendo exactamente `0.00`

#### Scenario: Una baja pendiente no altera ningún balance

- **WHEN** un miembro con `balance` `-300.00` sale del grupo y su baja queda
  `PENDIENTE`
- **THEN** su `balance` sigue siendo `-300.00`
- **AND** los balances de los que siguen en el grupo no cambian

#### Scenario: Una baja asumida lleva a cero al que se fue y reparte su saldo

- **WHEN** el creador asume una baja de `-300.00` y quedan 2 miembros en el grupo
- **THEN** el `balance` del que se fue pasa a `0.00`
- **AND** el `balance` de cada uno de los 2 miembros baja `150.00`
- **AND** la suma de todos los balances sigue siendo exactamente `0.00`

#### Scenario: Una baja asumida de alguien a quien le debían

- **WHEN** el creador asume una baja de `+300.00` y quedan 2 miembros en el grupo
- **THEN** el `balance` del que se fue pasa a `0.00`
- **AND** el `balance` de cada uno de los 2 miembros sube `150.00`

#### Scenario: Una baja no asumida deja los balances intactos

- **WHEN** el creador decide no asumir una baja de `-300.00`
- **THEN** el `balance` del que se fue sigue siendo `-300.00`
- **AND** ningún otro `balance` cambia

#### Scenario: El usuario no es miembro del grupo

- **WHEN** un usuario autenticado que no es miembro del grupo envía
  `GET /api/grupos/{id}/balances`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar

#### Scenario: El grupo no existe

- **WHEN** un usuario autenticado envía `GET /api/grupos/{id}/balances` para un
  `id` de grupo que no existe
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: Petición sin token

- **WHEN** se envía `GET /api/grupos/{id}/balances` sin un token JWT válido
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar

### Requirement: La suma de todos los balances es exactamente cero

En cualquier respuesta de `GET /api/grupos/{id}/balances`, la suma aritmética de
todos los `balance` devueltos SHALL ser exactamente `0.00`, sin desviación por
redondeo, para cualquier combinación de gastos, pagos y bajas del grupo. El centavo
sobrante del redondeo de cada gasto ya lo absorbe el pagador en la división
registrada, por lo que el balance no introduce ningún redondeo adicional. Cada pago
mueve exactamente su `monto` desde el `balance` del receptor al `balance` del
pagador, de modo que su efecto neto sobre la suma es cero. Cada baja asumida condona
exactamente el saldo del que se fue y reparte ese mismo monto entre quienes lo
absorben, de modo que su efecto neto sobre la suma también es cero.

#### Scenario: La suma cierra en cero con divisiones no exactas

- **WHEN** un grupo tiene varios gastos cuyos montos no se dividen de forma exacta
  entre sus miembros
- **THEN** la suma de todos los `balance` de `GET /api/grupos/{id}/balances` es
  exactamente `0.00`

#### Scenario: La suma cierra en cero con gastos y pagos combinados

- **WHEN** un grupo tiene una combinación cualquiera de gastos y de pagos
  registrados
- **THEN** la suma de todos los `balance` de `GET /api/grupos/{id}/balances` es
  exactamente `0.00`

#### Scenario: La suma cierra en cero con una baja asumida no divisible

- **WHEN** un grupo asume una baja cuyo saldo no se divide de forma exacta entre los
  miembros que la absorben
- **THEN** la suma de todos los `balance` es exactamente `0.00`

#### Scenario: La suma cierra en cero con bajas en los tres estados

- **WHEN** un grupo tiene bajas en estado `PENDIENTE`, `ASUMIDA` y `NO_ASUMIDA` a la
  vez, además de gastos y pagos
- **THEN** la suma de todos los `balance` es exactamente `0.00`

### Requirement: Grupo sin gastos devuelve todos los balances en cero

El sistema SHALL responder `GET /api/grupos/{id}/balances` de un grupo sin ningún
gasto ni pago registrado con `200 OK` y una entrada por cada miembro actual del
grupo, cada una con `balance` igual a `0.00`.

#### Scenario: Grupo recién creado sin gastos

- **WHEN** un miembro autenticado envía `GET /api/grupos/{id}/balances` para un
  grupo que no tiene gastos ni pagos
- **THEN** el sistema responde `200 OK` con una entrada por miembro y todos los
  `balance` en `0.00`

### Requirement: Consultar la liquidación mínima del grupo

El sistema SHALL exponer `GET /api/grupos/{id}/liquidacion` que, para un miembro
autenticado del grupo, devuelve `200 OK` con la lista de transferencias que lleva
todos los balances a cero. Cada transferencia SHALL tener la forma
`{ "de": <nombre del deudor>, "deId": <id del participante deudor>, "para":
<nombre del acreedor>, "paraId": <id del participante acreedor>, "monto": <importe
positivo a 2 decimales> }`. La suma de los `monto` que sale de cada deudor SHALL
igualar el valor absoluto de su balance negativo, y la suma de los `monto` que
entra a cada acreedor SHALL igualar su balance positivo.

#### Scenario: Liquidación del escenario Samaipata

- **WHEN** con los balances `Ana = +600.00`, `Beto = -200.00`, `Carla = -200.00`,
  `Diego = -200.00` un miembro envía `GET /api/grupos/{id}/liquidacion`
- **THEN** el sistema responde `200 OK` con exactamente 3 transferencias de
  `200.00` cada una, todas con `para` = Ana y `de` = Beto, Carla y Diego
  respectivamente

#### Scenario: El usuario no es miembro del grupo

- **WHEN** un usuario autenticado que no es miembro del grupo envía
  `GET /api/grupos/{id}/liquidacion`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar

#### Scenario: El grupo no existe

- **WHEN** un usuario autenticado envía `GET /api/grupos/{id}/liquidacion` para un
  `id` de grupo que no existe
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: Petición sin token

- **WHEN** se envía `GET /api/grupos/{id}/liquidacion` sin un token JWT válido
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar

### Requirement: La liquidación sin deudas pendientes es una lista vacía

Cuando todos los balances del grupo son `0.00` (grupo sin gastos, o gastos que ya
se compensan entre sí), el sistema SHALL responder `GET /api/grupos/{id}/liquidacion`
con `200 OK` y `[]`.

#### Scenario: Grupo sin gastos

- **WHEN** un miembro autenticado envía `GET /api/grupos/{id}/liquidacion` para un
  grupo sin gastos
- **THEN** el sistema responde `200 OK` con `[]`

#### Scenario: Gastos que ya están compensados

- **WHEN** todos los participantes del grupo tienen `balance` `0.00`
- **THEN** `GET /api/grupos/{id}/liquidacion` responde `200 OK` con `[]`

### Requirement: Algoritmo greedy de minimización de transferencias

La liquidación SHALL calcularse con el algoritmo greedy del borrador: (1) separar a
los participantes en acreedores (`balance > 0`) y deudores (`balance < 0`);
(2) ordenar ambos grupos por valor absoluto del balance de mayor a menor;
(3) el mayor deudor transfiere al mayor acreedor `min(|balance deudor|, balance
acreedor)`; (4) descontar ese `monto` de ambos y repetir hasta que no queden
deudores con saldo. El sistema SHALL NOT emitir transferencias de importe `0.00`.
Ningún participante con `balance` `0.00` aparece en la liquidación.

#### Scenario: Un deudor cubre a varios acreedores

- **WHEN** los balances son `D = -300.00`, `E = +200.00`, `F = +100.00`
- **THEN** la liquidación tiene 2 transferencias que salen de `D`: una hacia `E`
  por `200.00` y otra hacia `F` por `100.00`

#### Scenario: Un acreedor recibe de varios deudores

- **WHEN** los balances son `X = +300.00`, `Y = -100.00`, `Z = -200.00`
- **THEN** la liquidación tiene 2 transferencias hacia `X`: una de `Z` por
  `200.00` y otra de `Y` por `100.00`

#### Scenario: Nunca se emite una transferencia de cero

- **WHEN** se calcula la liquidación de cualquier grupo
- **THEN** ninguna transferencia de la respuesta tiene `monto` igual a `0.00`

### Requirement: Acceso restringido a miembros del grupo

Ambos endpoints (`/balances` y `/liquidacion`) SHALL exigir un token JWT válido y
SHALL resolver al participante solicitante a partir del token. El sistema SHALL
evaluar la existencia del grupo antes que la membresía: un `id` de grupo
inexistente devuelve `404 Not Found` aunque el solicitante no sea miembro, y un
grupo existente con un solicitante que no es miembro devuelve `403 Forbidden`.
Cualquier miembro del grupo PUEDE consultar ambos endpoints.

#### Scenario: Orden de comprobación 404 antes que 403

- **WHEN** un usuario autenticado que no es miembro envía `GET /balances` o
  `GET /liquidacion` sobre un `id` de grupo que no existe
- **THEN** el sistema responde `404 Not Found`, no `403 Forbidden`

#### Scenario: Cualquier miembro consulta

- **WHEN** un miembro del grupo que no es su creador envía `GET /balances` o
  `GET /liquidacion`
- **THEN** el sistema ejecuta la consulta y no responde `403 Forbidden`
