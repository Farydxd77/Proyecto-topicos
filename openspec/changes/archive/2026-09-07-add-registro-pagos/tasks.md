## 1. Entidad y repositorio

- [x] 1.1 Crear `entity/Pago.java` (extiende `BaseEntity`; `@Getter @Setter
  @SuperBuilder @NoArgsConstructor @AllArgsConstructor`; `id` IDENTITY; `@ManyToOne(LAZY)`
  a `grupo`, `pagador`, `receptor` todos `nullable = false`; `monto` `precision = 10,
  scale = 2` con `@Check(constraints = "monto > 0")`; `fecha` `LocalDate` `nullable
  = false`; `txId` `@Column(name = "tx_id", length = 100)` nullable). Verificar con
  `cd backend && mvnw.cmd -q test-compile`.
- [x] 1.2 Crear `repository/PagoRepository.java` (`extends JpaRepository<Pago,
  Long>`, con `List<Pago> findByGrupoIdOrderByFechaDesc(Long grupoId)` y
  `Optional<Pago> findByIdAndGrupoId(Long id, Long grupoId)`, sin `@Query`).
  Verificar con `mvnw.cmd -q test-compile`.

## 2. DTOs

- [x] 2.1 Crear `dto/request/RegistrarPagoRequest.java` (record con `Long
  receptorId` `@NotNull`; `BigDecimal monto` `@NotNull @Positive @Digits(integer =
  10, fraction = 2)`; `LocalDate fecha` `@NotNull`; `String txId` `@Size(max =
  100)`). Sin `pagadorId`. Verificar con `mvnw.cmd -q test-compile`.
- [x] 2.2 Crear `dto/request/ActualizarPagoRequest.java` (mismos cuatro campos y
  validaciones que `RegistrarPagoRequest`). Verificar con `mvnw.cmd -q
  test-compile`.
- [x] 2.3 Crear `dto/response/PagoResponse.java` (record `Long id`, `Long grupoId`,
  `ParticipanteDto pagador`, `ParticipanteDto receptor`, `BigDecimal monto`,
  `LocalDate fecha`, `String txId`). Verificar con `mvnw.cmd -q test-compile`.

## 3. Servicio

- [x] 3.1 Crear `service/PagoService.java` con los helpers privados calcados de
  `GastoService`: `participanteActual()`, `grupoDondeEsMiembro(grupoId,
  solicitante)` (404 de grupo antes que 403 de membresía), `pagoDelGrupo(grupoId,
  pagoId)` (`findByIdAndGrupoId` → `ResourceNotFoundException`), `receptorMiembro(grupoId,
  receptorId)` (`BadRequestException` si no es miembro), `validarDistintos(pagadorId,
  receptorId)` (`BadRequestException` si coinciden), `exigirPagador(pago,
  solicitante)` (`ForbiddenOperationException` si `pago.getPagador().getId()` ≠
  solicitante) y `toResponse(Pago)`. Verificar con `mvnw.cmd -q test-compile`.
- [x] 3.2 Implementar `registrar(Long grupoId, RegistrarPagoRequest req)`
  `@Transactional`: resuelve pagador = `participanteActual()`, valida grupo/membresía,
  `receptorMiembro`, `validarDistintos`, persiste el `Pago` con `monto.setScale(2,
  HALF_UP)` y devuelve `PagoResponse` (201 lo pone el controlador). Verificar con
  `mvnw.cmd -q test-compile`.
- [x] 3.3 Implementar `listar(Long grupoId)` y `obtenerDetalle(Long grupoId, Long
  pagoId)` `@Transactional(readOnly = true)`: validan grupo/membresía; `listar`
  mapea `findByGrupoIdOrderByFechaDesc`; `obtenerDetalle` usa `pagoDelGrupo`.
  Verificar con `mvnw.cmd -q test-compile`.
- [x] 3.4 Implementar `actualizar(Long grupoId, Long pagoId, ActualizarPagoRequest
  req)` `@Transactional`: valida grupo/membresía → `pagoDelGrupo` → `exigirPagador`
  → `receptorMiembro` + `validarDistintos` → actualiza `receptor`, `monto` (con
  `setScale(2)`), `fecha`, `txId` (el grupo y el pagador no cambian) → devuelve
  `PagoResponse`. Verificar con `mvnw.cmd -q test-compile`.
- [x] 3.5 Implementar `eliminar(Long grupoId, Long pagoId)` `@Transactional`: valida
  grupo/membresía → `pagoDelGrupo` → `exigirPagador` → `pagoRepository.delete(pago)`.
  Verificar con `mvnw.cmd -q test-compile`.

## 4. Controlador

- [x] 4.1 Crear `controller/PagoController.java` (`@RestController
  @RequestMapping("/api/grupos/{grupoId}/pagos")`) con `POST`
  (`@ResponseStatus(CREATED)`, `@Valid @RequestBody RegistrarPagoRequest`), `GET`
  lista, `GET /{pagoId}`, `PUT /{pagoId}` (`@Valid @RequestBody
  ActualizarPagoRequest`) y `DELETE /{pagoId}` (`@ResponseStatus(NO_CONTENT)`),
  todos delegando en `PagoService`. Verificar con `mvnw.cmd -q test-compile` y, tras
  arrancar `mvnw.cmd spring-boot:run`, que la tabla `pagos` se crea y `POST
  /api/grupos/{grupoId}/pagos` devuelve 201.

## 5. Impacto en balances

- [x] 5.1 En `util/BalanceUtil.java` añadir la sobrecarga
  `calcularBalances(ids, pagado, adeudado, pagosRealizadosPorId,
  pagosRecibidosPorId)` con `balance = pagado - adeudado + pagosRealizados -
  pagosRecibidos` (escala 2, `HALF_UP`) y hacer que la sobrecarga de tres
  argumentos delegue con `Map.of(), Map.of()`. Verificar con `mvnw.cmd -q
  test-compile` y que `BalanceUtilTest` sigue verde (`mvnw.cmd test
  -Dtest=BalanceUtilTest`).
- [x] 5.2 En `service/BalanceService.java` inyectar `PagoRepository`, extender
  `cargarContexto(grupoId)` para recorrer `findByGrupoIdOrderByFechaDesc` y construir
  `pagosRealizados` (por `pagador.id`) y `pagosRecibidos` (por `receptor.id`)
  sumando esos participantes a `participantes`, y pasar ambos mapas a la sobrecarga
  de cinco argumentos en `calcularBalances` y `calcularLiquidacion`. Verificar con
  `mvnw.cmd -q test-compile` y que `BalanceControllerTest` sigue verde.

## 6. Pruebas

- [x] 6.1 Añadir casos a `balances/BalanceUtilTest.java` para la sobrecarga de cinco
  argumentos: un pago del deudor al acreedor acerca ambos balances a 0; escenario
  Samaipata con un pago de 200 de Beto a Ana da `Ana +400 / Beto 0 / Carla -200 /
  Diego -200`; la suma sigue `0.00` con gastos y pagos combinados. Verificar con
  `mvnw.cmd test -Dtest=BalanceUtilTest`.
- [x] 6.2 Crear `pagos/PagoControllerTest.java` (`@SpringBootTest @Transactional`,
  patrón de `GastoControllerTest`): registro válido → 201; `monto <= 0` → 400;
  pagador == receptor → 400; receptor no miembro → 400; listado ordenado por fecha
  desc y `[]` si vacío; detalle 200 y 404 (inexistente y de otro grupo); `PUT` por
  el pagador → 200 (monto y receptor); `PUT`/`DELETE` por miembro no pagador → 403;
  usuario no miembro → 403; grupo inexistente → 404; sin token → 401. Verificar con
  `mvnw.cmd test -Dtest=PagoControllerTest`.
- [x] 6.3 Añadir a `balances/BalanceControllerTest.java` un caso end-to-end: crear
  un gasto, consultar `/balances`, registrar un pago del deudor al acreedor y
  comprobar que `/balances` y `/liquidacion` reflejan el pago y la suma sigue
  `0.00`. Verificar con `mvnw.cmd test -Dtest=BalanceControllerTest`.

## 7. Verificación final

- [x] 7.1 Ejecutar `cd backend && mvnw.cmd test` y confirmar que toda la suite pasa
  (incluidas las de gastos, balances y las nuevas de pagos).
- [x] 7.2 Confirmar que `openspec/specs/data-model.md` ya documenta la tabla `pagos`
  (sin cambios pendientes) y dejar anotado en el PR que el borrador
  `openspec/specs/pagos/spec.md` se reemplaza por el spec generado al archivar el
  cambio, con la fórmula de balances corregida (`+ pagosRealizados -
  pagosRecibidos`).
