## MODIFIED Requirements

### Requirement: Registrar un gasto de un grupo

El sistema SHALL exponer `POST /api/grupos/{id}/gastos` que, para un miembro
autenticado del grupo, registra un gasto con `descripcion`, `monto`, `pagadorId`,
`fecha` y, opcionalmente, `moneda`, `monedaNombre` y `division`. `descripcion` MUST
ser una cadena no vacía de hasta 255 caracteres; `monto` MUST ser un número mayor
que 0 con hasta 8 decimales; `fecha` MUST ser una fecha válida (`YYYY-MM-DD`);
`pagadorId` MUST identificar a un participante que sea miembro del grupo. Si
`moneda` se omite se asume `USDT`; si se envía, MUST ser un símbolo soportado
(fiat o cripto) — en caso contrario el sistema responde `400`.

El campo `division`, cuando se envía, SHALL ser una lista no vacía de pares
`{ participanteId, peso }` que determina entre quiénes y en qué proporción se
reparte el gasto. Cada `participanteId` MUST identificar a un miembro actual del
grupo y MUST NOT repetirse; cada `peso` MUST ser un entero entre 1 y 1000. Cuando
`division` se omite o llega como `null`, el sistema SHALL repartir el gasto entre
**todos** los miembros actuales del grupo en partes iguales, que es el
comportamiento por defecto.

El pagador MUST ser miembro del grupo, pero NO SHALL estar obligado a figurar en la
`division`: puede haber pagado algo que no consume.

El sistema SHALL convertir el `monto` a USDT según la capacidad `cambio-moneda`
(sin llamada externa cuando la moneda es `USDT`), SHALL calcular la división del
`montoUsdt` resultante (las filas de `gasto_participantes` quedan en USDT,
redondeadas a 2 decimales, guardando también el `peso` aplicado), y SHALL responder
`201 Created` con el gasto creado: `id`, `grupoId`, `descripcion`, `monto`
(original), `moneda`, `monedaNombre`, `montoUsdt`, `tasaCambio`, `pagador`, `fecha`
y la lista `division` de tripletas `participante` + `montoAdeudado` (en USDT) +
`peso`.

#### Scenario: Registro válido divide el monto entre todos los miembros

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con `monto`
  mayor que 0, un `pagadorId` que es miembro del grupo y sin `division`
- **THEN** el sistema persiste el gasto y una fila de `gasto_participantes` por cada
  miembro actual del grupo, todas con `peso` `1`
- **AND** responde `201 Created` con el gasto (incluidos `moneda`, `monedaNombre`,
  `montoUsdt`, `tasaCambio`) y su `division`
- **AND** la suma de los `montoAdeudado` de la `division` es exactamente igual al
  `montoUsdt` del gasto redondeado a 2 decimales

#### Scenario: Registro que excluye a un miembro

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` en un grupo
  de 4 miembros con una `division` que lista solo a 3 de ellos, todos con `peso` `1`
- **THEN** el sistema persiste una fila de `gasto_participantes` solo para esos 3
- **AND** el miembro excluido no aparece en la `division` de la respuesta ni adeuda
  nada por ese gasto
- **AND** la suma de los `montoAdeudado` sigue siendo igual al `montoUsdt`

#### Scenario: Registro con pesos desiguales

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con `monto`
  `300.00` en USDT y una `division` de dos participantes con pesos `2` y `1`
- **THEN** el primero adeuda `200.00` y el segundo `100.00`
- **AND** cada entrada de la `division` devuelve el `peso` que se aplicó

#### Scenario: El pagador queda fuera de la división

- **WHEN** un miembro autenticado registra un gasto cuyo `pagadorId` no figura en la
  `division`
- **THEN** el sistema responde `201 Created` y el pagador no adeuda nada por ese
  gasto
- **AND** la suma de los `montoAdeudado` sigue siendo igual al `montoUsdt`
- **AND** el balance del pagador sube por el total del gasto

#### Scenario: División vacía

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con
  `division` igual a una lista vacía
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar y
  no registra ningún gasto

#### Scenario: División con un participante repetido

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con una
  `division` que incluye dos veces el mismo `participanteId`
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar y
  no registra ningún gasto

#### Scenario: División con alguien que no es miembro del grupo

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con una
  `division` que incluye a un participante que no pertenece al grupo (o que no
  existe)
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar y
  no registra ningún gasto

#### Scenario: División con un peso fuera de rango

- **WHEN** un miembro autenticado envía `POST /api/grupos/{id}/gastos` con una
  `division` donde algún `peso` es `0`, negativo, mayor que `1000` o ausente
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar y
  no registra ningún gasto

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

Al registrar o editar un gasto **sin** `division` explícita, el sistema SHALL
repartir el `monto` entre los `n` miembros actuales del grupo así: cada miembro
distinto del pagador adeuda `montoPorPersona = monto / n` redondeado a 2 decimales
(modo HALF_UP), y el pagador adeuda `monto - montoPorPersona * (n - 1)`.

Al registrar o editar un gasto **con** `division` explícita, el sistema SHALL
repartir el monto en proporción al peso: siendo `pesoTotal` la suma de los pesos,
cada participante distinto del absorbente adeuda `monto * peso / pesoTotal`
redondeado a 2 decimales (modo HALF_UP), y el absorbente adeuda el monto menos la
suma de los demás.

El **absorbente** SHALL determinarse así, en orden: el pagador si figura en la
división; si no figura, el participante de mayor peso; a igualdad de peso, el de
menor `id`. El criterio SHALL ser determinista, de modo que repetir la misma
operación produzca siempre el mismo reparto.

En todos los casos, la suma de todos los `montoAdeudado` MUST ser exactamente igual
al `monto` del gasto en USDT redondeado a 2 decimales, incluidos los montos que no
son divisibles de forma exacta. Si el reparto queda en un solo participante, ese
participante adeuda el `monto` completo.

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

#### Scenario: Reparto proporcional exacto

- **WHEN** se registra un gasto de `300.00` con una división de pesos `2` y `1`
- **THEN** el participante de peso `2` adeuda `200.00` y el de peso `1` adeuda
  `100.00`
- **AND** la suma de lo adeudado es `300.00`

#### Scenario: Reparto proporcional con redondeo, pagador incluido

- **WHEN** se registra un gasto de `100.00` con una división de tres participantes
  de peso `1` cada uno, uno de los cuales es el pagador
- **THEN** los dos que no son el pagador adeudan `33.33` cada uno y el pagador
  adeuda `33.34`
- **AND** la suma de lo adeudado es `100.00`

#### Scenario: Reparto con redondeo y pagador excluido

- **WHEN** se registra un gasto de `100.00` cuyo pagador NO figura en la división,
  repartido entre tres participantes con pesos `3`, `1` y `1`
- **THEN** el participante de mayor peso absorbe el sobrante del redondeo
- **AND** la suma de lo adeudado es exactamente `100.00`

#### Scenario: Empate de pesos con el pagador excluido

- **WHEN** se registra un gasto cuyo pagador no figura en la división y todos los
  participantes tienen el mismo peso
- **THEN** absorbe el sobrante el participante de menor `id`
- **AND** repetir la misma operación produce el mismo reparto

### Requirement: Consultar el detalle de un gasto

El sistema SHALL exponer `GET /api/grupos/{id}/gastos/{gastoId}` que, para un
miembro autenticado del grupo, devuelve `200 OK` con los datos del gasto (`id`,
`grupoId`, `descripcion`, `monto`, `pagador`, `fecha`) y su `division` (lista de
tripletas `participante` + `montoAdeudado` + `peso`) tal como quedó registrada. El
`peso` devuelto SHALL ser el que se aplicó al calcular el reparto, de modo que un
cliente pueda reconstruir cómo se dividió el gasto sin deducirlo de los montos.
Ninguna respuesta SHALL incluir la contraseña de ningún usuario.

#### Scenario: El gasto existe y pertenece al grupo

- **WHEN** un miembro autenticado envía `GET /api/grupos/{id}/gastos/{gastoId}` y
  ese gasto pertenece al grupo `id`
- **THEN** el sistema responde `200 OK` con el gasto y su `division`
- **AND** cada entrada de la `division` incluye su `peso`
- **AND** el cuerpo no contiene ningún campo de contraseña

#### Scenario: Un gasto registrado sin división explícita

- **WHEN** un miembro autenticado consulta el detalle de un gasto que se registró
  sin `division`
- **THEN** todas las entradas de su `division` traen `peso` `1`

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
`fecha` del gasto indicado y, opcionalmente, `moneda`, `monedaNombre` y `division`.
Los cuatro primeros campos son obligatorios y se validan igual que en el registro;
`moneda` omitida se interpreta como `USDT`. El campo `division` se valida con las
mismas reglas que en el registro y, si se omite, la edición SHALL volver al reparto
equitativo entre todos los miembros actuales del grupo: la división anterior NO
SHALL conservarse de forma implícita.

El sistema SHALL volver a resolver la conversión a USDT con la tasa vigente al
momento de la edición (según la capacidad `cambio-moneda`), SHALL descartar la
división anterior y SHALL recalcular la división del nuevo `montoUsdt` aplicando la
misma regla de redondeo. El sistema SHALL responder `200 OK` con el gasto
actualizado (incluidos `moneda`, `monedaNombre`, `montoUsdt`, `tasaCambio`) y su
nueva `division`.

#### Scenario: Edición válida recalcula la división

- **WHEN** un miembro autenticado envía `PUT /api/grupos/{id}/gastos/{gastoId}` con
  un `monto` nuevo y datos válidos
- **THEN** el sistema recalcula `montoUsdt` y `tasaCambio` con la tasa del momento
  y reemplaza las filas de `gasto_participantes` por la división del nuevo
  `montoUsdt` entre los miembros actuales
- **AND** responde `200 OK` con el gasto actualizado
- **AND** la suma de los `montoAdeudado` de la nueva `division` es igual al nuevo
  `montoUsdt` redondeado a 2 decimales

#### Scenario: Edición que cambia la división

- **WHEN** un miembro autenticado edita un gasto enviando una `division` distinta de
  la que tenía
- **THEN** el sistema reemplaza las filas de `gasto_participantes` por la nueva
  división, con sus nuevos pesos
- **AND** los participantes que salieron de la división dejan de adeudar por ese
  gasto

#### Scenario: Edición sin división vuelve al reparto equitativo

- **WHEN** un miembro autenticado edita un gasto que tenía una `division`
  personalizada y envía la edición **sin** el campo `division`
- **THEN** el gasto queda repartido en partes iguales entre todos los miembros
  actuales del grupo, con `peso` `1` para cada uno

#### Scenario: Edición con una división inválida

- **WHEN** un miembro autenticado envía `PUT /api/grupos/{id}/gastos/{gastoId}` con
  una `division` vacía, con un participante repetido, con alguien que no es miembro
  del grupo, o con un peso fuera de rango
- **THEN** el sistema responde `400 Bad Request` con el formato de error estándar y
  no modifica el gasto ni su división

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
