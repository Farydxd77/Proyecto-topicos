## ADDED Requirements

### Requirement: La sección del grupo se organiza en pestañas con URL propia

Cada capacidad del grupo SHALL tener su propia pantalla, accesible por una URL
distinta bajo `/grupos/{id}`: un **Resumen** en la raíz, y pestañas para **Gastos**,
**Pagos**, **Balances** y **Miembros**. La cabecera del grupo —su nombre, su
descripción y las acciones de administración— SHALL ser común a todas las pestañas.

Cada pestaña SHALL ser un destino navegable de pleno derecho: compartir su enlace
SHALL abrir esa pestaña, recargar SHALL mantenerla, y el botón «atrás» del navegador
SHALL volver a la anterior. La aplicación MUST NOT resolver las pestañas con estado
interno que se pierda al recargar.

La pestaña activa SHALL estar señalada visualmente en todo momento.

#### Scenario: Se navega entre pestañas

- **WHEN** un miembro abre un grupo y elige la pestaña de balances
- **THEN** la dirección del navegador cambia a la de esa pestaña
- **AND** la cabecera del grupo sigue visible

#### Scenario: Se comparte el enlace de una pestaña

- **WHEN** alguien abre directamente la dirección de la pestaña de gastos de un grupo
  del que es miembro
- **THEN** ve esa pestaña, no el resumen

#### Scenario: Se recarga la página

- **WHEN** un miembro recarga estando en la pestaña de pagos
- **THEN** sigue en la pestaña de pagos

#### Scenario: Se usa el botón atrás

- **WHEN** un miembro va del resumen a miembros y después usa «atrás»
- **THEN** vuelve al resumen

#### Scenario: La pestaña activa se distingue

- **WHEN** un miembro está en cualquiera de las pestañas
- **THEN** esa pestaña se ve señalada y las demás no

### Requirement: Cambiar de pestaña no vuelve a pedir el grupo

Los datos comunes a todas las pestañas —el grupo y sus miembros— SHALL cargarse una
sola vez para toda la sección, y navegar entre pestañas MUST NOT dispararlos de nuevo.
Cada pestaña SHALL pedir únicamente los datos que muestra.

#### Scenario: Se recorren las pestañas

- **WHEN** un miembro entra al resumen y después pasa por gastos, pagos y balances
- **THEN** el grupo se consulta una sola vez
- **AND** cada pestaña consulta solo lo suyo

#### Scenario: Entrar al resumen no carga todo el grupo

- **WHEN** un miembro abre el resumen de un grupo
- **THEN** la aplicación no consulta la lista de gastos ni la de pagos

### Requirement: El resumen del grupo responde las tres preguntas de entrada

La pantalla de resumen SHALL mostrar, sin que haga falta navegar a ninguna pestaña:
**cuánto se gastó en total** como la cifra más prominente de la pantalla, **la
situación de quien mira** —su balance interpretado en palabras y su parte— y **el
avance de la liquidación**. SHALL ofrecer además accesos a las demás pestañas con el
recuento de lo que contienen.

La pantalla MUST NOT consultar las listas completas de gastos ni de pagos: los
recuentos salen de los totales del grupo, que es una sola consulta. Mostrar los
últimos movimientos costaría esas dos listas y desharía la razón de separar las
pantallas.

La pantalla SHALL destacar una sola cifra como principal: el total del grupo. Las
demás cifras SHALL presentarse en un nivel visual inferior.

#### Scenario: Se abre un grupo con actividad

- **WHEN** un miembro abre el resumen de un grupo con gastos y pagos
- **THEN** ve el total gastado como la cifra más grande de la pantalla
- **AND** ve su propio balance interpretado en palabras
- **AND** ve el avance de la liquidación
- **AND** ve cuántos gastos y cuántos pagos tiene el grupo, con acceso a cada pestaña

#### Scenario: Se abre un grupo recién creado

- **WHEN** un miembro abre el resumen de un grupo sin gastos ni miembros además de él
- **THEN** la pantalla explica cómo empezar en lugar de mostrar todo en cero sin
  contexto

#### Scenario: Hay una sola cifra principal

- **WHEN** un miembro mira el resumen
- **THEN** solo el total del grupo está en el tamaño destacado

### Requirement: Los avisos que piden acción aparecen en el resumen

Cuando el grupo tiene algo pendiente de decidir —una baja sin resolver, en concreto—
el resumen SHALL mostrarlo como aviso, con un enlace a la pestaña donde se gestiona.
La gestión completa SHALL seguir viviendo en su pestaña: el resumen duplica la señal,
no la funcionalidad.

Cuando no hay nada pendiente, el aviso MUST NOT ocupar lugar.

#### Scenario: Hay una baja pendiente de decidir

- **WHEN** el creador abre el resumen de un grupo con una baja pendiente
- **THEN** ve un aviso indicando que hay una decisión pendiente
- **AND** desde ahí puede ir a la pestaña donde se resuelve

#### Scenario: No hay nada pendiente

- **WHEN** un miembro abre el resumen de un grupo sin bajas pendientes
- **THEN** no se muestra ningún aviso

### Requirement: Cada capacidad del grupo vive en su pantalla

Los gastos, los pagos, los balances con la liquidación, y los miembros con las bajas
SHALL vivir cada uno en su propia pantalla dentro de la sección del grupo. Ninguna
pantalla SHALL contener más de una de esas capacidades.

Toda la funcionalidad disponible antes de la separación SHALL seguir disponible, y
MUST NOT requerir más pasos que antes para llegar a ella desde el resumen.

#### Scenario: Se registra un gasto

- **WHEN** un miembro quiere registrar un gasto
- **THEN** lo hace desde la pantalla de gastos del grupo

#### Scenario: Se resuelve una baja

- **WHEN** el creador quiere decidir sobre una baja
- **THEN** lo hace desde la pantalla de miembros del grupo

#### Scenario: Ninguna funcionalidad se perdió

- **WHEN** se recorren todas las pantallas del grupo
- **THEN** están disponibles el alta y la edición de gastos, el alta y la edición de
  pagos, los balances, la liquidación, la gestión de miembros, la transferencia del
  rol de creador, la salida del grupo y la resolución de bajas

### Requirement: El acceso denegado y el grupo inexistente se explican en la sección

Cuando el grupo no existe o quien entra no es miembro, la sección SHALL explicarlo con
la distinción que ya hacía —«no existe» frente a «no sos miembro»— y ofrecer una
salida, sea cual sea la pestaña que se haya pedido.

#### Scenario: Se pide una pestaña de un grupo ajeno

- **WHEN** alguien abre directamente la pestaña de balances de un grupo del que no es
  miembro
- **THEN** la aplicación explica que no tiene acceso por no ser miembro
- **AND** ofrece volver a su lista de grupos

#### Scenario: Se pide una pestaña de un grupo inexistente

- **WHEN** alguien abre una pestaña de un grupo que no existe
- **THEN** la aplicación explica que el grupo no existe
