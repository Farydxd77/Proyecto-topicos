## Why

Es el cierre de la aplicación. Con los cuatro cambios anteriores una persona puede
armar un grupo, sumar gente y cargar gastos en cualquier moneda, pero todavía tiene
que sacar a mano la única cuenta que le importa: quién le debe a quién y cuánto.
Eso es lo que da nombre al producto.

El backend ya lo resuelve entero en `BalanceUtil`: calcula el balance de cada
participante garantizando que la suma dé exactamente cero, y reduce esas deudas
cruzadas a la lista mínima de transferencias. Es la parte con más miga algorítmica
del proyecto y la única cuyos tests unitarios ya se ejecutaron y pasaron. Falta la
pantalla que lo muestre.

## What Changes

- El detalle del grupo gana una sección de balances: cuánto le corresponde recibir
  o pagar a cada integrante, expresado en USDT.
- Cada balance se presenta con su signo interpretado en palabras —le deben, debe, o
  está a mano— y no como un número con signo que haya que descifrar.
- Nueva sección de liquidación: la lista mínima de transferencias para saldar todo,
  cada una indicando quién le paga a quién y cuánto.
- La aplicación destaca qué le toca a quien está mirando, para que no tenga que
  buscarse en la lista.
- Estado saldado: cuando no hay deudas pendientes, se explica que está todo a mano
  en lugar de mostrar una lista vacía.
- Los balances se refrescan cuando cambian los gastos o la composición del grupo:
  las mutaciones de esos cambios invalidan también estas consultas.
- Nuevo módulo `src/api/balances.ts` y dos contratos nuevos en `src/api/types.ts`.
- Los mocks de MSW se amplían con los dos endpoints, replicando el algoritmo.

## Non-Goals

- No se registran pagos ni se marca una transferencia como realizada: el backend no
  tiene el concepto de liquidación saldada, y `TransferenciaDto` es un cálculo
  derivado de los gastos, no un registro persistido.
- No se exporta ni se comparte el resumen por ningún medio.
- No se muestra el histórico de cómo evolucionó el balance en el tiempo.
- No se replica el cálculo en el cliente: se muestra lo que el backend devuelve.
- No se convierte el resultado a la moneda local de nadie: los balances están en
  USDT porque así los calcula el backend.
- No se toca `backend/`.

## Capabilities

### New Capabilities

- `frontend-balances`: la pantalla que cierra la cuenta de un grupo. Muestra el
  balance de cada integrante y la lista mínima de transferencias que salda las
  deudas, con el estado de "todo a mano" cuando no hay nada pendiente, y mantiene
  esos números al día ante cualquier cambio en los gastos o en los miembros.

### Modified Capabilities

<!-- Ninguna. -->

## Impact

- **Código nuevo**: `src/api/balances.ts` y los componentes de balances y de
  liquidación.
- **Código modificado**: `src/api/types.ts` (`BalanceDto`, `TransferenciaDto`),
  `src/pages/GrupoDetallePage.tsx` (secciones nuevas), `src/mocks/db.ts` y
  `src/mocks/handlers.ts`, y las mutaciones de gastos y de miembros para que
  invaliden también estas consultas.
- **APIs consumidas**: `GET /api/grupos/{grupoId}/balances` y
  `GET /api/grupos/{grupoId}/liquidacion`.
- **Estado de servidor**: claves `['balances', grupoId]` y
  `['liquidacion', grupoId]`.
- **Dependencias**: ninguna nueva.
- **Requisito previo**: los cuatro cambios anteriores aplicados. En particular
  `add-frontend-gastos`, porque sin gastos cargados todos los balances son cero y
  la pantalla no se puede verificar de verdad.
