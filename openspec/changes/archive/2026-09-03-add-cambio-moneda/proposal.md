## Why

Hoy todo gasto se registra y se reparte en su moneda nominal, mezclando monedas
dentro de un mismo grupo y haciendo que balances y liquidación no tengan sentido
si dos gastos están en monedas distintas. El producto necesita una unidad común:
cada gasto se convierte a **USDT** al registrarse (o editarse) usando precios de
Binance P2P vía CriptoYa, y todo el resto del sistema (división, balances,
liquidación) opera ya en USDT. Existe un borrador de requisitos escrito a mano en
`openspec/specs/cambio-moneda/spec.md`; esta tarea lo formaliza e implementa.

## What Changes

- Nuevo `CriptoYaClient` (`@Component`, `RestClient`) que consulta
  `https://criptoya.com/api/binancep2p/{coin}/{fiat}/1`:
  - `convertirFiatAUsdt(String moneda, BigDecimal monto)`: consulta
    `USDT/{fiat}`, `tasa = 1 / bid`, `montoUsdt = monto * tasa`.
  - `convertirCriptoAUsdt(String moneda, BigDecimal monto)`: consulta
    `{coin}/USD` y `USDT/USD`, `tasa = bidCoin / bidUsdt`, `montoUsdt = monto * tasa`.
  - Cualquier fallo de la API externa (no responde, timeout, respuesta no 2xx)
    lanza `ServicioExternoNoDisponibleException` → `503 Service Unavailable`.
- `POST /api/grupos/{id}/gastos` y `PUT /api/grupos/{id}/gastos/{gastoId}` aceptan
  dos campos **opcionales** nuevos: `moneda` (símbolo) y `monedaNombre`. Si
  `moneda` se omite, el gasto es en `USDT` (`monedaNombre` = `Tether`), no hay
  llamada externa y `tasaCambio = 1`, `montoUsdt = monto`. Con `moneda = USDT`
  explícito, mismo comportamiento.
- El backend decide fiat vs. cripto con dos conjuntos embebidos de símbolos
  soportados (13 fiats + 34 criptos del borrador). Símbolo fuera de ambos →
  `400 Bad Request`.
- `moneda_nombre` se persiste **tal cual lo envía el cliente**; si `monedaNombre`
  se omite se guarda el símbolo (`moneda`). `USDT` implícito guarda `Tether`.
- La división del gasto pasa a hacerse sobre `montoUsdt` (redondeado a 2
  decimales para las filas de `gasto_participantes`, que siguen en USDT). El
  pagador sigue absorbiendo el centavo sobrante.
- `PUT` vuelve a consultar CriptoYa con la tasa del momento de la edición y
  recalcula `montoUsdt`, `tasaCambio` y la división.
- `BalanceService` / `BalanceUtil` — **sin cambios**: ya operan sobre
  `monto_adeudado`, que sigue en USDT. Balances y liquidación siguen en USDT y su
  suma sigue siendo exactamente `0.00`.

## Non-Goals

- No se implementa caché de tasas de cambio: cada registro/edición consulta
  CriptoYa en vivo.
- No se recalcula `montoUsdt` de gastos históricos si la tasa cambia después.
- No se expone `tasaCambio` en balances ni en liquidación.
- No se expone un endpoint de "monedas soportadas": el listado vive en el spec y
  lo consume el frontend.
- No se añade autenticación ni API key a CriptoYa (endpoint público).
- No se toca `frontend/`.
- `moneda` y `monedaNombre` son opcionales por compatibilidad: los gastos ya
  existentes y los tests actuales de `gastos` siguen funcionando como gastos en
  USDT sin cambios.

## Capabilities

### New Capabilities

- `cambio-moneda`: conversión automática del monto de un gasto a USDT al
  registrarlo o editarlo, usando precios de Binance P2P vía CriptoYa —
  distinción fiat/cripto, cálculo de la tasa, persistencia de `moneda`,
  `moneda_nombre`, `monto_usdt` y `tasa_cambio`, caso especial USDT sin llamada
  externa, y `503` cuando la API externa no está disponible.

### Modified Capabilities

- `gastos`: `POST /api/grupos/{id}/gastos` y
  `PUT /api/grupos/{id}/gastos/{gastoId}` aceptan `moneda` / `monedaNombre`
  opcionales, admiten `monto` con hasta 8 decimales, dividen el `montoUsdt`
  convertido (no el `monto` nominal), y la respuesta del gasto incluye `moneda`,
  `monedaNombre`, `montoUsdt` y `tasaCambio`.

## Impact

- **Código nuevo**:
  - `client/CriptoYaClient` (+ `client/package-info.java`) y `client/Conversion`
    (record del resultado de conversión).
  - `exception/ServicioExternoNoDisponibleException` + handler `503` en
    `GlobalExceptionHandler`.
  - `config/RestClientConfig`: `@Bean RestClient.Builder` con timeouts (conexión
    3 s, lectura 5 s). **Necesario** porque Spring Boot 4.1 no autoconfigura
    `RestClient.Builder`.
  - `test/.../cambiomoneda/CriptoYaClientTest`,
    `test/.../gastos/GastoMonedaControllerTest`.
- **Código modificado**:
  - `entity/Gasto`: 4 columnas nuevas (`moneda`, `moneda_nombre`, `monto_usdt`,
    `tasa_cambio`) con `@ColumnDefault`; `monto` pasa a `DECIMAL(20,8)` y
    `monto_usdt` / `tasa_cambio` a `DECIMAL(20,6)` (una tasa cripto no cabe en
    `DECIMAL(10,6)`); Javadoc aclarando que `monto` es el monto original, no USDT.
  - `dto/request/RegistrarGastoRequest` y `ActualizarGastoRequest`: campos
    `moneda` / `monedaNombre` opcionales; `@Digits` de `monto` a
    `integer = 12, fraction = 8`.
  - `dto/response/GastoResponse` y `GastoResumenDto`: campos `moneda`,
    `monedaNombre`, `montoUsdt`, `tasaCambio`.
  - `service/GastoService`: inyecta `CriptoYaClient`; resuelve la conversión en
    `registrar` y `actualizar`; la división opera sobre `montoUsdt`.
  - `service/BalanceService`: `cargarContexto` suma `gasto.getMontoUsdt()` (en
    USDT, redondeado a 2) como lo pagado por el pagador, en vez de
    `gasto.getMonto()` (moneda original) — necesario para conservar `Σ = 0` con
    gastos convertidos. `BalanceUtil` no cambia.
  - `application.properties`: `criptoya.base-url`.
- **Base de datos**: `ddl-auto=update` añade las 4 columnas a `gastos` (con
  `DEFAULT`). El cambio de tipo de `gastos.monto` a `numeric(20,8)` y de
  `monto_usdt` / `tasa_cambio` a `numeric(20,6)` es un `ALTER` manual (Hibernate
  `update` no altera tipos existentes). `gasto_participantes.monto_adeudado` no
  cambia (sigue `DECIMAL(10,2)` en USDT).
- **Dependencias**: ninguna nueva (`RestClient` viene con
  `spring-boot-starter-webmvc`; solo falta declarar el `RestClient.Builder`).
- **Red**: llamadas salientes a `criptoya.com` en cada `POST`/`PUT` de gasto con
  moneda distinta de USDT.
- **Specs**: al archivar, el delta crea `openspec/specs/cambio-moneda/spec.md`
  canónico (reemplazando el borrador) y actualiza dos requisitos de
  `openspec/specs/gastos/spec.md`.
