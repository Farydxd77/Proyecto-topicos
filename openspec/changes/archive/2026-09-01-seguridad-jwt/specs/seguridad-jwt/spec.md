## Purpose

Define la autenticación stateless por JSON Web Token del backend: cómo se emiten
y validan los tokens, qué rutas son públicas, y cómo responde el sistema cuando
falta un token válido en una ruta protegida.

## ADDED Requirements

### Requirement: Las rutas protegidas exigen un JWT válido
El sistema SHALL rechazar con HTTP 401 toda petición a una ruta que no sea
pública si no incluye un token JWT válido en el header `Authorization` con el
prefijo `Bearer `. El cuerpo de la respuesta 401 SHALL usar el formato de error
estándar del proyecto con los campos `timestamp`, `status`, `error`, `message` y
`path`.

#### Scenario: Petición sin header Authorization
- **WHEN** un cliente hace una petición a una ruta protegida sin header `Authorization`
- **THEN** el sistema responde HTTP 401 con el cuerpo de error estándar y no invoca al controller

#### Scenario: Header Authorization sin el prefijo Bearer
- **WHEN** un cliente envía `Authorization` con un valor que no empieza por `Bearer `
- **THEN** el sistema responde HTTP 401 con el cuerpo de error estándar

#### Scenario: Token con firma inválida
- **WHEN** un cliente envía `Bearer <token>` cuya firma no corresponde al secreto del servidor
- **THEN** el sistema responde HTTP 401 con el cuerpo de error estándar

#### Scenario: Token expirado
- **WHEN** un cliente envía un token cuya fecha de expiración ya pasó
- **THEN** el sistema responde HTTP 401 con el cuerpo de error estándar

#### Scenario: Token válido cuyo usuario no existe en la base de datos
- **WHEN** un cliente envía un token bien firmado y vigente cuyo `subject` no corresponde a ningún `username` almacenado
- **THEN** el sistema responde HTTP 401 con el cuerpo de error estándar

#### Scenario: Token válido de un usuario existente
- **WHEN** un cliente envía `Bearer <token>` bien firmado, vigente y cuyo `subject` corresponde a un `username` almacenado
- **THEN** la petición supera el filtro de seguridad, queda autenticada y llega al controller (la respuesta no es 401)

### Requirement: Rutas públicas accesibles sin token
El sistema SHALL permitir el acceso sin token a `/api/auth/**` y a
`/actuator/health` (y sus subrutas). Estas rutas SHALL NOT ser bloqueadas por la
capa de seguridad por ausencia de credenciales.

#### Scenario: Health check sin token
- **WHEN** un cliente hace `GET /actuator/health` sin header `Authorization`
- **THEN** el sistema responde HTTP 200

#### Scenario: Ruta de autenticación sin token
- **WHEN** un cliente hace una petición a una subruta de `/api/auth/` sin header `Authorization`
- **THEN** la capa de seguridad no bloquea la petición por falta de token (el resultado depende del handler correspondiente)

### Requirement: Emisión de tokens JWT
El sistema SHALL ofrecer una operación de generación de token que produzca un JWT
firmado con el `username` como `subject` y con una validez de 24 horas a partir
del momento de emisión.

#### Scenario: Token recién emitido
- **WHEN** se genera un token para un `username` dado
- **THEN** el token está firmado por el servidor, su `subject` es ese `username` y su expiración es 24 horas posterior a la emisión

### Requirement: Validación de tokens JWT
El sistema SHALL ofrecer una operación de validación que devuelva verdadero solo
cuando el token tiene firma válida y no está expirado, y falso cuando el token
está expirado, malformado o tiene firma inválida.

#### Scenario: Token íntegro y vigente
- **WHEN** se valida un token bien firmado y no expirado
- **THEN** la operación devuelve verdadero

#### Scenario: Token expirado, malformado o con firma inválida
- **WHEN** se valida un token expirado, con formato incorrecto o con firma que no corresponde al secreto del servidor
- **THEN** la operación devuelve falso y no lanza una excepción al llamador

### Requirement: Autenticación stateless
El sistema SHALL autenticar cada petición exclusivamente a partir del token
recibido, sin crear ni depender de sesiones HTTP de servidor.

#### Scenario: Sin sesión entre peticiones
- **WHEN** un cliente hace dos peticiones autenticadas consecutivas
- **THEN** cada una se autentica de forma independiente a partir de su propio token, sin cookie de sesión emitida por el servidor
