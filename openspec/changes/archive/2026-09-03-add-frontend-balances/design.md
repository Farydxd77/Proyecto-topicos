## Context

Ver `proposal.md` — Why. Lo que aporta el backend:

- `GET /api/grupos/{grupoId}/balances` → `BalanceDto(participante, balance)[]`.
  El backend garantiza que la suma de todos los balances sea exactamente `0`, y
  devuelve a **todos** los integrantes, incluidos los que están en cero.
- `GET /api/grupos/{grupoId}/liquidacion` → `TransferenciaDto(de, deId, para,
  paraId, monto)[]`. `de` y `para` son nombres legibles; `deId` y `paraId` son
  identificadores de participante. Sin deudas pendientes devuelve `[]`.
- Ambos endpoints exigen únicamente ser miembro: `403` si no lo es, `404` si el
  grupo no existe, con el `404` evaluado antes.
- Los montos están en USDT, porque es la unidad en la que el backend reparte
  `gasto_participantes`.
- `BalanceUtil` es el único componente del proyecto cuyos tests unitarios ya se
  ejecutaron y pasaron (11 casos), incluido el escenario Samaipata.

## Goals / Non-Goals

**Goals:**

- Traducir un número con signo a una frase que no haya que interpretar.
- Que quien mira encuentre su propia situación sin buscarse en una lista.
- Que los números nunca queden desactualizados respecto de los gastos.

**Non-Goals (nivel diseño):**

- No se recalcula ni se verifica el algoritmo en el cliente.
- No se introduce ninguna librería de gráficos.
- No se persiste ningún estado de "ya pagué".

## Decisions

### 1. El signo se traduce a una frase; el número se muestra siempre en positivo

Un balance positivo se presenta como «le deben X», uno negativo como «debe X», y
el cero como «está a mano». La cifra se muestra sin signo, y el sentido lo lleva la
frase.

- **Por qué:** «−200» obliga a recordar la convención del sistema para saber si eso
  significa que pagó de más o de menos. Es exactamente el tipo de ambigüedad que
  provoca discusiones en un grupo de viaje, que es el problema que la aplicación
  existe para evitar.
- **Por qué también el color es insuficiente:** distinguir deudor de acreedor solo
  por color falla con daltonismo y en escala de grises. El color acompaña la
  frase, no la reemplaza.
- **Alternativa descartada:** mostrar el número con signo y una leyenda que
  explique la convención — traslada a la persona un trabajo de interpretación que
  la interfaz puede hacer una sola vez.

### 2. Balances y liquidación son dos consultas independientes

`['balances', grupoId]` y `['liquidacion', grupoId]`, cada una a su endpoint.

- **Por qué no derivar la liquidación de los balances en el cliente:** reducir un
  conjunto de deudas a la lista **mínima** de transferencias es precisamente el
  algoritmo de `BalanceUtil`, ya implementado y probado. Reimplementarlo en el
  frontend duplicaría la lógica más delicada del sistema y garantizaría que las dos
  versiones se separen con el tiempo.
- **Alternativa descartada:** una única consulta que traiga ambas cosas — exigiría
  un endpoint nuevo en el backend, y este cambio declara no tocarlo.

### 3. Las mutaciones de gastos y de miembros invalidan también estas claves

Las mutaciones ya existentes de `add-frontend-gastos` (registrar, editar, eliminar)
y de `add-frontend-miembros` (agregar, quitar) se amplían para invalidar
`['balances', grupoId]` y `['liquidacion', grupoId]`.

- **Por qué se toca código de cambios anteriores:** el acoplamiento es real —un
  gasto cambia los balances— y tiene que vivir en algún lado. Ponerlo en las
  mutaciones que provocan el cambio es lo que garantiza que no se olvide; que las
  pantallas de balances "se refresquen solas" sin que nadie las invalide sería
  pensamiento mágico.
- **Por qué no `refetchInterval`:** sondear cada N segundos gasta peticiones cuando
  no pasa nada y sigue llegando tarde cuando pasa algo. La invalidación es exacta.
- **Alternativa descartada:** invalidar todo el caché del grupo tras cualquier
  mutación — más simple de escribir, pero vuelve a pedir datos que no cambiaron.

### 4. Lo propio se destaca comparando con el participante del perfil cacheado

Se compara `balance.participante.id` —y `deId` / `paraId` en las transferencias—
contra el `id` del participante leído de `['perfil']`, igual que hace
`add-frontend-grupos` para determinar el rol de creador.

- **Por qué la misma técnica:** ya está establecida en el proyecto, el dato está en
  caché, y respeta la convención de no duplicar en `AuthContext` nada que venga del
  backend.

### 5. El estado saldado se distingue del estado sin gastos

`[]` en la liquidación puede significar dos cosas distintas: que el grupo no tiene
gastos, o que los tiene y ya está todo compensado. La pantalla las distingue
mirando si hay gastos registrados —dato que la consulta `['gastos', grupoId]` ya
tiene en caché—.

- **Por qué importa:** «todavía no hay nada que saldar» y «ya están todos a mano»
  son mensajes muy distintos para quien organiza el viaje. El primero invita a
  cargar gastos; el segundo confirma que la cuenta cerró.
- **Alternativa descartada:** un único mensaje para ambos casos — pierde
  información que la aplicación ya tiene.

### 6. Los montos se muestran, no se suman

Igual que en gastos: los balances llegan como números JSON y la pantalla los
formatea, sin aritmética. En particular, la interfaz **no** verifica que la suma dé
cero.

- **Por qué no verificarlo:** es una invariante que el backend garantiza y que sus
  tests unitarios ya comprueban. Recalcularla en el cliente con aritmética de
  flotante podría dar `0.00000001` y mostrar una advertencia falsa sobre un
  resultado correcto.
- **Dónde sí se comprueba:** en la verificación manual de las tareas, contrastando
  contra el escenario Samaipata que los tests del backend ya cubren.

## Risks / Trade-offs

- **Los balances están en USDT y nadie paga en USDT en la práctica** → Mitigación:
  es la unidad en la que el backend reparte y la única común a gastos en monedas
  distintas. Convertir a una moneda local exigiría decidir cuál y a qué tasa, y
  cambiaría el contrato del backend; queda fuera de alcance y documentado.
- **La liquidación es un cálculo, no un registro:** no hay forma de marcar una
  transferencia como pagada, y la lista se recalcula entera ante cualquier cambio →
  Mitigación: la pantalla presenta la liquidación como una sugerencia de cómo
  saldar, no como una lista de tareas con estado. Registrar pagos sería una
  capacidad nueva del backend.
- **Quitar a un miembro con deudas** deja un grupo cuyos gastos históricos incluyen
  a alguien que ya no está → Mitigación: el backend calcula sobre lo que hay
  guardado; la pantalla muestra el resultado tal cual. Es una consecuencia real del
  modelo y se hace visible en lugar de disimularse.
- **Dos consultas separadas pueden verse en instantes distintos** si una resuelve
  antes que la otra tras una invalidación → Mitigación: ambas se invalidan juntas y
  el desfase es de milisegundos; ninguna de las dos muestra datos previos al cambio
  una vez resueltas.
- **Sin gastos cargados no se puede verificar nada de verdad:** todos los balances
  dan cero → Mitigación: las tareas de verificación parten de construir el
  escenario Samaipata completo, que es el mismo que los tests del backend ya
  validan y del que se conoce el resultado esperado.
