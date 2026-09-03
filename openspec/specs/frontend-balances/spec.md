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
signo. La aplicación SHALL indicar mientras carga que está calculando, y SHALL
ofrecer reintentar si la consulta falla.

#### Scenario: Un grupo con gastos desparejos

- **WHEN** un miembro abre los balances de un grupo donde una persona pagó de más y
  las demás de menos
- **THEN** ve a cada integrante con su balance
- **AND** de cada uno entiende si le deben, si debe, o si está a mano, sin tener
  que interpretar un signo

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

#### Scenario: Hay deudas pendientes

- **WHEN** un miembro abre la liquidación de un grupo con deudas
- **THEN** ve cada transferencia con quién paga, quién cobra y el monto
- **AND** entiende que ejecutándolas todas el grupo queda saldado

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
