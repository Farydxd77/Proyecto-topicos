## Purpose

Las pantallas con las que un miembro de un grupo registra lo que se gastó, lo
consulta, lo corrige y lo elimina. Define cómo se presenta un gasto que puede estar
en cualquier moneda soportada pero que se reparte en USDT: qué cifra es la que se
pagó, cuál es su equivalente, a qué tasa se convirtió, y cuánto le toca a cada
integrante del grupo.

## ADDED Requirements

### Requirement: Ver los gastos de un grupo

La aplicación SHALL mostrar, dentro del grupo, la lista de gastos registrados. De
cada gasto SHALL mostrar su descripción, el monto con su moneda original, el
equivalente en USDT, quién lo pagó y la fecha. La aplicación MUST NOT presentar el
monto original y el equivalente en USDT como si fueran la misma cifra ni omitir de
cuál se trata en cada caso. Si el grupo no tiene gastos, la aplicación SHALL
explicarlo y ofrecer registrar el primero.

#### Scenario: El grupo tiene gastos registrados

- **WHEN** un miembro abre un grupo con gastos registrados
- **THEN** ve la lista con la descripción, el monto y su moneda, el equivalente en
  USDT, el pagador y la fecha de cada uno
- **AND** puede distinguir sin ambigüedad cuál cifra es la moneda original y cuál
  el equivalente

#### Scenario: El grupo no tiene gastos

- **WHEN** un miembro abre un grupo sin gastos
- **THEN** ve un mensaje que lo explica y una forma visible de registrar el primero

#### Scenario: Un gasto en USDT

- **WHEN** la lista incluye un gasto registrado directamente en USDT
- **THEN** la aplicación no muestra una conversión redundante como si fuera una
  cifra distinta

#### Scenario: La carga de gastos falla

- **WHEN** la consulta de gastos falla porque el backend no responde
- **THEN** la aplicación muestra el error y ofrece reintentar sin dejar la sección
  en carga permanente

### Requirement: Registrar un gasto

La aplicación SHALL permitir a cualquier miembro del grupo registrar un gasto
indicando descripción, monto, moneda, quién pagó y la fecha. El pagador SHALL
elegirse entre los miembros actuales del grupo, y la moneda entre las soportadas
por el backend; ninguno de los dos SHALL escribirse a mano. La aplicación SHALL
validar antes de enviar que la descripción no esté vacía y que el monto sea mayor
que cero. Tras registrarlo con éxito, el gasto SHALL aparecer en la lista sin que
haga falta recargar la página.

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

### Requirement: Elegir la moneda del gasto

La aplicación SHALL ofrecer las monedas que el backend soporta, distinguiendo las
monedas fiat de las criptomonedas, y SHALL mostrar de cada una su símbolo junto a
un nombre legible. Cuando no se elige ninguna, el gasto SHALL registrarse en USDT.
La aplicación MUST NOT ofrecer monedas que el backend rechazaría; si aun así se
envía una no soportada, SHALL mostrar el mensaje del backend.

#### Scenario: Se elige una moneda fiat

- **WHEN** un miembro registra un gasto eligiendo una moneda fiat soportada
- **THEN** el gasto queda registrado en esa moneda
- **AND** la aplicación muestra el monto original en esa moneda y su equivalente en
  USDT

#### Scenario: Se elige una criptomoneda

- **WHEN** un miembro registra un gasto eligiendo una criptomoneda soportada
- **THEN** el gasto queda registrado en esa moneda con su equivalente en USDT

#### Scenario: No se elige moneda

- **WHEN** un miembro registra un gasto sin elegir moneda
- **THEN** el gasto se registra en USDT

#### Scenario: Se envía una moneda no soportada

- **WHEN** una moneda que el backend no soporta llega igualmente en la petición
- **THEN** la aplicación muestra el mensaje del backend indicando que no está
  soportada
- **AND** no se registra ningún gasto

### Requirement: Ver el detalle de un gasto con su división

La aplicación SHALL ofrecer el detalle de un gasto mostrando, además de sus datos,
la tasa de cambio que se aplicó y el reparto completo: cuánto le corresponde a cada
integrante del grupo. La aplicación SHALL indicar quién pagó, de modo que se
entienda que el reparto es una deuda hacia esa persona.

El reparto lo calcula el backend repartiendo el monto en USDT entre todos los
miembros con dos decimales y dejando que el pagador absorba el sobrante. La suma
del reparto SHALL coincidir con el monto en USDT del gasto **cuando ese monto tiene
como mucho dos decimales**, que es el caso de todo gasto registrado en USDT. Para
un gasto convertido desde otra moneda, el monto en USDT se guarda con seis
decimales mientras que cada parte del reparto se guarda con dos, así que la suma
puede diferir en menos de un centavo. Esa diferencia es del modelo de datos del
backend y la aplicación MUST NOT disimularla recalculando ni ajustando cifras: se
muestran los valores tal como los devuelve el backend.

#### Scenario: Se abre el detalle de un gasto

- **WHEN** un miembro abre el detalle de un gasto
- **THEN** ve la descripción, el monto original con su moneda, el equivalente en
  USDT, la tasa aplicada, el pagador, la fecha y el reparto por participante

#### Scenario: El reparto cuadra con el total en un gasto en USDT

- **WHEN** un miembro mira el reparto de un gasto registrado en USDT
- **THEN** la suma de los montos asignados coincide exactamente con el monto del
  gasto
- **AND** cuando la división no es exacta, el pagador es quien absorbe el sobrante

#### Scenario: El reparto de un gasto convertido puede diferir en centavos

- **WHEN** un miembro mira el reparto de un gasto convertido desde otra moneda cuyo
  monto en USDT tiene más de dos decimales
- **THEN** la aplicación muestra el reparto tal como lo devuelve el backend, sin
  recalcularlo ni ajustarlo para que cuadre

#### Scenario: El gasto no existe

- **WHEN** un miembro navega al detalle de un gasto que no existe en ese grupo
- **THEN** la aplicación lo explica y ofrece volver al grupo

### Requirement: Editar un gasto

La aplicación SHALL permitir a cualquier miembro del grupo modificar la
descripción, el monto, la moneda, el pagador y la fecha de un gasto, partiendo de
sus valores actuales, con las mismas validaciones del registro. Tras guardar, la
aplicación SHALL reflejar los valores nuevos y el reparto recalculado sin que haga
falta recargar la página. La aplicación SHALL permitir cancelar, descartando los
cambios.

#### Scenario: Se edita el monto de un gasto

- **WHEN** un miembro cambia el monto de un gasto y guarda
- **THEN** la pantalla muestra el monto nuevo, su equivalente en USDT recalculado y
  el reparto actualizado, sin recargar

#### Scenario: Se cambia la moneda de un gasto

- **WHEN** un miembro cambia la moneda de un gasto y guarda
- **THEN** la aplicación muestra la moneda nueva con su tasa y equivalente
  recalculados

#### Scenario: Se cancela la edición

- **WHEN** un miembro modifica los campos y cancela
- **THEN** los valores vuelven a los guardados y no se envía ninguna petición

### Requirement: Eliminar un gasto

La aplicación SHALL permitir a cualquier miembro del grupo eliminar un gasto,
pidiendo confirmación explícita e indicando de qué gasto se trata. Tras eliminarlo,
el gasto SHALL desaparecer de la lista sin recargar la página. Cancelar la
confirmación MUST NOT eliminar nada.

#### Scenario: Se elimina un gasto

- **WHEN** un miembro confirma la eliminación de un gasto
- **THEN** el gasto desaparece de la lista del grupo sin recargar

#### Scenario: Se cancela la confirmación

- **WHEN** un miembro abre la confirmación y la cancela
- **THEN** el gasto sigue existiendo y no se envió ninguna petición

### Requirement: Solo los miembros acceden a los gastos del grupo

La aplicación SHALL mostrar y permitir gestionar los gastos únicamente a quienes
integran el grupo. Cuando alguien que no es miembro intenta acceder a los gastos de
un grupo, la aplicación SHALL explicar que no tiene acceso en lugar de mostrar una
pantalla vacía o un error crudo. La gestión de gastos MUST NOT quedar restringida
al creador: cualquier miembro puede registrar, editar y eliminar.

#### Scenario: Un miembro no creador gestiona gastos

- **WHEN** un miembro que no creó el grupo registra, edita y elimina un gasto
- **THEN** las tres operaciones funcionan

#### Scenario: Alguien que no es miembro intenta ver los gastos

- **WHEN** una persona autenticada que no integra el grupo navega a sus gastos
- **THEN** la aplicación explica que no tiene acceso y ofrece volver a su lista de
  grupos

#### Scenario: El pagador indicado no es del grupo

- **WHEN** llega al backend un gasto cuyo pagador no integra el grupo
- **THEN** la aplicación muestra el mensaje del backend indicando que el pagador no
  es miembro
- **AND** no se registra ningún gasto

### Requirement: El servicio de cotización puede no estar disponible

Cuando el backend no logra obtener la cotización porque el servicio externo no
responde, la aplicación SHALL explicar que la conversión no está disponible en este
momento y que se puede reintentar, distinguiéndolo de un error en los datos
cargados. La aplicación SHALL conservar lo que la persona había escrito, de modo
que reintentar no obligue a cargar todo de nuevo. Los gastos ya registrados SHALL
seguir mostrándose con normalidad, porque su conversión quedó guardada al
registrarlos.

#### Scenario: La cotización no está disponible al registrar

- **WHEN** el backend responde que el servicio de cotización no está disponible
- **THEN** la aplicación explica que la conversión no se pudo obtener y que se
  puede reintentar
- **AND** conserva la descripción, el monto, la moneda, el pagador y la fecha ya
  cargados

#### Scenario: Los gastos previos siguen visibles

- **WHEN** el servicio de cotización no está disponible
- **THEN** la lista de gastos ya registrados se sigue mostrando con sus montos y
  equivalentes
- **AND** la indisponibilidad no se presenta como un error de esos gastos

#### Scenario: Un gasto en USDT no depende del servicio externo

- **WHEN** un miembro registra un gasto en USDT
- **THEN** el registro funciona aunque el servicio de cotización no esté disponible
