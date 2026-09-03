## 1. Lógica pura: `util/BalanceUtil`

- [x] 1.1 Crear `util/package-info.java` (paquete
  `com.cuentasclaras.backend.util`, mismo estilo que los otros `package-info`).
  Verificar con `mvnw.cmd compile`.
- [x] 1.2 Crear `util/BalanceUtil.java`: clase `final` con constructor privado y
  un `record Movimiento(Long deId, Long paraId, BigDecimal monto)` público
  (anidado o de paquete). Método `static Map<Long, BigDecimal> calcularBalances(
  Set<Long> participantesIds, Map<Long, BigDecimal> pagadoPorId,
  Map<Long, BigDecimal> adeudadoPorId)`: para cada id de `participantesIds`,
  `balance = pagadoPorId.getOrDefault(id, 0) - adeudadoPorId.getOrDefault(id, 0)`,
  cada uno con `setScale(2)`; devuelve un `LinkedHashMap` id → balance. Sin
  imports de Spring ni de JPA. Verificar con `mvnw.cmd compile`; la aritmética la
  cubre la tarea 5.
- [x] 1.3 En `util/BalanceUtil.java` añadir
  `static List<Movimiento> minimizarTransferencias(Map<Long, BigDecimal> balances)`:
  separa acreedores (`> 0`) y deudores (`< 0`), ignora los `0`; ordena acreedores
  por balance descendente y deudores por balance ascendente, con desempate por id
  ascendente; en bucle, el mayor deudor transfiere al mayor acreedor
  `min(|balanceDeudor|, balanceAcreedor)`, se descuenta de ambos y se elimina a
  quien llegue a `0`; nunca emite un `Movimiento` con `monto` `0`. Devuelve la
  lista de `Movimiento` en el orden en que se generaron. Verificar con
  `mvnw.cmd compile`; el comportamiento lo cubre la tarea 5.

## 2. DTOs de salida

- [x] 2.1 Crear `dto/response/BalanceDto.java` como
  `record BalanceDto(ParticipanteDto participante, BigDecimal balance)`. Verificar
  con `mvnw.cmd compile`.
- [x] 2.2 Crear `dto/response/TransferenciaDto.java` como
  `record TransferenciaDto(String de, Long deId, String para, Long paraId, BigDecimal monto)`.
  Verificar con `mvnw.cmd compile`.

## 3. Servicio: `BalanceService`

- [x] 3.1 Crear `service/BalanceService.java` (`@Service`, constructor injection de
  `GastoRepository`, `GastoParticipanteRepository`, `GrupoRepository`,
  `GrupoParticipanteRepository`, `ParticipanteRepository` y `UsuarioRepository`)
  con los privados: `Participante participanteActual()` (patrón de
  `GastoService`), `Grupo grupoDondeEsMiembro(Long grupoId, Participante solicitante)`
  (`findById` → `ResourceNotFoundException`; `findByGrupoIdAndParticipanteId`
  vacío → `ForbiddenOperationException`; `404` antes que `403`), y
  `ParticipanteDto toParticipanteDto(Participante p)` (lee
  `p.getUsuario().getUsername()`). Verificar con `mvnw.cmd compile`.
- [x] 3.2 En `service/BalanceService.java` añadir un privado
  `Map<Long, Participante> cargarContexto(Long grupoId)` que:
  reúne los `Participante` de los miembros actuales
  (`grupoParticipanteRepository.findByGrupoId`) y de todos los gastos del grupo
  (pagador de cada `Gasto`, y `participante` de cada fila de
  `gastoParticipanteRepository.findByGastoId`), y devuelve un `Map` id →
  `Participante`. También expone (por campos o devolviendo un pequeño record
  interno) los mapas `pagadoPorId` y `adeudadoPorId` acumulados en `BigDecimal`
  desde `gasto.getMonto()` y `fila.getMontoAdeudado()`. Verificar con
  `mvnw.cmd compile`.
- [x] 3.3 Añadir `List<BalanceDto> calcularBalances(Long grupoId)`
  (`@Transactional(readOnly = true)`): `grupoDondeEsMiembro(grupoId, participanteActual())`;
  usa `cargarContexto`; llama a
  `BalanceUtil.calcularBalances(ids, pagadoPorId, adeudadoPorId)`; mapea cada
  entrada a `BalanceDto(toParticipanteDto(participante), balance)`. El orden de la
  lista es estable (por id ascendente). Verificar con `mvnw.cmd compile`;
  comportamiento cubierto por 6.1–6.3.
- [x] 3.4 Añadir `List<TransferenciaDto> calcularLiquidacion(Long grupoId)`
  (`@Transactional(readOnly = true)`): `grupoDondeEsMiembro(...)`; usa
  `cargarContexto` + `BalanceUtil.calcularBalances`; pasa el `Map` de balances a
  `BalanceUtil.minimizarTransferencias`; mapea cada `Movimiento` a
  `TransferenciaDto(nombreDe, deId, nombrePara, paraId, monto)` resolviendo los
  nombres desde el `Map` id → `Participante`. Devuelve `[]` si no hay movimientos.
  Verificar con `mvnw.cmd compile`; comportamiento cubierto por 6.4–6.5.

## 4. Controller

- [x] 4.1 Crear `controller/BalanceController.java` (`@RestController`,
  `@RequestMapping("/api/grupos/{grupoId}")`, constructor injection de
  `BalanceService`) con `@GetMapping("/balances")` →
  `calcularBalances(grupoId)` (200) y `@GetMapping("/liquidacion")` →
  `calcularLiquidacion(grupoId)` (200). `grupoId` como `@PathVariable Long`.
  Verificar con `mvnw.cmd compile`.
- [x] 4.2 Confirmar que `/api/grupos/{grupoId}/balances` y
  `/api/grupos/{grupoId}/liquidacion` quedan cubiertos por
  `anyRequest().authenticated()` sin tocar `SecurityConfig`: arrancar la app y
  comprobar que `GET /api/grupos/1/balances` sin token devuelve `401` con el
  formato de error estándar.

## 5. Pruebas unitarias de `BalanceUtil`

- [x] 5.1 Crear `src/test/java/com/cuentasclaras/backend/balances/BalanceUtilTest.java`
  (JUnit puro, sin Spring) con los casos de `calcularBalances`: participante que
  solo pagó → balance positivo; que solo adeuda → negativo; miembro sin actividad
  → `0.00`; la suma de todos los balances es exactamente `0.00` incluso con
  divisiones no exactas (p. ej. `100.00` entre 3). Verificar con `mvnw.cmd test`.
- [x] 5.2 Añadir a `BalanceUtilTest` los casos de `minimizarTransferencias`:
  balances `{Ana:+600, Beto:-200, Carla:-200, Diego:-200}` → 3 movimientos de
  `200.00` hacia Ana; un deudor a varios acreedores
  (`{D:-300, E:+200, F:+100}` → `D→E 200`, `D→F 100`); un acreedor de varios
  deudores (`{X:+300, Y:-100, Z:-200}` → `Z→X 200`, `Y→X 100`); todos en `0` →
  lista vacía; ninguna transferencia con `monto` `0.00`; la suma de lo que sale de
  cada deudor y lo que entra a cada acreedor cuadra con su balance. Verificar con
  `mvnw.cmd test`.

## 6. Pruebas de integración de los endpoints

- [x] 6.1 Crear `src/test/java/com/cuentasclaras/backend/balances/BalanceControllerTest.java`
  siguiendo el patrón de `gastos/GastoControllerTest` (`@SpringBootTest`,
  `@Transactional`, `@TestPropertySource` con `jwt.secret`, `MockMvcBuilders` +
  `springSecurity()`, usuarios vía `POST /api/auth/register` con sufijo
  `System.nanoTime()`, grupo vía `POST /api/grupos`, miembros vía
  `POST /api/grupos/{id}/miembros`, gastos vía `POST /api/grupos/{id}/gastos`),
  con el **escenario Samaipata**: grupo de 4, un gasto de `800.00` pagado por Ana
  repartido entre los 4; `GET /api/grupos/{id}/balances` → `200` con
  `Ana = +600.00`, los otros `-200.00`, sin ningún campo de contraseña. Verificar
  con `mvnw.cmd test`.
- [x] 6.2 Añadir a `BalanceControllerTest`: `GET /api/grupos/{id}/balances` de un
  grupo sin gastos → `200` con una entrada por miembro, todas con `balance`
  `0.00`; y comprobar que en cualquier respuesta de balances la suma de los
  `balance` es exactamente `0.00`. Verificar con `mvnw.cmd test`.
- [x] 6.3 Añadir a `BalanceControllerTest`: `GET /api/grupos/{id}/balances` con
  usuario no miembro → `403`; grupo inexistente → `404`; sin token → `401`. Todos
  con el formato de error estándar. Verificar con `mvnw.cmd test`.
- [x] 6.4 Añadir a `BalanceControllerTest`: liquidación del escenario Samaipata:
  `GET /api/grupos/{id}/liquidacion` → `200` con exactamente 3 transferencias,
  cada una `{ de, deId, para, paraId, monto }` con `monto` `200.00`, `para` = Ana
  y `paraId` = id de Ana, y `de` ∈ {Beto, Carla, Diego}. Verificar con
  `mvnw.cmd test`.
- [x] 6.5 Añadir a `BalanceControllerTest`: liquidación de un grupo sin gastos →
  `200` con `[]`; usuario no miembro → `403`; grupo inexistente → `404`; sin token
  → `401`. Verificar con `mvnw.cmd test`.

## 7. Verificación final

- [x] 7.1 Ejecutar `mvnw.cmd test` y comprobar que toda la suite pasa (incluidos
  los tests preexistentes de auth, perfil, seguridad, gestión general, grupos y
  gastos) y que la app compila sin errores. Resultado: **123 tests, 0 fallos,
  BUILD SUCCESS**.
- [x] 7.2 Confirmar los criterios de aceptación del spec delta y que Hibernate no
  genera `alter table` al arrancar. El escenario Samaipata y los dos endpoints
  quedan ejercidos de punta a punta por los 18 tests de `balances`
  (`BalanceControllerTest` vía HTTP real con tokens JWT reales contra PostgreSQL +
  `BalanceUtilTest`); el arranque del contexto Spring con `ddl-auto=update` no
  emite ningún `alter table` / `create table` / `drop table`.
