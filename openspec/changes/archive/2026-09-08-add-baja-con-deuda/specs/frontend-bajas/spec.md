## ADDED Requirements

### Requirement: Resolver una baja pendiente desde la interfaz

La aplicación SHALL mostrar, en el detalle del grupo, las bajas pendientes de
decisión: quién salió y con cuánto saldo. Al creador —y solo a él— SHALL ofrecerle
las dos salidas: **que el grupo asuma esa deuda**, o **que no la asuma**.

Antes de asumir, la aplicación SHALL mostrar cuánto le queda a cada miembro actual si
lo hacen, para que la decisión no se tome a ciegas, y SHALL pedir confirmación
explícita. Cancelar MUST NOT resolver nada.

A los miembros que no son el creador la aplicación SHALL mostrarles la baja pendiente
como información, aclarando que la decisión la toma quien creó el grupo, sin
ofrecerles controles que van a fallar.

Tras resolverla, los balances, la liquidación y los totales del grupo SHALL quedar al
día sin recargar la página.

#### Scenario: El creador ve una baja pendiente

- **WHEN** el creador abre un grupo del que salió alguien con saldo pendiente
- **THEN** ve quién salió y con cuánto
- **AND** ve las dos opciones: asumir la deuda o no asumirla

#### Scenario: Se muestra el efecto antes de asumir

- **WHEN** el creador elige asumir la deuda de alguien que salió debiendo
- **THEN** antes de confirmar ve cuánto le tocaría a cada miembro actual

#### Scenario: El grupo asume la deuda

- **WHEN** el creador confirma que el grupo asume la deuda
- **THEN** la baja pasa a figurar como asumida, con su reparto
- **AND** los balances, la liquidación y los totales se actualizan sin recargar
- **AND** quien salió deja de figurar con deuda

#### Scenario: El grupo no asume la deuda

- **WHEN** el creador confirma que el grupo no asume la deuda
- **THEN** la baja pasa al apartado de deudas sin resolver
- **AND** ningún balance cambia

#### Scenario: Se cancela la confirmación

- **WHEN** el creador abre una de las dos confirmaciones y la cancela
- **THEN** la baja sigue pendiente
- **AND** no se envió ninguna petición al backend

#### Scenario: Un miembro no creador mira las bajas

- **WHEN** un miembro que no creó el grupo abre el detalle
- **THEN** ve las bajas pendientes como información
- **AND** no ve controles para resolverlas
- **AND** entiende que la decisión la toma el creador

#### Scenario: El backend rechaza la decisión

- **WHEN** la petición de resolver llega al backend y este la rechaza
- **THEN** la aplicación muestra el mensaje recibido
- **AND** la baja no cambia de estado

### Requirement: Ver las deudas sin resolver

La aplicación SHALL mostrar un apartado propio con las bajas que el grupo decidió no
asumir, separado de los balances de quienes siguen en el grupo. De cada una SHALL
indicar quién es, cuánto quedó debiendo o cuánto se le debe, y que el grupo decidió
no hacerse cargo, de modo que se entienda que es un tema abierto a arreglar por fuera
de la aplicación.

Cuando no hay ninguna deuda sin resolver, el apartado MUST NOT ocupar lugar en la
pantalla.

#### Scenario: Un grupo con una deuda sin resolver

- **WHEN** un miembro abre un grupo donde el grupo decidió no asumir la deuda de
  quien salió
- **THEN** ve esa persona en el apartado de deudas sin resolver, con su monto
- **AND** entiende que es un tema que el grupo tiene que arreglar por su cuenta

#### Scenario: Las deudas sin resolver no se mezclan con los balances

- **WHEN** un miembro mira los balances del grupo
- **THEN** distingue entre quienes siguen en el grupo y quienes quedaron con una
  deuda sin resolver

#### Scenario: Un grupo sin deudas sin resolver

- **WHEN** un miembro abre un grupo donde nadie salió, o donde todas las bajas se
  asumieron
- **THEN** el apartado de deudas sin resolver no se muestra

### Requirement: Ver las bajas ya asumidas

La aplicación SHALL permitir ver las bajas que el grupo ya asumió, con su reparto:
quién absorbió cuánto. Esto SHALL estar disponible para cualquier miembro, para que
un balance alterado por una baja se pueda explicar en lugar de aparecer como un
número sin origen.

#### Scenario: Se consulta una baja asumida

- **WHEN** un miembro mira una baja que el grupo asumió
- **THEN** ve quién salió, con cuánto, y cuánto absorbió cada integrante

#### Scenario: Un balance alterado por una baja se puede explicar

- **WHEN** un miembro nota que su balance cambió por una baja asumida
- **THEN** puede ver en la aplicación de qué baja viene y cuánto le tocó
