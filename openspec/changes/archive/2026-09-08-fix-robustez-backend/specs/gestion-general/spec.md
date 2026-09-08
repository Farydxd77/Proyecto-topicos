## MODIFIED Requirements

### Requirement: Buscar participantes por nombre, apellido o CI

El sistema SHALL aceptar en `GET /api/participantes` los parámetros de query
opcionales `nombre`, `apellido` y `ci`. `nombre` y `apellido` SHALL filtrar por
coincidencia parcial sin distinguir mayúsculas; `ci` SHALL filtrar por coincidencia
exacta. El filtro por `ci` SHALL devolver **todos** los participantes con ese CI: la
columna `participantes.ci` no es única y varios participantes pueden compartirlo,
sin que eso produzca un error del servidor. En todos los casos el sistema SHALL
devolver `200 OK` con un array JSON de los participantes que coinciden, o `[]` si no
hay coincidencias. Cuando se envía más de uno de estos parámetros, el sistema SHALL
aplicar exactamente uno con la precedencia `ci` > `nombre` > `apellido` e ignorar
los demás.

#### Scenario: Búsqueda por nombre parcial

- **WHEN** un usuario autenticado envía `GET /api/participantes?nombre={texto}` y al
  menos un participante contiene ese texto en su `nombre` (ignorando mayúsculas)
- **THEN** el sistema responde `200 OK` con el array de participantes que coinciden

#### Scenario: Búsqueda por apellido parcial

- **WHEN** un usuario autenticado envía `GET /api/participantes?apellido={texto}` y
  al menos un participante contiene ese texto en su `apellido` (ignorando
  mayúsculas)
- **THEN** el sistema responde `200 OK` con el array de participantes que coinciden

#### Scenario: Búsqueda por CI exacto

- **WHEN** un usuario autenticado envía `GET /api/participantes?ci={texto}` y existe
  un participante con ese `ci`
- **THEN** el sistema responde `200 OK` con un array que contiene ese participante

#### Scenario: Varios participantes comparten el mismo CI

- **WHEN** un usuario autenticado envía `GET /api/participantes?ci={texto}` y existe
  más de un participante con ese `ci`
- **THEN** el sistema responde `200 OK` con un array que contiene a todos ellos
- **AND** el sistema NO responde un error del servidor

#### Scenario: Búsqueda sin coincidencias

- **WHEN** un usuario autenticado filtra por `nombre`, `apellido` o `ci` y ningún
  participante coincide
- **THEN** el sistema responde `200 OK` con `[]`

#### Scenario: Se envían varios parámetros de filtro

- **WHEN** un usuario autenticado envía `GET /api/participantes` con `ci` y también
  `nombre` y/o `apellido`
- **THEN** el sistema aplica solo el filtro por `ci` e ignora `nombre` y `apellido`
