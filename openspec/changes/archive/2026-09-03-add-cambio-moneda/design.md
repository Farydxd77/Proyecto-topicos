## Context

Ver `proposal.md` — Why. Estado actual relevante:

- La capacidad `gastos` está implementada: `Gasto` (tabla `gastos`: `id`,
  `grupo_id`, `descripcion`, `monto` `DECIMAL(10,2)` con `@Check("monto > 0")`,
  `pagador_id`, `fecha`), `GastoParticipante` (`monto_adeudado` `DECIMAL(10,2)`),
  `GastoRepository`, `GastoParticipanteRepository`, `GastoService`,
  `GastoController` bajo `/api/grupos/{grupoId}/gastos`.
- `GastoService.registrar` / `actualizar` construyen el `Gasto`, obtienen los
  miembros actuales y llaman a un privado `calcularDivision(Gasto, BigDecimal
  monto, List<Participante>, Participante pagador)` que reparte `monto` a 2
  decimales con el pagador absorbiendo el residuo, y guardan las filas con
  `gastoParticipanteRepository.saveAll(...)`.
- `RegistrarGastoRequest` / `ActualizarGastoRequest` son `record` con
  `@NotBlank descripcion`, `@NotNull @Positive @Digits(integer = 8, fraction = 2)
  monto`, `@NotNull pagadorId`, `@NotNull fecha`.
- `BalanceService` / `BalanceUtil` suman `monto_adeudado` (USDT) y garantizan
  `Σ balances == 0.00`. No dependen de `Gasto.monto`.
- `GlobalExceptionHandler` mapea `BadRequestException` → `400`,
  `ResourceNotFoundException` → `404`, `ForbiddenOperationException` → `403`,
  `ConflictException` → `409`, y usa el helper `standardBody(...)`. No hay `503`.
- `spring-boot-starter-webmvc` trae `RestClient` y su auto‑configuración de
  `RestClient.Builder`. No hay cliente HTTP propio ni paquete `client/`.
- `application.properties` no tiene ninguna propiedad de CriptoYa. `SecurityConfig`
  ya protege `/api/grupos/**`.
- Ver modelo de datos y convenciones en CLAUDE.md.

## Goals / Non-Goals

**Goals:**

- Convertir el `monto` de un gasto a USDT al registrarlo/editarlo, con las
  fórmulas exactas del borrador (fiat: `1/bid`; cripto: `bidCoin/bidUsdt`; USDT:
  sin llamada), y persistir `moneda`, `moneda_nombre`, `monto_usdt`, `tasa_cambio`.
- Aislar la llamada externa en un `CriptoYaClient` fino y testeable; que su fallo
  se traduzca en un `503` con el formato de error estándar y sin gasto registrado.
- Mantener `moneda` / `monedaNombre` **opcionales** para no romper el contrato ni
  los tests existentes de `gastos` (gasto sin `moneda` == gasto en USDT).
- No tocar `BalanceService` / `BalanceUtil`: siguen operando sobre `monto_adeudado`
  en USDT.

**Non-Goals (nivel diseño):**

- No se cachea ninguna tasa; cada `POST`/`PUT` no‑USDT consulta CriptoYa en vivo.
- No se recalculan gastos históricos.
- No se introduce Flyway/Liquibase (el proyecto usa `ddl-auto=update`); el único
  cambio de tipo de columna se documenta como paso manual.
- No se expone `tasa_cambio` fuera de las respuestas de gasto.

## Decisions

### 1. `CriptoYaClient` como `@Component` con `RestClient` y URL base configurable

Nuevo `client/CriptoYaClient` (`@Component`) que recibe en el constructor un
`RestClient.Builder` (auto‑configurado por Spring) y `@Value("${criptoya.base-url}")`,
y construye un `RestClient` con `baseUrl(...)` y timeouts cortos
(`ClientHttpRequestFactorySettings` → conexión 3 s, lectura 5 s). Métodos, tal
como los nombra el borrador:

- `Conversion convertirFiatAUsdt(String moneda, BigDecimal montoOriginal)`:
  `GET /USDT/{moneda}/1` → `PrecioP2P{ask, bid, time}`;
  `tasa = ONE.divide(bid, 6, HALF_UP)`; `montoUsdt = montoOriginal.multiply(tasa)
  .setScale(6, HALF_UP)`.
- `Conversion convertirCriptoAUsdt(String moneda, BigDecimal montoOriginal)`:
  `GET /{moneda}/USD/1` y `GET /USDT/USD/1`;
  `tasa = bidMoneda.divide(bidUsdt, 6, HALF_UP)`;
  `montoUsdt = montoOriginal.multiply(tasa).setScale(6, HALF_UP)`.

`Conversion` es un `record Conversion(BigDecimal montoUsdt, BigDecimal tasaCambio)`
en `client/`. `PrecioP2P` es un `record` interno para deserializar la respuesta.

Cualquier `RestClientException` (timeout, conexión, `4xx`/`5xx`), o una respuesta
sin `bid` numérico (`null` o `<= 0`), se traduce a
`ServicioExternoNoDisponibleException`.

- **Por qué:** un cliente fino con una sola responsabilidad; la URL base
  parametrizada permite apuntarlo a un `MockRestServiceServer` en los tests. Los
  timeouts evitan que un CriptoYa lento bloquee el `POST` indefinidamente.
- **Por qué `RestClient` y no `WebClient`/`RestTemplate`:** `RestClient` es el
  cliente síncrono actual de Spring, ya disponible con `webmvc`; el flujo es
  bloqueante (dentro de un `@Transactional` de MVC), así que `WebClient` sería
  overkill.
- **Alternativa descartada:** parsear con `JsonNode` en vez de un `record` — el
  `record` documenta el contrato de CriptoYa y falla claro si cambia.

### 2. Símbolos soportados: dos conjuntos embebidos en `util/MonedasSoportadas`

Nueva clase `util/MonedasSoportadas` (final, sin estado) con
`Set<String> FIATS` (13 símbolos) y `Set<String> CRIPTOS` (34, incluye `USDT`)
tomados del borrador, y helpers `esFiat`, `esCripto`, `esUsdt`, `esSoportada`
(`FIATS.contains || CRIPTOS.contains`).

- **Por qué:** el borrador dice *"El backend determina internamente si es fiat o
  cripto"*. Un `Set` literal es lo más simple; no hay endpoint de "monedas
  soportadas" (Non‑Goal), así que no hace falta una entidad ni tabla.
- **Alternativa descartada:** un `enum Moneda` con nombre y categoría — más
  código, y el `moneda_nombre` lo aporta el cliente (decisión 4), no el enum.

### 3. `GastoService` resuelve la conversión; la división opera sobre `montoUsdt`

`GastoService` inyecta `CriptoYaClient`. Nuevo privado
`ResultadoConversion resolver(String monedaReq, String monedaNombreReq, BigDecimal
montoOriginal)` que devuelve `(String moneda, String monedaNombre, BigDecimal
montoUsdt, BigDecimal tasaCambio)`:

1. `moneda = (monedaReq en blanco) ? "USDT" : monedaReq.trim().toUpperCase()`.
2. `moneda.equals("USDT")` → `montoUsdt = montoOriginal.setScale(6, HALF_UP)`,
   `tasaCambio = 1`, sin llamada externa.
3. `!MonedasSoportadas.esSoportada(moneda)` →
   `BadRequestException("Moneda no soportada: " + moneda)` (antes de cualquier
   llamada externa).
4. `esFiat(moneda)` → `criptoYaClient.convertirFiatAUsdt(...)`;
   si no, `criptoYaClient.convertirCriptoAUsdt(...)`.
5. `monedaNombre = (monedaNombreReq en blanco) ? moneda : monedaNombreReq.trim()`;
   con la moneda por defecto (`USDT` implícito) y sin `monedaNombre` en el
   request → `"Tether"`.

`registrar` y `actualizar` llaman a `resolver(...)`, fijan
`moneda`/`monedaNombre`/`montoUsdt`/`tasaCambio` en el `Gasto`, y pasan a
`calcularDivision` el valor `montoUsdt.setScale(2, RoundingMode.HALF_UP)` en lugar
de `req.monto()`. El resto de `calcularDivision` no cambia: divide a 2 decimales y
el pagador absorbe el centavo.

- **Por qué dividir sobre `montoUsdt` redondeado a 2:** `gasto_participantes.
  monto_adeudado` sigue siendo `DECIMAL(10,2)` (el borrador: *"monto_adeudado …
  sigue en USDT sin cambios"*). Redondeando `montoUsdt` a 2 antes de repartir se
  mantiene el invariante `Σ monto_adeudado == montoUsdt(2 dec)` exacto, y por
  tanto `Σ balances == 0.00`. La fracción sub‑centavo de `montoUsdt` (posiciones
  3‑6) vive solo en la columna `monto_usdt` del gasto, informativa.
- **Por qué el orden 400‑símbolo antes que la llamada externa:** el borrador exige
  *"La validación del símbolo ocurre antes de cualquier llamada externa"*; además
  evita gastar una llamada en un símbolo inválido.
- **`@Valid` sigue corriendo primero** en el controller (monto `<= 0`, `pagadorId`
  nulo → `400`), igual que hoy.

### 4. `moneda_nombre` se persiste tal cual lo envía el cliente

`monedaNombre` es un campo **opcional** del request (`@Size(max = 50)`), y se
guarda verbatim en `gastos.moneda_nombre`. Si el request no lo trae, se guarda el
símbolo (`moneda`); con la moneda por defecto USDT, `"Tether"`. El backend **no**
deriva ni valida el nombre contra ninguna tabla.

- **Por qué:** decisión del usuario. El símbolo (`moneda`) sí se valida; el nombre
  es texto de presentación que el frontend ya conoce (el listado vive en el spec).
- **Alternativa descartada:** derivar `moneda_nombre` de una tabla símbolo→nombre
  en el backend — más robusto pero explícitamente no elegido.

### 5. `Gasto`: 4 columnas nuevas con `@ColumnDefault`; `monto` a `DECIMAL(20,8)`

En `entity/Gasto`:

```java
/** Monto ORIGINAL en la moneda del gasto (campo {@code moneda}). NO está en USDT;
 *  el valor convertido es {@code montoUsdt}. */
@Column(name = "monto", nullable = false, precision = 20, scale = 8)
private BigDecimal monto;

@Column(name = "moneda", nullable = false, length = 10)
@ColumnDefault("'USDT'")
private String moneda;

@Column(name = "moneda_nombre", nullable = false, length = 50)
@ColumnDefault("'Tether'")
private String monedaNombre;

@Column(name = "monto_usdt", nullable = false, precision = 20, scale = 6)
@ColumnDefault("0")
private BigDecimal montoUsdt;

@Column(name = "tasa_cambio", nullable = false, precision = 20, scale = 6)
@ColumnDefault("1")
private BigDecimal tasaCambio;
```

`@Check("monto > 0")` se mantiene. `@ColumnDefault` (Hibernate, ya usado con
`@Check` en esta entidad) emite el `DEFAULT` en el `add column`, de modo que las
filas de `gastos` preexistentes quedan como gastos en USDT válidos.

- **Por qué `precision = 20, scale = 8` para `monto`:** decisión del usuario —
  soportar cantidades cripto reales (p. ej. `0.00123456 BTC`).
- **Por qué `monto_usdt` / `tasa_cambio` a `DECIMAL(20,6)` y no `DECIMAL(10,6)`
  como el borrador:** ver decisión 12 — una tasa cripto (`bidCoin / bidUsdt`)
  supera `9999.99` y desborda `DECIMAL(10,6)`. La escala de 6 decimales se
  conserva.
- **Alternativa descartada:** dejar `monto` en `DECIMAL(10,2)` — limitaría los
  gastos cripto a 2 decimales.

### 6. Nueva `ServicioExternoNoDisponibleException` → `503`

`RuntimeException` simple en `exception/`, con constructor `(String message)` y
otro `(String message, Throwable cause)`. Handler nuevo en `GlobalExceptionHandler`
que devuelve `503` con `standardBody(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(),
request)`.

- **Por qué una excepción nueva:** ningún mapeo actual produce `503`; es un modo
  de fallo específico (dependencia externa caída) y reutilizable si aparecen más
  integraciones.
- **Alternativa descartada:** dejar propagar `RestClientException` y mapearla
  directamente — acopla el handler global a un tipo de Spring y no distingue "la
  API externa falló" de otros usos futuros de `RestClient`.

### 7. DTOs

- `RegistrarGastoRequest` / `ActualizarGastoRequest`: se añaden
  `@Size(max = 10) String moneda` y `@Size(max = 50) String monedaNombre` (ambos
  nullables), y el `@Digits` de `monto` pasa a `integer = 12, fraction = 8`.
- `GastoResponse`: se añaden `String moneda`, `String monedaNombre`,
  `BigDecimal montoUsdt`, `BigDecimal tasaCambio` (tras `monto`). `monto` sigue
  siendo el original.
- `GastoResumenDto`: se añaden `String moneda`, `String monedaNombre`,
  `BigDecimal montoUsdt`.
- `GastoParticipanteDto` no cambia (`montoAdeudado` ya está en USDT).
- `GastoService.toResponse` / `toResumen` mapean los campos nuevos desde el `Gasto`.

### 8. `criptoya.base-url` en `application.properties`

`criptoya.base-url=https://criptoya.com/api/binancep2p`. Es la única propiedad
nueva. Los tests la sobrescriben con `@TestPropertySource` para apuntar al mock.

### 9. Pruebas: cliente aislado + integración con `CriptoYaClient` mockeado

- `src/test/java/com/cuentasclaras/backend/cambiomoneda/CriptoYaClientTest.java` —
  `@RestClientTest(CriptoYaClient.class)` con `MockRestServiceServer` y
  `@TestPropertySource(properties = "criptoya.base-url=https://criptoya.test/api/binancep2p")`.
  Casos: conversión fiat (`expect USDT/BOB/1`, respuesta `{ask,bid}`, asserts
  `tasa == 1/bid` y `montoUsdt == monto*tasa`); conversión cripto (dos `expect`:
  `BTC/USD/1` y `USDT/USD/1`, `tasa == bidCoin/bidUsdt`); `500` del servidor →
  `ServicioExternoNoDisponibleException`; cuerpo sin `bid` → misma excepción.
- `src/test/java/com/cuentasclaras/backend/gastos/GastoMonedaControllerTest.java` —
  `@SpringBootTest`, `@Transactional`, `@MockitoBean CriptoYaClient`, montaje como
  `GastoControllerTest` (usuarios/grupo/miembros vía HTTP). Un `@BeforeAll`
  ejecuta `ALTER TABLE gastos ALTER COLUMN monto TYPE numeric(20,8)` vía
  `DataSource` (ver Migration Plan; idempotente). Casos:
  - `POST` con `moneda = BOB`, stub `convertirFiatAUsdt` → `Conversion(116.788321,
    0.145985)` → `201`; respuesta con `moneda`, `monedaNombre = Boliviano`,
    `montoUsdt`, `tasaCambio`, `monto = 800.00` original; `Σ montoAdeudado ==
    116.79`.
  - `POST` sin `moneda` → `201`, `moneda = USDT`, `monedaNombre = Tether`,
    `tasaCambio = 1`, `montoUsdt == monto`; `verifyNoInteractions(criptoYaClient)`.
  - `POST` con `moneda = XYZ` → `400`, sin interacción con el cliente.
  - `POST` con `moneda = BTC` y stub `thenThrow(ServicioExternoNoDisponibleException)`
    → `503` con formato estándar; ningún gasto persistido.
  - `POST` con `moneda = BTC`, `monto = 0.00123456`, stub cripto → `201`, `monto`
    preservado a 8 decimales.
  - `PUT` cambiando `moneda`/`monto`, stub devuelve otra `tasa` → `200`, nuevos
    `montoUsdt`/`tasaCambio`, división recalculada, `Σ` cuadra.
  - `GET /balances` de un grupo con un gasto en `BOB` convertido → `Σ == 0.00`,
    sin `moneda` ni `tasaCambio` en las entradas.
- Los tests preexistentes (`GastoControllerTest`, `GastoDivisionTest`,
  `BalanceControllerTest`) **no se tocan**: siempre omiten `moneda`, así que sus
  gastos son en USDT y `CriptoYaClient` no se invoca.

### 10. Specs: `cambio-moneda` nuevo + `gastos` MODIFIED

El delta tiene dos archivos: `specs/cambio-moneda/spec.md` (`## Purpose` +
`## ADDED Requirements`) y `specs/gastos/spec.md` (`## MODIFIED Requirements` con
los dos requisitos de registro y edición reescritos, conservando todos sus
scenarios). Al archivar, OpenSpec crea `openspec/specs/cambio-moneda/spec.md`
canónico (reemplaza el borrador) y funde los dos requisitos modificados en
`openspec/specs/gastos/spec.md`.

### 11. `RestClient.Builder` se declara como `@Bean` propio (descubierto en la implementación)

Spring Boot 4.1 **no autoconfigura** un `RestClient.Builder` con
`spring-boot-starter-webmvc` (a diferencia de 3.x), así que `CriptoYaClient` no
podía arrancar (`NoSuchBeanDefinitionException`). Se añade
`config/RestClientConfig` con `@Bean RestClient.Builder` que fija los timeouts
(conexión 3 s, lectura 5 s) vía `SimpleClientHttpRequestFactory`. `CriptoYaClient`
sigue recibiendo el `RestClient.Builder` por constructor (para que el test lo
sustituya por uno vinculado a `MockRestServiceServer`) y solo le aplica
`baseUrl(...)`. Las propiedades `spring.http.client.*` que se habían añadido a
`application.properties` se retiran: sin la autoconfig no las consume nadie.

### 12. `monto_usdt` / `tasa_cambio` a `DECIMAL(20,6)` y `BalanceService` en USDT (descubierto en la implementación)

- **Precisión:** `DECIMAL(10,6)` del borrador solo llega a `9999.999999`. La tasa
  cripto es `bidCoin / bidUsdt` (BTC/USDT ≈ 60000‑120000) y el `montoUsdt` de un
  gasto cripto mediano también supera ese límite → `numeric field overflow` en
  PostgreSQL. Se amplían ambas columnas a `precision = 20, scale = 6` (la escala
  de 6 decimales del borrador se conserva).
- **`BalanceService`:** `cargarContexto` sumaba `gasto.getMonto()` (moneda
  original) como lo pagado por el pagador, mientras `monto_adeudado` está en USDT.
  Para un gasto convertido eso rompe `Σ balances == 0` (se observó `Σ = 683.21`
  con un gasto en BOB). Ahora suma `gasto.getMontoUsdt().setScale(2, HALF_UP)`
  (misma escala que `monto_adeudado`). `BalanceUtil` no cambia. Esto corrige el
  proposal original ("BalanceService sin cambios") por una línea.

## Risks / Trade-offs

- **`ddl-auto=update` no cambia el tipo de columnas existentes** → los tres
  `ALTER COLUMN ... TYPE` (`monto` → `numeric(20,8)`, `monto_usdt` y `tasa_cambio`
  → `numeric(20,6)`) **no** los aplica Hibernate. Mitigación: paso manual
  documentado (Migration Plan) y ejecutado por `GastoMonedaControllerTest` en
  `@BeforeAll` para que el caso cripto se pruebe de verdad. En una BD nueva las
  columnas ya se crean con esos tipos.
- **Dependencia de una API externa en el camino de escritura** → registrar/editar
  un gasto no‑USDT puede fallar con `503` si CriptoYa está caído. Es el modo de
  fallo diseñado (el borrador lo exige); sin caché por decisión de alcance.
- **`montoUsdt` (6 dec) ≠ `Σ monto_adeudado` (2 dec)** → pequeña discrepancia de
  presentación entre el total del gasto y la suma de su división. El invariante
  de balances (`Σ == 0.00`) se mantiene exacto porque solo usa `monto_adeudado`.
- **`tasa` redondeada a 6 decimales antes de multiplicar** → `montoUsdt` puede
  diferir en la 6.ª posición de `montoOriginal / bid` calculado en alta precisión.
  Es exactamente la fórmula del borrador (`tasa = 1/bid; montoUsdt = monto*tasa`).
- **Timeouts 3 s / 5 s** → un CriptoYa lento devuelve `503` tras ~5 s; el cliente
  espera esa petición dentro del `@Transactional`. Aceptable para el volumen de
  esta fase.
- **`@ColumnDefault` en columnas `NOT NULL` nuevas sobre una tabla con filas** →
  PostgreSQL admite `ADD COLUMN ... NOT NULL DEFAULT ...`; las filas viejas quedan
  como gastos en USDT (`monto_usdt = 0`, `tasa_cambio = 1`). `monto_usdt = 0` en
  gastos históricos es inocuo: sus `monto_adeudado` ya están calculados y son los
  que usan balances.
- **Red saliente desde el backend** → nuevo requisito de despliegue (acceso a
  `criptoya.com`). Documentado en Impact.

## Migration Plan

1. Desplegar el código.
2. `ddl-auto=update` añade a `gastos` las columnas `moneda` (`varchar(10) not null
   default 'USDT'`), `moneda_nombre` (`varchar(50) not null default 'Tether'`),
   `monto_usdt` (`numeric(20,6) not null default 0`) y `tasa_cambio`
   (`numeric(20,6) not null default 1`). Las filas existentes toman los defaults.
3. **Manual, una sola vez** (Hibernate `update` no altera tipos de columnas
   existentes):
   ```sql
   ALTER TABLE gastos ALTER COLUMN monto TYPE numeric(20,8);
   ALTER TABLE gastos ALTER COLUMN monto_usdt TYPE numeric(20,6);
   ALTER TABLE gastos ALTER COLUMN tasa_cambio TYPE numeric(20,6);
   ```
   En una base nueva este paso no hace falta (las columnas se crean ya con esos
   tipos). `GastoMonedaControllerTest` ejecuta estos tres `ALTER` en `@BeforeAll`
   para que el caso cripto se pruebe de verdad.
4. **Rollback:** revertir el commit. Las 4 columnas nuevas pueden quedarse (son
   `NOT NULL` con default y no molestan) o borrarse; los tipos ampliados son
   compatibles hacia atrás y pueden quedarse.
