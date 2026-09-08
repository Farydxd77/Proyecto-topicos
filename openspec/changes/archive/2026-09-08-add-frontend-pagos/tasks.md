## 1. Contratos y cliente

- [x] 1.1 En `frontend/src/api/types.ts`, agregar una sección «Pagos» con
  `RegistrarPagoRequest { receptorId: number; monto: string; fecha: string; txId?:
  string }`, `ActualizarPagoRequest = RegistrarPagoRequest` y `PagoResponse { id,
  grupoId, pagador, receptor, monto: number, fecha, txId: string | null }`,
  documentando que el pagador no viaja en la petición. Verificar con `cd frontend &&
  npx tsc --noEmit`.
- [x] 1.2 Crear `frontend/src/api/pagos.ts` con `listarPagos`, `obtenerPago`,
  `registrarPago`, `actualizarPago` y `eliminarPago`, todas vía `apiFetch` sobre
  `/grupos/${grupoId}/pagos`, con comentarios sobre los códigos de error que devuelve
  cada una. Verificar con `npx tsc --noEmit`.
- [x] 1.3 En `frontend/src/lib/claves.ts`, agregar `clavePagos(grupoId)` y
  `clavePago(grupoId, pagoId)`, y sumar `clavePagos(grupoId)` a
  `clavesDerivadasDelGrupo`, explicando en el comentario por qué. Verificar con `npx
  tsc --noEmit`.
- [x] 1.4 En `frontend/src/lib/validacion.ts`, agregar `validarMontoPago(valor)` (8
  enteros y 2 decimales, mayor que 0) y `validarTxId(valor)` (opcional, máximo 100
  caracteres). Verificar con `npx tsc --noEmit`.

## 2. Formulario de pago

- [x] 2.1 Crear `frontend/src/components/FormularioPago.tsx` que reciba `grupo`,
  el `participanteId` propio, un `pago` opcional para editar, valores iniciales
  opcionales (`receptorIdInicial`, `montoInicial`) para el atajo de la liquidación, y
  los callbacks de envío y cancelación. Sin selector de pagador: una línea fija que
  diga a nombre de quién se registra. Verificar con `npx tsc --noEmit` y `npx oxlint
  src`.
- [x] 2.2 En `FormularioPago.tsx`, el selector de receptor lista a los miembros del
  grupo distintos del participante propio; si no hay ninguno, explicar que hace falta
  otro miembro en lugar de mostrar un selector vacío. Verificar con `npx tsc
  --noEmit` y `npx oxlint src`.
- [x] 2.3 En `FormularioPago.tsx`, aplicar `validarMontoPago`, `validarFecha` y
  `validarTxId` antes de enviar, y mostrar el error del backend sin vaciar el
  formulario. Verificar con `npx tsc --noEmit` y `npx oxlint src`.

## 3. Sección de pagos

- [x] 3.1 Crear `frontend/src/components/SeccionPagos.tsx` con la consulta de
  `listarPagos` pasada por `estadoDe`, el estado de carga, el error con reintento y
  el mensaje de lista vacía. Verificar con `npx tsc --noEmit` y `npx oxlint src`.
- [x] 3.2 En `SeccionPagos.tsx`, renderizar cada pago con pagador → receptor, monto
  en USDT, fecha y `txId` truncado con `title` completo. Verificar con `npx tsc
  --noEmit` y `npx oxlint src`.
- [x] 3.3 En `SeccionPagos.tsx`, mostrar editar y eliminar solo cuando
  `pago.pagador.id` es el participante propio; la eliminación con confirmación
  explícita. Las tres mutaciones invalidan `clavePagos` y
  `clavesDerivadasDelGrupo`. Verificar con `npx tsc --noEmit` y `npx oxlint src`.
- [x] 3.4 En `SeccionPagos.tsx`, exponer una forma de abrir el formulario con
  valores precargados, para que la sección de balances pueda dispararlo. Verificar
  con `npx tsc --noEmit` y `npx oxlint src`.

## 4. Atajo desde la liquidación

- [x] 4.1 En `components/SeccionBalances.tsx`, agregar en cada transferencia donde
  `deId` es el participante propio un botón «Registrar este pago» que invoque el
  callback recibido por props con `{ receptorId: paraId, monto }`. Verificar con `npx
  tsc --noEmit` y `npx oxlint src`.
- [x] 4.2 En `pages/GrupoDetallePage.tsx`, montar `SeccionPagos` debajo de
  `SeccionGastos` y cablear el atajo: el estado de «pago precargado» vive en la
  página y se pasa a las dos secciones. Verificar con `npx tsc --noEmit` y `npx
  oxlint src`.

## 5. Verificación final

- [x] 5.1 `cd frontend && npx tsc --noEmit && npx oxlint src && npm run build` sin
  errores.
- [x] 5.2 `cd backend && mvnw.cmd -q test` sigue sin fallos (este cambio no toca el
  backend, pero se confirma que nada se rompió).
- [x] 5.3 `openspec validate 2026-09-08-add-frontend-pagos --strict` pasa.
