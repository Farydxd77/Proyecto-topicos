## Purpose

Dar a la persona autenticada una pantalla donde consultar su propia cuenta y editarla:
sus datos personales de participante, su username y su contraseña, siempre operando
sobre la cuenta del token de la sesión y nunca sobre la de otra persona.

## ADDED Requirements

### Requirement: Consulta del perfil propio

La pantalla de perfil SHALL mostrar los datos de la cuenta de la persona autenticada:
username, nombre, apellido, CI y la fecha de creación de la cuenta. La pantalla MUST
NOT mostrar la contraseña ni ninguna representación de ella. Mientras los datos se
cargan, la pantalla SHALL indicar que la carga está en curso; si la carga falla, SHALL
mostrar el error y ofrecer reintentar.

#### Scenario: Perfil cargado

- **WHEN** una persona autenticada abre la pantalla de perfil
- **THEN** ve su username, nombre, apellido, CI y fecha de creación
- **AND** no ve ningún campo de contraseña

#### Scenario: Carga en curso

- **WHEN** la pantalla de perfil está pidiendo los datos al backend
- **THEN** muestra un estado de carga en lugar de campos vacíos

#### Scenario: Fallo al cargar el perfil

- **WHEN** la petición del perfil falla por un error del backend o de red
- **THEN** la pantalla muestra el mensaje de error
- **AND** ofrece una acción para reintentar la carga

### Requirement: Edición de los datos personales

La pantalla de perfil SHALL permitir editar el nombre y el apellido de la persona.
Ambos MUST ser obligatorios y de hasta 100 caracteres, espejando la validación del
backend. Tras una edición exitosa, la pantalla SHALL mostrar los datos actualizados
sin necesidad de recargar la página y SHALL confirmar el éxito de la operación.

#### Scenario: Edición exitosa

- **WHEN** una persona guarda un nombre y un apellido válidos
- **THEN** el backend persiste los nuevos valores
- **AND** la pantalla muestra los datos actualizados y confirma el guardado

#### Scenario: Nombre o apellido vacíos

- **WHEN** una persona intenta guardar con el nombre o el apellido en blanco
- **THEN** la pantalla señala el campo inválido
- **AND** no envía la petición al backend

#### Scenario: Cancelar la edición

- **WHEN** una persona modifica los campos y cancela sin guardar
- **THEN** la pantalla vuelve a mostrar los valores previamente guardados

### Requirement: El CI se muestra pero no se edita

La pantalla de perfil SHALL mostrar el CI de la persona como dato de solo lectura y
MUST NOT ofrecer ninguna forma de modificarlo, en coherencia con el backend, que ignora
cualquier CI recibido y conserva el registrado.

#### Scenario: CI visible y no editable

- **WHEN** una persona abre la pantalla de perfil
- **THEN** ve su CI
- **AND** no dispone de ningún control para modificarlo

### Requirement: Cambio de username

La pantalla de perfil SHALL permitir cambiar el username mediante una operación
separada de la edición de los datos personales. El nuevo username MUST tener entre 3 y
50 caracteres. Un cambio exitoso SHALL reflejarse de inmediato en la pantalla y en la
navegación, sin cerrar la sesión ni obligar a volver a autenticarse. Si el username ya
está en uso, la pantalla SHALL mostrar el conflicto y conservar el username anterior.

#### Scenario: Cambio de username exitoso

- **WHEN** una persona envía un username disponible y válido
- **THEN** el backend lo persiste
- **AND** la pantalla y la navegación muestran el nuevo username
- **AND** la sesión sigue activa

#### Scenario: Username ya en uso

- **WHEN** una persona envía un username que ya pertenece a otra cuenta y el backend
  responde 409
- **THEN** la pantalla muestra el mensaje de conflicto
- **AND** el username mostrado sigue siendo el anterior

#### Scenario: Username demasiado corto

- **WHEN** una persona envía un username de menos de 3 caracteres
- **THEN** la pantalla señala el campo inválido
- **AND** no envía la petición al backend

#### Scenario: Username sin cambios

- **WHEN** una persona envía su propio username actual sin modificarlo
- **THEN** la operación se resuelve con éxito
- **AND** la sesión sigue activa y el username no cambia

### Requirement: Cambio de contraseña

La pantalla de perfil SHALL permitir cambiar la contraseña mediante una operación
separada. La nueva contraseña MUST tener al menos 8 caracteres y la pantalla SHALL
pedir su confirmación, verificando que ambas coincidan antes de enviarla. Tras un
cambio exitoso, la pantalla SHALL confirmar la operación, vaciar los campos y mantener
la sesión activa. Los campos de contraseña MUST enmascararse y MUST NOT conservarse
después de la operación.

#### Scenario: Cambio de contraseña exitoso

- **WHEN** una persona envía una contraseña de al menos 8 caracteres y su confirmación
  coincidente
- **THEN** el backend la persiste hasheada
- **AND** la pantalla confirma el cambio y vacía los campos
- **AND** la sesión sigue activa

#### Scenario: La confirmación no coincide

- **WHEN** una persona envía una contraseña y una confirmación distintas
- **THEN** la pantalla señala que no coinciden
- **AND** no envía la petición al backend

#### Scenario: Contraseña demasiado corta

- **WHEN** una persona envía una contraseña de menos de 8 caracteres
- **THEN** la pantalla señala el campo inválido
- **AND** no envía la petición al backend

#### Scenario: Contraseñas enmascaradas

- **WHEN** una persona escribe en los campos de contraseña
- **THEN** el contenido se muestra enmascarado
