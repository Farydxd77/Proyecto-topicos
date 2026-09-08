# frontend-balances Specification

## Purpose

La pantalla que cierra la cuenta de un grupo: cuánto le corresponde recibir o pagar
a cada integrante, y la lista mínima de transferencias que deja a todos a mano.
Define cómo se traduce un balance con signo a una frase que se entiende sin
pensarla, y cómo esos números se mantienen al día cuando cambian los gastos o los
miembros.

## Requirements

### Requirement: Ver el balance de cada integrante

La aplicación SHALL mostrar, dentro del grupo, el balance de cada integrante
expresado en USDT. Cada balance SHALL presentarse interpretado en palabras —a quién
le deben, quién debe, y quién está a mano— y no únicamente como un número con
signo. Cuando una entrada corresponde a alguien que ya no integra el grupo pero
conserva saldo, la aplicación SHALL marcarla como tal, para que quien mira entienda
por qué aparece una persona que no está en la lista de integrantes. La aplicación
SHALL indicar mientras carga que está calculando, y SHALL ofrecer reintentar si la
consulta falla.

#### Scenario: Un grupo con gastos desparejos

- **WHEN** un miembro abre los balances de un grupo donde una persona pagó de más y
  las demás de menos
- **THEN** ve a cada integrante con su balance
- **AND** de cada uno entiende si le deben, si debe, o si está a mano, sin tener
  que interpretar un signo

#### Scenario: Alguien que salió del grupo con saldo pendiente

- **WHEN** un miembro abre los balances de un grupo donde una persona que ya no
  integra el grupo conserva saldo distinto de cero
- **THEN** ve a esa persona en la lista con su balance
- **AND** su fila está marcada de forma que se entiende que ya no es integrante del
  grupo

#### Scenario: Todos los que aparecen siguen en el grupo

- **WHEN** un miembro abre los balances de un grupo del que nadie salió
- **THEN** ninguna fila lleva la marca de ex-integrante

#### Scenario: Un grupo sin gastos

- **WHEN** un miembro abre los balances de un grupo sin gastos registrados
- **THEN** ve a todos los integrantes en cero
- **AND** la pantalla explica que todavía no hay nada que saldar

#### Scenario: La consulta falla

- **WHEN** la consulta de balances falla porque el backend no responde
- **THEN** la aplicación muestra el error y ofrece reintentar
- **AND** no queda en estado de carga permanente

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

### Requirement: Se destaca lo que le toca a quien mira

La aplicación SHALL señalar, tanto en los balances como en la liquidación, lo que
concierne a la persona autenticada: su propio balance, y las transferencias en las
que ella paga o cobra. La persona MUST NOT tener que buscarse a sí misma en una
lista para saber su situación.

#### Scenario: La persona tiene saldo a favor

- **WHEN** una persona a la que el grupo le debe abre los balances
- **THEN** su propia situación aparece destacada
- **AND** puede ver de un vistazo cuánto le corresponde recibir

#### Scenario: La persona debe dinero

- **WHEN** una persona que debe abre la liquidación
- **THEN** las transferencias en las que ella figura como quien paga aparecen
  destacadas

#### Scenario: La persona está a mano

- **WHEN** una persona con balance cero abre los balances
- **THEN** la aplicación le indica que está a mano

### Requirement: Los números se mantienen al día

La aplicación SHALL actualizar los balances y la liquidación cuando cambia algo que
los afecta: registrar, editar o eliminar un gasto, y agregar o quitar un miembro.
Tras cualquiera de esos cambios, la aplicación MUST NOT mostrar números calculados
antes del cambio.

#### Scenario: Se registra un gasto nuevo

- **WHEN** un miembro registra un gasto y luego mira los balances
- **THEN** los balances reflejan ese gasto sin que haga falta recargar la página

#### Scenario: Se elimina un gasto

- **WHEN** un miembro elimina un gasto y luego mira la liquidación
- **THEN** la liquidación ya no considera ese gasto

#### Scenario: Cambia la composición del grupo

- **WHEN** el creador agrega o quita un miembro y luego se miran los balances
- **THEN** los balances reflejan la composición actual del grupo

### Requirement: Solo los miembros ven los balances del grupo

La aplicación SHALL mostrar los balances y la liquidación únicamente a quienes
integran el grupo. Cuando alguien que no es miembro intenta acceder, la aplicación
SHALL explicar que no tiene acceso en lugar de mostrar una pantalla vacía o un
error crudo. Cuando el grupo no existe, SHALL explicarlo con un mensaje distinto.

#### Scenario: Un miembro cualquiera consulta los balances

- **WHEN** un miembro que no creó el grupo abre los balances y la liquidación
- **THEN** ve ambas cosas completas

#### Scenario: Alguien que no es miembro intenta consultarlos

- **WHEN** una persona autenticada que no integra el grupo navega a sus balances
- **THEN** la aplicación explica que no tiene acceso y ofrece volver a su lista de
  grupos

#### Scenario: El grupo no existe

- **WHEN** una persona navega a los balances de un grupo que no existe
- **THEN** la aplicación lo explica con un mensaje distinto al de falta de acceso

### Requirement: Ver los totales del grupo

La aplicación SHALL mostrar, en el detalle del grupo y antes del detalle por
persona, un panel con los totales del grupo en USDT: **cuánto se gastó en total**,
**cuánto se saldó ya** y **cuánto falta saldar**. También SHALL mostrar cuánto le
corresponde adeudar a quien está mirando, para que no tenga que buscarse en la lista
de balances.

La aplicación SHALL presentar «total gastado» y «ya saldado» como magnitudes
distintas y MUST NOT sugerir que la segunda deba alcanzar a la primera: quien paga un
gasto cubre su propia parte y esa porción nunca aparece como pago. El avance de la
liquidación SHALL medirse sobre lo que falta saldar, no sobre el total gastado.

La aplicación SHALL indicar mientras carga que está calculando, y SHALL ofrecer
reintentar si la consulta falla, sin quedar en carga permanente.

#### Scenario: Un grupo con gastos y pagos

- **WHEN** un miembro abre el detalle de un grupo con gastos y con pagos registrados
- **THEN** ve el total gastado del grupo, cuánto se saldó y cuánto falta saldar
- **AND** ve cuánto le corresponde adeudar a él

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

### Requirement: Ver el avance de la liquidación

La aplicación SHALL mostrar el avance de la liquidación del grupo como una
proporción de lo que ya se saldó sobre la deuda total —lo saldado más lo pendiente—,
acompañada de una lectura en palabras. El avance SHALL llegar a su máximo cuando no
queda nada por saldar, MUST NOT depender del total gastado, y SHALL actualizarse sin
recargar la página cada vez que se registra, edita o elimina un pago.

#### Scenario: Liquidación a medias

- **WHEN** parte de las deudas del grupo ya se transfirieron y parte no
- **THEN** el avance muestra la proporción saldada
- **AND** se entiende cuánto falta para que el grupo quede a mano

#### Scenario: Grupo completamente saldado

- **WHEN** en el grupo no queda ninguna deuda pendiente
- **THEN** el avance está al máximo
- **AND** la aplicación dice que está todo a mano, aunque el total pagado sea menor
  que el total gastado

#### Scenario: El avance se actualiza al registrar un pago

- **WHEN** un miembro registra un pago
- **THEN** el avance y los totales se actualizan sin recargar la página

#### Scenario: Grupo sin deudas todavía

- **WHEN** el grupo no tiene gastos, y por lo tanto no hay deuda
- **THEN** la aplicación no muestra un avance engañoso, sino que explica que no hay
  nada que saldar
