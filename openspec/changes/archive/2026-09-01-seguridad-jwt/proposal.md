## Why

Hoy `SecurityConfig` protege todo con HTTP Basic y un usuario en memoria generado
por Spring Boot; no hay JWT. El backend necesita autenticación por token para que
los futuros controllers de negocio queden protegidos y solo `/api/auth/**` y el
health check sean públicos. Esta es la infraestructura base sobre la que se
construirán los endpoints de registro/login en la siguiente tarea.

## What Changes

- **BREAKING** (para clientes que hoy usan HTTP Basic): se elimina `httpBasic` y
  el `authorizeHttpRequests` actual; la autenticación pasa a ser por
  `Authorization: Bearer <jwt>`.
- Nuevo `JwtUtil` en `security/`: `generateToken(username)` (firma HS256 con
  `jwt.secret`, `subject = username`, expiración `jwt.expiration` = 24 h) y
  `validateToken(token)` (true solo si firma válida y no expirado; false si
  expirado, malformado o firma inválida). Además expone `getUsername(token)`.
- Nuevo `JwtAuthFilter` en `security/` extendiendo `OncePerRequestFilter`: lee el
  header `Authorization`, exige el prefijo `Bearer `, valida el token, carga el
  usuario vía `UserDetailsServiceImpl` y coloca la autenticación en el
  `SecurityContext`. Si falta el token o es inválido, no autentica y deja que
  Spring Security responda 401.
- Nuevo `UserDetailsServiceImpl` en `security/` implementando `UserDetailsService`:
  busca el `Usuario` por `username` y devuelve un `UserDetails`; lanza
  `UsernameNotFoundException` si no existe.
- Nuevo `UsuarioRepository` en `repository/` (Spring Data JPA) con
  `findByUsername(String)` — dependencia mínima necesaria para el punto anterior.
- Nuevo `JwtAuthenticationEntryPoint` en `security/`: escribe el cuerpo 401 con el
  formato de error estándar de `CLAUDE.md` (`timestamp`, `status`, `error`,
  `message`, `path`).
- `SecurityConfig` reescrito: `SessionCreationPolicy.STATELESS`, CSRF desactivado,
  rutas públicas `/api/auth/**` y `/actuator/health/**`, todo lo demás
  `authenticated()`, `JwtAuthFilter` antes de
  `UsernamePasswordAuthenticationFilter`, y el `AuthenticationEntryPoint` anterior.
- Test de integración nuevo (`MockMvc`) que cubre los criterios de aceptación:
  health 200 sin token, ruta protegida 401 sin token (con formato estándar),
  ruta protegida sin 401 con token válido de `JwtUtil`.

## Capabilities

### New Capabilities
- `seguridad-jwt`: autenticación stateless por JWT — generación y validación de
  tokens, filtro de autenticación por request, resolución de usuario contra la
  BD, y política de rutas públicas/protegidas con respuesta 401 en formato
  estándar.

### Modified Capabilities
Ninguna. `backend-bootstrap` ya declara que `/actuator/health` es público y que
existe `config/`; ese comportamiento no cambia (esta capability lo asume, no lo
redefine).

## Impact

- **Código nuevo**:
  `security/JwtUtil.java`, `security/JwtAuthFilter.java`,
  `security/UserDetailsServiceImpl.java`,
  `security/JwtAuthenticationEntryPoint.java`,
  `repository/UsuarioRepository.java`,
  `src/test/.../SeguridadJwtIntegrationTest.java`.
- **Código modificado**: `config/SecurityConfig.java` (reescrito).
- **Sin cambios**: entidades, `BackendApplication`.
- **Config**: usa `jwt.secret` y `jwt.expiration` ya presentes en
  `application.properties`. Para tests se define un `jwt.secret` de test
  suficientemente largo para HS256.
- **Dependencias**: ninguna nueva; `jjwt 0.12.6` y `spring-security-test` ya están
  en el `pom.xml`.
- **API pública**: todos los endpoints (presentes y futuros) salvo
  `/api/auth/**` y `/actuator/health/**` exigen `Authorization: Bearer`.

## Non-Goals

- No se implementan `/api/auth/register` ni `/api/auth/login` (tarea siguiente),
  ni el `AuthController`, ni el `PasswordEncoder`/`AuthenticationManager` que
  necesitará el login.
- No se implementan controllers ni servicios de negocio.
- No hay refresh token, logout ni revocación/blacklist de tokens.
- No se cambian entidades ni el esquema de la base de datos.
- No se añade autorización por roles (todos los usuarios autenticados son iguales).
- No se toca el `frontend/`.
