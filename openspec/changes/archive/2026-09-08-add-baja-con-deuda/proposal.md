## Why

Hoy alguien puede salir de un grupo debiendo plata y el sistema no hace nada al
respecto: su deuda queda flotando en los balances con la marca «ya no integra el
grupo», y ahí se queda para siempre. La liquidación sigue proponiendo transferencias
hacia o desde una persona que ya no está, y el grupo no tiene forma de cerrar el
tema.

En la vida real el grupo decide una de dos cosas: **o se hacen cargo entre todos** —y
la deuda del que se fue se reparte— **o no se hacen cargo**, y esa deuda queda
apartada como un tema abierto que arreglarán por su cuenta. Ninguna de las dos se
puede expresar en la aplicación.

## What Changes

- **Nueva tabla `bajas_grupo`**: cuando alguien deja un grupo con saldo distinto de
  cero, el sistema registra la baja con el saldo que tenía y el estado `PENDIENTE`.
  Si el saldo es cero no se registra nada: no hay nada que decidir.
- **Nueva tabla `baja_participantes`**: cuando el grupo asume la deuda, congela quién
  absorbe cuánto, igual que `gasto_participantes` congela la división de un gasto.
- **Nuevos endpoints**:
  - `GET /api/grupos/{id}/bajas` — lista las bajas del grupo con su estado. Cualquier
    miembro puede consultarlas.
  - `PUT /api/grupos/{id}/bajas/{bajaId}` — reservado al creador, con cuerpo
    `{ "asumir": true | false }`, resuelve una baja pendiente.
- **Si el grupo asume** (`asumir: true`): el saldo del que se fue se reparte en
  partes iguales entre los miembros actuales, su balance pasa a `0.00` y desaparece
  de la liquidación. El reparto queda congelado: agregar o quitar miembros después no
  lo recalcula.
- **Si el grupo no asume** (`asumir: false`): la baja queda en estado `NO_ASUMIDA` y
  el saldo sigue tal cual en los balances, pero ahora identificado como un tema
  abierto en un apartado propio, en lugar de mezclado con los balances de los que
  siguen.
- **`GET /api/grupos/{id}/balances` incorpora las bajas asumidas** al cálculo. La
  invariante de que la suma da exactamente `0.00` se mantiene.
- **Frontend**: nueva sección «Bajas del grupo» con las decisiones pendientes —que
  solo el creador puede resolver, con confirmación que muestre cuánto le queda a cada
  uno si asumen— y el apartado «Deudas sin resolver» con las que no se asumieron.

## Capabilities

### New Capabilities

- `bajas`: registro de la salida de un participante con saldo pendiente, la decisión
  del grupo sobre si asume o no esa deuda, y el reparto congelado cuando la asume.
- `frontend-bajas`: la interfaz de esas decisiones y el apartado de deudas sin
  resolver.

### Modified Capabilities

- `grupos`: quitar a un miembro —o que se vaya por su cuenta— con saldo distinto de
  cero pasa a registrar una baja pendiente de decisión.
- `balances`: el cálculo incorpora las bajas asumidas, que llevan a cero el balance
  del que se fue y lo reparten entre quienes lo asumieron.

## Impact

- **Backend nuevo**: `entity/BajaGrupo`, `entity/BajaParticipante` +
  `BajaParticipanteId`, `repository/BajaGrupoRepository`,
  `repository/BajaParticipanteRepository`, `dto/request/ResolverBajaRequest`,
  `dto/response/BajaGrupoDto` y `BajaParticipanteDto`, `service/BajaService`,
  `controller/BajaController`.
- **Backend modificado**: `service/GrupoService` (registrar la baja al quitar o
  abandonar), `service/BalanceService` (aplicar las bajas asumidas),
  `util/BalanceUtil` (una sobrecarga que reciba los ajustes).
- **Base de datos**: dos tablas nuevas que Hibernate crea con `ddl-auto=update`. Los
  grupos existentes no se ven afectados: sin filas en `bajas_grupo`, el cálculo de
  balances es exactamente el de hoy.
- **Frontend nuevo**: `api/bajas.ts`, `components/SeccionBajas.tsx`.
- **Frontend modificado**: `api/types.ts`, `lib/claves.ts`,
  `pages/GrupoDetallePage.tsx`.
- **Documentación**: `CLAUDE.md` y `openspec/specs/data-model.md`.

## Non-Goals

- **No hay votación de los miembros.** La decisión la toma el creador del grupo, que
  es quien ya tiene todas las atribuciones de administración. Si alguien abandona por
  su cuenta no hay nadie más en pantalla para votar en ese momento, así que la baja
  queda pendiente y el creador la resuelve después.
- No se reparte la deuda asumida en proporciones desiguales: se divide en partes
  iguales entre los miembros actuales. Repartirla con pesos sería reusar el mecanismo
  de división de gastos y no hay evidencia de que haga falta.
- No se puede deshacer una baja ya resuelta ni volver a discutirla: es un registro
  histórico. Para revertir el efecto habría que registrar gastos o pagos que lo
  compensen.
- No se reincorpora automáticamente a quien vuelve al grupo: si se lo agrega de
  nuevo, su baja anterior queda como está y su actividad nueva cuenta aparte.
- No se notifica a nadie de la baja ni de la decisión.
- No se cobra ni se persigue la deuda no asumida: el apartado es informativo, para
  que el grupo la tenga a la vista y la arregle por fuera.
- No se registra una baja cuando el saldo del que se va es exactamente `0.00`.
