## 1. Contratos y catálogo de monedas

- [x] 1.1 En `src/api/types.ts` añadir los contratos de gasto espejando los records
  del backend: `RegistrarGastoRequest` y `ActualizarGastoRequest`
  (`descripcion`, `monto`, `moneda?`, `monedaNombre?`, `pagadorId`, `fecha`),
  `GastoParticipanteDto { participante: ParticipanteDto; montoAdeudado: number }`,
  `GastoResumenDto` y `GastoResponse` con todos sus campos. Documentar en
  comentario que `fecha` es `LocalDate` en formato `YYYY-MM-DD` y que los montos
  llegan como números JSON porque Jackson serializa `BigDecimal` así. Verificar con
  `npx tsc --noEmit`.
- [x] 1.2 Crear `src/lib/monedas.ts` con el catálogo espejo de `MonedasSoportadas`
  del backend: los 13 símbolos fiat y los 35 cripto, cada uno con su nombre legible
  en español, agrupados en dos listas. Incluir un comentario que señale
  `util/MonedasSoportadas.java` como fuente de verdad. Verificar contrastando
  símbolo por símbolo contra el archivo del backend que no falta ni sobra ninguno.
- [x] 1.3 Crear `src/api/gastos.ts` con las cinco funciones sobre `apiFetch` y
  rutas relativas anidadas bajo `/grupos/{grupoId}/gastos`: `listarGastos`,
  `obtenerGasto`, `registrarGasto`, `actualizarGasto` y `eliminarGasto`. Verificar
  con `npx tsc --noEmit`.

## 2. Sin API simulada

- [x] 2.1 No se crean handlers simulados para los endpoints de gastos: MSW se
  elimina del proyecto. Los codigos 400 de moneda no soportada y de pagador no
  miembro se verifican contra el backend real. Verificar que `src/mocks/` no
  existe.

## 3. Lista de gastos en el grupo

- [x] 3.1 Añadir a `GrupoDetallePage` la sección de gastos con la consulta
  `['gastos', grupoId]`, mostrando de cada gasto descripción, monto con su símbolo
  de moneda, equivalente en USDT etiquetado como tal, pagador y fecha. Verificar
  contra el backend real con gastos en dos monedas distintas que ambas cifras se
  distinguen sin ambigüedad.
- [x] 3.2 Verificar que un gasto registrado en USDT no muestra una conversión
  redundante: se ve una sola cifra, no «100 USDT (equivale a 100 USDT)».
- [x] 3.3 Añadir el estado vacío con invitación a registrar el primer gasto, y el
  estado de error con reintento. Verificar ambos: en un grupo recién creado, y
  deteniendo el backend.
- [x] 3.4 Hacer que cada gasto lleve a su detalle en
  `/grupos/:id/gastos/:gastoId`, y registrar esa ruta protegida en
  `src/router.tsx`. Verificar que la URL abierta directamente muestra el mismo
  gasto.

## 4. Registro de gasto

- [x] 4.1 Crear el formulario de registro con descripción, monto, moneda, pagador y
  fecha. El pagador se elige de `grupo.miembros` tomado del caché
  `['grupo', grupoId]`, y la moneda del catálogo de `src/lib/monedas.ts` agrupado
  en fiat y cripto. Verificar que ninguno de los dos se puede escribir a mano.
- [x] 4.2 Conectar el registro como mutación que invalida `['gastos', grupoId]`.
  Verificar contra el backend real que el gasto aparece en la lista sin recargar y
  que en PostgreSQL quedan las filas en `gastos` y en `gasto_participantes`.
- [x] 4.3 Añadir la validación previa: descripción obligatoria y de hasta 255
  caracteres, monto obligatorio y mayor que cero. Verificar que con monto `0`, con
  monto negativo y con descripción en blanco no sale ninguna petición a la red.
- [x] 4.4 Verificar el registro sin elegir moneda: el gasto queda en USDT con tasa
  1, y el registro funciona aun con el servicio de cotización caído.
- [x] 4.5 Verificar el registro con una moneda fiat y con una cripto contra el
  backend real, comprobando que el equivalente en USDT y la tasa que devuelve el
  backend son coherentes con el monto original.
- [x] 4.6 Verificar el `400` de moneda no soportada enviando un símbolo inventado
  desde las herramientas del navegador, y el `400` de pagador no miembro enviando
  un `pagadorId` ajeno al grupo. Comprobar que ambos muestran el mensaje del
  backend y conservan lo cargado.

## 5. Detalle del gasto

- [x] 5.1 Crear `src/pages/GastoDetallePage.tsx` con la consulta
  `['gasto', grupoId, gastoId]`, mostrando descripción, monto original con su
  moneda, equivalente en USDT, tasa aplicada, pagador, fecha y el reparto por
  participante. Verificar contra el backend real con un gasto de un grupo de tres
  miembros.
- [x] 5.2 Presentar el reparto dejando claro que es una deuda hacia el pagador, e
  indicar cuál de las filas corresponde a quien pagó. Verificar que se entiende sin
  explicación adicional.
- [x] 5.3 Verificar manualmente el reparto en dos casos. En un gasto **en USDT** con
  división no exacta (por ejemplo 100,01 entre 4), la suma coincide exactamente con
  el monto y el pagador absorbe el sobrante. En un gasto **convertido** desde otra
  moneda cuyo monto en USDT tiene más de dos decimales, la suma puede diferir en
  menos de un centavo, porque `montoUsdt` se guarda con seis decimales y cada parte
  del reparto con dos; comprobar que la pantalla muestra los valores del backend sin
  recalcularlos ni ajustarlos.
- [x] 5.4 Manejar el `404` de gasto inexistente y el `403` de no miembro con
  mensajes distintos, cada uno con una salida hacia el grupo o hacia la lista de
  grupos. Verificar navegando a un `gastoId` inventado, y abriendo un gasto de un
  grupo ajeno con otra cuenta.

## 6. Edición y eliminación

- [x] 6.1 Añadir la edición del gasto reutilizando el formulario del registro con
  los valores actuales, como mutación que invalida `['gasto', grupoId, gastoId]` y
  `['gastos', grupoId]`. Verificar que cambiar el monto actualiza el equivalente y
  el reparto en pantalla sin recargar.
- [x] 6.2 Verificar el cambio de moneda de un gasto ya registrado: la pantalla
  muestra la moneda nueva con su tasa y equivalente recalculados, y el detalle
  refleja la tasa efectivamente aplicada en la edición.
- [x] 6.3 Añadir la opción de cancelar la edición, que restaura los valores
  guardados sin enviar peticiones. Verificar en la pestaña de red.
- [x] 6.4 Añadir la eliminación con diálogo de confirmación propio que nombre el
  gasto. Verificar que cancelar no envía petición y que confirmar lo quita de la
  lista sin recargar y responde `404` al consultarlo después.
- [x] 6.5 Verificar que cualquier miembro —no solo el creador del grupo— puede
  registrar, editar y eliminar: hacer las tres operaciones desde una cuenta miembro
  no creadora y comprobar que ninguna devuelve `403`.

## 7. Servicio de cotización no disponible

- [x] 7.1 Manejar el `ApiError` con `status === 503` como estado de reintento: un
  aviso de que la conversión no está disponible ahora, distinto de un error de
  validación, conservando todos los campos cargados y ofreciendo reintentar el
  envío. Verificar contra el backend real registrando un gasto en una cripto
  soportada que no tenga mercado en CriptoYa; si todas las soportadas responden,
  dejar constancia de que el camino no se pudo provocar y de que el manejo está
  implementado.
- [x] 7.2 Verificar que con la cotización caída la lista de gastos ya registrados
  se sigue mostrando con normalidad y que la indisponibilidad no se presenta como
  un error de esos gastos.
- [x] 7.3 Verificar que reintentar tras el `503` registra el gasto sin volver a
  cargar los campos.

## 8. Verificación final

- [x] 8.1 Recorrer el flujo completo contra el backend real en un grupo de tres
  miembros: registrar un gasto en BOB, otro en USDT y otro en una cripto; abrir el
  detalle de cada uno y contrastar monto, equivalente, tasa y reparto; editar uno;
  eliminar otro. Comprobar en PostgreSQL que `gastos` y `gasto_participantes`
  quedan consistentes.
- [x] 8.3 Ejecutar `npx tsc --noEmit` y `npx oxlint` desde `frontend/` y comprobar
  que no hay errores.
