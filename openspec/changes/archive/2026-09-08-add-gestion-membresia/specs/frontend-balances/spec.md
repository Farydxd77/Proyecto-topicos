## MODIFIED Requirements

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
