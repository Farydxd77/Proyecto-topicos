## MODIFIED Requirements

### Requirement: Consultar el balance de cada participante de un grupo

El sistema SHALL exponer `GET /api/grupos/{id}/balances` que, para un miembro
autenticado del grupo, devuelve `200 OK` con una entrada por cada participante con
actividad en el grupo: cada miembro actual del grupo y, además, cualquier
participante que figure como pagador o en la división de algún gasto registrado del
grupo, o como pagador o receptor de algún pago registrado del grupo. Cada entrada
SHALL incluir la identificación del participante, su `balance` como número con 2
decimales y el booleano `esMiembroActual`, que SHALL ser `true` cuando el
participante pertenece hoy al grupo y `false` cuando aparece solo por su actividad
pasada. El `balance` SHALL calcularse como `balance = (suma de los montos en USDT de
los gastos que pagó) - (suma de los montoAdeudado de ese participante en todos los
gastos del grupo) + (suma de los montos de los pagos que ese participante realizó) -
(suma de los montos de los pagos que ese participante recibió)`. Los montos de
gastos y de pagos entran en el cálculo en USDT. Un `balance` positivo significa que
al participante le deben dinero; uno negativo, que debe dinero. Un pago del deudor
al acreedor acerca ambos balances a cero. El valor de `esMiembroActual` NO SHALL
influir en el cálculo del `balance` ni en qué participantes se incluyen.

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
