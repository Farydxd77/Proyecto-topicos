## La aritmética, explícita

Es la parte del cambio que más fácil se malinterpreta, así que queda escrita.

Sea un grupo con gastos `G1..Gn` y pagos `P1..Pm`, todo en USDT:

```
totalGastado       = Σ montoUsdt(Gi)
totalPagado        = Σ monto(Pj)
pendientePorSaldar = Σ balance(p) para todo p con balance(p) > 0
```

**`totalPagado` no tiende a `totalGastado`.** Quien paga un gasto ya cubrió su propia
parte en el momento de pagarlo, y nadie se transfiere dinero a sí mismo: esa porción
nunca va a aparecer como pago. En un grupo de 3 donde Ana paga 900 repartidos en
partes iguales, la deuda hacia Ana es 600, no 900, y cuando Beto y Carla le
transfieran sus 300 el grupo estará saldado con `totalPagado = 600` y
`totalGastado = 900`.

Lo que sí es una invariante útil, y la que gobierna la barra de progreso:

```
deudaTotalDelGrupo = totalPagado + pendientePorSaldar
progreso           = totalPagado / deudaTotalDelGrupo     (100% cuando pendiente = 0)
```

`pendientePorSaldar` se define como la suma de los balances **positivos** y no de los
negativos en valor absoluto, aunque den lo mismo: la suma de todos los balances es
exactamente cero (invariante ya especificada en `balances`), así que ambas
coinciden por construcción. Se elige el lado positivo porque es lo que alguien tiene
que **recibir**, que es lo que la barra mide.

## Decisión 1: un endpoint propio, no un campo más en balances

`GET /api/grupos/{id}/resumen` en lugar de agregar los totales a la respuesta de
`GET /api/grupos/{id}/balances`.

**Alternativa descartada: engordar la respuesta de balances.** Esa respuesta es una
lista, y meterle un objeto de totales al lado obligaría a envolverla en otra
estructura, rompiendo a todo cliente que hoy la itera directamente. Los frontends ya
la consumen como array.

**Alternativa descartada: calcularlo en el frontend sumando la lista de gastos.** Es
lo que el proyecto evita por convención: «los montos se formatean, nunca se operan»
(`CLAUDE.md`). Sumar `montoUsdt` en JavaScript sobre valores de 6 decimales arrastra
error de coma flotante, y además el frontend tendría que pedir la lista completa de
gastos y de pagos solo para totalizar.

## Decisión 2: reutiliza el `Contexto` que ya existe

`BalanceService.cargarContexto(grupoId)` ya recorre los gastos y los pagos del grupo
y arma los mapas de pagado, adeudado y pagos por participante. `resumen` se apoya en
eso: no agrega ni una consulta.

Para `totalGastado` hace falta un dato que el contexto hoy no guarda —la suma de los
`montoUsdt`—, así que se agrega al record `Contexto` en el mismo bucle que ya los
recorre. `totalPagado` sale de sumar `pagosRealizados`, que ya está.

**Alternativa descartada: consultas de agregación (`SUM`) en el repositorio.** Serían
más eficientes con muchos gastos, pero `CLAUDE.md` fija Query Methods de JPA sin
`@Query` manual, y una suma no se expresa con Query Methods. Con el volumen de un
grupo de viaje, recorrer la lista en memoria no es un problema.

## Decisión 3: `miParte` sale de la división, no de una fracción

`miParte` es la suma de los `montoAdeudado` del participante que consulta, no
`totalGastado / cantidadDeMiembros`. Con división personalizada esas dos cosas ya no
coinciden: quien queda fuera de un gasto no adeuda nada de él.

## Escala y redondeo

Los tres totales se devuelven con escala 2, coherente con `monto_adeudado` y con los
balances. `totalGastado` se calcula sumando los `montoUsdt` (escala 6) y redondeando
**al final**, no gasto por gasto: redondear antes acumularía el error.

Consecuencia aceptada: en un grupo con gastos convertidos desde otra moneda,
`totalGastado` puede diferir en centavos de la suma de lo que muestra la lista de
gastos, donde cada uno se redondea por separado. Es la misma diferencia que la spec
de `frontend-gastos` ya documenta para el reparto, y por el mismo motivo.

## Riesgos

- El panel es lo primero que se ve del grupo, así que un número mal entendido se
  vuelve la impresión principal. Se mitiga etiquetando «Falta saldar» en lugar de
  «Deuda», y no poniendo «Total pagado» al lado de «Total gastado» como si fueran
  comparables: van en bloques distintos, y la barra mide el pendiente.
