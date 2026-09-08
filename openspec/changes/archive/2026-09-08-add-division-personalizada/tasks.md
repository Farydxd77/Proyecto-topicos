## 1. Backend — modelo y contratos

- [x] 1.1 Crear `dto/request/DivisionParticipanteRequest.java` (record con `Long
  participanteId` `@NotNull` y `Integer peso` `@NotNull @Min(1) @Max(1000)`, con
  mensajes en español). Verificar con `cd backend && mvnw.cmd -q test-compile`.
- [x] 1.2 Agregar el campo `@Valid List<DivisionParticipanteRequest> division`
  (opcional, sin `@NotNull`) a `dto/request/RegistrarGastoRequest.java` y a
  `dto/request/ActualizarGastoRequest.java`. Verificar con `mvnw.cmd -q
  test-compile`.
- [x] 1.3 Agregar `@Column(name = "peso", nullable = false) @ColumnDefault("1")
  private Integer peso;` a `entity/GastoParticipante.java`. Verificar con `mvnw.cmd
  -q test-compile`.
- [x] 1.4 Agregar `Integer peso` a `dto/response/GastoParticipanteDto.java`.
  Verificar con `mvnw.cmd -q test-compile`.

## 2. Backend — cálculo del reparto

- [x] 2.1 En `service/GastoService.java`, agregar `validarDivision(Long grupoId,
  List<DivisionParticipanteRequest> division, List<Participante> miembros)` que
  lance `BadRequestException` ante lista vacía, `participanteId` repetido o
  `participanteId` que no es miembro actual del grupo. Verificar con `mvnw.cmd -q
  test-compile`.
- [x] 2.2 Reescribir `calcularDivision` para que reciba los pares
  `(Participante, peso)` ya resueltos y aplique el cálculo proporcional del design:
  `pesoTotal` = suma de pesos; cada no absorbente adeuda `monto * peso / pesoTotal`
  a 2 decimales HALF_UP; el absorbente adeuda el resto. Verificar con `mvnw.cmd -q
  test-compile`.
- [x] 2.3 Agregar `elegirAbsorbente(participantes, pagador)`: el pagador si figura
  en la lista; si no, el de mayor peso; a igualdad de peso, el de menor id.
  Verificar con `mvnw.cmd -q test-compile`.
- [x] 2.4 En `registrar` y en `actualizar`, resolver la lista de
  `(Participante, peso)` a partir de `req.division()` cuando viene, y de
  `miembrosActuales(grupoId)` con peso `1` cuando no viene. Verificar con `mvnw.cmd
  -q test-compile`.
- [x] 2.5 Ajustar `toResponse` para poblar el `peso` en cada `GastoParticipanteDto`.
  Verificar con `mvnw.cmd -q test-compile`.

## 3. Backend — tests

- [x] 3.1 En `gastos/GastoDivisionTest.java`, agregar los casos del reparto
  proporcional: `division_pesosDesiguales_repartenProporcionalmente()`,
  `division_excluyeAUnMiembro_noLeAdeudaNada()`,
  `division_pagadorExcluido_elDeMayorPesoAbsorbe()`,
  `division_pagadorExcluidoYPesosIguales_absorbeElDeMenorId()`,
  `division_ausente_mantieneElRepartoEquitativoConPesoUno()` y
  `division_deUnSoloParticipante_leAdeudaTodo()`. Cada uno verifica también que la
  suma sea exacta. Verificar con `mvnw.cmd -q test`.
- [x] 3.2 En `gastos/GastoControllerTest.java`, agregar la validación:
  `registrarGasto_divisionVacia_devuelve400()`,
  `registrarGasto_divisionConParticipanteRepetido_devuelve400()`,
  `registrarGasto_divisionConNoMiembro_devuelve400()`,
  `registrarGasto_divisionConPesoCero_devuelve400()` y
  `registrarGasto_divisionConPesoMayorA1000_devuelve400()`. Verificar con `mvnw.cmd
  -q test`.
- [x] 3.3 En `gastos/GastoControllerTest.java`, agregar la edición:
  `actualizarGasto_cambiaLaDivision_reemplazaLasFilas()` y
  `actualizarGasto_sinDivision_vuelveAlRepartoEquitativo()`. Verificar con `mvnw.cmd
  -q test`.
- [x] 3.4 En `balances/BalanceControllerTest.java`, agregar
  `balances_gastoConPagadorExcluido_leSubeElBalancePorElTotal()` y confirmar que la
  suma de balances sigue siendo `0.00`. Verificar con `mvnw.cmd -q test`.
- [x] 3.5 Correr la suite completa con `mvnw.cmd -q test`.

## 4. Documentación del modelo

- [x] 4.1 En `CLAUDE.md`, agregar `peso INTEGER NOT NULL DEFAULT 1` a la tabla
  `gasto_participantes`, quitar de *Fuera de alcance* la línea sobre excluir
  participantes y repartir en proporciones desiguales, y documentar la regla del
  absorbente del redondeo generalizada.
- [x] 4.2 En `openspec/specs/data-model.md`, reflejar la misma columna.

## 5. Frontend — contratos

- [x] 5.1 En `frontend/src/api/types.ts`: agregar `DivisionParticipanteRequest {
  participanteId: number; peso: number }`, el campo opcional `division?:
  DivisionParticipanteRequest[]` a `RegistrarGastoRequest`, y el campo `peso:
  number` a `GastoParticipanteDto`. Verificar con `cd frontend && npx tsc --noEmit`.
- [x] 5.2 En `frontend/src/lib/validacion.ts`: agregar `validarDivision(entradas)`
  que exija al menos un participante incluido y que cada peso sea un entero de 1 a
  1000. Verificar con `npx tsc --noEmit`.

## 6. Frontend — formulario de gasto

- [x] 6.1 En `components/FormularioGasto.tsx`, agregar el estado del reparto: modo
  (`equitativo` | `personalizado`) y, por miembro, si está incluido y cuántas partes
  tiene. Inicializar con todos incluidos y una parte, y al editar un gasto,
  reconstruir el estado desde la `division` recibida (modo `personalizado` si algún
  peso difiere de 1 o si falta algún miembro). Verificar con `npx tsc --noEmit`.
- [x] 6.2 En `components/FormularioGasto.tsx`, renderizar la sección «Cómo se
  reparte»: los dos modos como radio, y la tabla de miembros solo en modo
  personalizado. Verificar con `npx tsc --noEmit` y `npx oxlint src`.
- [x] 6.3 En `components/FormularioGasto.tsx`, mostrar la previsualización de cuánto
  le queda a cada participante incluido, calculada con la misma regla proporcional
  que el backend, y etiquetada como estimación. Verificar con `npx tsc --noEmit` y
  `npx oxlint src`.
- [x] 6.4 En `components/FormularioGasto.tsx`, enviar `division` solo en modo
  personalizado; en modo equitativo omitir el campo. Aplicar `validarDivision` antes
  de enviar. Verificar con `npx tsc --noEmit` y `npx oxlint src`.

## 7. Frontend — detalle del gasto

- [x] 7.1 En `pages/GastoDetallePage.tsx`, mostrar las partes de cada participante
  cuando el reparto no es equitativo (algún `peso` distinto de 1), y aclarar cuando
  el pagador no participa del gasto. Verificar con `npx tsc --noEmit` y `npx oxlint
  src`.

## 8. Verificación final

- [x] 8.1 `cd backend && mvnw.cmd -q test` sin fallos.
- [x] 8.2 `cd frontend && npx tsc --noEmit && npx oxlint src` sin errores.
- [x] 8.3 `openspec validate 2026-09-08-add-division-personalizada --strict` pasa.
