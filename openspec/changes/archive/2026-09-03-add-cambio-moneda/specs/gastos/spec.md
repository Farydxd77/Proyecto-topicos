## MODIFIED Requirements

### Requirement: Registrar un gasto de un grupo

El sistema SHALL exponer `POST /api/grupos/{id}/gastos` que, para un miembro
autenticado del grupo, registra un gasto con `descripcion`, `monto`, `pagadorId`,
`fecha` y, opcionalmente, `moneda` y `monedaNombre`. `descripcion` MUST ser una
cadena no vacía de hasta 255 caracteres; `monto` MUST ser un número mayor que 0
con hasta 8 decimales; `fecha` MUST ser una fecha válida (`YYYY-MM-DD`);
`pagadorId` MUST identificar a un participante que sea miembro del grupo. Si
`moneda` se omite se asume `USDT`; si se envía, MUST ser un símbolo soportado
(fiat o cripto) — en caso contrario el sistema responde `400`.

El sistema SHALL convertir el `monto` a USDT según la capacidad `cambio-moneda`
(sin llamada externa cuando la moneda es `USDT`), SHALL calcular la división del
`montoUsdt` resultante entre todos los miembros actuales del grupo (las filas de
`gasto_participantes` quedan en USDT, redondeadas a 2 decimales, con el pagador
absorbiendo el centavo sobrante), y SHALL responder `201 Created` con el gasto
creado: `id`, `grupoId`, `descripcion`, `monto` (original), `moneda`,
`monedaNombre`, `montoUsdt`, `tasaCambio`, `pagador`, `fecha` y la lista
`division` de pares `participante` + `montoAdeudado` (en USDT).

#### Scenario: Registro válido divide el monto entre todos los miembros

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con `monto`
  mayor que 0 y un `pagadorId` que es miembro del grupo
- **THEN** el sistema persiste el gasto y una fila de `gasto_participantes` por cada
  miembro actual del grupo
- **AND** responde `201 Created` con el gasto (incluidos `moneda`, `monedaNombre`,
  `montoUsdt`, `tasaCambio`) y su `division`
- **AND** la suma de los `montoAdeudado` de la `division` es exactamente igual al
  `montoUsdt` del gasto redondeado a 2 decimales

#### Scenario: Registro con moneda distinta de USDT

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con `moneda`
  `BOB` y `monedaNombre` `Boliviano`
- **THEN** el sistema convierte el `monto` a USDT vía CriptoYa y persiste `monto`
  (en BOB), `moneda` `BOB`, `monedaNombre` `Boliviano`, `montoUsdt` y `tasaCambio`
- **AND** la `division` se calcula sobre `montoUsdt`

#### Scenario: Registro sin campo moneda

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` sin `moneda`
- **THEN** el gasto se registra como USDT (`tasaCambio` `1`, `montoUsdt` igual al
  `monto`) sin consultar ninguna API externa

#### Scenario: Moneda no soportada

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con una
  `moneda` que no es un símbolo fiat ni cripto soportado
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar y
  no registra ningún gasto

#### Scenario: CriptoYa no disponible

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con una
  `moneda` distinta de USDT y la conversión vía CriptoYa falla
- **THEN** el sistema responde `503 Service Unavailable` con el formato de error
  estándar y no registra ningún gasto

#### Scenario: Monto menor o igual a cero

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con `monto`
  igual a 0 o negativo
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar y
  no registra ningún gasto

#### Scenario: El pagador no es miembro del grupo

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con un
  `pagadorId` que no corresponde a ningún miembro del grupo (o que no existe)
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar y
  no registra ningún gasto

#### Scenario: Campos obligatorios ausentes o inválidos

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con
  `descripcion` vacía, `fecha` ausente o `pagadorId` ausente
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar

#### Scenario: El usuario no es miembro del grupo

- **WHEN** un usuario autenticado que no es miembro del grupo envía
  `POST /api/grupos/{id}/gastos`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar

#### Scenario: El grupo no existe

- **WHEN** un usuario autenticado envía `POST /api/grupos/{id}/gastos` para un
  `id` de grupo que no existe
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: Petición sin token

- **WHEN** se envía `POST /api/grupos/{id}/gastos` sin un token JWT válido
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar

### Requirement: Editar un gasto y recalcular su división

El sistema SHALL exponer `PUT /api/grupos/{id}/gastos/{gastoId}` que, para un
miembro autenticado del grupo, reemplaza `descripcion`, `monto`, `pagadorId` y
`fecha` del gasto indicado y, opcionalmente, `moneda` y `monedaNombre`. Los
cuatro primeros campos son obligatorios y se validan igual que en el registro;
`moneda` omitida se interpreta como `USDT`. El sistema SHALL volver a resolver la
conversión a USDT con la tasa vigente al momento de la edición (según la capacidad
`cambio-moneda`), SHALL descartar la división anterior y SHALL recalcular la
división del nuevo `montoUsdt` entre **los miembros actuales del grupo**,
aplicando la misma regla de redondeo. El sistema SHALL responder `200 OK` con el
gasto actualizado (incluidos `moneda`, `monedaNombre`, `montoUsdt`, `tasaCambio`)
y su nueva `division`.

#### Scenario: Edición válida recalcula la división

- **WHEN** un miembro autenticado envía `PUT /api/grupos/{id}/gastos/{gastoId}` con
  un `monto` nuevo y datos válidos
- **THEN** el sistema recalcula `montoUsdt` y `tasaCambio` con la tasa del momento
  y reemplaza las filas de `gasto_participantes` por la división del nuevo
  `montoUsdt` entre los miembros actuales
- **AND** responde `200 OK` con el gasto actualizado
- **AND** la suma de los `montoAdeudado` de la nueva `division` es igual al nuevo
  `montoUsdt` redondeado a 2 decimales

#### Scenario: Edición con moneda no soportada o CriptoYa caído

- **WHEN** un miembro autenticado envía `PUT /api/grupos/{id}/gastos/{gastoId}` con
  una `moneda` no soportada, o con una `moneda` distinta de USDT y CriptoYa falla
- **THEN** el sistema responde `400` (moneda no soportada) o `503` (CriptoYa
  caído) con el formato de error estándar y no modifica el gasto ni su división

#### Scenario: Monto inválido o pagador no miembro en la edición

- **WHEN** un miembro autenticado envía `PUT /api/grupos/{id}/gastos/{gastoId}` con
  `monto` menor o igual a 0, o con un `pagadorId` que no es miembro del grupo
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar y
  no modifica el gasto ni su división

#### Scenario: El gasto no existe o no pertenece al grupo

- **WHEN** un miembro autenticado envía `PUT /api/grupos/{id}/gastos/{gastoId}`
  para un `gastoId` que no existe o pertenece a otro grupo
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: El usuario no es miembro del grupo

- **WHEN** un usuario autenticado que no es miembro del grupo envía
  `PUT /api/grupos/{id}/gastos/{gastoId}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar
