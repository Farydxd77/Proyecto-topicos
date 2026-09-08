## 1. Backend — transferir el rol de creador

- [x] 1.1 Crear `dto/request/TransferirCreadorRequest.java` (record con `Long
  participanteId` `@NotNull(message = "El participanteId es obligatorio")`).
  Verificar con `cd backend && mvnw.cmd -q test-compile`.
- [x] 1.2 En `service/GrupoService.java`, agregar `transferirCreador(Long grupoId,
  TransferirCreadorRequest req)` `@Transactional`: `grupoDondeEsCreador` → si
  `req.participanteId()` es el creador actual, `BadRequestException` → si no es
  miembro del grupo, `BadRequestException` → `grupo.setCreador(nuevo)` → `save` →
  `toResponse`. Verificar con `mvnw.cmd -q test-compile`.
- [x] 1.3 En `controller/GrupoController.java`, agregar `@PutMapping("/{id}/creador")`
  que devuelve `GrupoResponse` (200). Verificar con `mvnw.cmd -q test-compile`.

## 2. Backend — abandonar el grupo

- [x] 2.1 Reescribir `quitarMiembro(Long grupoId, Long participanteId)` en
  `service/GrupoService.java` con el orden de comprobaciones del design: guarda
  `grupoDondeEsMiembro` (403 si el solicitante no pertenece) → si
  `participanteId` es el creador, `BadRequestException` → si `participanteId` no es
  el solicitante y el solicitante no es el creador, `ForbiddenOperationException` →
  buscar la membresía (404 si no existe) → borrarla. Verificar con `mvnw.cmd -q
  test-compile`.

## 3. Backend — `esMiembroActual` en los balances

- [x] 3.1 Agregar el campo `boolean esMiembroActual` a
  `dto/response/BalanceDto.java`, después de `balance`. Verificar con `mvnw.cmd -q
  test-compile`.
- [x] 3.2 En `service/BalanceService.java`, hacer que `cargarContexto` guarde en el
  record `Contexto` un `Set<Long> miembrosActuales` con los ids que ya recorre en el
  bucle de `grupoParticipanteRepository.findByGrupoId`, y que `calcularBalances` lo
  use para poblar el nuevo campo del DTO. No cambia el cálculo del balance ni el
  conjunto de participantes incluidos. Verificar con `mvnw.cmd -q test-compile`.

## 4. Backend — tests

- [x] 4.1 En `grupos/GrupoMiembrosControllerTest.java`, agregar los casos de
  transferencia: `transferirCreador_creador_devuelve200ConNuevoCreador()`,
  `transferirCreador_nuevoCreadorEjerceLosPrivilegios()`,
  `transferirCreador_creadorSalientePierdeLosPrivilegios()`,
  `transferirCreador_aNoMiembro_devuelve400()`,
  `transferirCreador_aSiMismo_devuelve400()`,
  `transferirCreador_miembroNoCreador_devuelve403()` y
  `transferirCreador_grupoInexistente_devuelve404()`. Verificar con `mvnw.cmd -q
  test`.
- [x] 4.2 En `grupos/GrupoMiembrosControllerTest.java`, agregar los casos de salida
  voluntaria: `abandonarGrupo_miembroNoCreador_devuelve204YPierdeElGrupo()`,
  `abandonarGrupo_creador_devuelve400()`,
  `abandonarGrupo_creadorTrasTransferir_devuelve204()`,
  `quitarAOtro_miembroNoCreador_devuelve403()` y
  `quitarMiembro_solicitanteNoEsMiembro_devuelve403()`. Actualizar el test existente
  que esperaba `403` para la auto-baja de un miembro no creador. Verificar con
  `mvnw.cmd -q test`.
- [x] 4.3 En `balances/BalanceControllerTest.java`, agregar
  `balances_incluyenEsMiembroActual()` y
  `balances_exMiembroConDeuda_apareceConEsMiembroActualFalse()` (registra un gasto,
  el creador quita al deudor, y verifica que sigue con su balance, con
  `esMiembroActual` en `false`, y que la suma da `0.00`). Verificar con `mvnw.cmd -q
  test`.
- [x] 4.4 Correr la suite completa con `mvnw.cmd -q test` y confirmar que no hay
  fallos ni errores.

## 5. Frontend — contratos y cliente

- [x] 5.1 En `frontend/src/api/types.ts`: agregar `TransferirCreadorRequest {
  participanteId: number }` en la sección de miembros del grupo, y el campo
  `esMiembroActual: boolean` a `BalanceDto`. Verificar con `cd frontend && npx tsc
  --noEmit`.
- [x] 5.2 En `frontend/src/api/grupos.ts`: agregar `transferirCreador(grupoId,
  datos): Promise<GrupoResponse>` (`PUT /grupos/{id}/creador`) y
  `abandonarGrupo(grupoId, participanteId): Promise<void>` (reusa `DELETE
  /grupos/{id}/miembros/{participanteId}`, documentando que el backend admite la
  auto-baja de un miembro no creador). Verificar con `npx tsc --noEmit`.

## 6. Frontend — gestión de miembros

- [x] 6.1 En `components/GestionMiembros.tsx`, agregar la acción de transferir el
  rol: visible solo para el creador, con un selector de los demás miembros y
  confirmación que nombre a la persona. La mutación invalida `claveGrupo(grupoId)` y
  `CLAVE_GRUPOS`, y siembra la respuesta en el caché del detalle. Verificar con `npx
  tsc --noEmit` y `npx oxlint`.
- [x] 6.2 En `components/GestionMiembros.tsx`, agregar la acción de abandonar el
  grupo: visible solo para los miembros que no son el creador, con confirmación que
  advierta que dejará de ver gastos y balances. Al creador se le muestra en su lugar
  la indicación de transferir el rol o eliminar el grupo. Verificar con `npx tsc
  --noEmit` y `npx oxlint`.
- [x] 6.3 En `pages/GrupoDetallePage.tsx`, pasar a `GestionMiembros` lo que necesite
  para resolver «quién soy» (el participante del perfil cacheado) y navegar a
  `/grupos` cuando la salida termina bien, invalidando `CLAVE_GRUPOS`. Verificar con
  `npx tsc --noEmit` y `npx oxlint`.

## 7. Frontend — marca de ex-miembro en balances

- [x] 7.1 En `components/SeccionBalances.tsx`, mostrar una marca junto al nombre
  cuando `esMiembroActual` es `false`, con un texto que explique que ya no integra el
  grupo. No cambiar el orden ni el formato de los montos. Verificar con `npx tsc
  --noEmit` y `npx oxlint`.

## 8. Verificación final

- [x] 8.1 `cd backend && mvnw.cmd -q test` sin fallos.
- [x] 8.2 `cd frontend && npx tsc --noEmit && npx oxlint` sin errores.
- [x] 8.3 `openspec validate 2026-09-08-add-gestion-membresia --strict` pasa.
