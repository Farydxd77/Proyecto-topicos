## 1. Repositorios

- [x] 1.1 En `repository/UsuarioRepository.java` agregar
  `List<Usuario> findByUsernameContainingIgnoreCase(String username)`. Verificar
  con `mvnw.cmd compile` que compila.
- [x] 1.2 En `repository/ParticipanteRepository.java` agregar
  `List<Participante> findByNombreContainingIgnoreCase(String nombre)`,
  `List<Participante> findByApellidoContainingIgnoreCase(String apellido)` y
  `Optional<Participante> findByCi(String ci)`. Verificar con `mvnw.cmd compile`.

## 2. Servicio de usuarios

- [x] 2.1 Crear `service/UsuarioService.java` (`@Service`, constructor injection de
  `UsuarioRepository`) con `List<UsuarioDto> listar(String username)`:
  si `username` es `null`/blanco usa `findAll()`, si no
  `findByUsernameContainingIgnoreCase`; mapea cada `Usuario` a
  `UsuarioDto(id, username)`. Método `@Transactional(readOnly = true)`. Verificar
  con `mvnw.cmd compile`.
- [x] 2.2 En `service/UsuarioService.java` añadir
  `UsuarioDto obtenerPorId(Long id)` (`@Transactional(readOnly = true)`): usa
  `findById`, lanza `ResourceNotFoundException("Usuario no encontrado: " + id)` si
  no existe, y mapea a `UsuarioDto`. Verificar con `mvnw.cmd compile`.

## 3. Servicio de participantes

- [x] 3.1 Crear `service/ParticipanteService.java` (`@Service`, constructor
  injection de `ParticipanteRepository` y `UsuarioRepository`) con un mapper
  privado `ParticipanteDto toDto(Participante p)` que lee
  `p.getUsuario().getUsername()`. Verificar con `mvnw.cmd compile`.
- [x] 3.2 Añadir `List<ParticipanteDto> listar(String ci, String nombre, String apellido)`
  (`@Transactional(readOnly = true)`): aplica un único criterio con precedencia
  `ci` (`findByCi`, resultado `Optional` → lista de 0/1) > `nombre`
  (`findByNombreContainingIgnoreCase`) > `apellido`
  (`findByApellidoContainingIgnoreCase`); si no viene ninguno usa `findAll()`.
  Mapea con `toDto`. Verificar con `mvnw.cmd compile`.
- [x] 3.3 Añadir `ParticipanteDto obtenerPorId(Long id)`
  (`@Transactional(readOnly = true)`): `findById`, lanza
  `ResourceNotFoundException("Participante no encontrado: " + id)` si no existe.
  Verificar con `mvnw.cmd compile`.
- [x] 3.4 Añadir `ParticipanteDto obtenerPorUsuarioId(Long usuarioId)`
  (`@Transactional(readOnly = true)`): si `usuarioRepository.findById(usuarioId)`
  está vacío lanza `ResourceNotFoundException("Usuario no encontrado: " + usuarioId)`;
  si `participanteRepository.findByUsuarioId(usuarioId)` está vacío lanza
  `ResourceNotFoundException("El usuario no tiene un participante vinculado")`;
  si no, mapea con `toDto`. Verificar con `mvnw.cmd compile`.

## 4. Controllers

- [x] 4.1 Crear `controller/UsuarioController.java` (`@RestController`,
  `@RequestMapping("/api/usuarios")`, constructor injection de `UsuarioService` y
  `ParticipanteService`) con:
  `GET ""` → `usuarioService.listar(@RequestParam(required=false) username)` (200);
  `GET "/{id}"` → `usuarioService.obtenerPorId(id)` (200);
  `GET "/{id}/participante"` → `participanteService.obtenerPorUsuarioId(id)` (200).
  Verificar con `mvnw.cmd compile`.
- [x] 4.2 Crear `controller/ParticipanteController.java` (`@RestController`,
  `@RequestMapping("/api/participantes")`, constructor injection de
  `ParticipanteService`) con:
  `GET ""` → `participanteService.listar(@RequestParam(required=false) ci, nombre, apellido)` (200);
  `GET "/{id}"` → `participanteService.obtenerPorId(id)` (200).
  Verificar con `mvnw.cmd compile`.
- [x] 4.3 Confirmar que `SecurityConfig` deja `/api/usuarios/**` y
  `/api/participantes/**` bajo `anyRequest().authenticated()` (sin cambios de
  código); verificar arrancando la app y comprobando `401` sin token en
  `GET /api/usuarios`.

## 5. Pruebas

- [x] 5.1 Crear `test/.../gestiongeneral/UsuarioControllerTest.java` (integración
  con MockMvc + usuario autenticado de prueba) que cubra:
  `GET /api/usuarios` devuelve `200` con array de objetos `{id, username}` y
  **sin** campo de contraseña; lista vacía → `200` con `[]`.
- [x] 5.2 Añadir casos a `UsuarioControllerTest`:
  `GET /api/usuarios?username=` con coincidencia parcial e ignorando mayúsculas
  devuelve los usuarios; sin coincidencias devuelve `200` con `[]`;
  `GET /api/usuarios/{id}` existente devuelve `200`; inexistente devuelve `404`
  con formato de error estándar.
- [x] 5.3 Añadir caso a `UsuarioControllerTest`:
  `GET /api/usuarios/{id}/participante` devuelve `200` con
  `{id, nombre, apellido, ci, username}` cuando el usuario tiene participante;
  `404` cuando el usuario no existe.
- [x] 5.4 Crear `test/.../gestiongeneral/ParticipanteControllerTest.java` que
  cubra: `GET /api/participantes` devuelve `200` con array de
  `{id, nombre, apellido, ci, username}` sin contraseña; lista vacía → `[]`;
  `GET /api/participantes/{id}` existente → `200`, inexistente → `404`.
- [x] 5.5 Añadir casos a `ParticipanteControllerTest`:
  búsqueda por `?nombre=` y por `?apellido=` (parcial, case-insensitive) devuelve
  los que coinciden; `?ci=` exacto devuelve un array con el participante;
  búsqueda sin coincidencias devuelve `200` con `[]`; enviando `ci` + `nombre`
  juntos se aplica solo `ci`.
- [x] 5.6 Añadir caso a ambos tests: cualquier endpoint de `/api/usuarios` o
  `/api/participantes` sin token válido devuelve `401` con formato de error
  estándar.
- [x] 5.7 Ejecutar `mvnw.cmd test` y verificar que toda la suite pasa y la app
  compila sin errores.
