## Purpose

Establecer que el frontend consume la API real del backend y no una simulación:
define de dónde salen los datos, cómo se elige explícitamente el modo simulado
cuando hace falta trabajar sin base de datos, y cómo se comporta la aplicación
cuando el backend no responde. Es la frontera entre "la interfaz se ve bien" y
"la aplicación funciona".

## ADDED Requirements

### Requirement: El origen de datos por defecto es el backend real

La aplicación SHALL dirigir toda petición de datos al backend real. En desarrollo
las peticiones SHALL usar rutas relativas bajo `/api` y llegar al backend a través
del proxy del servidor de desarrollo, de modo que el navegador observe un único
origen. La aplicación MUST NOT iniciar la API simulada salvo que se active de
forma explícita.

#### Scenario: Arranque en desarrollo sin configuración adicional

- **WHEN** se arranca el servidor de desarrollo del frontend sin definir ninguna
  variable de entorno adicional, con el backend en ejecución
- **THEN** la aplicación no registra ningún interceptor de peticiones simuladas
- **AND** las peticiones a `/api/**` llegan al backend real y sus respuestas son
  las que produce el backend

#### Scenario: Las peticiones no cruzan de origen en el navegador

- **WHEN** la aplicación realiza cualquier petición a la API desde el navegador en
  desarrollo
- **THEN** la petición se dirige al mismo origen desde el que se sirve la
  aplicación
- **AND** el navegador no bloquea la respuesta por política de origen cruzado

### Requirement: No existe una API simulada

El backend real es el único origen de datos de la aplicación. El sistema MUST NOT
incluir una API simulada, ni interceptores de peticiones, ni un service worker que
responda por el backend, ni en desarrollo ni en producción. La aplicación tampoco
SHALL depender de ninguna variable de entorno para decidir contra qué habla: hay un
solo modo.

#### Scenario: Arranque del servidor de desarrollo

- **WHEN** se arranca el servidor de desarrollo del frontend
- **THEN** no se registra ningún service worker ni interceptor de peticiones
- **AND** toda petición a `/api/**` llega al backend real

#### Scenario: Compilación para producción

- **WHEN** se genera el artefacto de producción del frontend
- **THEN** el artefacto no contiene código de simulación de la API ni un service
  worker asociado

#### Scenario: El proyecto no arrastra la dependencia

- **WHEN** se inspeccionan las dependencias del frontend
- **THEN** no figura ninguna librería de simulación de API

### Requirement: Las capacidades ya construidas funcionan contra el backend real

El flujo de autenticación y de perfil que el frontend ya implementa SHALL
comportarse contra el backend real igual que se especificó contra la simulación:
registro, inicio de sesión, persistencia de la sesión entre recargas, consulta y
edición del perfil, cambio de username, cambio de contraseña, cierre de sesión y
cierre automático ante token rechazado. Cualquier diferencia observable entre lo
que devuelve el backend real y lo que asumía la simulación SHALL resolverse a
favor del backend real.

#### Scenario: Registro e inicio de sesión contra el backend real

- **WHEN** una persona se registra desde la aplicación con datos válidos y el
  backend real en ejecución
- **THEN** la cuenta queda creada en la base de datos
- **AND** la aplicación inicia sesión de inmediato con el token devuelto
- **AND** al cerrar sesión y volver a entrar con esas credenciales el acceso
  funciona

#### Scenario: El perfil muestra los datos que persistió el backend

- **WHEN** una persona autenticada edita su nombre y apellido y luego recarga la
  página
- **THEN** la pantalla muestra los datos nuevos leídos del backend real
- **AND** los cambios siguen presentes tras reiniciar el servidor de desarrollo

#### Scenario: El username duplicado lo rechaza el backend real

- **WHEN** una persona intenta registrarse o cambiar su username a uno que ya
  existe en la base de datos
- **THEN** la aplicación muestra el mensaje de conflicto que devuelve el backend
- **AND** conserva los datos ya cargados en el formulario

#### Scenario: La sesión expirada cierra la sesión

- **WHEN** la aplicación realiza una petición a una ruta protegida con un token que
  el backend real rechaza
- **THEN** la aplicación cierra la sesión, avisa que expiró y redirige al inicio de
  sesión

### Requirement: La aplicación informa cuando el backend no está disponible

Cuando el backend no responde —porque no está arrancado, porque la base de datos
no está disponible o porque falla la red—, la aplicación SHALL mostrar un mensaje
que distinga ese fallo de un error de datos, y MUST NOT quedar en estado de carga
permanente ni mostrar una pantalla en blanco. La aplicación SHALL ofrecer reintentar
la operación sin recargar la página.

#### Scenario: El backend no está arrancado

- **WHEN** una persona usa la aplicación con el servidor de desarrollo en marcha
  pero el backend detenido
- **THEN** la pantalla muestra un mensaje de que no se pudo conectar
- **AND** ofrece reintentar
- **AND** la interfaz permanece utilizable

#### Scenario: El backend responde pero la base de datos no está disponible

- **WHEN** el backend está arrancado pero no puede acceder a la base de datos y
  responde con un error de servidor
- **THEN** la aplicación muestra un mensaje de error legible y no un detalle
  técnico crudo
- **AND** no interpreta el fallo como una sesión expirada ni cierra la sesión

### Requirement: La puesta en marcha está documentada

El repositorio SHALL documentar cómo levantar el sistema completo: los tres
procesos implicados y el orden en que deben arrancar. La documentación SHALL
indicar de qué depende cada proceso, de modo que un fallo de arranque sea
diagnosticable sin leer el código.

#### Scenario: Alguien levanta el proyecto por primera vez

- **WHEN** una persona sigue la documentación de puesta en marcha desde un clon
  limpio del repositorio
- **THEN** puede arrancar base de datos, backend y frontend y registrarse en la
  aplicación
- **AND** la documentación indica qué puerto usa cada proceso

#### Scenario: La interfaz no se puede usar sin backend

- **WHEN** una persona arranca solo el frontend, sin base de datos ni backend
- **THEN** la documentación deja claro que los tres procesos son necesarios
- **AND** la aplicación muestra el error de conexión en lugar de aparentar
  funcionar
