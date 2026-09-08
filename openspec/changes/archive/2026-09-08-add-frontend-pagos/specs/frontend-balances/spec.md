## MODIFIED Requirements

### Requirement: Ver la liquidación del grupo

La aplicación SHALL mostrar la lista de transferencias que salda las deudas del
grupo, indicando de cada una quién paga, quién cobra y cuánto. La aplicación SHALL
presentar esa lista como lo que es: la manera de quedar a mano con la menor
cantidad de movimientos posible.

La liquidación SHALL ser accionable y no solo informativa: en cada transferencia
donde quien mira es el deudor, la aplicación SHALL ofrecer registrarla como pago, de
modo que el ciclo liquidación → pago → balances se pueda cerrar sin transcribir los
datos a mano en otra sección.

#### Scenario: Hay deudas pendientes

- **WHEN** un miembro abre la liquidación de un grupo con deudas
- **THEN** ve cada transferencia con quién paga, quién cobra y el monto
- **AND** entiende que ejecutándolas todas el grupo queda saldado

#### Scenario: La transferencia propia se puede registrar como pago

- **WHEN** un miembro ve en la liquidación una transferencia en la que él es quien
  paga
- **THEN** se le ofrece registrarla como pago
- **AND** al hacerlo, la liquidación se recalcula y esa transferencia desaparece o se
  reduce

#### Scenario: No hay nada pendiente

- **WHEN** un miembro abre la liquidación de un grupo donde nadie debe nada
- **THEN** la aplicación explica que está todo a mano
- **AND** no muestra una lista vacía sin contexto

#### Scenario: Un grupo sin gastos

- **WHEN** un miembro abre la liquidación de un grupo sin gastos registrados
- **THEN** la aplicación explica que no hay nada que saldar
