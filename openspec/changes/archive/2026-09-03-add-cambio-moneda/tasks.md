## 1. Excepción y formato de error

- [x] 1.1 Crear `exception/ServicioExternoNoDisponibleException.java`:
  `RuntimeException` pública con constructores `(String message)` y
  `(String message, Throwable cause)` que delegan en `super(...)`. Mismo estilo
  que `BadRequestException`. Verificar con `mvnw.cmd compile`.
- [x] 1.2 En `exception/GlobalExceptionHandler.java` añadir
  `@ExceptionHandler(ServicioExternoNoDisponibleException.class)` que devuelve
  `503` con `standardBody(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request)`.
  Verificar con `mvnw.cmd compile` y revisar que el JSON sigue el formato de error
  estándar.

## 2. Entidad y configuración

- [x] 2.1 En `entity/Gasto.java`: cambiar `@Column` de `monto` a
  `precision = 20, scale = 8` y añadir Javadoc aclarando que `monto` es el monto
  ORIGINAL en la moneda del gasto (no USDT). Añadir 4 campos con
  `org.hibernate.annotations.ColumnDefault`:
  `@Column(name = "moneda", nullable = false, length = 10) @ColumnDefault("'USDT'") String moneda`;
  `@Column(name = "moneda_nombre", nullable = false, length = 50) @ColumnDefault("'Tether'") String monedaNombre`;
  `@Column(name = "monto_usdt", nullable = false, precision = 10, scale = 6) @ColumnDefault("0") BigDecimal montoUsdt`;
  `@Column(name = "tasa_cambio", nullable = false, precision = 10, scale = 6) @ColumnDefault("1") BigDecimal tasaCambio`.
  Mantener `@Check("monto > 0")`. Verificar con `mvnw.cmd compile` y arrancando la
  app: Hibernate debe emitir `add column` para las 4 columnas nuevas (con
  `default`).
- [x] 2.2 En `backend/src/main/resources/application.properties` añadir
  `criptoya.base-url=https://criptoya.com/api/binancep2p`. Verificar arrancando la
  app (la propiedad se resuelve sin error).

## 3. Cliente CriptoYa y monedas soportadas

- [x] 3.1 Crear `util/MonedasSoportadas.java`: clase `final` con constructor
  privado, `Set<String> FIATS` (ARS, BRL, CLP, COP, MXN, PEN, VES, BOB, UYU, DOP,
  PYG, USD, EUR) y `Set<String> CRIPTOS` (USDT, BTC, ETH, USDC, DAI, UXD, USDP,
  WLD, BNB, SOL, XRP, ADA, AVAX, DOGE, TRX, LINK, DOT, MATIC, SHIB, LTC, BCH, EOS,
  XLM, FTM, AAVE, UNI, ALGO, BAT, PAXG, CAKE, AXS, SLP, MANA, SAND, CHZ) como
  `Set.of(...)` inmutables; helpers `static boolean esFiat/esCripto/esUsdt/esSoportada`.
  Verificar con `mvnw.cmd compile`.
- [x] 3.2 Crear `client/package-info.java` (paquete
  `com.cuentasclaras.backend.client`) y `client/Conversion.java` como
  `record Conversion(BigDecimal montoUsdt, BigDecimal tasaCambio)`. Verificar con
  `mvnw.cmd compile`.
- [x] 3.3 Crear `client/CriptoYaClient.java` (`@Component`): constructor con
  `RestClient.Builder builder` y `@Value("${criptoya.base-url}") String baseUrl`;
  construye un `RestClient` con `baseUrl(baseUrl)` y timeouts (conexión 3 s,
  lectura 5 s) vía `ClientHttpRequestFactorySettings` /
  `ClientHttpRequestFactoryBuilder`. `record PrecioP2P(BigDecimal ask, BigDecimal bid, Long time)`
  interno. Método privado `PrecioP2P precio(String path)` que hace el `GET`,
  y traduce cualquier `RestClientException` o `bid` nulo/`<= 0` a
  `ServicioExternoNoDisponibleException`. Verificar con `mvnw.cmd compile`.
- [x] 3.4 En `client/CriptoYaClient.java` implementar
  `Conversion convertirFiatAUsdt(String moneda, BigDecimal montoOriginal)`:
  `precio("/USDT/" + moneda + "/1")`; `tasa = BigDecimal.ONE.divide(bid, 6, HALF_UP)`;
  `montoUsdt = montoOriginal.multiply(tasa).setScale(6, HALF_UP)`; devuelve
  `Conversion(montoUsdt, tasa)`. Verificar con `mvnw.cmd compile`; comportamiento
  cubierto por la tarea 6.
- [x] 3.5 En `client/CriptoYaClient.java` implementar
  `Conversion convertirCriptoAUsdt(String moneda, BigDecimal montoOriginal)`:
  `bidMoneda = precio("/" + moneda + "/USD/1").bid()`;
  `bidUsdt = precio("/USDT/USD/1").bid()`;
  `tasa = bidMoneda.divide(bidUsdt, 6, HALF_UP)`;
  `montoUsdt = montoOriginal.multiply(tasa).setScale(6, HALF_UP)`. Verificar con
  `mvnw.cmd compile`; comportamiento cubierto por la tarea 6.

## 4. DTOs

- [x] 4.1 En `dto/request/RegistrarGastoRequest.java`: añadir
  `@Size(max = 10) String moneda` y `@Size(max = 50) String monedaNombre` (ambos
  nullables, sin `@NotBlank`) y cambiar el `@Digits` de `monto` a
  `integer = 12, fraction = 8`. Verificar con `mvnw.cmd compile`.
- [x] 4.2 En `dto/request/ActualizarGastoRequest.java`: los mismos cambios que 4.1.
  Verificar con `mvnw.cmd compile`.
- [x] 4.3 En `dto/response/GastoResponse.java` añadir, tras `monto`:
  `String moneda, String monedaNombre, BigDecimal montoUsdt, BigDecimal tasaCambio`.
  Verificar con `mvnw.cmd compile`.
- [x] 4.4 En `dto/response/GastoResumenDto.java` añadir
  `String moneda, String monedaNombre, BigDecimal montoUsdt`. Verificar con
  `mvnw.cmd compile`.

## 5. Servicio

- [x] 5.1 En `service/GastoService.java` inyectar `CriptoYaClient` en el
  constructor y añadir un `record ResultadoConversion(String moneda, String monedaNombre,
  BigDecimal montoUsdt, BigDecimal tasaCambio)` privado. Verificar con
  `mvnw.cmd compile`.
- [x] 5.2 Añadir el privado
  `ResultadoConversion resolver(String monedaReq, String monedaNombreReq, BigDecimal montoOriginal)`:
  normaliza `moneda` (`en blanco` → `USDT`, si no `trim().toUpperCase()`); si es
  `USDT` → `montoUsdt = montoOriginal.setScale(6, HALF_UP)`, `tasa = 1`, sin
  llamada; si `!MonedasSoportadas.esSoportada(moneda)` →
  `BadRequestException("Moneda no soportada: " + moneda)`; si `esFiat` →
  `criptoYaClient.convertirFiatAUsdt`, si no `convertirCriptoAUsdt`;
  `monedaNombre = (monedaNombreReq en blanco) ? moneda : monedaNombreReq.trim()`,
  con el default USDT sin nombre → `"Tether"`. Verificar con `mvnw.cmd compile`.
- [x] 5.3 En `registrar(...)`: llamar a `resolver(req.moneda(), req.monedaNombre(),
  req.monto())`, fijar `moneda`/`monedaNombre`/`montoUsdt`/`tasaCambio` en el
  `Gasto` antes de guardarlo, y pasar a `calcularDivision`
  `resultado.montoUsdt().setScale(2, RoundingMode.HALF_UP)` en vez de `req.monto()`.
  Verificar con `mvnw.cmd compile`; comportamiento cubierto por la tarea 7.
- [x] 5.4 En `actualizar(...)`: mismos cambios que 5.3 (resolver la conversión de
  nuevo, fijar los 4 campos, dividir el `montoUsdt` redondeado a 2). Verificar con
  `mvnw.cmd compile`; comportamiento cubierto por la tarea 7.
- [x] 5.5 En `toResponse(...)` y `toResumen(...)` de `GastoService`: mapear los
  campos nuevos (`moneda`, `monedaNombre`, `montoUsdt`, `tasaCambio` en la
  respuesta completa; `moneda`, `monedaNombre`, `montoUsdt` en el resumen) desde
  el `Gasto`. Verificar con `mvnw.cmd compile`.

## 6. Pruebas del cliente

- [x] 6.1 Crear `src/test/java/com/cuentasclaras/backend/cambiomoneda/CriptoYaClientTest.java`
  con `@RestClientTest(CriptoYaClient.class)` y
  `@TestPropertySource(properties = "criptoya.base-url=https://criptoya.test/api/binancep2p")`,
  autowireando `MockRestServiceServer` y `CriptoYaClient`. Casos:
  `convertirFiatAUsdt("BOB", 800.00)` espera `GET .../USDT/BOB/1`, responde
  `{"ask":7.10,"bid":6.85,"time":1}` → `tasaCambio` `isEqualByComparingTo(1/6.85 a 6 dec)`
  y `montoUsdt` `isEqualByComparingTo(800.00 * tasa)`. Verificar con
  `mvnw.cmd test`.
- [x] 6.2 Añadir a `CriptoYaClientTest`: `convertirCriptoAUsdt("BTC", 0.01)` espera
  dos peticiones (`GET .../BTC/USD/1` y `GET .../USDT/USD/1`), respondiendo `bid`
  `60000.00` y `1.00` → `tasaCambio` `60000` y `montoUsdt` `600.000000`. Verificar
  con `mvnw.cmd test`.
- [x] 6.3 Añadir a `CriptoYaClientTest`: el servidor responde `500` →
  `convertirFiatAUsdt` lanza `ServicioExternoNoDisponibleException`; y un cuerpo
  `{"ask":1}` sin `bid` → misma excepción. Verificar con `mvnw.cmd test`.

## 7. Pruebas de integración de moneda

- [x] 7.1 Crear `src/test/java/com/cuentasclaras/backend/gastos/GastoMonedaControllerTest.java`
  (`@SpringBootTest`, `@Transactional`, `@TestPropertySource` con `jwt.secret`,
  `@MockitoBean CriptoYaClient`, montaje como `GastoControllerTest`). Un
  `@BeforeAll static` autowirea el `DataSource` y ejecuta
  `ALTER TABLE gastos ALTER COLUMN monto TYPE numeric(20,8)` (idempotente). Caso
  fiat: `POST` con `moneda = BOB`, `monedaNombre = Boliviano`, `monto = 800.00`;
  stub `convertirFiatAUsdt(...)` → `Conversion(new BigDecimal("116.788321"), new BigDecimal("0.145985"))`
  → `201` con `moneda`, `monedaNombre = Boliviano`, `montoUsdt`, `tasaCambio`,
  `monto = 800.00`; suma de `division[*].montoAdeudado` == `116.79`; la fila de
  `gastos` en BD tiene las 4 columnas. Verificar con `mvnw.cmd test`.
- [x] 7.2 Añadir a `GastoMonedaControllerTest`: `POST` **sin** `moneda` → `201`
  con `moneda = USDT`, `monedaNombre = Tether`, `tasaCambio = 1`,
  `montoUsdt == monto`, y `verifyNoInteractions(criptoYaClient)`; `POST` con
  `moneda = USDT` explícita → mismo resultado, sin interacción. Verificar con
  `mvnw.cmd test`.
- [x] 7.3 Añadir a `GastoMonedaControllerTest`: `POST` con `moneda = XYZ` → `400`
  con formato de error estándar y `verifyNoInteractions(criptoYaClient)`; `POST`
  con `moneda = BTC` y stub `thenThrow(new ServicioExternoNoDisponibleException("CriptoYa no disponible"))`
  → `503` con formato de error estándar y ningún gasto persistido
  (`GET .../gastos` posterior no lo lista). Verificar con `mvnw.cmd test`.
- [x] 7.4 Añadir a `GastoMonedaControllerTest`: `POST` con `moneda = BTC`,
  `monto = 0.00123456`, stub `convertirCriptoAUsdt` → `Conversion(new BigDecimal("74.074000"), new BigDecimal("60000"))`
  → `201`; la respuesta y la fila en BD conservan `monto` `0.00123456` (8
  decimales). Verificar con `mvnw.cmd test`.
- [x] 7.5 Añadir a `GastoMonedaControllerTest`: `PUT` de un gasto en `BOB`
  cambiando `monto`/`moneda`, con el stub devolviendo otra `tasa` → `200` con
  `montoUsdt`/`tasaCambio` nuevos y la `division` recalculada cuya suma == nuevo
  `montoUsdt` redondeado a 2. Verificar con `mvnw.cmd test`.
- [x] 7.6 Añadir a `GastoMonedaControllerTest`: registrar un gasto en `BOB`
  (convertido vía stub) y luego `GET /api/grupos/{id}/balances` → `200` con la
  suma de balances exactamente `0.00` y sin campos `moneda` ni `tasaCambio` en las
  entradas. Verificar con `mvnw.cmd test`.

## 8. Ajustes descubiertos en la implementación

- [x] 8.1 Ejecutar `mvnw.cmd test` y comprobar que toda la suite pasa, incluidos
  **sin modificar** los tests preexistentes de `gastos`
  (`GastoControllerTest`, `GastoDivisionTest`) y de `balances`
  (`BalanceControllerTest`, `BalanceUtilTest`) — sus gastos siguen siendo en USDT
  y `CriptoYaClient` no se invoca — y que la app compila sin errores. Resultado:
  **138 tests, 0 fallos, BUILD SUCCESS**.
- [x] 8.2 Confirmar los criterios de aceptación del spec delta y que Hibernate
  añade las 4 columnas a `gastos` (con `default`) al arrancar. Registrar el paso
  manual `ALTER TABLE gastos ALTER COLUMN monto TYPE numeric(20,8)`,
  `... monto_usdt TYPE numeric(20,6)`, `... tasa_cambio TYPE numeric(20,6)` en la
  BD de desarrollo (ver Migration Plan del design) y confirmar que un gasto cripto
  se persiste sin pérdida.
- [x] 8.3 **Bean `RestClient.Builder`** (aprobado durante la implementación):
  Spring Boot 4.1 no autoconfigura `RestClient.Builder`, así que
  `CriptoYaClient` no arrancaba. Crear `config/RestClientConfig.java` con un
  `@Bean RestClient.Builder` que fija los timeouts (conexión 3 s, lectura 5 s) vía
  `SimpleClientHttpRequestFactory`. Se quitan las propiedades `spring.http.client.*`
  de `application.properties` (no las consume nadie sin la autoconfig). Verificado
  con `mvnw.cmd test` (el contexto vuelve a cargar).
- [x] 8.4 **Ampliar `monto_usdt` / `tasa_cambio` y corregir `BalanceService`**
  (aprobado durante la implementación): `DECIMAL(10,6)` del borrador desborda con
  tasas cripto (BTC/USDT ≈ 60000+). `entity/Gasto`: `monto_usdt` y `tasa_cambio`
  pasan a `precision = 20, scale = 6`. `BalanceService.cargarContexto` sumaba
  `gasto.getMonto()` (moneda original) como lo pagado — rompía `Σ balances == 0`
  para gastos convertidos; ahora suma `gasto.getMontoUsdt().setScale(2, HALF_UP)`.
  El `ALTER` manual de la BD incluye estas dos columnas. Verificado con
  `mvnw.cmd test`.
