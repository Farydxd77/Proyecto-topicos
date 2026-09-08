## Why

El backend expone la capacidad `pagos` completa desde el cambio `registro-pagos`, y
los balances ya la incorporan al cálculo. El frontend no sabe que existe: no hay ni
un solo `pagos` en todo `frontend/src`.

El efecto es que la aplicación queda a mitad de camino. Le dice al usuario «Beto le
debe 200 a Ana» y no le ofrece ninguna forma de marcar que Beto ya se los pagó. La
liquidación va a mostrar la misma deuda para siempre, y la única salida es registrar
un gasto falso que la compense. La funcionalidad está construida y no se puede usar.

## What Changes

- Nuevo cliente `api/pagos.ts` con las cinco operaciones del backend, y los
  contratos correspondientes en `api/types.ts`.
- Nuevas claves de TanStack Query para pagos, sumadas a `clavesDerivadasDelGrupo`
  para que registrar un gasto o cambiar la membresía invalide también la lista de
  pagos, y para que registrar un pago invalide balances y liquidación.
- Nueva sección **Pagos** en el detalle del grupo, debajo de gastos: lista los pagos
  registrados (quién le pagó a quién, cuánto, cuándo y el `txId` si lo hay) y
  permite registrar uno nuevo.
- Nuevo formulario de pago. No tiene selector de pagador: el backend fija al
  participante del token, así que la interfaz lo dice en lugar de fingir una
  elección. El receptor se elige entre los demás miembros del grupo, el monto va
  siempre en USDT y el `txId` es opcional.
- Editar y eliminar un pago se ofrecen **solo sobre los pagos propios**, que es lo
  único que el backend permite (`403` para los demás).
- **Atajo desde la liquidación**: cada transferencia sugerida en la que el usuario
  que mira es el deudor ofrece «Registrar este pago», que abre el formulario con el
  receptor y el monto ya cargados. Es lo que cierra el ciclo
  liquidación → pago → balances.

## Capabilities

### New Capabilities

- `frontend-pagos`: ver los pagos de un grupo, registrar un pago, editar y eliminar
  los propios, y el atajo que convierte una transferencia sugerida de la liquidación
  en un pago registrado.

### Modified Capabilities

- `frontend-balances`: la sección de liquidación deja de ser solo informativa y
  ofrece registrar cada transferencia sugerida en la que quien mira es el deudor.

## Impact

- **Frontend nuevo**: `api/pagos.ts`, `components/SeccionPagos.tsx`,
  `components/FormularioPago.tsx`.
- **Frontend modificado**: `api/types.ts` (`RegistrarPagoRequest`,
  `ActualizarPagoRequest`, `PagoResponse`), `lib/claves.ts` (`clavePagos`,
  `clavePago` y su inclusión en `clavesDerivadasDelGrupo`),
  `lib/validacion.ts` (`validarMontoPago`, `validarTxId`),
  `components/SeccionBalances.tsx` (el atajo desde la liquidación),
  `pages/GrupoDetallePage.tsx` (montar la sección nueva).
- **Sin cambios de backend**: la capacidad `pagos` ya está completa y no se toca.
- **Sin cambios de base de datos.**

## Non-Goals

- No se crea una pantalla de detalle de un pago con ruta propia: un pago tiene siete
  campos y se ve entero en la lista, a diferencia de un gasto, que arrastra una
  división.
- No se valida en el cliente que el monto del pago coincida con la deuda pendiente:
  el backend admite pagar de más o de menos a propósito, y el frontend no inventa
  una restricción que el contrato no tiene.
- No se verifica el `txId` contra ninguna blockchain ni se enlaza a un explorador:
  se muestra tal como se guardó.
- No se ofrece registrar un pago a nombre de otra persona, porque el backend lo
  prohíbe: el pagador es siempre quien tiene el token.
- No se agrega conversión de moneda al pago: va siempre en USDT, igual que en el
  backend.
- No se notifica al receptor (sigue sin haber email ni notificaciones).
- No se muestran los pagos dentro de la lista de gastos ni en un historial unificado:
  son dos secciones separadas.
