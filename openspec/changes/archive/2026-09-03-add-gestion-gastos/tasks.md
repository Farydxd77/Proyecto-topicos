## 1. Repositorios

- [x] 1.1 Crear `repository/GastoRepository.java`:
  `interface GastoRepository extends JpaRepository<Gasto, Long>` con
  `List<Gasto> findByGrupoIdOrderByFechaDesc(Long grupoId)` y
  `Optional<Gasto> findByIdAndGrupoId(Long id, Long grupoId)`. Verificar con
  `mvnw.cmd compile` y con el arranque del contexto Spring
  (`mvnw.cmd test -Dtest=BackendApplicationTests`), que falla si un Query Method
  no es derivable.
- [x] 1.2 Crear `repository/GastoParticipanteRepository.java`:
  `interface GastoParticipanteRepository extends JpaRepository<GastoParticipante, GastoParticipanteId>`
  con `List<GastoParticipante> findByGastoId(Long gastoId)` y
  `void deleteByGastoId(Long gastoId)`. Verificar igual que 1.1.

## 2. DTOs

- [x] 2.1 Crear `dto/request/RegistrarGastoRequest.java` como `record` con
  `@NotBlank @Size(max = 255) String descripcion`,
  `@NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal monto`,
  `@NotNull Long pagadorId` y `@NotNull LocalDate fecha`. Verificar con
  `mvnw.cmd compile`.
- [x] 2.2 Crear `dto/request/ActualizarGastoRequest.java` como `record` con los
  mismos campos y las mismas validaciones que 2.1. Verificar con `mvnw.cmd compile`.
- [x] 2.3 Crear `dto/response/GastoParticipanteDto.java` como
  `record GastoParticipanteDto(ParticipanteDto participante, BigDecimal montoAdeudado)`.
  Verificar con `mvnw.cmd compile`.
- [x] 2.4 Crear `dto/response/GastoResumenDto.java` como
  `record GastoResumenDto(Long id, String descripcion, BigDecimal monto, ParticipanteDto pagador, LocalDate fecha)`.
  Verificar con `mvnw.cmd compile`.
- [x] 2.5 Crear `dto/response/GastoResponse.java` como
  `record GastoResponse(Long id, Long grupoId, String descripcion, BigDecimal monto, ParticipanteDto pagador, LocalDate fecha, List<GastoParticipanteDto> division)`.
  Verificar con `mvnw.cmd compile`.

## 3. Servicio: base, guardas y mapeo

- [x] 3.1 Crear `service/GastoService.java` (`@Service`, constructor injection de
  `GastoRepository`, `GastoParticipanteRepository`, `GrupoRepository`,
  `GrupoParticipanteRepository`, `ParticipanteRepository` y `UsuarioRepository`)
  con los privados de mapeo: `ParticipanteDto toParticipanteDto(Participante p)`
  (lee `p.getUsuario().getUsername()`), `GastoResumenDto toResumen(Gasto g)` y
  `GastoResponse toResponse(Gasto g, List<GastoParticipante> division)`. Verificar
  con `mvnw.cmd compile`.
- [x] 3.2 Añadir el privado `Participante participanteActual()` copiando el patrón
  de `GrupoService.participanteActual()` (username desde `SecurityContextHolder`,
  `usuarioRepository.findByUsername`, `participanteRepository.findByUsuarioId`,
  lanzando `ResourceNotFoundException` si falta alguno). Verificar con
  `mvnw.cmd compile`.
- [x] 3.3 Añadir el privado `Grupo grupoDondeEsMiembro(Long grupoId, Participante solicitante)`:
  `grupoRepository.findById(grupoId)` vacío →
  `ResourceNotFoundException("Grupo no encontrado: " + grupoId)`; luego
  `grupoParticipanteRepository.findByGrupoIdAndParticipanteId(grupoId, solicitante.getId())`
  vacío → `ForbiddenOperationException("No eres miembro de este grupo")`. El `404`
  se evalúa siempre antes que el `403`. Verificar con `mvnw.cmd compile`.
- [x] 3.4 Añadir el privado
  `List<GastoParticipante> calcularDivision(Gasto gasto, BigDecimal monto, List<Participante> miembros, Participante pagador)`:
  `n = miembros.size()`; `porPersona = monto.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP)`;
  cada miembro distinto del pagador adeuda `porPersona`; el pagador adeuda
  `monto.subtract(porPersona.multiply(BigDecimal.valueOf(n - 1)))`. Devuelve una
  lista de `GastoParticipante` nuevos, cada uno con su
  `GastoParticipanteId(gasto.getId(), participante.getId())`, `setGasto`,
  `setParticipante` y `setMontoAdeudado`. Verificar con `mvnw.cmd compile`; la
  aritmética la cubre la tarea 8.

## 4. Servicio: registrar

- [x] 4.1 Añadir `GastoResponse registrar(Long grupoId, RegistrarGastoRequest req)`
  (`@Transactional`): `grupoDondeEsMiembro(grupoId, participanteActual())`;
  resolver el pagador con
  `grupoParticipanteRepository.findByGrupoIdAndParticipanteId(grupoId, req.pagadorId())`
  vacío → `BadRequestException("El pagador no es miembro del grupo")`, si no
  `.getParticipante()`; construir y guardar el `Gasto` (`grupo`, `descripcion`,
  `monto`, `pagador`, `fecha`) con `gastoRepository.save`; obtener los miembros con
  `grupoParticipanteRepository.findByGrupoId(grupoId)`; `calcularDivision(...)` y
  `gastoParticipanteRepository.saveAll(division)`; devolver
  `toResponse(gasto, division)`. Verificar con `mvnw.cmd compile`; comportamiento
  cubierto por las tareas 7.1 y 8.

## 5. Servicio: listar, detalle, editar, eliminar

- [x] 5.1 Añadir `List<GastoResumenDto> listar(Long grupoId)`
  (`@Transactional(readOnly = true)`): `grupoDondeEsMiembro(...)`;
  `gastoRepository.findByGrupoIdOrderByFechaDesc(grupoId)` → mapear con
  `toResumen`; lista vacía si no hay gastos. Verificar con `mvnw.cmd compile`;
  comportamiento cubierto por 7.2.
- [x] 5.2 Añadir `GastoResponse obtenerDetalle(Long grupoId, Long gastoId)`
  (`@Transactional(readOnly = true)`): `grupoDondeEsMiembro(...)`;
  `gastoRepository.findByIdAndGrupoId(gastoId, grupoId)` vacío →
  `ResourceNotFoundException("Gasto no encontrado: " + gastoId)`;
  `gastoParticipanteRepository.findByGastoId(gastoId)`; devolver
  `toResponse(gasto, division)`. Verificar con `mvnw.cmd compile`; comportamiento
  cubierto por 7.3.
- [x] 5.3 Añadir `GastoResponse actualizar(Long grupoId, Long gastoId, ActualizarGastoRequest req)`
  (`@Transactional`): `grupoDondeEsMiembro(...)`; cargar el gasto con
  `findByIdAndGrupoId` (→ `404`); resolver el pagador como en 4.1 (→ `400` si no es
  miembro); asignar `descripcion`, `monto`, `pagador` y `fecha` y `gastoRepository.save`;
  `gastoParticipanteRepository.deleteByGastoId(gastoId)` seguido de
  `gastoParticipanteRepository.flush()`; recalcular con los miembros actuales
  (`findByGrupoId`) y `saveAll`; devolver `toResponse`. Verificar con
  `mvnw.cmd compile`; comportamiento cubierto por 7.4 y 8.
- [x] 5.4 Añadir `void eliminar(Long grupoId, Long gastoId)` (`@Transactional`):
  `grupoDondeEsMiembro(...)`; cargar el gasto con `findByIdAndGrupoId` (→ `404`);
  `gastoParticipanteRepository.deleteByGastoId(gastoId)` y luego
  `gastoRepository.delete(gasto)`. Verificar con `mvnw.cmd compile`;
  comportamiento cubierto por 7.5.

## 6. Controller

- [x] 6.1 Crear `controller/GastoController.java` (`@RestController`,
  `@RequestMapping("/api/grupos/{grupoId}/gastos")`, constructor injection de
  `GastoService`) con: `@PostMapping` + `@ResponseStatus(CREATED)` →
  `registrar(grupoId, @Valid @RequestBody RegistrarGastoRequest)`;
  `@GetMapping` → `listar(grupoId)`;
  `@GetMapping("/{gastoId}")` → `obtenerDetalle(grupoId, gastoId)`;
  `@PutMapping("/{gastoId}")` → `actualizar(grupoId, gastoId, @Valid @RequestBody ActualizarGastoRequest)`;
  `@DeleteMapping("/{gastoId}")` + `@ResponseStatus(NO_CONTENT)` →
  `eliminar(grupoId, gastoId)`. Todos los path params `@PathVariable Long`.
  Verificar con `mvnw.cmd compile`.
- [x] 6.2 Confirmar que `/api/grupos/{grupoId}/gastos/**` queda cubierto por
  `anyRequest().authenticated()` sin tocar `SecurityConfig`: arrancar la app y
  comprobar que `GET /api/grupos/1/gastos` sin token devuelve `401` con el formato
  de error estándar.

## 7. Pruebas de endpoints

- [x] 7.1 Crear `src/test/java/com/cuentasclaras/backend/gastos/GastoControllerTest.java`
  siguiendo el patrón de `grupos/GrupoControllerTest` (`@SpringBootTest`,
  `@Transactional`, `@TestPropertySource` con `jwt.secret`, `MockMvcBuilders` +
  `springSecurity()`, usuarios vía `POST /api/auth/register` con sufijo
  `System.nanoTime()`, grupo vía `POST /api/grupos`, miembros vía
  `POST /api/grupos/{id}/miembros`), con los casos de registro:
  datos válidos → `201` con `id`, `grupoId`, `descripcion`, `monto`, `pagador`,
  `fecha` y `division`, y sin ningún campo de contraseña; `monto` = 0 o negativo →
  `400`; `descripcion` en blanco o `fecha`/`pagadorId` ausentes → `400`;
  `pagadorId` que no es miembro → `400`; no miembro → `403`; grupo inexistente →
  `404`. Verificar con `mvnw.cmd test`.
- [x] 7.2 Añadir a `GastoControllerTest` los casos de listado: grupo con varios
  gastos → `200` ordenados por `fecha` descendente con campos `id`, `descripcion`,
  `monto`, `pagador`, `fecha`; grupo sin gastos → `200` con `[]`; no miembro →
  `403`; grupo inexistente → `404`. Verificar con `mvnw.cmd test`.
- [x] 7.3 Añadir a `GastoControllerTest` los casos de detalle: miembro sobre un
  gasto del grupo → `200` con el gasto y su `division` (suma de `montoAdeudado`
  == `monto`); `gastoId` inexistente → `404`; `gastoId` de otro grupo → `404`;
  no miembro → `403`. Verificar con `mvnw.cmd test`.
- [x] 7.4 Añadir a `GastoControllerTest` los casos de edición: miembro con datos
  válidos y `monto` nuevo → `200` con los valores actualizados y la `division`
  recalculada (suma == nuevo `monto`); `monto` <= 0 → `400` y gasto sin cambios;
  `pagadorId` no miembro → `400`; `gastoId` inexistente o de otro grupo → `404`;
  no miembro → `403`. Verificar con `mvnw.cmd test`.
- [x] 7.5 Añadir a `GastoControllerTest` los casos de eliminación: miembro (aunque
  no sea el pagador) → `204` sin cuerpo y `GET .../{gastoId}` posterior → `404`;
  `gastoId` inexistente o de otro grupo → `404`; no miembro → `403`. Verificar
  con `mvnw.cmd test`.
- [x] 7.6 Añadir a `GastoControllerTest` el caso de autenticación: `POST`, `GET`,
  `GET/{gastoId}`, `PUT` y `DELETE` bajo `/api/grupos/{id}/gastos` sin token
  válido devuelven `401` con el formato de error estándar. Verificar con
  `mvnw.cmd test`.

## 8. Pruebas de la división

- [x] 8.1 Crear `src/test/java/com/cuentasclaras/backend/gastos/GastoDivisionTest.java`
  con el mismo montaje que 7.1 y los casos de aritmética: gasto de `100.00` en un
  grupo de 3 miembros → los dos no pagadores adeudan `33.33` y el pagador `33.34`,
  suma `100.00`; gasto de `90.00` en grupo de 3 → cada uno `30.00`, suma `90.00`;
  grupo de 1 miembro, gasto de `50.00` → una entrada de `50.00`. Verificar con
  `mvnw.cmd test`.
- [x] 8.2 Añadir a `GastoDivisionTest` el caso de aislamiento frente a cambios de miembros:
  registrar un gasto en un grupo de 2 miembros, luego agregar un tercer
  miembro con `POST /api/grupos/{id}/miembros`; `GET .../{gastoId}` del gasto
  anterior sigue con 2 entradas y los mismos `montoAdeudado`; un gasto nuevo se
  divide entre los 3. Verificar con `mvnw.cmd test`.
- [x] 8.3 Añadir a `GastoDivisionTest` el caso de recálculo en la edición: sobre
  un gasto de un grupo de 3, hacer `PUT` cambiando el `monto` a un valor no
  divisible de forma exacta (p. ej. `10.00`); la nueva `division` tiene 3 entradas
  y su suma es `10.00`. Verificar con `mvnw.cmd test`.

## 9. Verificación final

- [x] 9.1 Ejecutar `mvnw.cmd test` y comprobar que toda la suite pasa (incluidos
  los tests preexistentes de auth, perfil, seguridad, gestión general y grupos) y
  que la app compila sin errores. Resultado: **105 tests, 0 fallos, BUILD SUCCESS**
  (incluyó corregir un fallo preexistente de `grupos`, ver 9.3).
- [x] 9.2 Confirmar los criterios de aceptación del spec delta y que Hibernate no
  genera `alter table` al arrancar. Los cinco endpoints quedan ejercidos por los 31
  tests de integración (`GastoControllerTest` + `GastoDivisionTest`) vía HTTP real
  con tokens JWT reales contra PostgreSQL; el arranque del contexto Spring con
  `ddl-auto=update` no emite ningún `alter table` / `create table` / `drop table`.
- [x] 9.3 Corregir el fallo **preexistente**
  `GrupoMiembrosControllerTest.quitarMiembro_creadorQuitaAOtro_devuelve204YDejaDeVerElGrupo`
  (ajeno a `gastos`, destapado por la task 9.1). `GrupoService.quitarMiembro`
  quitaba el miembro solo de la colección en memoria y hacía
  `grupoRepository.save(grupo)` (`merge`), que con el bag
  `@OneToMany(orphanRemoval = true)` no programa el `DELETE` de
  `grupo_participantes`. Se sustituye por un borrado explícito con
  `grupoParticipanteRepository.delete(membresia)` y el `404` pasa a resolverse con
  `findByGrupoIdAndParticipanteId`. Verificado con `mvnw.cmd test`.
