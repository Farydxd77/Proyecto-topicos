## MODIFIED Requirements

### Requirement: Ver los totales del grupo

La aplicación SHALL mostrar, en la pantalla de **resumen** del grupo, los totales en
USDT: **cuánto se gastó en total**, **cuánto se saldó ya** y **cuánto falta saldar**.
También SHALL mostrar cuánto le corresponde adeudar a quien está mirando, para que no
tenga que buscarse en la lista de balances, que vive en su propia pestaña.

El total gastado SHALL ser la cifra más prominente de esa pantalla.

La aplicación SHALL presentar «total gastado» y «ya saldado» como magnitudes
distintas y MUST NOT sugerir que la segunda deba alcanzar a la primera: quien paga un
gasto cubre su propia parte y esa porción nunca aparece como pago. El avance de la
liquidación SHALL medirse sobre lo que falta saldar, no sobre el total gastado.

La aplicación SHALL indicar mientras carga que está calculando, y SHALL ofrecer
reintentar si la consulta falla, sin quedar en carga permanente.

#### Scenario: Un grupo con gastos y pagos

- **WHEN** un miembro abre el resumen de un grupo con gastos y con pagos registrados
- **THEN** ve el total gastado del grupo, cuánto se saldó y cuánto falta saldar
- **AND** ve cuánto le corresponde adeudar a él

#### Scenario: El total gastado es la cifra principal

- **WHEN** un miembro abre el resumen del grupo
- **THEN** el total gastado se muestra en el tamaño destacado
- **AND** ninguna otra cifra de esa pantalla lo usa

#### Scenario: Los dos totales no se presentan como comparables

- **WHEN** un miembro mira el panel de totales
- **THEN** entiende que «ya saldado» mide el avance de las transferencias y no una
  fracción del total gastado

#### Scenario: Un grupo con gastos en varias monedas

- **WHEN** un grupo tiene gastos en distintas monedas
- **THEN** el total se muestra en USDT
- **AND** la aplicación aclara que el total está expresado en USDT

#### Scenario: Un grupo sin gastos

- **WHEN** un miembro abre un grupo sin gastos registrados
- **THEN** el panel muestra los totales en cero
- **AND** explica que todavía no hay nada gastado

#### Scenario: La consulta falla

- **WHEN** la consulta de totales falla porque el backend no responde
- **THEN** la aplicación muestra el error y ofrece reintentar
- **AND** no queda en estado de carga permanente
