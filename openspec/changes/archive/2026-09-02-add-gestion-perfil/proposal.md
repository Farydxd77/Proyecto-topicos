## Why

Hoy un usuario autenticado puede registrarse e iniciar sesión, pero no tiene forma
de ver ni editar su propia cuenta después del registro. Falta exponer la gestión
del perfil propio (datos de acceso y datos personales del participante vinculado),
que es la base para cualquier pantalla de "Mi cuenta" en la fase 2.

## What Changes

- Nuevo endpoint `GET /api/perfil/me`: devuelve los datos del usuario autenticado y
  su participante vinculado (id, username, nombre, apellido, ci, created_at), nunca
  la contraseña.
- Nuevo endpoint `PUT /api/perfil/me`: actualiza `nombre` y `apellido` del
  participante vinculado. El `ci` **no** es editable (se ignora si viene en el body).
- Nuevo endpoint `PUT /api/perfil/me/username`: cambia el username del usuario si el
  nuevo no existe; devuelve 409 si ya está en uso.
- Nuevo endpoint `PUT /api/perfil/me/password`: cambia la contraseña, hasheada con
  BCrypt, exigiendo mínimo 8 caracteres.
- Nueva excepción `ResourceNotFoundException` + manejo en `GlobalExceptionHandler`
  para el caso defensivo de participante no encontrado (404 con formato estándar).
- Todos los endpoints operan siempre sobre el usuario del token JWT (`/me`); no
  reciben id de otro usuario.

## Capabilities

### New Capabilities

- `perfil`: gestión de la cuenta propia del usuario autenticado — consulta y edición
  de sus datos de acceso (username, password) y de su información personal
  (nombre, apellido) a través de endpoints `/api/perfil/me`.

### Modified Capabilities

<!-- Ninguna: no cambian requisitos de capacidades existentes. -->

## Impact

- **Código nuevo**: `PerfilController`, `PerfilService`, DTOs
  (`PerfilResponse`, `ActualizarPerfilRequest`, `CambiarUsernameRequest`,
  `CambiarPasswordRequest`), `ResourceNotFoundException`.
- **Código modificado**: `ParticipanteRepository` (agregar `findByUsuarioId`),
  `GlobalExceptionHandler` (handler para `ResourceNotFoundException`).
- **APIs**: nueva ruta base `/api/perfil/**`, protegida por JWT (ya cubierta por
  `anyRequest().authenticated()` en `SecurityConfig`, sin cambios de configuración).
- **Base de datos**: sin cambios de esquema (usa tablas `usuarios` y `participantes`
  existentes).
- **Dependencias**: ninguna nueva.
- **Fuera de alcance**: sin cambios en frontend.
