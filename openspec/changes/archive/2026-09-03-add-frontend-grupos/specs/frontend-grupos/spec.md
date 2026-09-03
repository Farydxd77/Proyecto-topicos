## Purpose

Las pantallas con las que una persona autenticada gestiona sus grupos de viaje:
ver a cuáles pertenece, crear uno nuevo, consultar el detalle con sus miembros y
—cuando es quien lo creó— editarlo o eliminarlo. Define además cómo la interfaz
distingue al creador del resto de miembros, de modo que nadie vea una acción que
el backend le va a rechazar.

## ADDED Requirements

### Requirement: Ver la lista de mis grupos

La aplicación SHALL ofrecer una pantalla que liste los grupos en los que la
persona autenticada es miembro, mostrando de cada uno su nombre, su descripción
cuando la tenga, y quién lo creó. La pantalla SHALL indicar mientras carga que
está cargando, y SHALL permitir abrir el detalle de cualquier grupo listado. Si la
persona no pertenece a ningún grupo, la pantalla SHALL mostrar un mensaje que lo
explique y una manera de crear el primero, y MUST NOT mostrar una lista vacía sin
contexto.

#### Scenario: La persona pertenece a varios grupos

- **WHEN** una persona autenticada que es miembro de dos grupos abre la pantalla
  de grupos
- **THEN** ve los dos grupos, cada uno con su nombre y su creador
- **AND** puede abrir el detalle de cualquiera de ellos

#### Scenario: La persona no pertenece a ningún grupo

- **WHEN** una persona autenticada que no es miembro de ningún grupo abre la
  pantalla de grupos
- **THEN** ve un mensaje que explica que todavía no tiene grupos
- **AND** ve una manera visible de crear el primero

#### Scenario: Los grupos ajenos no aparecen

- **WHEN** existen grupos creados por otras personas en los que quien mira no es
  miembro
- **THEN** esos grupos no aparecen en su lista

#### Scenario: La carga falla

- **WHEN** la consulta de la lista falla porque el backend no responde
- **THEN** la pantalla muestra el mensaje de error y ofrece reintentar
- **AND** no queda en estado de carga permanente

### Requirement: Crear un grupo

La aplicación SHALL permitir crear un grupo indicando un nombre obligatorio y una
descripción opcional. El nombre SHALL validarse antes de enviar: no puede estar
vacío ni contener solo espacios, y no puede superar los 100 caracteres. Tras crear
el grupo con éxito, la aplicación SHALL reflejarlo sin que haga falta recargar la
página. Si el backend rechaza la petición, la aplicación SHALL mostrar el mensaje
recibido y conservar lo que la persona ya había escrito.

#### Scenario: Creación con nombre y descripción

- **WHEN** una persona crea un grupo con un nombre y una descripción válidos
- **THEN** el grupo queda creado con ella como creadora y como única miembro
- **AND** aparece en su lista de grupos sin recargar la página

#### Scenario: Creación solo con nombre

- **WHEN** una persona crea un grupo indicando solo el nombre
- **THEN** el grupo queda creado y la pantalla no muestra una descripción vacía
  como si fuera un dato faltante

#### Scenario: Nombre vacío

- **WHEN** una persona intenta crear un grupo con el nombre vacío o compuesto solo
  de espacios
- **THEN** la aplicación muestra que el nombre es obligatorio
- **AND** no envía la petición al backend

#### Scenario: Nombre demasiado largo

- **WHEN** una persona escribe un nombre de más de 100 caracteres
- **THEN** la aplicación se lo indica antes de enviar

#### Scenario: El backend rechaza la creación

- **WHEN** el backend responde con un error a la creación
- **THEN** la aplicación muestra el mensaje del backend
- **AND** conserva el nombre y la descripción ya escritos

### Requirement: Ver el detalle de un grupo

La aplicación SHALL ofrecer una pantalla de detalle que muestre el nombre, la
descripción, el creador y la lista completa de miembros del grupo, identificando a
cada persona de forma legible. La pantalla SHALL indicar cuál de los miembros es
el creador. Cuando el grupo no existe o la persona no es miembro, la pantalla
SHALL explicar la situación en lugar de mostrar una pantalla vacía o un error
crudo.

#### Scenario: Un miembro abre el detalle

- **WHEN** una persona que es miembro del grupo abre su detalle
- **THEN** ve el nombre, la descripción, el creador y todos los miembros
- **AND** el creador aparece señalado como tal dentro de la lista

#### Scenario: Alguien que no es miembro abre el detalle

- **WHEN** una persona autenticada que no es miembro del grupo navega a la URL de
  su detalle
- **THEN** la pantalla explica que no tiene acceso a ese grupo
- **AND** ofrece volver a su lista de grupos

#### Scenario: El grupo no existe

- **WHEN** una persona navega al detalle de un grupo que no existe
- **THEN** la pantalla explica que el grupo no existe
- **AND** ofrece volver a su lista de grupos

#### Scenario: El grupo fue eliminado por su creador

- **WHEN** una persona tiene abierto el detalle de un grupo que su creador
  elimina, y vuelve a consultarlo
- **THEN** la pantalla explica que el grupo ya no existe y no muestra datos
  obsoletos como si siguieran vigentes

### Requirement: Solo el creador ve las acciones de administración

La aplicación SHALL mostrar las acciones de editar y eliminar el grupo únicamente
a quien lo creó. A los miembros que no son creadores la aplicación MUST NOT
ofrecerles esas acciones. Esta distinción es de presentación y no sustituye la
verificación del backend: si una acción reservada llega a enviarse igual, la
aplicación SHALL mostrar el mensaje de permiso denegado que devuelve el backend
sin romperse.

#### Scenario: El creador ve las acciones

- **WHEN** el creador de un grupo abre su detalle
- **THEN** ve disponibles las acciones de editar y eliminar el grupo

#### Scenario: Un miembro no creador no ve las acciones

- **WHEN** un miembro que no creó el grupo abre su detalle
- **THEN** no ve las acciones de editar ni de eliminar
- **AND** sí ve toda la información del grupo y sus miembros

#### Scenario: Una acción reservada se envía de todos modos

- **WHEN** una acción de administración llega al backend enviada por alguien que
  no es el creador
- **THEN** la aplicación muestra el mensaje de permiso denegado del backend
- **AND** la pantalla queda en un estado coherente, sin datos a medio actualizar

### Requirement: Editar un grupo

La aplicación SHALL permitir al creador modificar el nombre y la descripción del
grupo, partiendo de los valores actuales. El nombre SHALL validarse igual que al
crear. La aplicación SHALL permitir cancelar la edición, descartando los cambios y
restaurando los valores guardados. Tras guardar con éxito, la pantalla SHALL
mostrar los valores nuevos sin recargar la página, y la lista de miembros y el
creador SHALL permanecer sin cambios.

#### Scenario: El creador edita el grupo

- **WHEN** el creador cambia el nombre y la descripción y guarda
- **THEN** la pantalla muestra los valores nuevos sin recargar
- **AND** al volver a la lista de grupos el nombre aparece actualizado

#### Scenario: Se cancela la edición

- **WHEN** el creador modifica los campos y cancela en lugar de guardar
- **THEN** los valores vuelven a ser los que estaban guardados
- **AND** no se envía ninguna petición al backend

#### Scenario: Nombre vacío al editar

- **WHEN** el creador deja el nombre vacío o solo con espacios e intenta guardar
- **THEN** la aplicación se lo indica y no envía la petición
- **AND** el grupo conserva su nombre anterior

### Requirement: Eliminar un grupo

La aplicación SHALL permitir al creador eliminar el grupo, pidiendo siempre una
confirmación explícita antes de hacerlo e indicando que la acción no se puede
deshacer. Tras eliminarlo, la aplicación SHALL llevar a la persona de vuelta a la
lista de grupos, donde el grupo eliminado ya no SHALL aparecer. Cancelar la
confirmación MUST NOT eliminar nada.

#### Scenario: El creador elimina el grupo

- **WHEN** el creador confirma la eliminación del grupo
- **THEN** el grupo desaparece y la aplicación lleva a la lista de grupos
- **AND** el grupo eliminado ya no aparece en la lista

#### Scenario: Se cancela la confirmación

- **WHEN** el creador abre la confirmación de eliminar y la cancela
- **THEN** el grupo sigue existiendo
- **AND** no se envió ninguna petición al backend

#### Scenario: La eliminación desaparece de la lista de los demás miembros

- **WHEN** el creador elimina un grupo que tenía otros miembros
- **THEN** ese grupo deja de aparecer también en la lista de esos miembros
