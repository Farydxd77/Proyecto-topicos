## Purpose

Establecer los cimientos de la aplicación web de Cuentas Claras: una aplicación de
página única, en español, que reemplaza el andamiaje de ejemplo del generador, define
el mapa de rutas y la navegación, se comunica con la API del backend y traduce sus
respuestas de error al formato que la interfaz muestra al usuario.

## ADDED Requirements

### Requirement: Identidad propia de la aplicación

La aplicación web SHALL presentarse como Cuentas Claras y MUST NOT conservar ningún
resto del andamiaje de ejemplo del generador de proyectos. El documento HTML SHALL
declarar el idioma español y llevar como título "Cuentas Claras". Los recursos
gráficos de ejemplo que no formen parte de la interfaz final SHALL ser eliminados del
repositorio.

#### Scenario: Carga inicial de la aplicación

- **WHEN** una persona abre la aplicación en el navegador
- **THEN** la pestaña muestra el título "Cuentas Claras"
- **AND** el documento declara el idioma español
- **AND** no se muestra ningún contador, logotipo ni texto de ejemplo del generador

### Requirement: Mapa de rutas de la aplicación

La aplicación SHALL exponer rutas navegables y enlazables mediante la barra de
direcciones del navegador: `/login` y `/registro` como rutas públicas, y `/perfil`
como ruta privada. La raíz `/` SHALL redirigir a `/perfil` cuando hay sesión activa y
a `/login` cuando no la hay. Una ruta desconocida SHALL mostrar una página de "no
encontrado" con un enlace de vuelta a la aplicación, sin dejar la pantalla en blanco.

#### Scenario: Navegación directa por URL

- **WHEN** una persona con sesión activa escribe `/perfil` en la barra de direcciones
  y recarga
- **THEN** la aplicación muestra la pantalla de perfil, sin volver al inicio

#### Scenario: Raíz con sesión activa

- **WHEN** una persona con sesión activa navega a `/`
- **THEN** la aplicación la redirige a `/perfil`

#### Scenario: Raíz sin sesión

- **WHEN** una persona sin sesión navega a `/`
- **THEN** la aplicación la redirige a `/login`

#### Scenario: Ruta inexistente

- **WHEN** una persona navega a una ruta que no existe
- **THEN** la aplicación muestra una página de "no encontrado" con un enlace de
  regreso
- **AND** no muestra una pantalla en blanco ni un error del navegador

### Requirement: Navegación con funcionalidad futura señalizada

Las pantallas privadas SHALL compartir un marco común con navegación. La navegación
SHALL incluir los destinos previstos de Grupos y Gastos, que MUST presentarse
visiblemente deshabilitados y MUST NOT ser navegables, indicando que aún no están
disponibles. La navegación SHALL identificar la sesión activa mostrando el username y
ofrecer la acción de cerrar sesión.

#### Scenario: Persona autenticada ve la navegación

- **WHEN** una persona con sesión activa entra a una pantalla privada
- **THEN** ve la navegación con su username y la acción de cerrar sesión
- **AND** ve los destinos Grupos y Gastos marcados como no disponibles

#### Scenario: Intento de usar un destino no disponible

- **WHEN** una persona intenta activar el destino Grupos o Gastos
- **THEN** la aplicación no navega a ninguna parte y la ruta actual no cambia

### Requirement: Comunicación con la API del backend

La aplicación SHALL consumir la API del backend bajo la ruta `/api` del mismo origen
desde el que se sirve, de modo que el navegador no requiera una configuración de
CORS en el backend durante el desarrollo. Toda petición a un recurso protegido SHALL
incluir la cabecera `Authorization` con el token de la sesión activa en el esquema
`Bearer`. Las peticiones a los recursos públicos de autenticación MUST NOT incluir esa
cabecera.

#### Scenario: Petición a un recurso protegido

- **WHEN** la aplicación pide un recurso protegido teniendo sesión activa
- **THEN** la petición viaja con la cabecera `Authorization: Bearer {token}`

#### Scenario: Petición de inicio de sesión

- **WHEN** la aplicación envía las credenciales de inicio de sesión o de registro
- **THEN** la petición no incluye cabecera `Authorization`

#### Scenario: Peticiones de mismo origen sin CORS

- **WHEN** la aplicación pide cualquier recurso de la API en desarrollo
- **THEN** la petición se dirige a `/api` del mismo origen que sirve la aplicación
- **AND** se resuelve correctamente sin que ninguna política de CORS sea necesaria

#### Scenario: Independencia de quién responde la API

- **WHEN** cambia el origen de las respuestas de la API entre desarrollo y producción
- **THEN** el código de la aplicación no cambia
- **AND** la aplicación sigue dirigiéndose a las mismas rutas relativas bajo `/api`

### Requirement: Traducción del formato de error estándar del backend

La aplicación SHALL interpretar el formato de error estándar del backend
(`timestamp`, `status`, `error`, `message`, `path`) y exponer su `message` a la
interfaz. Cuando la respuesta sea un 400 de validación que incluya un mapa de errores
por campo, la aplicación SHALL asociar cada mensaje a su campo correspondiente en el
formulario que originó la petición. Cuando la respuesta de error no tenga cuerpo o no
siga el formato estándar, la aplicación SHALL mostrar un mensaje genérico y MUST NOT
fallar ni quedar en un estado de carga permanente.

#### Scenario: Error con mensaje del backend

- **WHEN** el backend responde con un error en formato estándar
- **THEN** la interfaz muestra el `message` recibido

#### Scenario: Error de validación con detalle por campo

- **WHEN** el backend responde 400 con un mapa de errores por campo
- **THEN** la interfaz muestra cada mensaje junto al campo del formulario que le
  corresponde

#### Scenario: Respuesta de error sin cuerpo interpretable

- **WHEN** el backend o la red responden con un error sin cuerpo o con un cuerpo que
  no sigue el formato estándar
- **THEN** la interfaz muestra un mensaje de error genérico
- **AND** el formulario vuelve a quedar utilizable

#### Scenario: Respuesta exitosa sin cuerpo

- **WHEN** una operación termina con éxito y el backend responde sin cuerpo
- **THEN** la aplicación la trata como exitosa y no muestra ningún error
