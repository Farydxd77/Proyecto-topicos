## Purpose

La gestión de quiénes integran un grupo desde la interfaz: cómo el creador
encuentra a una persona registrada en el sistema y la suma al grupo, cómo la quita,
y cómo la aplicación presenta las tres reglas que impone el backend —solo el
creador administra, no se puede agregar a quien ya es miembro, y el creador no
puede quitarse a sí mismo—. Es lo que convierte un grupo de una persona en algo
sobre lo que se puede repartir un gasto.

## ADDED Requirements

### Requirement: Buscar a una persona para agregarla

La aplicación SHALL permitir al creador buscar personas registradas en el sistema
para sumarlas al grupo, por CI, por nombre o por apellido. La búsqueda por CI SHALL
ser exacta, y las de nombre y apellido SHALL encontrar coincidencias parciales sin
distinguir mayúsculas de minúsculas. Los resultados SHALL identificar a cada
persona con datos suficientes para no confundirla con otra: nombre, apellido, CI y
username. La aplicación MUST NOT pedir que se escriba un identificador interno.

#### Scenario: Búsqueda por nombre con resultados

- **WHEN** el creador busca por una parte del nombre de alguien registrado
- **THEN** ve los resultados que coinciden, cada uno con nombre, apellido, CI y
  username
- **AND** puede elegir a cualquiera de ellos para agregarlo

#### Scenario: Búsqueda que no distingue mayúsculas

- **WHEN** el creador busca escribiendo en un caso distinto al almacenado
- **THEN** los resultados incluyen igualmente a quienes coinciden

#### Scenario: Búsqueda sin resultados

- **WHEN** el creador busca algo que no coincide con nadie
- **THEN** la aplicación indica que no se encontró a nadie con ese criterio
- **AND** la pantalla queda lista para buscar de nuevo

#### Scenario: Búsqueda por CI exacto

- **WHEN** el creador busca por el CI completo de una persona registrada
- **THEN** ve a esa persona en los resultados

#### Scenario: La búsqueda falla

- **WHEN** la búsqueda falla porque el backend no responde
- **THEN** la aplicación muestra el error y permite reintentar sin perder lo
  escrito

### Requirement: Agregar un miembro al grupo

La aplicación SHALL permitir al creador agregar al grupo a una persona elegida de
los resultados de la búsqueda. Tras agregarla con éxito, la lista de miembros del
grupo SHALL reflejarla sin que haga falta recargar la página, y esa persona SHALL
pasar a ver el grupo entre los suyos. Si el backend rechaza la operación, la
aplicación SHALL mostrar el mensaje recibido sin alterar la lista de miembros.

#### Scenario: Se agrega a una persona nueva

- **WHEN** el creador elige de la búsqueda a alguien que aún no es miembro y lo
  agrega
- **THEN** esa persona aparece en la lista de miembros sin recargar la página
- **AND** el grupo pasa a aparecer en la lista de grupos de esa persona

#### Scenario: El backend rechaza la operación

- **WHEN** el backend responde con un error al agregar
- **THEN** la aplicación muestra el mensaje recibido
- **AND** la lista de miembros queda como estaba

### Requirement: Quien ya es miembro no se puede volver a agregar

La aplicación SHALL señalar en los resultados de la búsqueda a las personas que ya
integran el grupo, y MUST NOT ofrecer agregarlas de nuevo. Si aun así la petición
llega al backend y este responde con un conflicto, la aplicación SHALL explicar que
esa persona ya es miembro y MUST NOT duplicarla en la lista.

#### Scenario: Un miembro actual aparece en la búsqueda

- **WHEN** el creador busca a alguien que ya integra el grupo
- **THEN** esa persona aparece en los resultados marcada como miembro actual
- **AND** no se ofrece la acción de agregarla

#### Scenario: Se intenta agregar a alguien que ya es miembro

- **WHEN** la petición de agregar a un miembro actual llega igualmente al backend
- **THEN** la aplicación explica que esa persona ya es miembro del grupo
- **AND** la lista de miembros no la muestra dos veces

### Requirement: Quitar un miembro del grupo

La aplicación SHALL permitir al creador quitar del grupo a cualquier miembro que no
sea él mismo, pidiendo confirmación explícita e indicando a quién se va a quitar.
Tras quitarlo, la lista de miembros SHALL actualizarse sin recargar la página, y
esa persona SHALL dejar de ver el grupo entre los suyos. Cancelar la confirmación
MUST NOT quitar a nadie.

#### Scenario: El creador quita a un miembro

- **WHEN** el creador confirma que quiere quitar a un miembro
- **THEN** esa persona desaparece de la lista de miembros sin recargar la página
- **AND** el grupo deja de aparecer en la lista de grupos de esa persona

#### Scenario: Se cancela la confirmación

- **WHEN** el creador abre la confirmación de quitar a alguien y la cancela
- **THEN** esa persona sigue siendo miembro
- **AND** no se envió ninguna petición al backend

#### Scenario: Se intenta quitar a quien ya no es miembro

- **WHEN** la petición de quitar llega al backend para alguien que ya no integra el
  grupo
- **THEN** la aplicación explica que esa persona no es miembro del grupo
- **AND** la lista mostrada queda al día con lo que dice el backend

### Requirement: El creador no puede quitarse a sí mismo

La aplicación MUST NOT ofrecer la acción de quitar del grupo sobre el propio
creador. Si la petición llega igualmente al backend y este la rechaza, la
aplicación SHALL explicar que el creador no puede quitarse a sí mismo del grupo, y
la composición del grupo SHALL quedar intacta.

#### Scenario: El creador se mira a sí mismo en la lista

- **WHEN** el creador ve la lista de miembros de su grupo
- **THEN** junto a su propia fila no aparece la acción de quitar
- **AND** junto a las demás filas sí aparece

#### Scenario: La petición de auto-baja llega al backend

- **WHEN** la petición de quitar al creador llega igualmente al backend
- **THEN** la aplicación explica que el creador no puede quitarse a sí mismo
- **AND** la lista de miembros no cambia

### Requirement: Solo el creador gestiona los miembros

La aplicación SHALL mostrar la búsqueda para agregar y las acciones de quitar
únicamente a quien creó el grupo. Los demás miembros SHALL seguir viendo la lista
completa de integrantes, pero MUST NOT ver forma alguna de modificarla. Si una de
esas acciones llega al backend enviada por alguien que no es el creador, la
aplicación SHALL mostrar el mensaje de permiso denegado sin romperse.

#### Scenario: Un miembro no creador abre el detalle

- **WHEN** un miembro que no creó el grupo abre su detalle
- **THEN** ve la lista completa de miembros
- **AND** no ve el buscador para agregar ni ninguna acción de quitar

#### Scenario: Una acción de gestión llega desde un no creador

- **WHEN** una petición de agregar o de quitar llega al backend enviada por alguien
  que no es el creador
- **THEN** la aplicación muestra el mensaje de permiso denegado
- **AND** la composición del grupo no cambia
