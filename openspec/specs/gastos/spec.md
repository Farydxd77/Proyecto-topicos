# gastos Specification

## Purpose

Permitir que los miembros de un grupo registren, consulten, editen y eliminen los
gastos que comparten. Al registrar o editar un gasto, su monto se reparte de forma
equitativa entre todos los miembros del grupo en ese momento; el pagador absorbe el
centavo sobrante del redondeo, de modo que la suma de lo adeudado siempre iguala el
monto total. Los gastos ya registrados no se ven afectados por cambios posteriores
en la membresía del grupo.

## Requirements

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

### Requirement: Reparto equitativo con el pagador absorbiendo el redondeo

Al registrar o editar un gasto, el sistema SHALL repartir el `monto` entre los `n`
miembros actuales del grupo así: cada miembro distinto del pagador adeuda
`montoPorPersona = monto / n` redondeado a 2 decimales (modo HALF_UP), y el pagador
adeuda `monto - montoPorPersona * (n - 1)`. La suma de todos los `montoAdeudado`
MUST ser exactamente igual al `monto` del gasto en todos los casos, incluidos los
montos que no son divisibles de forma exacta. Si el grupo tiene un solo miembro (el
propio pagador), ese miembro adeuda el `monto` completo.

#### Scenario: División no exacta

- **WHEN** se registra un gasto de `100.00` en un grupo de 3 miembros
- **THEN** los dos miembros que no son el pagador adeudan `33.33` cada uno
- **AND** el pagador adeuda `33.34`
- **AND** la suma de lo adeudado es `100.00`

#### Scenario: División exacta

- **WHEN** se registra un gasto de `90.00` en un grupo de 3 miembros
- **THEN** cada uno de los 3 miembros adeuda `30.00`
- **AND** la suma de lo adeudado es `90.00`

#### Scenario: Grupo de un solo miembro

- **WHEN** un miembro que es el único integrante del grupo registra un gasto de
  `50.00` pagado por sí mismo
- **THEN** la `division` tiene una sola entrada con `montoAdeudado` igual a `50.00`

### Requirement: Listar los gastos de un grupo

El sistema SHALL exponer `GET /api/grupos/{id}/gastos` que, para un miembro
autenticado del grupo, devuelve `200 OK` con la lista de gastos del grupo ordenada
por `fecha` descendente. Cada elemento SHALL incluir `id`, `descripcion`, `monto`,
`pagador` y `fecha`. Si el grupo no tiene gastos, el sistema SHALL devolver
`200 OK` con `[]`.

#### Scenario: El grupo tiene gastos

- **WHEN** un miembro autenticado envía `GET /api/grupos/{id}/gastos` y el grupo
  tiene al menos un gasto
- **THEN** el sistema responde `200 OK` con un array de gastos ordenado por `fecha`
  de más reciente a más antigua

#### Scenario: El grupo no tiene gastos

- **WHEN** un miembro autenticado envía `GET /api/grupos/{id}/gastos` y el grupo no
  tiene ningún gasto
- **THEN** el sistema responde `200 OK` con `[]`

#### Scenario: El usuario no es miembro del grupo

- **WHEN** un usuario autenticado que no es miembro del grupo envía
  `GET /api/grupos/{id}/gastos`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar

#### Scenario: El grupo no existe

- **WHEN** un usuario autenticado envía `GET /api/grupos/{id}/gastos` para un `id`
  de grupo que no existe
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

### Requirement: Consultar el detalle de un gasto

El sistema SHALL exponer `GET /api/grupos/{id}/gastos/{gastoId}` que, para un
miembro autenticado del grupo, devuelve `200 OK` con los datos del gasto (`id`,
`grupoId`, `descripcion`, `monto`, `pagador`, `fecha`) y su `division` (lista de
pares `participante` + `montoAdeudado`) tal como quedó registrada. Ninguna
respuesta SHALL incluir la contraseña de ningún usuario.

#### Scenario: El gasto existe y pertenece al grupo

- **WHEN** un miembro autenticado envía `GET /api/grupos/{id}/gastos/{gastoId}` y
  ese gasto pertenece al grupo `id`
- **THEN** el sistema responde `200 OK` con el gasto y su `division`
- **AND** el cuerpo no contiene ningún campo de contraseña

#### Scenario: El gasto no existe o no pertenece al grupo

- **WHEN** un miembro autenticado envía `GET /api/grupos/{id}/gastos/{gastoId}`
  para un `gastoId` que no existe o que pertenece a otro grupo
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: El usuario no es miembro del grupo

- **WHEN** un usuario autenticado que no es miembro del grupo envía
  `GET /api/grupos/{id}/gastos/{gastoId}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar

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

### Requirement: Eliminar un gasto

El sistema SHALL exponer `DELETE /api/grupos/{id}/gastos/{gastoId}` que, para un
miembro autenticado del grupo, elimina el gasto indicado y todas sus filas en
`gasto_participantes`, y SHALL responder `204 No Content` sin cuerpo. Un miembro
PUEDE eliminar un gasto aunque no sea su pagador.

#### Scenario: Eliminación válida

- **WHEN** un miembro autenticado envía `DELETE /api/grupos/{id}/gastos/{gastoId}`
  para un gasto que pertenece al grupo
- **THEN** el sistema elimina el gasto y sus filas de `gasto_participantes`
- **AND** responde `204 No Content` sin cuerpo
- **AND** un `GET /api/grupos/{id}/gastos/{gastoId}` posterior responde
  `404 Not Found`

#### Scenario: El gasto no existe o no pertenece al grupo

- **WHEN** un miembro autenticado envía `DELETE /api/grupos/{id}/gastos/{gastoId}`
  para un `gastoId` que no existe o pertenece a otro grupo
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: El usuario no es miembro del grupo

- **WHEN** un usuario autenticado que no es miembro del grupo envía
  `DELETE /api/grupos/{id}/gastos/{gastoId}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar

### Requirement: Los gastos históricos no se ven afectados por cambios de membresía

La división de un gasto SHALL quedar fijada con los miembros del grupo existentes
al momento de registrarlo o editarlo. Agregar o quitar miembros del grupo después
NO SHALL modificar la división de gastos ya registrados; solo los gastos
registrados o editados después del cambio de membresía reflejan la nueva
composición.

#### Scenario: Un nuevo miembro no altera gastos anteriores

- **WHEN** se registra un gasto en un grupo de 2 miembros y luego se agrega un
  tercer miembro al grupo
- **THEN** la `division` del gasto anterior sigue teniendo 2 entradas y los mismos
  `montoAdeudado`
- **AND** un gasto registrado después del alta se divide entre los 3 miembros

### Requirement: Acceso restringido a miembros del grupo

Todos los endpoints bajo `/api/grupos/{id}/gastos` SHALL exigir un token JWT
válido y SHALL resolver al participante solicitante a partir del token. El sistema
SHALL evaluar la existencia del grupo antes que la membresía: un `id` de grupo
inexistente devuelve `404 Not Found` aunque el solicitante no sea miembro, y un
grupo existente con un solicitante que no es miembro devuelve `403 Forbidden`.
Cualquier miembro del grupo PUEDE ejecutar cualquiera de las operaciones de gasto
(no hay distinción entre creador del grupo, pagador y demás miembros).

#### Scenario: Orden de comprobación 404 antes que 403

- **WHEN** un usuario autenticado que no es miembro envía cualquier operación de
  gasto sobre un `id` de grupo que no existe
- **THEN** el sistema responde `404 Not Found`, no `403 Forbidden`

#### Scenario: Cualquier miembro opera sobre cualquier gasto

- **WHEN** un miembro del grupo edita o elimina un gasto cuyo pagador es otro
  miembro
- **THEN** el sistema ejecuta la operación y no responde `403 Forbidden`

#### Scenario: Petición sin token

- **WHEN** se envía cualquier operación bajo `/api/grupos/{id}/gastos` sin un token
  JWT válido
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar
