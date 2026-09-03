## Purpose

Permitir que una persona cree su cuenta e inicie sesión desde el navegador, que su
sesión sobreviva a las recargas de página, y que las pantallas privadas de la
aplicación queden fuera del alcance de quien no tenga una sesión válida.

## ADDED Requirements

### Requirement: Registro de una cuenta nueva

La aplicación SHALL ofrecer una pantalla pública de registro que pida username,
contraseña, nombre, apellido y CI. Las restricciones del formulario MUST espejar las
del backend: username de 3 a 50 caracteres, contraseña de al menos 8 caracteres,
nombre y apellido de hasta 100 caracteres, CI de hasta 20 caracteres, y ninguno en
blanco. Un registro exitoso SHALL iniciar sesión de inmediato con el token devuelto y
llevar a la persona a su perfil, sin pedirle que inicie sesión otra vez.

#### Scenario: Registro exitoso

- **WHEN** una persona envía el formulario de registro con datos válidos y un username
  disponible
- **THEN** la aplicación guarda la sesión con el token recibido
- **AND** la lleva a la pantalla de perfil ya autenticada

#### Scenario: Username ya en uso

- **WHEN** una persona se registra con un username que ya existe y el backend responde
  409
- **THEN** la aplicación muestra el mensaje de conflicto recibido
- **AND** conserva los datos ya cargados en el formulario para que pueda corregir solo
  el username
- **AND** no inicia ninguna sesión

#### Scenario: Datos que no cumplen las restricciones

- **WHEN** una persona envía el formulario con una contraseña de menos de 8 caracteres
  o algún campo obligatorio vacío
- **THEN** la aplicación señala los campos inválidos
- **AND** no envía la petición al backend

#### Scenario: Rechazo de validación del backend

- **WHEN** el backend responde 400 con errores por campo pese a la validación previa
- **THEN** la aplicación muestra cada mensaje junto a su campo
- **AND** no inicia ninguna sesión

### Requirement: Inicio de sesión

La aplicación SHALL ofrecer una pantalla pública de inicio de sesión que pida username
y contraseña. Un inicio de sesión exitoso SHALL guardar el token de la sesión y llevar
a la persona a la pantalla privada que intentaba visitar, o a su perfil si no había
ninguna. Las credenciales inválidas SHALL producir un mensaje de error sin revelar si
el username existe.

#### Scenario: Credenciales válidas

- **WHEN** una persona envía username y contraseña correctos
- **THEN** la aplicación guarda la sesión y la lleva a la pantalla privada

#### Scenario: Credenciales inválidas

- **WHEN** una persona envía credenciales incorrectas y el backend responde 401
- **THEN** la aplicación muestra un mensaje de credenciales inválidas
- **AND** no distingue si el fallo fue por el username o por la contraseña
- **AND** permanece en la pantalla de inicio de sesión

#### Scenario: Regreso a la pantalla pretendida

- **WHEN** una persona sin sesión intenta entrar a una ruta privada y luego inicia
  sesión correctamente
- **THEN** la aplicación la lleva a la ruta privada que había intentado visitar

#### Scenario: Envío en curso

- **WHEN** una persona envía el formulario y la respuesta todavía no llegó
- **THEN** la aplicación indica que la operación está en curso
- **AND** impide reenviar el mismo formulario mientras tanto

### Requirement: Persistencia de la sesión entre recargas

La aplicación SHALL conservar el token de la sesión en el almacenamiento del navegador
de modo que sobreviva a recargas de página y a la reapertura de la pestaña. Al
arrancar, la aplicación SHALL restaurar la sesión desde ese almacenamiento antes de
decidir qué pantalla mostrar. Un token vencido o ilegible SHALL descartarse y tratarse
como ausencia de sesión.

#### Scenario: Recarga con sesión activa

- **WHEN** una persona con sesión activa recarga la página
- **THEN** sigue autenticada y no vuelve a la pantalla de inicio de sesión

#### Scenario: Token vencido al arrancar

- **WHEN** la aplicación arranca y el token almacenado ya venció
- **THEN** descarta el token almacenado
- **AND** trata la situación como si no hubiera sesión

#### Scenario: Almacenamiento con contenido ilegible

- **WHEN** la aplicación arranca y el contenido almacenado no es un token interpretable
- **THEN** lo descarta sin fallar
- **AND** muestra la pantalla de inicio de sesión

### Requirement: Protección de las rutas privadas

Las rutas privadas SHALL ser inaccesibles sin sesión activa. Una persona sin sesión
que intente entrar a una ruta privada SHALL ser redirigida a la pantalla de inicio de
sesión, y la ruta pretendida SHALL recordarse para llevarla allí tras autenticarse.
Mientras la aplicación restaura la sesión al arrancar, MUST NOT mostrar por un instante
la pantalla de inicio de sesión a alguien que sí tiene sesión válida.

#### Scenario: Acceso sin sesión a una ruta privada

- **WHEN** una persona sin sesión navega directamente a `/perfil`
- **THEN** la aplicación la redirige a `/login`
- **AND** recuerda `/perfil` como destino pretendido

#### Scenario: Restauración de sesión al arrancar

- **WHEN** la aplicación arranca con un token válido almacenado y la ruta pedida es
  privada
- **THEN** muestra un estado de carga hasta resolver la sesión
- **AND** no muestra la pantalla de inicio de sesión en el intervalo

#### Scenario: Persona ya autenticada en una pantalla pública

- **WHEN** una persona con sesión activa navega a `/login` o `/registro`
- **THEN** la aplicación la redirige a su perfil

### Requirement: Cierre de sesión

La aplicación SHALL ofrecer una acción explícita de cerrar sesión, disponible en las
pantallas privadas. Cerrar sesión SHALL eliminar el token del almacenamiento del
navegador, descartar los datos de la persona que quedaran en memoria y llevar a la
pantalla de inicio de sesión.

#### Scenario: Cierre de sesión manual

- **WHEN** una persona con sesión activa usa la acción de cerrar sesión
- **THEN** la aplicación elimina el token del almacenamiento
- **AND** la lleva a la pantalla de inicio de sesión

#### Scenario: Datos no recuperables tras cerrar sesión

- **WHEN** una persona cierra sesión y luego navega hacia atrás en el historial
- **THEN** no ve los datos de perfil de la sesión anterior
- **AND** la aplicación la redirige a la pantalla de inicio de sesión

### Requirement: Cierre de sesión automático ante token rechazado

Cuando el backend rechace una petición con 401 por token ausente, inválido o vencido,
la aplicación SHALL cerrar la sesión automáticamente y llevar a la persona a la
pantalla de inicio de sesión, informando que su sesión expiró. Esta reacción MUST NOT
dispararse por los 401 de credenciales inválidas de la propia pantalla de inicio de
sesión.

#### Scenario: Token vencido durante el uso

- **WHEN** una persona con la pantalla abierta realiza una acción y el backend responde
  401 porque su token venció
- **THEN** la aplicación cierra la sesión y elimina el token almacenado
- **AND** la lleva a la pantalla de inicio de sesión informando que la sesión expiró

#### Scenario: 401 de credenciales inválidas al iniciar sesión

- **WHEN** el backend responde 401 a un intento de inicio de sesión con credenciales
  incorrectas
- **THEN** la aplicación muestra el error en el formulario
- **AND** no lo trata como una sesión expirada
