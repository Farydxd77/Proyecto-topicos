## Contexto

El backend ya define todo el contrato (ver `openspec/specs/pagos/spec.md`). Lo que
falta es exclusivamente cliente. Las convenciones del frontend que aplican, de
`CLAUDE.md`:

- Toda llamada pasa por `apiFetch`; rutas relativas (`/api/...`).
- `src/api/types.ts` es la única definición de los contratos y espeja los `record`
  del backend.
- Las claves de TanStack Query viven en `lib/claves.ts`, porque las mutaciones de
  una capacidad invalidan consultas de otra.
- Toda pantalla que consulta pasa por `estadoDe(consulta)`.
- Los montos se formatean, nunca se operan.

## Decisión 1: el formulario no tiene selector de pagador

`RegistrarPagoRequest` no lleva `pagadorId`: el backend lo resuelve del token. El
formulario muestra una línea fija —«Pagás vos: {nombre}»— en lugar de un `select`
deshabilitado o de un campo que no se envía.

**Alternativa descartada: un selector con una sola opción.** Sugiere que hay una
elección posible y no la hay; el primer usuario que quiera registrar un pago ajeno
va a intentarlo y a chocar con un `403` que la interfaz podía haber evitado.

Esto marca una asimetría deliberada con el formulario de gasto, que **sí** deja
elegir pagador. La diferencia no es de diseño de interfaz: es que el contrato del
backend es distinto en cada caso.

## Decisión 2: editar y borrar solo en los pagos propios

`PagoService.exigirPagador` responde `403` a cualquiera que no sea quien registró el
pago. La interfaz espeja esa regla comparando `pago.pagador.id` con el participante
del perfil cacheado, igual que `GestionMiembros` hace con el creador del grupo.

**Alternativa descartada: mostrar las acciones siempre y explicar el `403` cuando
llega.** Ofrecer un botón que se sabe que va a fallar es peor que no ofrecerlo. El
manejo del `403` se mantiene igual por si el estado local quedó viejo, pero no es el
camino esperado.

## Decisión 3: el atajo desde la liquidación

En `SeccionBalances`, cada transferencia sugerida en la que `deId` coincide con el
participante propio suma un botón que abre el formulario de pago con `receptorId` y
`monto` precargados.

Es la decisión que le da sentido al cambio completo: sin ella, el usuario ve
«tenés que pagarle 200 a Ana» en una sección y tiene que ir a otra a tipear
manualmente «Ana» y «200». Con ella, la liquidación deja de ser una lista de tareas
que hay que transcribir.

**Alternativa descartada: registrar el pago directo desde la liquidación, sin
formulario.** Saltea la fecha y el `txId`, y sobre todo saltea la confirmación de un
registro que después hay que borrar a mano si fue un error.

Los valores llegan **precargados, no fijos**: el usuario puede cambiar el monto
antes de confirmar, porque el backend admite pagar de más o de menos y en la vida
real se paga redondeando.

## Decisión 4: las claves de pagos entran en `clavesDerivadasDelGrupo`

`clavesDerivadasDelGrupo(grupoId)` hoy devuelve gastos, balances y liquidación. Se le
suma `clavePagos(grupoId)`.

El acoplamiento es real en las dos direcciones:

- Registrar, editar o borrar un **pago** cambia los balances y la liquidación.
- Quitar a un **miembro** puede dejar pagos de alguien que ya no está, y el listado
  los sigue mostrando.

Ponerlo en la lista central es exactamente el motivo por el que esa lista existe: que
una mutación nueva no se olvide de invalidar la mitad.

**Nota sobre un caso que la lista no cubre:** un gasto nuevo no cambia los pagos.
Invalidar de más es barato (una consulta que vuelve igual) y el costo de invalidar
de menos es un número mal mostrado, así que se prefiere la lista única.

## Decisión 5: sin ruta de detalle para un pago

Un gasto tiene una página propia porque arrastra una división por participante, una
tasa de cambio y un monto original en otra moneda. Un pago tiene pagador, receptor,
monto, fecha y `txId`: entra entero en una fila de la lista, y editarlo se resuelve
expandiendo esa fila.

**Alternativa descartada: `/grupos/:id/pagos/:pagoId`.** Agrega una ruta, una
consulta y una pantalla de «no existe» para mostrar lo mismo que ya se ve.

## Riesgos

- El detalle del grupo suma una cuarta sección y se hace largo. Se mitiga
  manteniendo el formulario de pago colapsado detrás de un botón, igual que el de
  gasto.
- El `txId` puede ser una cadena larga sin espacios (un hash) y romper el ancho en
  pantallas chicas. Se muestra truncado con `title` completo.
