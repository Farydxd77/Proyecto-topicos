## Context

Ver `proposal.md` — Why. Estado actual relevante:

- La capacidad `gastos` ya está implementada: `Gasto` (tabla `gastos`: `id`,
  `grupo_id`, `descripcion`, `monto` `DECIMAL(10,2)`, `pagador_id`, `fecha`),
  `GastoParticipante` (tabla `gasto_participantes`, `@EmbeddedId`
  `(gastoId, participanteId)`, `monto_adeudado` `DECIMAL(10,2)`),
  `GastoRepository` (`findByGrupoIdOrderByFechaDesc`, `findByIdAndGrupoId`),
  `GastoParticipanteRepository` (`findByGastoId`, `deleteByGastoId`),
  `GastoService`, `GastoController`. **Invariante ya garantizado por `gastos`:**
  para cada gasto, `Σ monto_adeudado == monto`, y el pagador absorbe el centavo
  sobrante del redondeo.
- La capacidad `grupos` está implementada:
  `GrupoParticipanteRepository.findByGrupoId(Long)` (→ `List`) y
  `findByGrupoIdAndParticipanteId(Long, Long)` (→ `Optional`), `GrupoRepository`,
  `GrupoService` con los privados `participanteActual()` y `grupoDondeEsMiembro`
  (ambos `private`, no reutilizables desde fuera). `GastoService` ya reimplementó
  `grupoDondeEsMiembro` localmente por ese motivo.
- `ParticipanteRepository.findByUsuarioId(Long)` ya existe.
- `GlobalExceptionHandler` mapea `ResourceNotFoundException` → `404`,
  `ForbiddenOperationException` → `403`, y `JwtAuthenticationEntryPoint` el `401`,
  todos con el formato de error estándar.
- `SecurityConfig` aplica `anyRequest().authenticated()` salvo `/api/auth/**` y
  `/actuator/health/**`; `/api/grupos/**` ya queda protegido.
- `dto/response/ParticipanteDto(id, nombre, apellido, ci, username)` ya existe y no
  contiene contraseña.
- **No existe todavía el paquete `util/`** (CLAUDE.md lo prevé con `BalanceUtil`).
- Ver modelo de datos completo y convenciones en CLAUDE.md.

## Goals / Non-Goals

**Goals:**

- Implementar los dos `GET` del spec delta reutilizando los patrones establecidos
  por `grupos` y `gastos`: controller delgado, `@Service` con constructor
  injection, Query Methods JPA sin `@Query`, errores centralizados, mapeo a DTO
  dentro de la transacción.
- Aislar el cálculo (agregación de balances + greedy de liquidación) en
  `util/BalanceUtil` como funciones puras sobre datos planos, testeables sin
  Spring.
- Garantizar por construcción que `Σ balances == 0` exacto y que la liquidación
  deja todos los balances en `0`.
- No modificar ninguna entidad JPA ni el esquema de base de datos.

**Non-Goals (nivel diseño):**

- No se introduce un mapper genérico ni MapStruct.
- No se refactoriza `GrupoService`/`GastoService` para extraer un guardián de
  membresía compartido (ver decisión 3).
- No se busca el óptimo global de número de transferencias; se usa el greedy del
  borrador.
- No se añade paginación, cacheo ni `@EntityGraph`.

## Decisions

### 1. `BalanceService` orquesta; `BalanceUtil` tiene la lógica pura

`BalanceService` (`@Service`, `@Transactional(readOnly = true)`) inyecta
`GastoRepository`, `GastoParticipanteRepository`, `GrupoRepository`,
`GrupoParticipanteRepository`, `ParticipanteRepository` y `UsuarioRepository`.
Expone:

- `List<BalanceDto> calcularBalances(Long grupoId)`
- `List<TransferenciaDto> calcularLiquidacion(Long grupoId)`

Cada método: resuelve al solicitante, valida grupo+membresía, carga los gastos y
sus divisiones, construye un `Map<Long, BigDecimal>` de balances **llamando a
`BalanceUtil`**, y mapea a DTO (resolviendo nombres de participante).

`BalanceUtil` es una clase con métodos `static`, **sin** anotaciones de Spring ni
imports de JPA:

- `Map<Long, BigDecimal> calcularBalances(Set<Long> participantesIds,
  Map<Long, BigDecimal> pagadoPorId, Map<Long, BigDecimal> adeudadoPorId)` —
  para cada id devuelve `pagado - adeudado` (0 por defecto), con la suma
  normalizada a `0.00` exacto.
- `List<Movimiento> minimizarTransferencias(Map<Long, BigDecimal> balances)` —
  el greedy; `Movimiento` es un `record Movimiento(Long deId, Long paraId,
  BigDecimal monto)` en `util/`.

- **Por qué:** el borrador dice explícitamente *"El cálculo de balances y
  liquidación vive en BalanceService"* y *"La lógica pura de cálculo vive en
  util/BalanceUtil"*. La orquestación (transacción, repos, mapeo de DTO, guardas)
  queda en el servicio; la aritmética y el algoritmo, que es lo que hay que probar
  a fondo, queda en `BalanceUtil` sin necesidad de `@SpringBootTest`.
- **Alternativa descartada:** meter todo en `BalanceService` — haría el test del
  algoritmo un test de integración lento y con más ruido.

### 2. El conjunto de balances = miembros actuales ∪ participantes con actividad

`BalanceService` construye el conjunto de participantes a balancear como la unión
de: (a) los miembros actuales del grupo (`grupoParticipanteRepository.findByGrupoId`)
y (b) todo participante que aparezca como `pagador` de un gasto del grupo o en la
`división` de alguno (de las filas de `gasto_participantes`).

- **Por qué:** el borrador exige *"La suma de todos los balances siempre es
  exactamente 0"* como criterio de aceptación. Los gastos son una foto: si un
  participante fue **quitado** del grupo después de participar en un gasto, sus
  `monto_adeudado` / pagos siguen en la base. Excluirlo rompería la suma en cero y
  ocultaría una deuda real. Incluir a los miembros actuales sin actividad cubre el
  caso *"grupo sin gastos → todos los balances en 0"*.
- **Consecuencia observable:** un ex‑miembro con saldo pendiente aparece en
  `/balances` y puede aparecer en `/liquidacion`. Es el comportamiento correcto
  para "dejar el grupo a mano"; queda documentado en el requisito de balances.
- **Alternativa descartada:** listar solo los miembros actuales — más simple, pero
  incompatible con el invariante de suma cero del borrador.

### 3. La verificación de miembro del grupo se reimplementa localmente en `BalanceService`

`grupoDondeEsMiembro(grupoId, solicitante)` en `BalanceService`, idéntico al de
`GastoService`: `grupoRepository.findById(grupoId)` vacío →
`ResourceNotFoundException("Grupo no encontrado: " + grupoId)`; luego
`grupoParticipanteRepository.findByGrupoIdAndParticipanteId(...)` vacío →
`ForbiddenOperationException("No eres miembro de este grupo")`. El `404` se evalúa
**siempre antes** que el `403`. `participanteActual()` copia el patrón de
`GrupoService`/`GastoService`.

- **Por qué:** son ~6 líneas sobre repositorios que ya existen; extraer ahora un
  `GrupoGuard` compartido obligaría a tocar `GrupoService` y `GastoService`
  (capacidades archivadas y probadas) y reampliar su verificación. La deuda
  técnica de "tres copias del guardián" queda anotada para un refactor posterior.
- **Alternativa descartada (recomendada a futuro):** un `GrupoGuard` /
  `GrupoValidacionService` inyectable que devuelva el `Grupo` validado, usado por
  las tres capacidades.

### 4. Agregación de balances: `pagado` y `adeudado` desde los gastos, sin redondeo extra

`BalanceService` recorre `gastoRepository.findByGrupoIdOrderByFechaDesc(grupoId)`:

- `pagadoPorId[gasto.pagador.id] += gasto.monto`
- por cada fila de `gastoParticipanteRepository.findByGastoId(gasto.id)`:
  `adeudadoPorId[fila.participante.id] += fila.montoAdeudado`

`BalanceUtil.calcularBalances` hace `balance = pagado - adeudado` con `BigDecimal`
(aritmética exacta, escala 2). Como para cada gasto `Σ montoAdeudado == monto`
(invariante de `gastos`), `Σ (pagado - adeudado) == Σ monto - Σ monto == 0`
exacto, sin necesidad de repartir ningún centavo aquí.

- **Por qué:** el borrador dice *"balance = suma de gastos que pagó - suma de
  monto_adeudado de todos sus gastos"* y *"Si el monto no divide exactamente, el
  pagador absorbe el centavo sobrante"* — ese ajuste ya lo hizo `gastos` al
  registrar. `balances` solo suma valores ya cuadrados.
- **Salida a 2 decimales:** cada `balance` se devuelve con `setScale(2)` para que
  el JSON sea estable (`-200.00`, no `-200`).
- **Alternativa descartada:** recalcular la división desde `gasto.monto` en
  `balances` — duplicaría la lógica de `gastos` y podría divergir si cambia el
  redondeo.

### 5. Algoritmo greedy de liquidación, con desempate determinista

`BalanceUtil.minimizarTransferencias(Map<Long,BigDecimal> balances)`:

1. `acreedores` = entradas con `balance > 0`; `deudores` = con `balance < 0`
   (se ignoran las de `balance == 0`).
2. Ordenar `acreedores` por `balance` descendente y `deudores` por `balance`
   ascendente (más negativo primero); **desempate por `participanteId`
   ascendente**.
3. Mientras queden deudores con saldo: el primer deudor `d` y el primer acreedor
   `a`; `monto = min(|balance(d)|, balance(a))`; emitir
   `Movimiento(d, a, monto)`; `balance(d) += monto`, `balance(a) -= monto`;
   quitar de la lista a quien quede en `0`.
4. Nunca se emite un `Movimiento` de `monto == 0`.

- **Por qué:** es literalmente el algoritmo del borrador ("Algoritmo de
  liquidación"). El desempate por id no lo pide el borrador pero hace la salida
  **determinista** y por tanto testeable (sin él, dos deudores con el mismo saldo
  podrían salir en cualquier orden).
- **Terminación y exactitud:** todos los balances entran a escala 2 y suman 0;
  cada paso lleva al menos un participante a 0 y conserva la suma, así que el bucle
  termina con todos en 0 y todos los `monto` son múltiplos de `0.01`.
- **No es el óptimo global** de número de transferencias (problema NP‑difícil);
  para el caso de uso real (grupos pequeños) el greedy da un resultado
  suficientemente bueno y predecible. Anotado en Non-Goals del proposal.

### 6. DTOs de salida

- `BalanceDto(ParticipanteDto participante, BigDecimal balance)` para
  `GET /balances`. Reutiliza `ParticipanteDto` (sin contraseña), igual que
  `GastoParticipanteDto` en `gastos`.
- `TransferenciaDto(String de, Long deId, String para, Long paraId,
  BigDecimal monto)` para `GET /liquidacion`, con exactamente los campos y nombres
  del borrador (`de`, `deId`, `para`, `paraId`, `monto`). `de`/`para` son el
  `nombre` del participante (no `nombre apellido`); `deId`/`paraId` desambiguan si
  dos participantes comparten nombre.

- **Por qué:** el formato de `liquidacion` está fijado por el borrador al detalle;
  el de `balances` no, y se elige la forma coherente con el resto de la API
  (`ParticipanteDto` + un `BigDecimal`).
- **Alternativa descartada:** aplanar también `BalanceDto` a
  `(nombre, participanteId, balance)` — se prefiere reutilizar `ParticipanteDto`.

### 7. Controller anidado bajo `/api/grupos/{grupoId}`

`BalanceController` (`@RestController`,
`@RequestMapping("/api/grupos/{grupoId}")`, constructor injection de
`BalanceService`) con `@GetMapping("/balances")` y `@GetMapping("/liquidacion")`,
`grupoId` como `@PathVariable Long`. Ambos devuelven `200` con el cuerpo (lista;
`[]` si vacío).

- **Por qué:** recurso REST distinto de gastos y de grupos; clase propia. El
  prefijo `{grupoId}` mantiene la jerarquía de rutas del borrador.
- **Alternativa descartada:** añadir los dos `GET` a `GrupoController` o
  `GastoController` — mezcla responsabilidades.

### 8. `balances` como spec canónico nuevo

El delta usa `## ADDED Requirements` con `## Purpose`. Al archivar, OpenSpec
generará `openspec/specs/balances/spec.md` en formato canónico, sustituyendo el
borrador manual. Mismo camino que `gestion-general`, `grupos` y `gastos`.

### 9. Pruebas: integración con MockMvc + test unitario puro

- `src/test/java/com/cuentasclaras/backend/balances/BalanceControllerTest.java`
  (`@SpringBootTest`, `@Transactional`, `@TestPropertySource` con `jwt.secret`,
  `MockMvcBuilders` + `springSecurity()`, usuarios vía `POST /api/auth/register`
  con sufijo `System.nanoTime()`, grupo vía `POST /api/grupos`, miembros vía
  `POST /api/grupos/{id}/miembros`, gastos vía `POST /api/grupos/{id}/gastos`):
  el escenario Samaipata de punta a punta, grupo sin gastos, suma cero, `[]` en
  liquidación, `403`/`404`/`401`.
- `src/test/java/com/cuentasclaras/backend/balances/BalanceUtilTest.java` — test
  JUnit puro (sin Spring): agregación, invariante suma cero, greedy con varios
  repartos (un deudor a varios acreedores, un acreedor de varios deudores,
  ya compensado → vacío, nunca `monto` cero), y los números Samaipata.

- **Por qué:** el algoritmo se prueba a fondo y rápido en el test unitario; el
  test de integración verifica el cableado HTTP, los códigos y el formato JSON
  exacto de `TransferenciaDto`.

## Risks / Trade-offs

- **Un ex‑miembro con saldo pendiente aparece en `/balances` y `/liquidacion`** →
  necesario para conservar `Σ balances == 0` y para poder saldar con él.
  Mitigación: documentado en el requisito de balances; el `nombre` + `id` lo
  identifican.
- **El greedy no minimiza globalmente el número de transferencias** → aceptable
  por el borrador y por el tamaño real de los grupos. Mitigación: Non-Goal
  explícito.
- **`de`/`para` usan solo el `nombre`** → dos participantes con el mismo nombre se
  ven igual en texto. Mitigación: `deId`/`paraId` desambiguan; es el formato que
  fija el borrador.
- **N gastos ⇒ N llamadas a `findByGastoId`** (N+1) → volumen bajo en esta fase.
  Mitigación: si molesta, un `findByGasto_Grupo_Id(grupoId)` agregado sin `@Query`
  lo resuelve sin cambiar el contrato.
- **Aritmética `BigDecimal`**: todas las entradas vienen a escala 2 desde
  `DECIMAL(10,2)`; las sumas y `min` conservan la escala. La salida se normaliza
  con `setScale(2)`. Sin `double` en ningún punto.
- **`ddl-auto=update`**: capacidad de solo lectura; no hay entidades nuevas ni
  cambios de esquema.

## Migration Plan

No hay migración de datos ni de esquema: no se crean, renombran ni borran columnas
o tablas, no se modifica ninguna entidad, y los dos endpoints son nuevos y de solo
lectura. El despliegue es el de siempre (`mvnw.cmd test` y arrancar la app). El
rollback es revertir el commit; no queda estado que limpiar.
