## ADDED Requirements

### Requirement: Elegir cómo se reparte un gasto

El formulario de gasto SHALL ofrecer dos modos de reparto: **entre todos, en partes
iguales** —preseleccionado, y equivalente a lo que la aplicación hacía hasta
ahora— y **personalizado**, donde quien registra elige qué integrantes participan
del gasto y con cuántas partes cada uno. El modo personalizado SHALL estar
colapsado hasta que se lo elige, para no cargar el formulario en el caso habitual.

En el modo personalizado, la aplicación SHALL listar a todos los miembros actuales
del grupo con una casilla para incluirlos y un campo numérico para sus partes,
preseleccionando a todos con una parte cada uno. La aplicación SHALL validar antes
de enviar que haya al menos un participante incluido y que cada cantidad de partes
sea un entero entre 1 y 1000. El pagador NO SHALL estar obligado a participar del
gasto.

Mientras se edita el reparto, la aplicación SHALL mostrar cuánto le queda a cada
participante incluido, para que quien registra vea el efecto de las partes antes de
confirmar. Ese cálculo es una previsualización: el reparto que vale es el que
devuelve el backend.

#### Scenario: El modo equitativo es el de por defecto

- **WHEN** un miembro abre el formulario para registrar un gasto
- **THEN** el modo de reparto preseleccionado es «entre todos, en partes iguales»
- **AND** al enviarlo así, el gasto se reparte entre todos los integrantes del grupo

#### Scenario: Excluir a alguien de un gasto

- **WHEN** un miembro elige el modo personalizado y desmarca a un integrante
- **THEN** al registrar el gasto esa persona no figura en el reparto
- **AND** no adeuda nada por ese gasto

#### Scenario: Repartir en partes desiguales

- **WHEN** un miembro elige el modo personalizado y le asigna a un integrante el
  doble de partes que a otro
- **THEN** el reparto que muestra el detalle del gasto le asigna al primero el doble
  que al segundo

#### Scenario: Previsualización del reparto

- **WHEN** un miembro cambia las partes de un integrante en el modo personalizado
- **THEN** la aplicación actualiza cuánto le queda a cada participante incluido
- **AND** el total previsualizado coincide con el monto del gasto

#### Scenario: Ningún participante incluido

- **WHEN** un miembro desmarca a todos los integrantes en el modo personalizado
- **THEN** la aplicación se lo indica y no envía la petición

#### Scenario: Partes fuera de rango

- **WHEN** un miembro indica para algún integrante una cantidad de partes que no es
  un entero entre 1 y 1000
- **THEN** la aplicación se lo indica y no envía la petición

#### Scenario: El pagador no participa del gasto

- **WHEN** un miembro registra un gasto en modo personalizado dejando fuera del
  reparto a quien pagó
- **THEN** la aplicación lo acepta y el gasto queda registrado
- **AND** el detalle muestra al pagador sin nada adeudado por ese gasto

## MODIFIED Requirements

### Requirement: Registrar un gasto

La aplicación SHALL permitir a cualquier miembro del grupo registrar un gasto
indicando descripción, monto, moneda, quién pagó, la fecha y cómo se reparte. El
pagador SHALL elegirse entre los miembros actuales del grupo, y la moneda entre las
soportadas por el backend; ninguno de los dos SHALL escribirse a mano. La aplicación
SHALL validar antes de enviar que la descripción no esté vacía, que el monto sea
mayor que cero y que el reparto elegido sea válido. Tras registrarlo con éxito, el
gasto SHALL aparecer en la lista sin que haga falta recargar la página.

#### Scenario: Registro con datos válidos

- **WHEN** un miembro registra un gasto con descripción, monto positivo, moneda,
  pagador y fecha válidos
- **THEN** el gasto queda registrado y aparece en la lista sin recargar
- **AND** la lista muestra su equivalente en USDT calculado por el backend

#### Scenario: El pagador se elige entre los miembros

- **WHEN** un miembro abre el formulario de registro
- **THEN** puede elegir como pagador a cualquier integrante actual del grupo,
  incluido él mismo
- **AND** no puede indicar a alguien que no pertenece al grupo

#### Scenario: Monto no positivo

- **WHEN** un miembro intenta registrar un gasto con monto cero o negativo
- **THEN** la aplicación se lo indica y no envía la petición

#### Scenario: Descripción vacía

- **WHEN** un miembro intenta registrar un gasto sin descripción o solo con
  espacios
- **THEN** la aplicación se lo indica y no envía la petición

#### Scenario: El backend rechaza el registro

- **WHEN** el backend responde con un error al registrar
- **THEN** la aplicación muestra el mensaje recibido
- **AND** conserva lo que ya se había cargado en el formulario

### Requirement: Ver el detalle de un gasto con su división

La aplicación SHALL ofrecer el detalle de un gasto mostrando, además de sus datos,
la tasa de cambio que se aplicó y el reparto completo: cuánto le corresponde a cada
participante. Cuando el gasto se repartió con partes desiguales, la aplicación SHALL
mostrar también las partes de cada uno, para que se entienda de dónde salen los
montos. Cuando alguien del grupo quedó fuera del reparto, MUST NOT aparecer en él.
La aplicación SHALL indicar quién pagó, de modo que se entienda que el reparto es
una deuda hacia esa persona.

El reparto lo calcula el backend repartiendo el monto en USDT con dos decimales y
dejando que uno de los participantes absorba el sobrante. La suma del reparto SHALL
coincidir con el monto en USDT del gasto **cuando ese monto tiene como mucho dos
decimales**, que es el caso de todo gasto registrado en USDT. Para un gasto
convertido desde otra moneda, el monto en USDT se guarda con seis decimales mientras
que cada parte del reparto se guarda con dos, así que la suma puede diferir en menos
de un centavo. Esa diferencia es del modelo de datos del backend y la aplicación
MUST NOT disimularla recalculando ni ajustando cifras: se muestran los valores tal
como los devuelve el backend.

#### Scenario: Se abre el detalle de un gasto

- **WHEN** un miembro abre el detalle de un gasto
- **THEN** ve la descripción, el monto original con su moneda, el equivalente en
  USDT, la tasa aplicada, el pagador, la fecha y el reparto por participante

#### Scenario: Un gasto con partes desiguales

- **WHEN** un miembro abre el detalle de un gasto que se repartió con partes
  distintas
- **THEN** junto a cada participante ve cuántas partes le tocaron
- **AND** los montos guardan esa proporción

#### Scenario: Un gasto del que alguien quedó excluido

- **WHEN** un miembro abre el detalle de un gasto en el que un integrante del grupo
  no participó
- **THEN** esa persona no aparece en el reparto

#### Scenario: El reparto cuadra con el total en un gasto en USDT

- **WHEN** un miembro mira el reparto de un gasto registrado en USDT
- **THEN** la suma de los montos asignados coincide exactamente con el monto del
  gasto

#### Scenario: El reparto de un gasto convertido puede diferir en centavos

- **WHEN** un miembro mira el reparto de un gasto convertido desde otra moneda cuyo
  monto en USDT tiene más de dos decimales
- **THEN** la aplicación muestra el reparto tal como lo devuelve el backend, sin
  recalcularlo ni ajustarlo para que cuadre

#### Scenario: El gasto no existe

- **WHEN** un miembro navega al detalle de un gasto que no existe en ese grupo
- **THEN** la aplicación lo explica y ofrece volver al grupo
