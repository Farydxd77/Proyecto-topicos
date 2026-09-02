## 1. Repositorio y excepción de soporte

- [x] 1.1 En `repository/ParticipanteRepository.java` agregar
  `Optional<Participante> findByUsuarioId(Long usuarioId)`. Verificar con
  `mvnw.cmd compile` que compila.
- [x] 1.2 Crear `exception/ResourceNotFoundException.java` (extiende
  `RuntimeException`, constructor con `String message`). Verificar con
  `mvnw.cmd compile`.
- [x] 1.3 En `exception/GlobalExceptionHandler.java` añadir
  `@ExceptionHandler(ResourceNotFoundException.class)` que devuelve 404 con
  `standardBody(HttpStatus.NOT_FOUND, ex.getMessage(), request)`. Verificar con
  `mvnw.cmd compile` y revisar que el JSON sigue el formato de error estándar.

## 2. DTOs

- [x] 2.1 Crear `dto/response/PerfilResponse.java` como `record` con
  `Long id, Long usuarioId, String username, String nombre, String apellido,
  String ci, Instant createdAt`. Sin ningún campo de contraseña. Verificar con
  `mvnw.cmd compile`.
- [x] 2.2 Crear `dto/request/ActualizarPerfilRequest.java` como `record` con
  `@NotBlank @Size(max=100) String nombre` y `@NotBlank @Size(max=100) String
  apellido` (sin campo `ci`). Verificar con `mvnw.cmd compile`.
- [x] 2.3 Crear `dto/request/CambiarUsernameRequest.java` como `record` con
  `@NotBlank @Size(min=3, max=50) String username`. Verificar con
  `mvnw.cmd compile`.
- [x] 2.4 Crear `dto/request/CambiarPasswordRequest.java` como `record` con
  `@NotBlank @Size(min=8) String password`. Verificar con `mvnw.cmd compile`.

## 3. Servicio

- [x] 3.1 Crear `service/PerfilService.java` (`@Service`, constructor injection de
  `UsuarioRepository`, `ParticipanteRepository`, `PasswordEncoder`) con un método
  privado `usuarioActual()` que resuelve el `username` desde
  `SecurityContextHolder` y carga el `Usuario` (lanza `ResourceNotFoundException`
  si no existe). Verificar con `mvnw.cmd compile`.
- [x] 3.2 Implementar `PerfilResponse obtenerMiPerfil()`
  (`@Transactional(readOnly = true)`): carga el participante con
  `findByUsuarioId`, lanza `ResourceNotFoundException` si no hay, y mapea a
  `PerfilResponse`. Verificar con `mvnw.cmd compile`.
- [x] 3.3 Implementar `PerfilResponse actualizarMiPerfil(ActualizarPerfilRequest)`
  (`@Transactional`): actualiza solo `nombre` y `apellido` del participante, no
  toca `ci`, guarda y devuelve `PerfilResponse`. Verificar con `mvnw.cmd compile`.
- [x] 3.4 Implementar `PerfilResponse cambiarUsername(CambiarUsernameRequest)`
  (`@Transactional`): si el nuevo username es igual al actual, no-op; si
  `usuarioRepository.findByUsername(nuevo)` devuelve otro usuario, lanza
  `UsernameAlreadyExistsException`; si no, actualiza y devuelve `PerfilResponse`.
  Verificar con `mvnw.cmd compile`.
- [x] 3.5 Implementar `void cambiarPassword(CambiarPasswordRequest)`
  (`@Transactional`): setea `passwordEncoder.encode(nueva)` en el usuario y
  guarda. Verificar con `mvnw.cmd compile`.

## 4. Controller

- [x] 4.1 Crear `controller/PerfilController.java` (`@RestController`,
  `@RequestMapping("/api/perfil")`, constructor injection de `PerfilService`) con:
  `GET /me` → `obtenerMiPerfil()` (200); `PUT /me` → `actualizarMiPerfil` con
  `@Valid @RequestBody` (200); `PUT /me/username` → `cambiarUsername` con `@Valid`
  (200); `PUT /me/password` → `cambiarPassword` con `@Valid` (200, cuerpo vacío).
  Verificar con `mvnw.cmd compile`.
- [x] 4.2 Confirmar que `SecurityConfig` deja `/api/perfil/**` bajo
  `anyRequest().authenticated()` (sin cambios de código); verificar arrancando la
  app y comprobando 401 sin token en `GET /api/perfil/me`.

## 5. Pruebas

- [x] 5.1 Crear `PerfilControllerTest` (test de integración con MockMvc + usuario
  autenticado de prueba) que cubra: GET /me devuelve 200 con username, nombre,
  apellido, ci, createdAt y **sin** campo de contraseña.
- [x] 5.2 Añadir casos: PUT /me actualiza nombre y apellido (200) e ignora `ci`
  enviado en el cuerpo; PUT /me con nombre o apellido vacío devuelve 400 con
  formato de error estándar.
- [x] 5.3 Añadir casos: PUT /me/username con username libre devuelve 200; con
  username ya usado por otro usuario devuelve 409; con username < 3 caracteres
  devuelve 400.
- [x] 5.4 Añadir casos: PUT /me/password con >= 8 caracteres devuelve 200 y la
  nueva contraseña permite login por BCrypt; con < 8 caracteres devuelve 400.
- [x] 5.5 Añadir caso: cualquier endpoint `/api/perfil/me` sin token válido
  devuelve 401.
- [x] 5.6 Ejecutar `mvnw.cmd test` y verificar que toda la suite pasa y la app
  compila sin errores.
