## Contexto

`GastoService.calcularDivision` hoy hace una cosa: divide el `montoUsdt` (redondeado
a 2 decimales) entre los `n` miembros actuales del grupo, y el pagador se queda con
el sobrante. El modelo lo refleja: `gasto_participantes` guarda solo
`(gasto_id, participante_id, monto_adeudado)`.

Los balances dependen de una invariante: para cada gasto, la suma de los
`monto_adeudado` es exactamente igual al `montoUsdt` del gasto redondeado a 2
decimales. Todo lo que sigue la respeta.

## Decisión 1: un solo mecanismo —pesos— para los dos casos de uso

La petición lleva `division: [{ participanteId, peso }]`. Excluir a alguien es no
listarlo. Repartir desigual es darle un peso distinto. Un reparto equitativo entre
los listados es todos con peso `1`.

**Alternativa descartada: dos campos separados**, uno para excluir
(`participantesExcluidos`) y otro para proporciones (`proporciones`). Duplica el
concepto: quien manda proporciones ya está diciendo implícitamente quién participa, y
habría que definir qué pasa si los dos campos se contradicen.

**Alternativa descartada: `tipoDivision: EQUITATIVA | PARTES | MONTOS` con un
payload distinto por modo.** Es lo que hacen las apps grandes, pero triplica la
superficie de validación y de redondeo para un proyecto que hoy tiene un solo modo. Y
`EQUITATIVA` es un caso particular de `PARTES` con todos los pesos en 1, así que el
enum no aporta expresividad, solo ramas.

**Alternativa descartada: pesos decimales o porcentajes.** Un porcentaje obliga a
validar que la suma dé exactamente 100, lo que es un problema con decimales (33.33 ×
3 = 99.99). Con pesos enteros la suma es lo que sea y el reparto se hace en
proporción, sin restricción artificial.

El rango `1..1000` es arbitrario pero razonable: cubre cualquier reparto realista
(hasta milésimas si se usan pesos grandes) y acota el tamaño del numerador en la
división, que se hace con `BigDecimal` de todos modos.

## Decisión 2: `division` ausente ≠ `division` vacía

- Campo ausente o `null` → reparto equitativo entre todos los miembros actuales. Es
  el comportamiento de hoy y lo que garantiza que ningún cliente existente se rompa.
- Lista vacía `[]` → `400`. Un gasto que no le toca a nadie no tiene sentido y
  rompería la invariante de suma (no habría dónde poner el monto).

Esta distinción es la que hace que el cambio sea aditivo. El frontend lo aprovecha:
el modo «Entre todos, en partes iguales» simplemente no manda el campo.

## Decisión 3: el peso se persiste

Se agrega `gasto_participantes.peso INTEGER NOT NULL DEFAULT 1`.

**Alternativa descartada: deducir los pesos de los `monto_adeudado` al editar.** Es
lossy. Con un gasto de `100.00` repartido `33.33 / 33.33 / 33.34`, deducir «1, 1, 1»
exige adivinar que la diferencia de un centavo es redondeo y no un peso distinto. Con
montos más grandes la ambigüedad es peor.

El `DEFAULT 1` es lo que hace que la migración sea invisible: `ddl-auto=update`
agrega la columna, las filas existentes quedan en `1`, y un gasto viejo releído tiene
todos los pesos iguales, que es exactamente el reparto que ya tenía.

## Decisión 4: quién absorbe el redondeo

Regla generalizada, en este orden:

1. El **pagador**, si figura en la división.
2. Si no, el participante de **mayor peso**.
3. A igualdad de peso, el de **menor id**.

El paso 1 conserva la regla que ya está en `CLAUDE.md` («el pagador de un gasto
absorbe los centavos sobrantes del redondeo») para todos los casos que hoy existen.
Los pasos 2 y 3 solo entran en juego en el caso nuevo —pagador excluido— y son
deterministas, que es lo único que realmente importa: sin un criterio fijo, dos
ediciones idénticas podrían repartir el centavo a personas distintas.

## Decisión 5: el cálculo proporcional

Con `total` = `montoUsdt` a 2 decimales, `pesoTotal` = suma de los pesos:

```
para cada participante i distinto del absorbente:
    monto_i = (total * peso_i / pesoTotal) redondeado a 2 decimales, HALF_UP
monto_absorbente = total - suma(monto_i)
```

Calcular el absorbente por resta, y no con su propia división, es lo que garantiza la
invariante de suma exacta sin depender de cómo caigan los redondeos. Es la misma
técnica que ya usa el reparto equitativo.

**Consecuencia aceptada:** el absorbente puede terminar con un monto que no respeta
su proporción exacta, por unos centavos. Con pesos grandes y montos chicos la
distorsión relativa crece (un gasto de `0.05` repartido 1:999 le da todo al
segundo). Es inherente a trabajar con 2 decimales y no se corrige.

## Decisión 6: el pagador puede quedar fuera de la división

Hoy `pagadorMiembro` exige que el pagador sea miembro del grupo, y eso se mantiene:
es quien puso la plata y tiene que estar en el balance. Lo que se levanta es la
suposición implícita de que además consume.

Efecto en los balances: el pagador suma `montoUsdt` en `pagado` y `0` en `adeudado`,
así que su balance sube por el total del gasto. Es correcto: pagó algo que no
consumió, se lo deben entero.

## Validación de la división

En este orden, todas con `400`:

1. Lista presente pero vacía.
2. Un `participanteId` repetido.
3. Un `participanteId` que no es miembro actual del grupo.
4. Un `peso` fuera de `1..1000` o ausente (lo cubre Bean Validation con `@Valid` en
   el campo, que valida cada elemento de la lista).

La comprobación 3 usa la misma fuente que el reparto equitativo
(`grupoParticipanteRepository.findByGrupoId`), así que no hay forma de que un
participante pase la validación y después no exista al repartir.

## Riesgos

- La columna nueva depende de que `ddl-auto=update` aplique el `DEFAULT`. Se verifica
  con un test que registra un gasto sin `division` y comprueba que las filas quedan
  con peso `1`.
- El formulario de gasto es la pantalla más cargada del frontend y sumarle un modo de
  reparto la complica. Se mitiga manteniendo el modo equitativo preseleccionado y
  colapsando la parte personalizada hasta que se elige.
