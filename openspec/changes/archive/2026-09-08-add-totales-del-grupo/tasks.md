## 1. Backend

- [x] 1.1 Crear `dto/response/ResumenGrupoDto.java` (record con `BigDecimal
  totalGastado`, `totalPagado`, `pendientePorSaldar`, `miParte`, `int
  cantidadGastos`, `int cantidadPagos`). Verificar con `cd backend && mvnw.cmd -q
  test-compile`.
- [x] 1.2 En `service/BalanceService.java`, agregar al record `Contexto` los campos
  `BigDecimal totalGastadoUsdt` y `int cantidadGastos`, poblados en el bucle de
  gastos que ya existe (sumando `montoUsdt` en escala 6, sin redondear por gasto), y
  `int cantidadPagos` en el bucle de pagos. Verificar con `mvnw.cmd -q test-compile`.
- [x] 1.3 En `service/BalanceService.java`, agregar `resumen(Long grupoId)`
  `@Transactional(readOnly = true)`: valida membresía igual que los otros dos
  métodos, arma el `Contexto`, calcula `pendientePorSaldar` como la suma de los
  balances positivos, `miParte` como el `adeudadoPorId` del participante actual, y
  redondea todo a 2 decimales al final. Verificar con `mvnw.cmd -q test-compile`.
- [x] 1.4 En `controller/BalanceController.java`, agregar `@GetMapping("/resumen")`
  que devuelve `ResumenGrupoDto`. Verificar con `mvnw.cmd -q test-compile`.

## 2. Backend — tests

- [x] 2.1 Crear `balances/ResumenGrupoControllerTest.java` con el montaje habitual y
  los casos: `resumen_grupoSinGastos_todoEnCero()`,
  `resumen_escenarioSamaipata_totalGastadoYPendiente()`,
  `resumen_trasUnPago_bajaElPendienteYSubeElPagado()`,
  `resumen_grupoSaldado_pendienteCeroYPagadoMenorQueGastado()` (el caso de la
  aclaración contable: 900 gastado, 600 pagado, 0 pendiente),
  `resumen_pagadorExcluidoDelGasto_pagadoIgualaGastado()`,
  `resumen_miParteRespetaLaDivision()`,
  `resumen_alEliminarUnPago_vuelveElPendiente()`,
  `resumen_usuarioNoMiembro_devuelve403()`,
  `resumen_grupoInexistente_devuelve404()` y `resumen_sinToken_devuelve401()`.
  Verificar con `mvnw.cmd -q test`.
- [x] 2.2 Correr la suite completa con `mvnw.cmd -q test`.

## 3. Frontend — contratos

- [x] 3.1 En `frontend/src/api/types.ts`, agregar `ResumenGrupoDto` con los seis
  campos, documentando en un comentario que `totalPagado` NO converge a
  `totalGastado` y por qué. Verificar con `cd frontend && npx tsc --noEmit`.
- [x] 3.2 En `frontend/src/api/balances.ts`, agregar `obtenerResumen(grupoId)`.
  Verificar con `npx tsc --noEmit`.
- [x] 3.3 En `frontend/src/lib/claves.ts`, agregar `claveResumen(grupoId)` y sumarla
  a `clavesDerivadasDelGrupo`. Verificar con `npx tsc --noEmit`.

## 4. Frontend — panel de totales

- [x] 4.1 Crear `components/PanelTotales.tsx`: consulta pasada por `estadoDe`,
  estado de carga, error con reintento, y los tres totales en bloques separados —
  «Total del viaje» destacado, y «Ya saldado» / «Falta saldar» juntos como el par que
  sí se compara entre sí. Verificar con `npx tsc --noEmit` y `npx oxlint src`.
- [x] 4.2 En `PanelTotales.tsx`, agregar la barra de avance calculada como
  `totalPagado / (totalPagado + pendientePorSaldar)`, con lectura en palabras y el
  caso de deuda cero resuelto sin mostrar un avance engañoso. Verificar con `npx tsc
  --noEmit` y `npx oxlint src`.
- [x] 4.3 En `PanelTotales.tsx`, mostrar «Tu parte» y la aclaración de que todo está
  expresado en USDT. Verificar con `npx tsc --noEmit` y `npx oxlint src`.
- [x] 4.4 En `pages/GrupoDetallePage.tsx`, montar `PanelTotales` arriba de
  `GestionMiembros`. Verificar con `npx tsc --noEmit` y `npx oxlint src`.

## 5. Verificación final

- [x] 5.1 `cd backend && mvnw.cmd -q test` sin fallos.
- [x] 5.2 `cd frontend && npx tsc --noEmit && npx oxlint src && npm run build` sin
  errores.
- [x] 5.3 `openspec validate 2026-09-08-add-totales-del-grupo --strict` pasa.
