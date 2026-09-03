## Context

Ver `proposal.md` — Why. Lo que aporta el backend y condiciona este diseño:

- `GastoResponse(id, grupoId, descripcion, monto, moneda, monedaNombre, montoUsdt,
  tasaCambio, pagador, fecha, division)` y
  `GastoResumenDto(id, descripcion, monto, moneda, monedaNombre, montoUsdt,
  pagador, fecha)` — la lista **no** trae `tasaCambio` ni `division`.
- `GastoParticipanteDto(participante, montoAdeudado)`.
- `RegistrarGastoRequest` y `ActualizarGastoRequest` tienen la misma forma:
  `descripcion` `@NotBlank @Size(max = 255)`, `monto` `@NotNull @Positive
  @Digits(integer = 12, fraction = 8)`, `moneda` opcional `@Size(max = 10)`,
  `monedaNombre` opcional `@Size(max = 50)`, `pagadorId` `@NotNull`, `fecha`
  `@NotNull` como `LocalDate`.
- **Autorización**: todas las operaciones exigen únicamente ser miembro
  (`grupoDondeEsMiembro`). No hay comprobación de creador. Cualquier miembro
  registra, edita y elimina cualquier gasto del grupo.
- **División**: el backend reparte `montoUsdt` entre **todos** los miembros
  actuales, a 2 decimales con `HALF_UP`, y el pagador absorbe el sobrante. No se
  puede excluir a nadie ni asignar proporciones.
- **Conversión**: USDT no consulta nada y usa tasa `1`. Cualquier otra moneda
  soportada consulta CriptoYa; si falla, el backend lanza
  `ServicioExternoNoDisponibleException` → `503`. Una moneda fuera de
  `MonedasSoportadas` → `400` con «Moneda no soportada: X».
- `MonedasSoportadas` define 13 fiat y 35 cripto como conjuntos de símbolos, sin
  nombres legibles.
- Los `BigDecimal` de Java los serializa Jackson como **números** JSON, no como
  cadenas.

## Goals / Non-Goals

**Goals:**

- Que en ningún momento se confunda el monto pagado con su equivalente en USDT.
- Que la precisión de los montos no se degrade en el camino de ida ni de vuelta.
- Que el `503` de cotización se lea como «volvé a intentar», no como «tus datos
  están mal».

**Non-Goals (nivel diseño):**

- No se replica el cálculo de la división en el cliente: se muestra lo que el
  backend devuelve.
- No se consulta CriptoYa desde el navegador.
- No se introduce una librería de manejo de dinero.

## Decisions

### 1. Los montos se envían y se muestran como cadenas; nunca se opera con ellos

Los campos de monto se manejan como `string` en el formulario y se envían tal
cual. Los montos que llegan del backend se muestran formateados a partir de su
representación textual, sin aritmética en el cliente.

- **Por qué:** `monto` admite hasta 8 decimales y `tasaCambio` es
  `DECIMAL(10,6)`. El `number` de JavaScript es un flotante de doble precisión:
  `0.1 + 0.2` no da `0.3`, y un valor cripto como `0.00000001` sobrevive pero
  cualquier operación encadenada acumula error. Como el frontend **no necesita**
  calcular nada —el backend ya calculó conversión y división—, la forma más segura
  es no operar.
- **Cuidado concreto:** Jackson serializa `BigDecimal` como número JSON, así que
  `JSON.parse` ya lo convierte a `number` antes de que el código lo toque. Por eso
  la regla operativa es: mostrar y formatear, jamás sumar, restar ni comparar
  montos. La única suma que la interfaz podría querer hacer —verificar que la
  división cuadra— se declara explícitamente fuera de alcance como cálculo y solo
  se comprueba durante la verificación manual.
- **Alternativa descartada:** incorporar `decimal.js` o `big.js` — resolvería la
  aritmética, pero no hay aritmética que resolver; sería una dependencia para un
  problema que este diseño evita por construcción.

### 2. El catálogo de monedas se espeja en `src/lib/monedas.ts` con nombres legibles

Se crea un módulo con los mismos símbolos que `MonedasSoportadas`, agrupados en
fiat y cripto, y con el nombre completo de cada uno.

- **Por qué el frontend aporta los nombres:** el backend solo conoce símbolos; el
  campo `monedaNombre` es un dato que el cliente **envía**, no que reciba
  calculado. Alguien tiene que traducir `BOB` a «Boliviano», y hacerlo en el
  frontend evita meter una tabla de traducción en el dominio del backend.
- **Por qué agrupadas:** treinta y cinco criptos en una lista plana con trece fiat
  hace difícil encontrar la moneda del país. La agrupación es puramente de
  presentación.
- **Riesgo aceptado:** el catálogo puede desincronizarse si el backend agrega una
  moneda. Mitigación: el `400` de moneda no soportada se maneja igual, y el módulo
  lleva un comentario apuntando a `MonedasSoportadas` como fuente.
- **Alternativa descartada:** un endpoint `GET /api/monedas` — sería lo correcto a
  futuro, pero exige tocar el backend, y este cambio declara no hacerlo.

### 3. Dos claves de consulta, y las mutaciones invalidan ambas

`['gastos', grupoId]` para la lista y `['gasto', grupoId, gastoId]` para el
detalle. Registrar, editar y eliminar invalidan la lista; editar y eliminar
invalidan además el detalle correspondiente.

- **Por qué la clave del detalle incluye `grupoId`:** el identificador de gasto es
  único globalmente, pero la ruta es anidada y el backend valida que el gasto
  pertenezca al grupo. Incluir ambos hace que la clave espeje la ruta real y evita
  colisiones si el mismo gasto se mirara desde un contexto distinto.
- **Qué más se invalida:** cuando exista la pantalla de balances, sus claves
  también deberán invalidarse desde acá, porque un gasto cambia los balances. Ese
  enganche pertenece al cambio de balances y se anota allí.

### 4. La lista y el detalle son pantallas distintas, no una expansión

El detalle vive en su propia ruta bajo `/grupos/:id/gastos/:gastoId`.

- **Por qué:** `GastoResumenDto` no trae `tasaCambio` ni `division`, así que
  expandir una fila exigiría igualmente pedir el detalle. Con ruta propia, el
  detalle es enlazable y compartible, y el mapa de rutas sigue espejando la
  jerarquía del backend.

### 5. El monto original es el dato principal; el equivalente en USDT es secundario

En cada gasto se muestra primero `monto` con su símbolo de moneda, y el
`montoUsdt` como información derivada explícitamente etiquetada. Cuando la moneda
ya es USDT, no se repite la cifra.

- **Por qué:** el monto original es lo que la persona efectivamente pagó y lo
  único que puede verificar contra su recibo. El equivalente es un cálculo del
  sistema. Presentarlos con la misma jerarquía invita a confundirlos, y en un
  gasto en BOB la diferencia entre ambas cifras es de un orden de magnitud.
- **Por qué no repetir en USDT:** mostrar «100 USDT (equivale a 100 USDT)» hace
  dudar de si son dos cosas distintas.

### 6. El `503` se trata como estado de reintento, no como error de formulario

Ante un `ApiError` con `status === 503`, el formulario conserva todos sus valores,
muestra un aviso de que la cotización no está disponible ahora, y ofrece reintentar
el envío.

- **Por qué separarlo del `400`:** un `400` significa «corregí lo que cargaste»; un
  `503` significa «lo que cargaste está bien, el problema es de afuera». Tratarlos
  igual manda a la persona a revisar datos correctos.
- **Por qué conservar el formulario:** es la diferencia entre pulsar reintentar y
  volver a cargar cinco campos.

### 7. El pagador se elige de `grupo.miembros`, ya disponible en caché

El selector de pagador se llena desde la consulta `['grupo', grupoId]` que la
pantalla de detalle de grupo ya tiene cargada.

- **Por qué:** el backend valida con `400` que el pagador sea miembro; ofrecer solo
  miembros previene el error en origen sin ninguna petición adicional.

## Risks / Trade-offs

- **La división se reparte siempre entre todos los miembros actuales**, sin poder
  excluir a nadie → es una limitación del backend, no una omisión de este cambio.
  Se documenta como tal; permitir excluir participantes exigiría cambiar
  `gasto_participantes` y el servicio.
- **Cambiar la composición del grupo no recalcula los gastos ya registrados:** la
  división quedó guardada con los miembros del momento → Mitigación: se hace
  visible en el detalle del gasto, que muestra el reparto real guardado. Es
  información honesta, aunque pueda sorprender.
- **Cualquier miembro puede editar o borrar el gasto de otro**, incluido uno que no
  pagó → es la regla del backend. La interfaz no la disimula: cualquier miembro ve
  las acciones. Restringirlas en el cliente daría una falsa sensación de control.
- **Editar un gasto vuelve a consultar CriptoYa** y puede aplicar una tasa distinta
  a la original, cambiando el equivalente sin que se haya tocado el monto →
  Mitigación: el detalle muestra siempre la tasa aplicada, de modo que la
  diferencia sea explicable y no un misterio.
- **El catálogo de monedas del frontend puede quedar corto** frente al del backend
  → Mitigación: el `400` de moneda no soportada se maneja, y la verificación
  contrasta ambas listas.
- **El `503` depende de un servicio de terceros** que no se puede provocar a
  voluntad contra el backend real → Mitigación: se verifica en modo simulado, donde
  el mock sí puede devolverlo, y contra el real se comprueba al menos el camino de
  USDT, que no consulta nada.
