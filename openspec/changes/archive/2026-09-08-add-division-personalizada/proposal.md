## Why

El backend reparte cada gasto siempre entre **todos** los miembros del grupo y
siempre en partes iguales. Es la limitación más visible de la aplicación en cuanto
se usa de verdad: nadie viaja repartiendo cada cuenta de forma idéntica entre todos.
Los dos casos que aparecen enseguida son «yo no comí postre» (hay que poder excluir a
alguien de un gasto puntual) y «la habitación doble la usamos dos y la simple uno»
(hay que poder repartir en proporciones desiguales).

Hoy la única salida es registrar gastos falsos que compensen, lo que ensucia el
historial y rompe la utilidad de los balances.

## What Changes

- `RegistrarGastoRequest` y `ActualizarGastoRequest` aceptan un campo **opcional**
  `division`: una lista de `{ participanteId, peso }`.
  - **Si no viene** (o viene `null`), el comportamiento es exactamente el actual:
    reparto equitativo entre todos los miembros del grupo. Nada de lo que hoy
    funciona cambia.
  - **Si viene**, el gasto se reparte solo entre los participantes listados, en
    proporción a su `peso`. Excluir a alguien es no incluirlo; repartir desigual es
    darle un peso distinto.
- `peso` es un entero de 1 a 1000. Un reparto equitativo entre los listados es
  simplemente todos con peso `1`.
- Nueva columna `gasto_participantes.peso INTEGER NOT NULL DEFAULT 1`, para que al
  editar un gasto se pueda volver a mostrar cómo se repartió y no haya que deducirlo
  de los montos.
- `GastoParticipanteDto` expone el `peso`, de modo que el detalle del gasto y el
  formulario de edición lo puedan mostrar.
- **El pagador ya no tiene por qué participar del gasto.** Sigue teniendo que ser
  miembro del grupo, pero puede quedar fuera de la división (pagó algo que no
  consumió). La regla del redondeo se generaliza: absorbe el sobrante el pagador si
  está en la división y, si no está, el participante de mayor peso (a igualdad de
  peso, el de menor id).
- **Frontend**: el formulario de gasto suma una sección «Cómo se reparte» con dos
  modos —«Entre todos, en partes iguales» (el de hoy, preseleccionado) y
  «Personalizado», donde se elige quién participa y con cuántas partes—. El detalle
  del gasto muestra las partes de cada uno cuando el reparto no fue equitativo.

## Capabilities

### Modified Capabilities

- `gastos`: el alta y la edición aceptan una división explícita por participante y
  peso; el reparto equitativo entre todos los miembros pasa a ser el valor por
  defecto en lugar de la única opción. La regla de absorción del redondeo se
  generaliza para cubrir el caso del pagador excluido.
- `frontend-gastos`: el formulario permite elegir quién participa de un gasto y con
  qué proporción, y el detalle muestra el reparto resultante.

## Impact

- **Backend nuevo**: `dto/request/DivisionParticipanteRequest` (record con
  `participanteId` y `peso`).
- **Backend modificado**: `dto/request/RegistrarGastoRequest` y
  `ActualizarGastoRequest` (campo `division` con `@Valid`),
  `entity/GastoParticipante` (columna `peso`),
  `dto/response/GastoParticipanteDto` (campo `peso`), `service/GastoService`
  (validación de la división y nuevo `calcularDivision` proporcional).
- **Base de datos**: Hibernate con `ddl-auto=update` agrega
  `gasto_participantes.peso` con `DEFAULT 1`, así que las filas existentes quedan
  con peso `1` y su reparto sigue siendo el mismo. `CLAUDE.md` y
  `openspec/specs/data-model.md` documentan la columna.
- **Frontend modificado**: `api/types.ts` (`DivisionParticipanteRequest`, campo
  `division` en las peticiones de gasto, campo `peso` en `GastoParticipanteDto`),
  `components/FormularioGasto.tsx` (la sección de reparto),
  `pages/GastoDetallePage.tsx` (mostrar las partes), `lib/validacion.ts`
  (validación de la división en el cliente).
- **Contrato de la API**: aditivo y retrocompatible. Un cliente que no envíe
  `division` obtiene exactamente el comportamiento anterior.

## Non-Goals

- No se admite repartir por **montos exactos** ni por **porcentajes**: el peso
  entero cubre ambos casos de forma aproximada y evita un tercer modo de división con
  sus propias reglas de redondeo. Si más adelante hace falta, es un cambio aparte.
- No se guarda un «tipo de división» explícito en la base: un gasto con todos los
  pesos iguales y todos los miembros presentes es indistinguible de uno equitativo, y
  eso es correcto porque el resultado es el mismo.
- No se recalcula la división de los gastos ya registrados: siguen con peso `1` para
  todos sus participantes, que es exactamente el reparto que ya tenían.
- No se valida que la división cubra a todos los miembros del grupo: excluir es
  justamente el punto.
- No se permite incluir en la división a alguien que no es miembro del grupo.
- No cambia la conversión de moneda: el reparto se sigue haciendo sobre el
  `montoUsdt` redondeado a 2 decimales.
- No cambian los balances ni la liquidación: siguen sumando `montoAdeudado`, que
  ahora simplemente se calcula de otra forma.
