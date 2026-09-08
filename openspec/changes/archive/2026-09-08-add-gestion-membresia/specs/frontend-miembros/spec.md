## ADDED Requirements

### Requirement: Transferir el rol de creador desde la interfaz

La aplicación SHALL ofrecer al creador del grupo, y solo a él, una acción para
transferirle el rol a otro miembro. La acción SHALL dejar elegir entre los miembros
actuales del grupo distintos del propio creador, y SHALL pedir confirmación
explícita nombrando a la persona que recibirá el rol, porque quien lo entrega no
puede recuperarlo por su cuenta. Tras la transferencia, la interfaz SHALL reflejar
sin recargar la página que el creador cambió: quien entregó el rol deja de ver las
acciones de gestión y quien lo recibió empieza a verlas. Cancelar la confirmación
MUST NOT transferir nada.

#### Scenario: El creador transfiere el rol

- **WHEN** el creador elige a otro miembro y confirma la transferencia
- **THEN** la interfaz muestra a esa persona como creador del grupo sin recargar la
  página
- **AND** quien entregó el rol deja de ver el buscador para agregar y las acciones
  de quitar
- **AND** sigue apareciendo en la lista de miembros

#### Scenario: Se cancela la confirmación

- **WHEN** el creador abre la confirmación de transferir y la cancela
- **THEN** el creador del grupo no cambia
- **AND** no se envió ninguna petición al backend

#### Scenario: Un grupo de un solo miembro

- **WHEN** el creador es el único integrante del grupo
- **THEN** la acción de transferir el rol no se ofrece, o se ofrece deshabilitada
  explicando que no hay a quién transferirlo

#### Scenario: Un miembro no creador abre el detalle

- **WHEN** un miembro que no creó el grupo abre su detalle
- **THEN** no ve la acción de transferir el rol de creador

#### Scenario: La transferencia es rechazada por el backend

- **WHEN** la petición de transferir llega al backend y este la rechaza
- **THEN** la aplicación muestra el mensaje de error del backend
- **AND** el creador mostrado no cambia

### Requirement: Abandonar el grupo desde la interfaz

La aplicación SHALL ofrecer a los miembros que no son el creador una acción para
abandonar el grupo, con confirmación explícita que advierta que dejarán de ver sus
gastos y balances. Tras abandonarlo, la aplicación SHALL llevar a la persona a su
lista de grupos, donde ese grupo ya no SHALL aparecer. La aplicación MUST NOT
ofrecer esta acción al creador; en su lugar SHALL indicarle que primero debe
transferir el rol o eliminar el grupo.

#### Scenario: Un miembro abandona el grupo

- **WHEN** un miembro que no es el creador confirma que quiere abandonar el grupo
- **THEN** la aplicación lo lleva a su lista de grupos
- **AND** ese grupo ya no aparece en la lista

#### Scenario: Se cancela la confirmación

- **WHEN** un miembro abre la confirmación de abandonar y la cancela
- **THEN** sigue siendo miembro del grupo
- **AND** no se envió ninguna petición al backend

#### Scenario: El creador no ve la acción de abandonar

- **WHEN** el creador abre el detalle de su grupo
- **THEN** no ve la acción de abandonar el grupo
- **AND** ve, en su lugar, la indicación de que debe transferir el rol o eliminar el
  grupo

#### Scenario: La salida es rechazada por el backend

- **WHEN** la petición de abandonar llega al backend y este la rechaza
- **THEN** la aplicación muestra el mensaje de error del backend
- **AND** la persona sigue viendo el grupo

## MODIFIED Requirements

### Requirement: Solo el creador gestiona los miembros

La aplicación SHALL mostrar la búsqueda para agregar, las acciones de quitar a otro
y la transferencia del rol de creador únicamente a quien es creador del grupo en ese
momento. Los demás miembros SHALL seguir viendo la lista completa de integrantes,
pero MUST NOT ver forma alguna de modificarla, salvo la acción de abandonar el grupo,
que actúa solo sobre su propia membresía. Si una de las acciones reservadas al
creador llega al backend enviada por alguien que no lo es, la aplicación SHALL
mostrar el mensaje de permiso denegado sin romperse.

#### Scenario: Un miembro no creador abre el detalle

- **WHEN** un miembro que no creó el grupo abre su detalle
- **THEN** ve la lista completa de miembros
- **AND** no ve el buscador para agregar, ni acciones de quitar a otros, ni la
  transferencia del rol
- **AND** sí ve la acción de abandonar el grupo

#### Scenario: Una acción de gestión llega desde un no creador

- **WHEN** una petición de agregar, de quitar a otro o de transferir el rol llega al
  backend enviada por alguien que no es el creador
- **THEN** la aplicación muestra el mensaje de permiso denegado
- **AND** la composición del grupo no cambia

#### Scenario: Quien recibe el rol pasa a ver la gestión

- **WHEN** un miembro recibe el rol de creador mediante una transferencia
- **THEN** pasa a ver el buscador para agregar, las acciones de quitar y la
  transferencia del rol
- **AND** deja de ver la acción de abandonar el grupo
