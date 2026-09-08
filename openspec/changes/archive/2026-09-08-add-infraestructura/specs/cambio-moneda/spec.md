## ADDED Requirements

### Requirement: Caché de cotizaciones

El sistema SHALL cachear en memoria cada cotización obtenida de CriptoYa, indexada
por el par consultado. Mientras una cotización cacheada sea más reciente que
`criptoya.cache-ttl` (60 segundos por defecto), el sistema SHALL reutilizarla y MUST
NOT hacer una llamada externa. Superado ese tiempo, el sistema SHALL volver a
consultar CriptoYa y refrescar la entrada.

La caché SHALL vivir únicamente en memoria: reiniciar el backend la vacía. La caché
MUST NOT alterar la tasa que se persiste en el gasto, que SHALL ser siempre la que
efectivamente se aplicó al convertir.

#### Scenario: Dos gastos seguidos en la misma moneda

- **WHEN** se registran dos gastos en la misma moneda fiat dentro del tiempo de
  vigencia de la caché
- **THEN** el sistema consulta CriptoYa una sola vez
- **AND** los dos gastos quedan con la misma `tasaCambio`

#### Scenario: Cotización vencida

- **WHEN** se registra un gasto en una moneda cuya cotización cacheada superó el
  tiempo de vigencia
- **THEN** el sistema consulta CriptoYa de nuevo
- **AND** el gasto queda con la tasa recién obtenida

#### Scenario: Monedas distintas no comparten entrada

- **WHEN** se registra un gasto en una moneda fiat y luego otro en una moneda fiat
  distinta
- **THEN** el sistema consulta CriptoYa para cada par
- **AND** cada gasto queda con la tasa de su propia moneda

#### Scenario: USDT no toca la caché

- **WHEN** se registra un gasto en USDT
- **THEN** el sistema no consulta CriptoYa ni consulta la caché
- **AND** la `tasaCambio` es `1`

## MODIFIED Requirements

### Requirement: CriptoYa no disponible

Si al convertir un gasto la llamada a CriptoYa no responde, agota el tiempo de
espera, o devuelve una respuesta que no es `2xx` o no contiene un `bid` numérico, el
sistema SHALL intentar degradar antes de fallar:

- Si existe una cotización cacheada de ese mismo par con una antigüedad menor o igual
  a `criptoya.cache-max-stale` (24 horas por defecto), el sistema SHALL usarla y
  completar la operación normalmente. La `tasaCambio` que se persiste SHALL ser la de
  esa cotización, de modo que el gasto refleje la tasa realmente aplicada.
- Si no existe ninguna cotización cacheada de ese par, o la que hay superó esa
  antigüedad, el sistema SHALL responder `503 Service Unavailable` con el formato de
  error estándar y no registrar ni modificar el gasto.

El sistema MUST NOT inventar una tasa fija ni codificada cuando no tiene ninguna
cotización previa de ese par.

#### Scenario: La API externa falla durante el registro

- **WHEN** un miembro envía `POST /api/grupos/{id}/gastos` con una `moneda` distinta
  de USDT, CriptoYa devuelve error o no responde, y no hay ninguna cotización
  cacheada utilizable de ese par
- **THEN** el sistema responde `503 Service Unavailable` con el formato de error
  estándar
- **AND** no se persiste ningún gasto ni fila de `gasto_participantes`

#### Scenario: La API externa falla y no hay cotización previa

- **WHEN** un miembro envía `POST /api/grupos/{id}/gastos` con una `moneda` distinta
  de USDT, CriptoYa devuelve error o no responde, y el sistema no tiene ninguna
  cotización cacheada de ese par
- **THEN** el sistema responde `503 Service Unavailable` con el formato de error
  estándar
- **AND** no se persiste ningún gasto ni fila de `gasto_participantes`

#### Scenario: La API externa falla habiendo una cotización reciente

- **WHEN** un miembro registra un gasto en una moneda cuya cotización ya está
  cacheada, y CriptoYa falla al intentar refrescarla
- **THEN** el sistema registra el gasto usando la cotización cacheada
- **AND** la `tasaCambio` persistida es la de esa cotización

#### Scenario: La cotización cacheada es demasiado vieja

- **WHEN** CriptoYa falla y la única cotización cacheada de ese par supera la
  antigüedad máxima tolerada
- **THEN** el sistema responde `503 Service Unavailable` con el formato de error
  estándar
- **AND** no se persiste ningún gasto

#### Scenario: La API externa falla durante la edición

- **WHEN** un miembro envía `PUT /api/grupos/{id}/gastos/{gastoId}` con una `moneda`
  distinta de USDT, CriptoYa falla y no hay cotización cacheada utilizable
- **THEN** el sistema responde `503` y el gasto conserva sus valores anteriores
