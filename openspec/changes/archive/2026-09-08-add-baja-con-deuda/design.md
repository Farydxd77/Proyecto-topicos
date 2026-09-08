## La aritmética

Cada baja asumida introduce dos ajustes al balance, y por construcción se cancelan:

```
balance(p) = pagado(p) − adeudado(p) + pagosRealizados(p) − pagosRecibidos(p)
             + condonado(p) − absorbido(p)
```

Para una baja de la persona `s` con saldo `S` al irse, asumida por `k` miembros:

```
condonado(s) = −S
absorbido(m) = −S / k     para cada miembro m que la asume
```

Con `S = −300` (Carla se fue debiendo 300) y `k = 2`:

- Carla: `−300 + 300 = 0`
- Ana y Beto: `−150` cada uno
- Suma del ajuste: `+300 − 300 = 0` → la invariante de suma cero se mantiene

**Funciona igual con el signo opuesto.** Si el que se va tiene saldo `+300` —el grupo
le debe—, `condonado = −300` lo lleva a cero y `absorbido = −150` **sube** el balance
de cada uno en 150, porque dejan de deberle. Un solo mecanismo con signo cubre los
dos casos, así que `baja_participantes.monto` admite valores negativos y está
documentado.

## Decisión 1: dos tablas, con el reparto congelado

`bajas_grupo` (la cabecera: quién se fue, con cuánto, y en qué estado) y
`baja_participantes` (quién absorbe cuánto). Es el mismo par que
`gastos` / `gasto_participantes`.

**Alternativa descartada: repartir en el momento del cálculo, sin persistir.** Si el
grupo asume una deuda y después entra un miembro nuevo, un reparto calculado al vuelo
le cargaría parte de una deuda que se generó antes de que llegara. Congelarlo es lo
mismo que ya se hace con la división de un gasto, y por el mismo motivo.

**Alternativa descartada: modelar la asunción como `Pago`s.** Zeroaría al que se fue
y debitaría al resto con el modelo existente, pero mentiría en el historial: la lista
de pagos mostraría transferencias que nunca ocurrieron. El registro de pagos tiene
que seguir siendo el de la plata que se movió de verdad.

## Decisión 2: el saldo se congela al momento de la baja

`bajas_grupo.saldo` guarda el balance que la persona tenía cuando salió, no se
recalcula después.

Esto importa porque el saldo de un ex-miembro **puede seguir moviéndose**: alguien
podría registrar un pago hacia él, o editar un gasto viejo en el que participaba. Si
el reparto se calculara sobre el saldo vigente, la decisión que tomó el creador
dejaría de corresponder con lo que se repartió.

**Consecuencia aceptada:** si el saldo del que se fue cambia después de asumida la
baja, su balance deja de ser exactamente `0.00` —queda en la diferencia—, y eso es lo
correcto: es plata nueva que apareció después de la decisión y que el grupo no
decidió sobre ella.

## Decisión 3: la decisión es del creador, y llega después de la baja

La salida —propia o por expulsión— crea la baja en estado `PENDIENTE`. El creador la
resuelve con `PUT /api/grupos/{id}/bajas/{bajaId}` y `{asumir: true|false}`.

**Alternativa descartada: preguntar en el momento de quitar al miembro.** Solo
funcionaría para la expulsión. Cuando alguien abandona por su cuenta no hay nadie del
grupo en pantalla para decidir, y hacer que la salida dependa de una decisión ajena
la bloquearía.

**Alternativa descartada: votación de todos los miembros.** Es lo que el usuario
describió («si los demás quieren asumir»), y es defendible, pero exige un modelo de
votos, un criterio de mayoría, qué pasa si alguien no vota nunca, y una pantalla
propia. El creador ya concentra todas las atribuciones de administración del grupo
(editar, borrar, agregar y quitar miembros), así que resolver la baja encaja con el
modelo de permisos que ya existe. Queda anotado como el punto a revisar si el grupo
quiere una decisión más colectiva.

## Decisión 4: `NO_ASUMIDA` no cambia ningún número

Es importante: rechazar no borra la deuda ni la mueve. El balance del que se fue
queda exactamente como estaba. Lo único que cambia es **dónde se muestra**: pasa de
estar mezclado entre los balances de los que siguen a tener un apartado propio,
«Deudas sin resolver».

Eso hace que el trabajo de esta rama sea casi todo de interfaz, y que el cálculo del
backend solo tenga que ignorar las bajas que no son `ASUMIDA`.

## Decisión 5: el estado es un enum de tres valores

`PENDIENTE`, `ASUMIDA`, `NO_ASUMIDA`, persistido con `@Enumerated(EnumType.STRING)`.

Un booleano `asumida` no alcanza: haría indistinguible «todavía no lo decidieron» de
«decidieron que no», y la interfaz necesita separar exactamente eso —una es una
tarea pendiente del creador, la otra es un tema cerrado que quedó anotado—.

`EnumType.STRING` y no `ORDINAL` porque un ordinal rompe en cuanto se agrega un valor
en el medio, y en la base se lee como un número sin significado.

## Orden de comprobaciones

Para `PUT /api/grupos/{id}/bajas/{bajaId}`:

1. Grupo inexistente → `404`
2. Solicitante no es el creador → `403`
3. Baja inexistente o de otro grupo → `404`
4. Baja ya resuelta (`ASUMIDA` o `NO_ASUMIDA`) → `409`
5. `asumir: true` sin miembros actuales que puedan absorber → `400`

El paso 4 usa `409` y no `400` porque no es un dato inválido: es una operación
correcta sobre un recurso que ya está en otro estado.

## Riesgos

- Es la primera vez que el balance de alguien se altera por algo que no es un gasto
  ni un pago. Se acota con tests que verifican la suma cero en cada combinación:
  baja asumida, no asumida, pendiente, y con actividad posterior a la baja.
- Un grupo con muchas bajas asumidas acumula ajustes que hacen más difícil explicar
  de dónde sale un balance. Se mitiga mostrando cada baja con su reparto en la
  interfaz, en vez de esconder el ajuste dentro del número.
