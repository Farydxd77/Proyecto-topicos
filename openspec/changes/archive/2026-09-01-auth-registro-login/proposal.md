## Why

El change `seguridad-jwt` dejó la infraestructura de validación de JWT lista, pero
`/api/auth/**` sigue sin handlers: no hay forma de crear un usuario ni de obtener
un token. Sin registro/login, ningún cliente puede llegar a los endpoints
protegidos. Esta tarea implementa los dos endpoints públicos que faltan.

## What Changes

- Nuevo `POST /api/auth/register`: recibe `username`, `password`, `nombre`,
  `apellido`, `ci`; valida los campos; si el `username` está libre, hashea el
  password con BCrypt, crea el `Usuario` y su `Participante` vinculado (1:1) en
  una sola transacción, genera un JWT y responde **201 Created** con el token y
  los datos del participante.
- Nuevo `POST /api/auth/login`: recibe `username` y `password`; si las
  credenciales son correctas responde **200 OK** con un JWT (24 h) y los datos
  básicos del usuario; si no, **401 Unauthorized** con mensaje genérico que no
  revela si el `username` existe.
- Nuevo `GlobalExceptionHandler` (`@RestControllerAdvice` en `exception/`): traduce
  errores de validación a **400**, conflicto de `username` a **409** y
  credenciales inválidas a **401**, todos con el formato de error estándar de
  `CLAUDE.md` (`timestamp`, `status`, `error`, `message`, `path`); en el 400
  incluye el detalle por campo.
- Nuevo bean `PasswordEncoder` (`BCryptPasswordEncoder`) en `config/`.
- Nuevos DTOs en `dto/request/` (`RegisterRequest`, `LoginRequest`) y
  `dto/response/` (`RegisterResponse`, `LoginResponse`); ninguna respuesta
  incluye jamás la contraseña.
- Nuevo `ParticipanteRepository`.
- Nuevos `AuthController` y `AuthService`.
- Pruebas de integración de los dos endpoints y sus casos de error.

## Capabilities

### New Capabilities
- `auth`: registro de nuevos usuarios y autenticación por credenciales de
  usuarios existentes, con emisión del JWT que consume la capa `seguridad-jwt`.

### Modified Capabilities
Ninguna. `seguridad-jwt` ya declara que `/api/auth/**` es público y define la
emisión/validación de tokens; esta capability los usa sin cambiar ese contrato.

## Impact

- **Código nuevo**:
  `controller/AuthController.java`, `service/AuthService.java`,
  `exception/GlobalExceptionHandler.java`,
  `exception/UsernameAlreadyExistsException.java`,
  `repository/ParticipanteRepository.java`,
  `dto/request/RegisterRequest.java`, `dto/request/LoginRequest.java`,
  `dto/response/RegisterResponse.java`, `dto/response/LoginResponse.java`,
  y un método `passwordEncoder()` en un `@Configuration`.
- **Código modificado**: ninguno previsto (SecurityConfig ya abre `/api/auth/**`).
  Si el `PasswordEncoder` se añade a `SecurityConfig`, ese archivo se toca.
- **Base de datos**: sin cambios de esquema; se insertan filas en `usuarios` y
  `participantes`.
- **Dependencias**: ninguna nueva (`spring-boot-starter-validation` y
  `spring-security-crypto` ya están vía los starters).
- **Contrato HTTP**: se activan `POST /api/auth/register` (201) y
  `POST /api/auth/login` (200); errores 400/401/409 en formato estándar.

## Non-Goals

- No refresh token, no logout, no recuperación de contraseña.
- No roles ni permisos.
- No endpoint de "usuario actual" (`GET /api/auth/me`) ni edición de perfil.
- No verificación de email ni rate-limiting de intentos de login.
- No se modifican entidades ni el esquema de la base de datos.
- No se toca el `frontend/`.
