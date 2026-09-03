## 1. Contratos y capa de API

- [x] 1.1 En `src/api/types.ts` añadir
  `BalanceDto { participante: ParticipanteDto; balance: number }` y
  `TransferenciaDto { de: string; deId: number; para: string; paraId: number; monto: number }`,
  documentando en comentario que los montos están en USDT y que el backend
  garantiza que la suma de los balances sea exactamente cero. Verificar con
  `npx tsc --noEmit`.
- [x] 1.2 Crear `src/api/balances.ts` con `obtenerBalances(grupoId)` y
  `obtenerLiquidacion(grupoId)` sobre `apiFetch` con rutas relativas. Verificar con
  `npx tsc --noEmit`.

## 2. Sin API simulada

- [x] 2.1 No se replica el algoritmo de `BalanceUtil` en ningun mock: MSW se elimina
  del proyecto y el escenario Samaipata se verifica contra el backend real, que es
  donde ese algoritmo vive y esta probado. Verificar que `src/mocks/` no existe.

## 3. Sección de balances

- [x] 3.1 Añadir a `GrupoDetallePage` la sección de balances con la consulta
  `['balances', grupoId]`, listando a cada integrante con su balance. Verificar
  contra el backend real en un grupo con gastos que aparecen todos los integrantes,
  incluidos los que están en cero.
- [x] 3.2 Traducir el signo a una frase: balance positivo como «le deben», negativo
  como «debe» y cero como «está a mano», mostrando siempre la cifra sin signo.
  Acompañar con color, pero sin que el color sea el único indicador. Verificar que
  la situación de cada persona se entiende sin conocer la convención de signos.
- [x] 3.3 Añadir el estado de carga y el de error con reintento. Verificar
  deteniendo el backend y comprobando que la sección no queda en carga permanente y
  que reintentar funciona sin recargar la página.
- [x] 3.4 Añadir el mensaje para un grupo sin gastos: todos en cero y una
  explicación de que todavía no hay nada que saldar. Verificar en un grupo recién
  creado.

## 4. Sección de liquidación

- [x] 4.1 Añadir la sección de liquidación con la consulta
  `['liquidacion', grupoId]`, mostrando cada transferencia con quién paga, quién
  cobra y el monto, presentada como la forma de quedar a mano con la menor cantidad
  de movimientos. Verificar contra el backend real con un grupo con deudas.
- [x] 4.2 Distinguir el estado saldado del estado sin gastos, mirando si hay gastos
  registrados en el caché `['gastos', grupoId]`: «ya están todos a mano» cuando hay
  gastos y la liquidación viene vacía, y «todavía no hay nada que saldar» cuando no
  hay gastos. Verificar los dos casos: un grupo recién creado, y un grupo donde
  todos pagaron lo mismo.
- [x] 4.3 Añadir el estado de error con reintento, igual que en balances. Verificar
  deteniendo el backend.

## 5. Destacar lo propio

- [x] 5.1 Señalar el balance de la persona autenticada comparando
  `balance.participante.id` con el `id` del participante leído de `['perfil']` ya
  cacheado, sin duplicarlo en `AuthContext`. Verificar con dos cuentas del mismo
  grupo que cada una ve destacada su propia fila y no la de la otra.
- [x] 5.2 Señalar en la liquidación las transferencias donde la persona figura como
  quien paga o quien cobra, comparando contra `deId` y `paraId`. Verificar con dos
  cuentas que cada una ve destacadas las suyas.
- [x] 5.3 Verificar el caso de balance cero: la persona ve indicado que está a mano
  y no una fila destacada que sugiera acción pendiente.

## 6. Mantener los números al día

- [x] 6.1 Ampliar las tres mutaciones de gastos (registrar, editar, eliminar) para
  que además invaliden `['balances', grupoId]` y `['liquidacion', grupoId]`.
  Verificar registrando un gasto y comprobando que los balances cambian sin
  recargar la página.
- [x] 6.2 Ampliar las dos mutaciones de miembros (agregar, quitar) con las mismas
  invalidaciones. Verificar agregando un miembro a un grupo con gastos y
  comprobando que los balances reflejan la composición nueva.
- [x] 6.3 Verificar la eliminación: borrar un gasto y comprobar que la liquidación
  deja de considerarlo, sin recargar.

## 7. Permisos y errores

- [x] 7.1 Manejar el `403` con un mensaje de que no tiene acceso al grupo y una
  salida hacia la lista de grupos. Verificar con dos cuentas: abrir con la cuenta B
  la URL de un grupo creado por la cuenta A.
- [x] 7.2 Manejar el `404` con un mensaje distinto, indicando que el grupo no
  existe. Verificar navegando a un `grupoId` inventado.
- [x] 7.3 Verificar que cualquier miembro —no solo el creador— ve balances y
  liquidación completos, consultándolos desde una cuenta miembro no creadora.

## 8. Verificación final

- [x] 8.1 Reproducir el escenario Samaipata completo contra el backend real: un
  grupo de cuatro integrantes donde Ana paga un gasto de 800 USDT que se reparte
  entre los cuatro. Comprobar que la pantalla muestra Ana +600, y Beto, Carla y
  Diego −200 cada uno, y que la liquidación son exactamente 3 transferencias de 200
  hacia Ana. Es el mismo escenario que los tests de `BalanceUtil` ya validan, así
  que el resultado esperado se conoce de antemano.
- [x] 8.2 Verificar el caso compensado: agregar un segundo gasto que deje a todos en
  cero y comprobar que la liquidación pasa a estar vacía y la pantalla dice que
  están todos a mano.
- [x] 8.4 Ejecutar `npx tsc --noEmit` y `npx oxlint` desde `frontend/` y comprobar
  que no hay errores.
- [x] 8.5 Recorrer la aplicación completa de punta a punta contra el backend real
  como cierre de todo el frontend: registrarse, crear un grupo, agregar miembros,
  cargar gastos en tres monedas distintas, revisar la división de cada uno, y leer
  los balances y la liquidación. Comprobar que ninguna pantalla queda colgada y que
  todos los datos persisten tras recargar.
