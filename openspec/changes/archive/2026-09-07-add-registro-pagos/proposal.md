## Why

Hoy el grupo puede ver su balance y la liquidación mínima, pero no hay forma de
registrar que un participante ya le transfirió dinero a otro para saldar su deuda.
Sin ese registro, los balances nunca reflejan los pagos hechos fuera de la app y la
liquidación sigue mostrando deudas ya pagadas. Esta capacidad cierra el ciclo:
registrar el pago en USDT y que los balances lo descuenten.

## What Changes

- Nueva entidad `Pago` y tabla `pagos` (grupo, pagador, receptor, monto en USDT,
  fecha, `tx_id` opcional). El modelo de datos ya la documenta en `CLAUDE.md` y
  `openspec/specs/data-model.md`.
- Nuevos endpoints REST anidados bajo el grupo:
  - `POST /api/grupos/{grupoId}/pagos` — registra un pago (201).
  - `GET /api/grupos/{grupoId}/pagos` — lista los pagos del grupo ordenados por
    fecha descendente (200, `[]` si no hay).
  - `GET /api/grupos/{grupoId}/pagos/{pagoId}` — detalle de un pago (200).
  - `PUT /api/grupos/{grupoId}/pagos/{pagoId}` — edita monto, receptor, fecha y
    `tx_id` de un pago (200). El pagador queda fijado al participante que lo
    registró.
  - `DELETE /api/grupos/{grupoId}/pagos/{pagoId}` — elimina un pago (204).
- El pago solo lo puede registrar el propio pagador (el participante resuelto del
  token JWT). Editar y eliminar solo lo puede hacer ese mismo pagador. Cualquier
  miembro del grupo puede listar y ver los pagos.
- **BREAKING** (de contrato de lectura): `GET /api/grupos/{id}/balances` y
  `GET /api/grupos/{id}/liquidacion` pasan a incluir los pagos en el cálculo.
  Para cada participante: `balance = pagadoEnGastosUSDT − adeudado − pagosRecibidos
  + pagosRealizados`. Un pago del deudor al acreedor acerca ambos balances a cero;
  la suma de todos los balances sigue siendo exactamente `0.00`.
- Se corrige la fórmula de impacto en balances del borrador
  `openspec/specs/pagos/spec.md`, que tenía los signos de `pagosRecibidos` y
  `pagosRealizados` invertidos (un pago aumentaba la deuda del pagador).

## Capabilities

### New Capabilities

- `pagos`: registro de pagos en USDT entre participantes de un grupo (alta,
  listado, detalle, edición y borrado), con las reglas de permiso (solo el pagador
  registra/edita/borra; cualquier miembro consulta) y los casos límite de
  validación.

### Modified Capabilities

- `balances`: el cálculo de `GET /api/grupos/{id}/balances` y
  `GET /api/grupos/{id}/liquidacion` incorpora los pagos del grupo. Cambia la
  definición de `balance` de cada participante y se mantiene la invariante de que
  la suma de balances es `0.00`.

## Impact

- **Backend nuevo**: `entity/Pago`, `repository/PagoRepository`,
  `dto/request/RegistrarPagoRequest`, `dto/request/ActualizarPagoRequest`,
  `dto/response/PagoResponse`, `service/PagoService`, `controller/PagoController`.
- **Backend modificado**: `service/BalanceService` (y, si aplica, `util/BalanceUtil`)
  para sumar `pagosRealizados` y restar `pagosRecibidos` por participante.
- **Base de datos**: Hibernate `ddl-auto=update` crea la tabla `pagos` al arrancar;
  sin migración manual.
- **Seguridad**: los nuevos endpoints quedan bajo la regla general (todo lo que no
  es `/api/auth/**` exige JWT válido); no cambia `SecurityConfig`.
- **Documentación**: `openspec/specs/data-model.md` ya incluye la tabla `pagos`;
  se corrige la fórmula del borrador `openspec/specs/pagos/spec.md` al archivar.
- **Sin cambios de frontend** en este cambio.

## Non-Goals

- No se verifica el `tx_id` contra ninguna blockchain ni se consulta un explorador.
- No se notifica al receptor del pago (sin email ni notificaciones in-app).
- No hay pagos parciales automáticos ni conciliación contra la liquidación
  sugerida: el monto y el receptor los elige quien registra el pago.
- No se restringe el monto del pago al saldo pendiente entre ambos participantes
  (se admite registrar de más o de menos).
- No se registra ni expone quién creó el pago más allá de que sea el pagador.
- Sin cambios en la capacidad de gastos ni en la conversión de moneda: el pago se
  registra directamente en USDT, sin llamada a CriptoYa.
- Sin trabajo de frontend (pantallas, hooks de React Query, tipos en
  `src/api/types.ts`): queda para un cambio posterior.
