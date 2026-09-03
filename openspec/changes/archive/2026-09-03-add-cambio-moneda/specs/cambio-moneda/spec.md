## Purpose

Convertir automáticamente el monto de cada gasto a USDT en el momento de
registrarlo o editarlo, usando precios de Binance P2P vía la API pública de
CriptoYa. El gasto conserva su monto y moneda originales; el resto del sistema
(división, balances, liquidación) opera en USDT. Cuando la moneda es USDT no hay
llamada externa; cuando CriptoYa no responde, la operación falla con `503` y el
gasto no se registra.

## ADDED Requirements

### Requirement: Conversión de un gasto en moneda fiat a USDT

Al registrar o editar un gasto cuya `moneda` es uno de los símbolos fiat
soportados (`ARS`, `BRL`, `CLP`, `COP`, `MXN`, `PEN`, `VES`, `BOB`, `UYU`, `DOP`,
`PYG`, `USD`, `EUR`), el sistema SHALL consultar
`GET https://criptoya.com/api/binancep2p/USDT/{moneda}/1`, tomar el campo `bid` de
la respuesta, calcular `tasaCambio = 1 / bid` y `montoUsdt = montoOriginal *
tasaCambio`, y persistir `moneda`, `moneda_nombre`, `monto_usdt` y `tasa_cambio`
en el gasto. `monto_usdt` y `tasa_cambio` se almacenan con 6 decimales.

#### Scenario: Registro de un gasto fiat

- **WHEN** un miembro envía `POST /api/grupos/{id}/gastos` con `monto` `800.00`,
  `moneda` `BOB` y CriptoYa responde `{"ask": 7.10, "bid": 6.85, "time": ...}` para
  `USDT/BOB`
- **THEN** el sistema calcula `tasaCambio = 1 / 6.85` y
  `montoUsdt = 800.00 * tasaCambio`
- **AND** persiste el gasto con `monto` `800.00`, `moneda` `BOB`,
  `monto_usdt ≈ 116.788321` y `tasa_cambio ≈ 0.145985`
- **AND** la división en `gasto_participantes` se calcula sobre `monto_usdt`

#### Scenario: La conversión fiat usa el bid, no el ask

- **WHEN** se convierte un monto fiat y la respuesta de CriptoYa trae `ask` y `bid`
  distintos
- **THEN** el sistema usa exclusivamente `bid` para `tasaCambio = 1 / bid`

### Requirement: Conversión de un gasto en criptomoneda a USDT

Al registrar o editar un gasto cuya `moneda` es uno de los símbolos cripto
soportados distintos de `USDT` (`BTC`, `ETH`, `USDC`, `DAI`, `BNB`, `SOL`, `XRP`,
`ADA`, `AVAX`, `DOGE`, `TRX`, `LINK`, `DOT`, `MATIC`, `SHIB`, `LTC`, `BCH`, `EOS`,
`XLM`, `FTM`, `AAVE`, `UNI`, `ALGO`, `BAT`, `PAXG`, `CAKE`, `AXS`, `SLP`, `MANA`,
`SAND`, `CHZ`, `UXD`, `USDP`, `WLD`), el sistema SHALL consultar
`GET .../binancep2p/{moneda}/USD/1` y `GET .../binancep2p/USDT/USD/1`, tomar el
`bid` de cada una, calcular `tasaCambio = bidMoneda / bidUsdt` y
`montoUsdt = montoOriginal * tasaCambio`, y persistir `moneda`, `moneda_nombre`,
`monto_usdt` y `tasa_cambio`.

#### Scenario: Registro de un gasto cripto

- **WHEN** un miembro envía `POST /api/grupos/{id}/gastos` con `monto` `0.01`,
  `moneda` `BTC`, y CriptoYa responde `bid` `60000.00` para `BTC/USD` y `bid`
  `1.00` para `USDT/USD`
- **THEN** el sistema calcula `tasaCambio = 60000.00 / 1.00 = 60000` y
  `montoUsdt = 0.01 * 60000 = 600.000000`
- **AND** persiste el gasto con `monto` `0.01`, `moneda` `BTC` y
  `monto_usdt` `600.000000`

#### Scenario: La conversión cripto requiere las dos consultas

- **WHEN** se convierte un monto cripto
- **THEN** el sistema realiza ambas consultas (`{moneda}/USD` y `USDT/USD`) y
  combina sus `bid`; si cualquiera de las dos falla, la conversión falla

### Requirement: Gasto en USDT sin conversión externa

Cuando la `moneda` del gasto se omite en el request, o es exactamente `USDT`, el
sistema SHALL NOT consultar CriptoYa. En ese caso `tasa_cambio = 1` y
`monto_usdt` es igual a `monto` (el monto original), con 6 decimales. `moneda` se
persiste como `USDT`; `moneda_nombre` se persiste como `Tether` cuando el request
no envía `monedaNombre`.

#### Scenario: Registro sin campo moneda

- **WHEN** un miembro envía `POST /api/grupos/{id}/gastos` sin el campo `moneda`
- **THEN** el sistema no llama a ninguna API externa
- **AND** persiste el gasto con `moneda` `USDT`, `moneda_nombre` `Tether`,
  `tasa_cambio` `1.000000` y `monto_usdt` igual al `monto`

#### Scenario: Registro con moneda USDT explícita

- **WHEN** un miembro envía `POST /api/grupos/{id}/gastos` con `moneda` `USDT`
- **THEN** el sistema no llama a CriptoYa y aplica `tasa_cambio = 1`,
  `monto_usdt = monto`

### Requirement: Recálculo de la conversión al editar un gasto

`PUT /api/grupos/{id}/gastos/{gastoId}` SHALL volver a resolver la conversión con
la tasa vigente al momento de la edición: si la `moneda` resultante no es `USDT`,
consulta CriptoYa de nuevo y recalcula `monto_usdt` y `tasa_cambio`; luego
recalcula la división en USDT entre los miembros actuales del grupo. El
`monto_usdt` y la `tasa_cambio` anteriores no se conservan.

#### Scenario: Edición que cambia el monto

- **WHEN** un miembro edita un gasto en `BOB` cambiando el `monto` y CriptoYa
  responde una tasa distinta a la del registro
- **THEN** el sistema recalcula `monto_usdt` y `tasa_cambio` con la respuesta
  nueva y reemplaza la división con la nueva `monto_usdt`

#### Scenario: Edición que cambia la moneda a USDT

- **WHEN** un miembro edita un gasto poniendo `moneda` `USDT`
- **THEN** el sistema no llama a CriptoYa, fija `tasa_cambio = 1` y
  `monto_usdt = monto`, y recalcula la división

### Requirement: Símbolo de moneda no soportado

Si el request de registro o edición trae una `moneda` que no está en el conjunto
de fiats soportados ni en el de criptos soportados, el sistema SHALL responder
`400 Bad Request` con el formato de error estándar y no registrar ni modificar el
gasto. La validación del símbolo ocurre antes de cualquier llamada externa.

#### Scenario: Moneda desconocida

- **WHEN** un miembro envía `POST /api/grupos/{id}/gastos` con `moneda` `XYZ`
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar
- **AND** no se llama a CriptoYa y no se registra ningún gasto

### Requirement: CriptoYa no disponible

Si al convertir un gasto la llamada a CriptoYa no responde, agota el tiempo de
espera, o devuelve una respuesta que no es `2xx` o no contiene un `bid` numérico,
el sistema SHALL responder `503 Service Unavailable` con el formato de error
estándar y no registrar ni modificar el gasto.

#### Scenario: La API externa falla durante el registro

- **WHEN** un miembro envía `POST /api/grupos/{id}/gastos` con una `moneda`
  distinta de USDT y CriptoYa devuelve error o no responde
- **THEN** el sistema responde `503 Service Unavailable` con el formato de error
  estándar
- **AND** no se persiste ningún gasto ni fila de `gasto_participantes`

#### Scenario: La API externa falla durante la edición

- **WHEN** un miembro envía `PUT /api/grupos/{id}/gastos/{gastoId}` con una
  `moneda` distinta de USDT y CriptoYa falla
- **THEN** el sistema responde `503` y el gasto conserva sus valores anteriores

### Requirement: Persistencia del monto original y de los datos de conversión

El sistema SHALL conservar siempre el `monto` original en la `moneda` original del
gasto; la conversión no lo sobrescribe. Cada gasto SHALL almacenar `moneda`
(símbolo, hasta 10 caracteres), `moneda_nombre` (hasta 50 caracteres, tal como lo
envía el cliente en `monedaNombre`, o el símbolo si el request no lo trae),
`monto_usdt` (6 decimales) y `tasa_cambio` (6 decimales).

#### Scenario: El monto original se preserva tras la conversión

- **WHEN** se registra un gasto de `800.00` `BOB`
- **THEN** el gasto almacenado tiene `monto` `800.00` y `moneda` `BOB`, además del
  `monto_usdt` convertido

#### Scenario: moneda_nombre se guarda tal cual lo envía el cliente

- **WHEN** un miembro envía `POST /api/grupos/{id}/gastos` con `moneda` `BOB` y
  `monedaNombre` `Boliviano`
- **THEN** el gasto almacenado tiene `moneda_nombre` `Boliviano`

### Requirement: Balances y liquidación permanecen en USDT

`GET /api/grupos/{id}/balances` y `GET /api/grupos/{id}/liquidacion` SHALL
calcularse íntegramente en USDT: lo adeudado por cada participante es la suma de
sus `monto_adeudado` (ya en USDT) y lo pagado es la suma del `monto_usdt` de los
gastos que pagó (redondeado a 2 decimales, la misma escala que `monto_adeudado`),
**no** el `monto` en la moneda original. La suma de todos los balances SHALL
seguir siendo exactamente `0.00`. No se expone `tasa_cambio` ni `moneda` en estas
respuestas.

#### Scenario: Balances de un grupo con gastos en varias monedas

- **WHEN** un grupo tiene gastos registrados en `BOB`, `USD` y `BTC`, cada uno ya
  convertido a USDT
- **THEN** `GET /api/grupos/{id}/balances` devuelve el balance de cada
  participante en USDT y su suma es exactamente `0.00`
- **AND** ninguna entrada incluye `tasa_cambio` ni `moneda`
