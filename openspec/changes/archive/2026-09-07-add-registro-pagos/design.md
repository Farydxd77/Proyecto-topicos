## Context

Ver `proposal.md` — Why. El backend ya tiene la capacidad de gastos y la de
balances/liquidación. Este cambio añade una tercera capacidad hermana (`pagos`) que
sigue el mismo patrón vertical ya establecido: `entity → repository → dto → service
→ controller`, controlador anidado bajo `/api/grupos/{grupoId}/...`, resolución del
participante desde el token JWT y guardas privadas en el servicio.

Estado actual relevante:

- `GastoService` y `BalanceService` ya implementan, casi con el mismo código, los
  helpers `participanteActual()`, `grupoDondeEsMiembro(grupoId, solicitante)` (que
  comprueba 404 de grupo antes que 403 de membresía) y `gastoDelGrupo(...)` con
  `findByIdAndGrupoId`. `PagoService` replicará ese estilo.
- `BalanceService.cargarContexto(grupoId)` arma tres mapas (`participantes`,
  `pagado`, `adeudado`) y llama a `BalanceUtil.calcularBalances(ids, pagado,
  adeudado)`, que es lógica pura sin Spring ni JPA y está cubierta por
  `BalanceUtilTest`.
- Modelo de datos de la tabla `pagos` en `CLAUDE.md` (sección "Modelo de datos" →
  `pagos`) y en `openspec/specs/data-model.md`: `id BIGSERIAL PK`, `grupo_id`,
  `pagador_id`, `receptor_id` (todas `NOT NULL`, FK), `monto DECIMAL(10,2) NOT NULL`
  (siempre USDT), `fecha DATE NOT NULL`, `tx_id VARCHAR(100)` nullable, más
  `created_at`/`updated_at`.
- Hibernate `ddl-auto=update` crea la tabla al arrancar; no hay migraciones.

## Goals / Non-Goals

**Goals:**

- Añadir la capacidad `pagos` reutilizando el patrón de la capacidad `gastos`, sin
  introducir abstracciones nuevas.
- Que `GET /api/grupos/{id}/balances` y `/liquidacion` incorporen los pagos sin
  romper la invariante "suma de balances = 0.00" ni la lógica pura de `BalanceUtil`.
- Mantener `BalanceUtil` como función pura y testeable de forma aislada.

**Non-Goals (a nivel de diseño):**

- No se factoriza el código compartido de `GastoService`/`BalanceService`/
  `PagoService` (helpers de identidad y de membresía) en una clase común: la
  duplicación ya existe entre los dos servicios actuales y unificarla es un
  refactor aparte, fuera del alcance de este cambio.
- No se añade una tabla intermedia tipo `pago_participantes`: un pago es una fila
  simple (un pagador, un receptor), a diferencia de un gasto con su división.

## Decisions

### 1. `Pago` como entidad simple que hereda de `BaseEntity`

`Pago` extiende `BaseEntity` (igual que `Gasto`) con `@Getter @Setter @SuperBuilder
@NoArgsConstructor @AllArgsConstructor`. Campos:

- `id` — `@Id @GeneratedValue(strategy = IDENTITY)`.
- `grupo` — `@ManyToOne(fetch = LAZY) @JoinColumn(name = "grupo_id", nullable =
  false)`.
- `pagador` — `@ManyToOne(fetch = LAZY) @JoinColumn(name = "pagador_id", nullable =
  false)`. A diferencia de `Gasto.pagador` (que es `nullable = true`), aquí es
  obligatorio: el spec exige pagador y receptor.
- `receptor` — `@ManyToOne(fetch = LAZY) @JoinColumn(name = "receptor_id", nullable
  = false)`.
- `monto` — `@Column(nullable = false, precision = 10, scale = 2)` con
  `@Check(constraints = "monto > 0")` (mismo enfoque que `Gasto`). Es siempre USDT,
  por eso no hay campos de moneda/conversión.
- `fecha` — `LocalDate`, `@Column(nullable = false)`.
- `txId` — `@Column(name = "tx_id", length = 100)` nullable.

**Alternativa descartada:** reutilizar/heredar de `Gasto` o compartir una
superclase con campos de dinero. Rechazada: `Gasto` arrastra moneda, `monedaNombre`,
`montoUsdt`, `tasaCambio` y `descripcion`, que un pago no tiene; la herencia
introduciría columnas nulas y semántica confusa.

### 2. `PagoRepository` con Query Methods, sin `@Query`

```java
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByGrupoIdOrderByFechaDesc(Long grupoId);
    Optional<Pago> findByIdAndGrupoId(Long id, Long grupoId);
}
```

Idéntico en forma a `GastoRepository`. `findByIdAndGrupoId` cubre el caso "el pago
existe pero es de otro grupo → 404". No se necesita `deleteByGrupoId` ni borrado en
cascada de hijos porque no hay tabla hija.

### 3. DTOs: `RegistrarPagoRequest`, `ActualizarPagoRequest`, `PagoResponse`

- `RegistrarPagoRequest(Long receptorId, BigDecimal monto, LocalDate fecha, String
  txId)` con validación Bean Validation:
  - `receptorId` — `@NotNull`.
  - `monto` — `@NotNull @Positive @Digits(integer = 10, fraction = 2)`.
  - `fecha` — `@NotNull`.
  - `txId` — `@Size(max = 100)` (opcional, puede ser `null`).
  - **No lleva `pagadorId`**: el pagador se resuelve del token. Enviar `pagadorId`
    permitiría registrar un pago a nombre de otro, lo que el spec prohíbe. Esto es
    una diferencia deliberada con `RegistrarGastoRequest`, que sí lleva `pagadorId`
    porque un gasto se puede registrar en nombre de cualquier miembro.
- `ActualizarPagoRequest` — mismos cuatro campos y mismas validaciones que
  `RegistrarPagoRequest`. Se crea como record separado para seguir la convención ya
  usada en gastos (`RegistrarGastoRequest` / `ActualizarGastoRequest` coexisten
  aunque tengan la misma forma).
  **Alternativa descartada:** un único record compartido para alta y edición.
  Rechazada por consistencia con el resto del backend y para dejar espacio a que
  diverjan sin refactor.
- `PagoResponse(Long id, Long grupoId, ParticipanteDto pagador, ParticipanteDto
  receptor, BigDecimal monto, LocalDate fecha, String txId)`. Reutiliza
  `ParticipanteDto` (id, nombre, apellido, ci, username), igual que
  `GastoResponse`.

### 4. `PagoService`: guardas replicadas del patrón existente

Métodos públicos, con `@Transactional` en escritura y `@Transactional(readOnly =
true)` en lectura:

- `registrar(Long grupoId, RegistrarPagoRequest req) → PagoResponse`
- `listar(Long grupoId) → List<PagoResponse>`
- `obtenerDetalle(Long grupoId, Long pagoId) → PagoResponse`
- `actualizar(Long grupoId, Long pagoId, ActualizarPagoRequest req) → PagoResponse`
- `eliminar(Long grupoId, Long pagoId) → void`

Helpers privados (copiados del estilo de `GastoService`):

- `participanteActual()` — resuelve `Usuario` → `Participante` desde el
  `SecurityContextHolder`.
- `grupoDondeEsMiembro(grupoId, solicitante)` — `findById` del grupo → 404 si no
  existe; `findByGrupoIdAndParticipanteId` → 403 si no es miembro. **El 404 de
  grupo se evalúa antes que el 403 de membresía.**
- `pagoDelGrupo(grupoId, pagoId)` — `findByIdAndGrupoId` → 404
  (`ResourceNotFoundException`).
- `receptorMiembro(grupoId, receptorId)` — `findByGrupoIdAndParticipanteId` →
  `BadRequestException` ("El receptor no es miembro del grupo") si no lo es.
- `validarDistintos(pagadorId, receptorId)` — `BadRequestException` ("El pagador y
  el receptor no pueden ser la misma persona") si coinciden.
- `exigirPagador(pago, solicitante)` — usado en `actualizar` y `eliminar`:
  `ForbiddenOperationException` si `pago.getPagador().getId()` ≠ `solicitante`.
  Como solo el pagador puede crear el pago, `pagador_id` ya identifica a quien lo
  registró; no hace falta una columna `creador_id` aparte.

Orden de comprobaciones en `actualizar`/`eliminar`: grupo existe (404) → solicitante
es miembro (403) → pago existe en el grupo (404) → solicitante es el pagador (403) →
en `actualizar`, validación de cuerpo (400: monto, receptor miembro, pagador ≠
receptor). `monto <= 0` lo rechaza Bean Validation (`@Positive`) antes de llegar al
servicio; el `@Check` de la entidad es la última red.

`monto` se persiste con `setScale(2, HALF_UP)` para fijar la escala USDT, igual que
`BalanceService` ya hace con `montoUsdt`.

### 5. `PagoController` anidado bajo el grupo

`@RestController @RequestMapping("/api/grupos/{grupoId}/pagos")`, con los cinco
métodos calcados de `GastoController`: `POST` (`@ResponseStatus(CREATED)`), `GET`
lista, `GET /{pagoId}`, `PUT /{pagoId}`, `DELETE /{pagoId}`
(`@ResponseStatus(NO_CONTENT)`). `@Valid @RequestBody` en `POST` y `PUT`. La ruta
usa `{grupoId}` (no `{id}`) para alinearse con `GastoController` y
`BalanceController`; el borrador `openspec/specs/pagos/spec.md` escribía `{id}`, que
es solo el nombre del path param.

Ningún cambio en `SecurityConfig`: todo lo que no es `/api/auth/**` ya exige JWT, así
que las peticiones sin token devuelven 401 con el formato estándar vía
`JwtAuthenticationEntryPoint`.

### 6. Impacto en balances: overload de `BalanceUtil.calcularBalances`

Se añade una sobrecarga de cinco argumentos y la actual de tres pasa a delegar:

```java
public static Map<Long, BigDecimal> calcularBalances(
        Set<Long> ids, Map<Long, BigDecimal> pagadoPorId, Map<Long, BigDecimal> adeudadoPorId) {
    return calcularBalances(ids, pagadoPorId, adeudadoPorId, Map.of(), Map.of());
}

public static Map<Long, BigDecimal> calcularBalances(
        Set<Long> ids,
        Map<Long, BigDecimal> pagadoPorId,
        Map<Long, BigDecimal> adeudadoPorId,
        Map<Long, BigDecimal> pagosRealizadosPorId,
        Map<Long, BigDecimal> pagosRecibidosPorId) {
    // balance = pagado - adeudado + pagosRealizados - pagosRecibidos
}
```

`BalanceService.cargarContexto` se extiende para recorrer
`pagoRepository.findByGrupoIdOrderByFechaDesc(grupoId)` y construir dos mapas nuevos
(`pagosRealizados` por `pagador.id`, `pagosRecibidos` por `receptor.id`), sumando
además esos participantes a `participantes`. Los dos sitios que llaman a
`calcularBalances` (`calcularBalances` y `calcularLiquidacion`) pasan a usar la
sobrecarga de cinco argumentos. `minimizarTransferencias` no cambia: opera sobre el
mapa de balances ya resuelto.

**Alternativas descartadas:**

- *Plegar los montos de pagos dentro de los mapas `pagado`/`adeudado`* (sumar el
  pago al `pagado` del pagador y al `adeudado` del receptor). Aritméticamente da el
  mismo resultado, pero mezcla "gasto pagado" con "pago enviado" y esconde la regla
  de liquidación de la función pura que la testea.
- *Cambiar la firma de tres argumentos in situ* (sin overload). Rechazada: rompería
  las ~8 llamadas de `BalanceUtilTest` sin beneficio; el overload deja los tests
  actuales intactos y añade los nuevos sobre la firma de cinco.
- *Un `PagoBalanceService` separado que ajuste los balances después.* Rechazada:
  duplica la carga del contexto del grupo y el ensamblado del DTO.

## Risks / Trade-offs

- **Duplicación de los helpers de identidad/membresía en un tercer servicio.** →
  Aceptada a corto plazo: es el patrón vigente del proyecto; unificarla es un
  refactor transversal con su propio cambio. Se mantiene el código byte a byte
  igual para que ese refactor futuro sea mecánico.
- **`ddl-auto=update` no añade la FK `receptor_id` ni el `CHECK monto > 0` sobre
  tablas `pagos` preexistentes con datos.** → No aplica: la tabla se crea nueva con
  este cambio; no hay entorno con datos previos.
- **Un pago puede dejar un balance "sobre-pagado"** (p. ej. Beto paga 300 cuando
  solo debía 200): la liquidación mostraría entonces a Beto como acreedor por 100.
  → Es intencional (ver `proposal.md` — Non-Goals): el sistema no limita el monto
  al saldo pendiente. La invariante suma = 0.00 se mantiene igual.
- **Editar el `receptor` de un pago cambia dos balances a la vez.** → Cubierto por
  el recálculo completo en cada `GET /balances`; no hay estado derivado que
  actualizar.
- **Escala de `monto`.** Bean Validation limita a 2 decimales en la entrada y el
  servicio fuerza `setScale(2)`; así el aporte a balances entra ya con escala 2 y
  no introduce redondeo nuevo, preservando la invariante.
