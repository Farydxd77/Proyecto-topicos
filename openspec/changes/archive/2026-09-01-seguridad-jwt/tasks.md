## 1. Acceso a datos de usuario

- [x] 1.1 Crear `repository/UsuarioRepository.java`:
      `public interface UsuarioRepository extends JpaRepository<Usuario, Long>`
      con `Optional<Usuario> findByUsername(String username)`. Verificar con
      `./mvnw clean compile`.

## 2. Utilidad JWT

- [x] 2.1 Crear `security/JwtUtil.java` (`@Component`): inyectar
      `@Value("${jwt.secret}")` y `@Value("${jwt.expiration}")`; derivar la clave
      con `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))`. Métodos:
      `String generateToken(String username)` (`subject = username`, `issuedAt =
      now`, `expiration = now + jwt.expiration`, `signWith(key)`);
      `boolean validateToken(String token)` (parsear con `verifyWith(key)`;
      devolver `false` capturando `JwtException` —incluye expirado, malformado y
      firma inválida—, sin propagar); `String getUsername(String token)`
      (devuelve el `subject`). Verificar con `./mvnw clean compile`.

## 3. Resolución de usuario para Spring Security

- [x] 3.1 Crear `security/UserDetailsServiceImpl.java` (`@Service`) implementando
      `UserDetailsService`: `loadUserByUsername` usa
      `UsuarioRepository.findByUsername`; si está vacío lanza
      `UsernameNotFoundException`; si existe devuelve
      `org.springframework.security.core.userdetails.User` con `username`, el
      hash de `password` y `Collections.emptyList()` de authorities. Verificar
      con `./mvnw clean compile`.

## 4. Respuesta 401 en formato estándar

- [x] 4.1 Crear `security/JwtAuthenticationEntryPoint.java` (`@Component`)
      implementando `AuthenticationEntryPoint`: en `commence(...)` fijar
      `status = 401`, `contentType = application/json`, y escribir con un
      `ObjectMapper` el cuerpo `{ "timestamp": Instant.now().toString(),
      "status": 401, "error": "Unauthorized", "message": <mensaje o "No
      autenticado">, "path": request.getRequestURI() }`. Verificar con
      `./mvnw clean compile`.

## 5. Filtro de autenticación por request

- [x] 5.1 Crear `security/JwtAuthFilter.java` (`@Component`) extendiendo
      `OncePerRequestFilter`: leer header `Authorization`; si es `null` o no
      empieza por `"Bearer "`, continuar la cadena sin autenticar; si hay token,
      validarlo con `JwtUtil`; si es válido, obtener `username`, cargar
      `UserDetails` con `UserDetailsServiceImpl`, construir
      `UsernamePasswordAuthenticationToken(userDetails, null,
      userDetails.getAuthorities())` con
      `WebAuthenticationDetailsSource` y ponerlo en el `SecurityContext`;
      cualquier excepción (token inválido, `UsernameNotFoundException`) se captura
      y se continúa la cadena SIN autenticación. Nunca escribe la respuesta.
      Verificar con `./mvnw clean compile`.

## 6. Configuración de seguridad

- [x] 6.1 Reescribir `config/SecurityConfig.java` (`@Configuration`
      `@EnableWebSecurity`): `SecurityFilterChain` con `csrf.disable()`,
      `sessionManagement` = `STATELESS`, `authorizeHttpRequests` =
      `requestMatchers("/api/auth/**", "/actuator/health/**").permitAll()` +
      `anyRequest().authenticated()`,
      `exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)`,
      `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`.
      Quitar `httpBasic`. Verificar con `./mvnw clean compile`.

## 7. Pruebas

- [x] 7.1 Crear `src/test/java/com/cuentasclaras/backend/security/JwtUtilTest.java`
      (test unitario, sin contexto Spring; instanciar `JwtUtil` e inyectar
      secreto largo y expiración por reflexión o constructor de test): verifica
      que `generateToken` produce un token cuyo `subject` es el username y cuya
      expiración cae ~24 h después; que `validateToken` da `true` para ese token;
      `false` para un token manipulado; `false` para un token emitido con
      expiración en el pasado. Verificar con `./mvnw test -Dtest=JwtUtilTest`.
- [x] 7.2 Crear
      `src/test/java/com/cuentasclaras/backend/security/SeguridadJwtIntegrationTest.java`
      (`@SpringBootTest` + `@Transactional`, `@TestPropertySource` con
      `jwt.secret` ≥ 32 bytes y `spring.jpa.hibernate.ddl-auto=update`; el
      `MockMvc` se arma con
      `MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())`
      porque el slice `@AutoConfigureMockMvc` no está en el classpath de test en
      Spring Boot 4. Nombre `...IntegrationTest` —no `...IT`— para que Surefire lo
      ejecute en `mvn test`): guarda un `Usuario` real; `GET /actuator/health`
      sin token → 200; `GET /api/grupos` sin token → 401 y el cuerpo JSON tiene
      `timestamp`, `status=401`, `error`, `message`, `path`; `GET /api/grupos`
      con `Authorization: Bearer <JwtUtil.generateToken(username)>` → status
      distinto de 401. Verificar con
      `./mvnw test -Dtest=SeguridadJwtIntegrationTest`.

## 8. Verificación integral

- [x] 8.1 Ejecutar `./mvnw clean test` y verificar que compila y todos los tests
      pasan.
- [x] 8.2 Confirmar con `git diff --stat` que solo se añadieron/modificaron los
      archivos previstos (`repository/UsuarioRepository`, `security/*`,
      `config/SecurityConfig`, tests) y que no se tocaron entidades ni
      `application.properties` de producción.
- [x] 8.3 Arrancar la app contra PostgreSQL (`./mvnw spring-boot:run`) y
      comprobar manualmente: `GET /actuator/health` → 200 sin token;
      `GET /api/grupos` sin token → 401 con el JSON de error estándar.
- [x] 8.4 Repasar los criterios de aceptación de
      `openspec/specs/seguridad-jwt/spec.md` y marcarlos todos como cumplidos.
