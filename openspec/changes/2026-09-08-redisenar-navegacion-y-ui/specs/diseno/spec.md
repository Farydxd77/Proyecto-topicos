## ADDED Requirements

### Requirement: Los colores, tamaños y espaciados salen de un sistema de tokens

La aplicación SHALL definir su paleta, su escala tipográfica y su elevación como
tokens con nombre de **rol** —color de marca, tinta, lienzo, saldo a favor, saldo en
contra— y no como referencias directas a una paleta genérica del framework. Las
pantallas SHALL usar esos tokens.

Cambiar el color de marca de la aplicación SHALL requerir editar un solo lugar.

#### Scenario: Se cambia el color de marca

- **WHEN** se modifica el token del color de marca
- **THEN** cambian todos los elementos de marca de la aplicación
- **AND** no hace falta editar ninguna pantalla

#### Scenario: Una pantalla nueva usa el sistema

- **WHEN** se agrega una pantalla
- **THEN** toma sus colores y tamaños de los tokens existentes, sin inventar valores

### Requirement: Los montos se presentan de forma consistente

Todo monto de dinero SHALL mostrarse con **cifras de ancho fijo**, de modo que un
número que se actualiza no desplace lo que tiene alrededor. El signo del saldo SHALL
comunicarse con color **y** con palabras, nunca solo con color, para que se entienda
en escala de grises y con daltonismo.

La aplicación SHALL disponer de una única forma de renderizar un monto, compartida por
todas las pantallas: un mismo saldo a favor SHALL verse igual en el resumen, en los
balances y en la fila de un pago.

El formateo MUST NOT hacer aritmética sobre los montos: el backend ya calculó la
conversión y el reparto.

#### Scenario: Un monto se actualiza

- **WHEN** un monto en pantalla cambia de valor tras una operación
- **THEN** el contenido que lo rodea no se desplaza

#### Scenario: Un saldo a favor y uno en contra

- **WHEN** se muestran un saldo a favor y uno en contra
- **THEN** se distinguen por color y también por el texto que los acompaña

#### Scenario: El mismo monto en dos pantallas

- **WHEN** el mismo saldo aparece en el resumen y en los balances
- **THEN** se ve con el mismo formato y el mismo tratamiento de color

### Requirement: Hay una sola cifra principal por pantalla

Cada pantalla SHALL destacar como máximo **una** cifra en el tamaño mayor de la
escala. El resto de las cifras SHALL presentarse en niveles inferiores.

#### Scenario: Una pantalla con varias cifras

- **WHEN** una pantalla muestra varios montos
- **THEN** solo uno usa el tamaño destacado

### Requirement: Los estados vacíos explican y ofrecen la acción

Cuando una lista está vacía, la aplicación SHALL explicar qué falta y, cuando exista
una acción que corresponda, ofrecerla ahí mismo. MUST NOT limitarse a una línea de
texto sin salida.

#### Scenario: Un grupo sin gastos

- **WHEN** un miembro abre la pantalla de gastos de un grupo sin gastos
- **THEN** la pantalla explica que todavía no hay ninguno
- **AND** ofrece registrar el primero desde ahí

#### Scenario: Un usuario sin grupos

- **WHEN** alguien sin grupos abre su lista
- **THEN** la pantalla explica para qué sirven los grupos
- **AND** ofrece crear el primero

#### Scenario: Una lista vacía sin acción posible

- **WHEN** una lista está vacía y quien mira no tiene permiso para agregar nada
- **THEN** la pantalla lo explica sin ofrecer una acción que va a fallar

### Requirement: Cada consulta tiene estado de carga y de error con reintento

Toda pantalla que consulta datos SHALL indicar mientras carga que está trabajando, y
ante un fallo SHALL mostrar el error y ofrecer reintentar. Ninguna pantalla SHALL
quedar en carga permanente.

#### Scenario: Una consulta en curso

- **WHEN** una pantalla está esperando datos
- **THEN** lo indica

#### Scenario: Una consulta que falla

- **WHEN** la consulta de una pantalla falla
- **THEN** se muestra el error y se ofrece reintentar

### Requirement: La aplicación es usable en pantalla chica

Las pantallas SHALL adaptarse a un ancho de teléfono sin scroll horizontal. Las
pestañas del grupo SHALL seguir siendo accesibles y legibles. Los textos largos —un
nombre de grupo, un hash de transacción— SHALL truncarse en lugar de romper el ancho.

#### Scenario: Se abre en un teléfono

- **WHEN** se abre cualquier pantalla en un ancho de teléfono
- **THEN** no hay scroll horizontal
- **AND** las pestañas del grupo siguen siendo alcanzables

#### Scenario: Un texto largo

- **WHEN** un nombre o un identificador excede el ancho disponible
- **THEN** se trunca y el valor completo sigue disponible

### Requirement: La interfaz es operable sin ratón y se entiende con lector de pantalla

Los elementos interactivos SHALL ser alcanzables con teclado y SHALL mostrar un foco
visible. Los mensajes de error de un campo SHALL estar asociados a su campo. Los
elementos que comunican progreso o estado SHALL exponerlo de forma accesible, no solo
visual.

#### Scenario: Se recorre un formulario con teclado

- **WHEN** alguien recorre un formulario con el tabulador
- **THEN** cada campo y botón recibe un foco visible

#### Scenario: Un campo con error

- **WHEN** un campo queda inválido
- **THEN** su mensaje de error está asociado al campo

#### Scenario: El avance de la liquidación

- **WHEN** se muestra el avance de la liquidación
- **THEN** su valor está disponible para un lector de pantalla
