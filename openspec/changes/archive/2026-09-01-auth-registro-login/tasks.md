## 1. DTOs de entrada y salida

- [x] 1.1 Crear `dto/request/RegisterRequest.java` como `record` con
      `username` (`@NotBlank @Size(min = 3, max = 50)`),
      `password` (`@NotBlank @Size(min = 8)`),
      `nombre` (`@NotBlank @Size(max = 100)`),
      `apellido` (`@NotBlank @Size(max = 100)`),
      `ci` (`@NotBlank @Size(max = 20)`). Verificar con `./mvnw clean compile`.
- [x] 1.2 Crear `dto/request/LoginRequest.java` como `record` con
      `username` (`@NotBlank`) y `password` (`@NotBlank`). Verificar con
      `./mvnw clean compile`.
- [x] 1.3 Crear `dto/response/RegisterResponse.java` como `record`
      `RegisterResponse(String token, ParticipanteDto participante)` con un
      `record ParticipanteDto(Long id, String nombre, String apellido, String ci,
      String username)` (mismo archivo o `dto/response/ParticipanteDto.java`).
      Sin ningún campo de contraseña. Verificar con `./mvnw clean compile`.
- [x] 1.4 Crear `dto/response/LoginResponse.java` como `record`
      `LoginResponse(String token, UsuarioDto usuario)` con
      `record UsuarioDto(Long id, String username)`. Sin contraseña. Verificar
      con `./mvnw clean compile`.

## 2. Repositorio

- [x] 2.1 Crear `repository/ParticipanteRepository.java`
      (`extends JpaRepository<Participante, Long>`). Verificar con
      `./mvnw clean compile`.

## 3. Manejo de errores

- [x] 3.1 Crear `exception/UsernameAlreadyExistsException.java`
      (`extends RuntimeException`, constructor con mensaje). Verificar con
      `./mvnw clean compile`.
- [x] 3.2 Crear `exception/GlobalExceptionHandler.java` (`@RestControllerAdvice`)
      con `@ExceptionHandler` para:
      `MethodArgumentNotValidException` → 400 + cuerpo estándar
      (`timestamp`, `status`, `error`, `message`, `path`) más `errors`
      (`Map<campo, mensaje>` desde `getBindingResult().getFieldErrors()`);
      `UsernameAlreadyExistsException` → 409 + cuerpo estándar;
      `org.springframework.security.authentication.BadCredentialsException` → 401
      + cuerpo estándar con `message` genérico fijo `"Credenciales inválidas"`.
      El `path` sale de `HttpServletRequest.getRequestURI()`. Verificar con
      `./mvnw clean compile`.

## 4. Configuración

- [x] 4.1 Añadir a `config/SecurityConfig.java` un
      `@Bean PasswordEncoder passwordEncoder()` que devuelva
      `new BCryptPasswordEncoder()`. No tocar la cadena de filtros existente.
      Verificar con `./mvnw clean compile`.

## 5. Servicio

- [x] 5.1 Crear `service/AuthService.java` (`@Service`) con `PasswordEncoder`,
      `JwtUtil`, `UsuarioRepository` y `ParticipanteRepository` inyectados.
      Método `@Transactional RegisterResponse register(RegisterRequest req)`:
      si `usuarioRepository.findByUsername(req.username())` existe →
      `throw new UsernameAlreadyExistsException(...)`; si no, crear `Usuario`
      (`password = passwordEncoder.encode(req.password())`) y guardarlo, crear
      `Participante` vinculado y guardarlo, generar
      `jwtUtil.generateToken(usuario.getUsername())`, devolver `RegisterResponse`
      con el `ParticipanteDto`. Verificar con `./mvnw clean compile`.
- [x] 5.2 Añadir a `AuthService` `LoginResponse login(LoginRequest req)`:
      `usuarioRepository.findByUsername(req.username())`
      `.filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))`
      `.map(u -> new LoginResponse(jwtUtil.generateToken(u.getUsername()), new UsuarioDto(u.getId(), u.getUsername())))`
      `.orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"))`.
      Verificar con `./mvnw clean compile`.

## 6. Controller

- [x] 6.1 Crear `controller/AuthController.java` (`@RestController`
      `@RequestMapping("/api/auth")`):
      `@PostMapping("/register")` → `@ResponseStatus(HttpStatus.CREATED)`,
      recibe `@Valid @RequestBody RegisterRequest`, delega en
      `authService.register`, devuelve `RegisterResponse`;
      `@PostMapping("/login")` recibe `@Valid @RequestBody LoginRequest`, delega
      en `authService.login`, devuelve `LoginResponse` (200). Verificar con
      `./mvnw clean compile`.

## 7. Pruebas

- [x] 7.1 Crear
      `src/test/java/com/cuentasclaras/backend/auth/AuthIntegrationTest.java`
      (`@SpringBootTest` + `@Transactional`; `MockMvc` vía
      `MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())`;
      `@TestPropertySource` con `jwt.secret` ≥ 32 bytes y
      `spring.jpa.hibernate.ddl-auto=update`). Casos:
      (a) `POST /api/auth/register` válido → 201, cuerpo con `token` y
      `participante` sin campo de contraseña; en BD el `Usuario` tiene el
      `password` hasheado (`passwordEncoder.matches(raw, stored)` es true y
      `stored != raw`) y existe el `Participante` vinculado;
      (b) el `token` del registro pasa una ruta protegida sin 401;
      (c) registrar el mismo `username` dos veces → segunda vez 409 con cuerpo
      estándar;
      (d) register con `password` de 4 caracteres → 400 con `errors.password`;
      (e) register sin `nombre`/`apellido`/`ci` → 400 listando esos campos;
      (f) `POST /api/auth/login` con las credenciales del registro → 200 con
      `token` y `usuario` sin contraseña;
      (g) login con password incorrecta → 401;
      (h) login con `username` inexistente → 401 con el mismo `message` que (g).
      Verificar con `./mvnw test -Dtest=AuthIntegrationTest`.

## 8. Verificación integral

- [x] 8.1 `./mvnw clean test` — compila y todos los tests pasan.
- [x] 8.2 `git diff --stat`: solo se añadieron los archivos previstos y se
      modificó `config/SecurityConfig.java`; entidades, `application.properties`
      y `pom.xml` intactos.
- [x] 8.3 Arrancar contra PostgreSQL (`./mvnw spring-boot:run`) y comprobar con
      `curl`: register nuevo → 201 con token; mismo username → 409; login ok →
      200 con token; login con password mala → 401 genérico; register con
      password corto → 400. En ningún cuerpo aparece la contraseña.
- [x] 8.4 Repasar los criterios de aceptación de `openspec/specs/auth/spec.md` y
      marcarlos todos como cumplidos.
