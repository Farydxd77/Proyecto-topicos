# auth Specification

## Purpose

Define el registro de nuevos usuarios y la autenticación por credenciales: qué
recibe y devuelve cada endpoint público de `/api/auth`, cómo se valida la
entrada, y cómo se protege la contraseña y la privacidad de las cuentas.

## Requirements

### Requirement: Registro de un nuevo usuario
El sistema SHALL exponer `POST /api/auth/register` que reciba `username`,
`password`, `nombre`, `apellido` y `ci`. Si el `username` no está en uso, el
sistema SHALL crear un `Usuario` con la contraseña almacenada como hash BCrypt y,
en la misma operación, un `Participante` vinculado a ese usuario (relación 1 a 1),
y SHALL responder HTTP 201 con un token JWT y los datos del participante creado
(identificador, `nombre`, `apellido`, `ci`, `username`). La respuesta SHALL NOT
incluir la contraseña ni su hash.

#### Scenario: Registro con datos válidos y username libre
- **WHEN** un visitante envía `POST /api/auth/register` con todos los campos válidos y un `username` que no existe
- **THEN** el sistema responde HTTP 201, persiste el `Usuario` (contraseña hasheada con BCrypt) y su `Participante`, y el cuerpo incluye un token JWT y los datos del participante, sin contraseña

#### Scenario: Token del registro utilizable
- **WHEN** un cliente usa el token devuelto por un registro exitoso como `Authorization: Bearer` contra una ruta protegida
- **THEN** la petición no es rechazada con 401 por la capa de seguridad

### Requirement: Registro rechaza username duplicado
El sistema SHALL rechazar con HTTP 409 un `POST /api/auth/register` cuyo
`username` ya exista, con el cuerpo de error estándar (`timestamp`, `status`,
`error`, `message`, `path`), y SHALL NOT crear ningún registro.

#### Scenario: Username ya registrado
- **WHEN** un visitante envía `POST /api/auth/register` con un `username` que ya pertenece a otro usuario
- **THEN** el sistema responde HTTP 409 con el cuerpo de error estándar y no crea `Usuario` ni `Participante`

### Requirement: Validación de la entrada de registro
El sistema SHALL validar la petición de registro y responder HTTP 400 con el
cuerpo de error estándar y el detalle de cada campo inválido cuando: `password`
tiene menos de 8 caracteres, `username` tiene menos de 3 caracteres, `ci` está
vacío, o `nombre` o `apellido` están vacíos, o falta cualquiera de esos campos.

#### Scenario: Password demasiado corto
- **WHEN** un visitante envía `POST /api/auth/register` con `password` de menos de 8 caracteres
- **THEN** el sistema responde HTTP 400 con el cuerpo de error estándar indicando el campo `password`

#### Scenario: Campos obligatorios ausentes o vacíos
- **WHEN** un visitante envía `POST /api/auth/register` sin `username`, `nombre`, `apellido` o `ci`, o con esos valores vacíos
- **THEN** el sistema responde HTTP 400 con el cuerpo de error estándar listando cada campo inválido

### Requirement: Login de un usuario existente
El sistema SHALL exponer `POST /api/auth/login` que reciba `username` y
`password`. Si las credenciales corresponden a un usuario existente, el sistema
SHALL responder HTTP 200 con un token JWT válido por 24 horas y los datos básicos
del usuario (identificador y `username`). La respuesta SHALL NOT incluir la
contraseña ni su hash.

#### Scenario: Credenciales correctas
- **WHEN** un usuario registrado envía `POST /api/auth/login` con su `username` y `password` correctos
- **THEN** el sistema responde HTTP 200 con un token JWT (validez 24 h) y los datos básicos del usuario, sin contraseña

### Requirement: Login no revela existencia de cuentas
El sistema SHALL responder HTTP 401 con un mensaje genérico ante un
`POST /api/auth/login` con credenciales inválidas, sin distinguir entre
"`username` inexistente" y "contraseña incorrecta".

#### Scenario: Contraseña incorrecta
- **WHEN** un visitante envía `POST /api/auth/login` con un `username` existente y una `password` incorrecta
- **THEN** el sistema responde HTTP 401 con un mensaje genérico que no indica que el `username` sí existe

#### Scenario: Username inexistente
- **WHEN** un visitante envía `POST /api/auth/login` con un `username` que no existe
- **THEN** el sistema responde HTTP 401 con el mismo mensaje genérico que para una contraseña incorrecta

### Requirement: Los endpoints de auth son públicos
El sistema SHALL permitir `POST /api/auth/register` y `POST /api/auth/login` sin
token de autenticación.

#### Scenario: Acceso sin token
- **WHEN** un visitante llama a `POST /api/auth/register` o `POST /api/auth/login` sin header `Authorization`
- **THEN** la capa de seguridad no bloquea la petición y el endpoint la procesa
