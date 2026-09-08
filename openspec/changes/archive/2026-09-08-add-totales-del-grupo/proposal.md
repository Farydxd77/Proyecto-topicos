## Why

La aplicación muestra el detalle de cada gasto y el balance de cada persona, pero
nunca responde la pregunta más simple que se hace un grupo de viaje: **¿cuánto
gastamos en total?** Hoy hay que sumar los gastos a mano de la lista.

Tampoco hay forma de ver el avance de la liquidación. Los balances dicen quién debe
cuánto, pero no cuánto se saldó ya ni cuánto falta, así que no se percibe el progreso
a medida que la gente va pagando.

## What Changes

- **Nuevo endpoint `GET /api/grupos/{id}/resumen`** que devuelve, en USDT:
  - `totalGastado`: suma de los `montoUsdt` de todos los gastos del grupo.
  - `totalPagado`: suma de los montos de todos los pagos registrados.
  - `pendientePorSaldar`: suma de los balances positivos, es decir lo que todavía
    hay que transferir para que todos queden a mano.
  - `miParte`: lo que le corresponde adeudar al participante que consulta, sumando
    su `montoAdeudado` en todos los gastos.
  - `cantidadGastos` y `cantidadPagos`.
- **Frontend**: un panel de totales al principio del detalle del grupo, con «Total
  del viaje», «Ya saldado» y «Falta saldar», más una barra de progreso de la
  liquidación y el dato de cuánto le tocó a quien mira.

**Aclaración de contabilidad que este cambio deja explícita en la interfaz:**
`totalPagado` **no** converge a `totalGastado` y no debe hacerlo. Quien paga un gasto
ya cubrió su propia parte y nunca se transfiere dinero a sí mismo, así que esa parte
jamás aparece como pago. Lo que sí cierra es `totalPagado + pendientePorSaldar`
respecto de la deuda inicial del grupo: la barra de progreso llega al 100% cuando
`pendientePorSaldar` es `0`, no cuando `totalPagado` iguala a `totalGastado`.

## Capabilities

### New Capabilities

- `resumen-grupo`: los totales agregados de un grupo —cuánto se gastó, cuánto se
  saldó, cuánto falta y cuánto le toca a quien consulta— con las reglas de cálculo
  y la relación entre ellos.

### Modified Capabilities

- `frontend-balances`: la vista de balances suma un panel de totales del grupo con
  el avance de la liquidación.

## Impact

- **Backend nuevo**: `dto/response/ResumenGrupoDto`, un método `resumen(Long
  grupoId)` en `service/BalanceService`, y un endpoint en `controller/BalanceController`.
- **Backend modificado**: `service/BalanceService` reutiliza el `Contexto` que ya
  carga para balances y liquidación; no hay consultas nuevas a la base.
- **Frontend nuevo**: `components/PanelTotales.tsx`.
- **Frontend modificado**: `api/types.ts` (`ResumenGrupoDto`), `api/balances.ts`
  (`obtenerResumen`), `lib/claves.ts` (`claveResumen`, sumada a
  `clavesDerivadasDelGrupo`), `pages/GrupoDetallePage.tsx`.
- **Sin cambios de base de datos**: todo se deriva de gastos y pagos ya guardados.

## Non-Goals

- No se muestra el total en la moneda original de cada gasto: el grupo puede tener
  gastos en BOB, USD y BTC a la vez, y sumarlos solo tiene sentido en USDT.
- No se agrega un desglose por categoría ni por persona más allá de `miParte`: no
  existe el concepto de categoría en el modelo.
- No se agrega un total por rango de fechas ni una evolución temporal.
- No se persiste ningún total: se calcula en cada consulta a partir de los gastos y
  pagos, que es la única fuente de verdad.
- No se cambia el cálculo de balances ni de liquidación.
- No se muestra el progreso de cada persona por separado, solo el del grupo.
