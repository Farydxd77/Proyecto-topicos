## Why

Con grupos y miembros el frontend ya tiene sobre quién repartir, pero todavía no
hay nada que repartir: registrar gastos es la razón por la que existe la
aplicación. El backend expone los cinco endpoints de
`/api/grupos/{grupoId}/gastos` y además resuelve algo que ninguna interfaz debería
improvisar: cada gasto puede registrarse en su propia moneda —trece fiat y más de
treinta cripto— y el backend lo convierte a USDT contra CriptoYa, guarda la tasa
usada, y reparte el monto convertido entre los miembros dejando que el pagador
absorba el sobrante del redondeo.

Todo eso ya funciona del lado del servidor. Falta la pantalla que lo use y que
muestre con honestidad lo que está pasando: cuánto se pagó, en qué moneda, a qué
tasa, y cuánto le toca a cada uno.

## What Changes

- El detalle del grupo gana una sección de gastos con la lista de los registrados,
  mostrando de cada uno la descripción, el monto en su moneda original, su
  equivalente en USDT, quién pagó y la fecha.
- Nueva pantalla de detalle de gasto: además de lo anterior, la tasa de cambio
  aplicada y la división completa, con lo que le corresponde a cada participante.
- Registro de un gasto: descripción, monto, moneda, quién pagó y fecha. El pagador
  se elige entre los miembros del grupo; la moneda, entre las soportadas.
- Edición y eliminación de un gasto, disponibles para cualquier miembro del grupo.
- La interfaz distingue con claridad el monto original del monto en USDT, y nunca
  los presenta como si fueran la misma cifra.
- Manejo del caso en que CriptoYa no responde: el backend devuelve `503` y la
  aplicación lo explica como una indisponibilidad temporal, no como un error de
  los datos cargados.
- Nuevo módulo `src/api/gastos.ts`, nuevos contratos en `src/api/types.ts`, y la
  lista de monedas soportadas espejando `MonedasSoportadas` del backend.
- Los mocks de MSW se amplían con los cinco endpoints y una conversión simulada.

## Non-Goals

- No se calculan balances ni liquidación: es el cambio `add-frontend-balances`.
- No se elige quién participa de cada gasto: el backend reparte siempre entre
  **todos** los miembros actuales del grupo, y no expone forma de excluir a nadie
  ni de asignar porcentajes desiguales.
- No se editan los montos adeudados de la división a mano: los calcula el backend.
- No se consulta la cotización desde el frontend: la conversión la hace el backend
  al registrar o editar, y el frontend muestra la tasa que le devuelve.
- No se muestra un histórico de tasas ni gráficos de cotización.
- No se adjuntan comprobantes ni fotos.
- No hay filtrado, búsqueda ni paginación de gastos.
- No se toca `backend/`.

## Capabilities

### New Capabilities

- `frontend-gastos`: las pantallas con las que un miembro registra los gastos de un
  grupo, los consulta, los edita y los elimina. Cubre la selección de moneda y la
  presentación honesta de la conversión a USDT —monto original, equivalente y tasa
  aplicada—, la visualización de cómo queda repartido el gasto entre los miembros,
  y el comportamiento cuando el servicio externo de cotización no está disponible.

### Modified Capabilities

<!-- Ninguna. -->

## Impact

- **Código nuevo**: `src/api/gastos.ts`, `src/lib/monedas.ts` (catálogo espejo de
  `MonedasSoportadas`), `src/pages/GastoDetallePage.tsx`, y los componentes de
  formulario de gasto, selector de moneda y lista de gastos.
- **Código modificado**: `src/api/types.ts` (contratos de gasto),
  `src/pages/GrupoDetallePage.tsx` (sección de gastos), `src/router.tsx` (ruta de
  detalle de gasto), `src/mocks/db.ts` y `src/mocks/handlers.ts`.
- **APIs consumidas**: `POST`, `GET`, `GET /{gastoId}`, `PUT /{gastoId}` y
  `DELETE /{gastoId}` bajo `/api/grupos/{grupoId}/gastos`.
- **Estado de servidor**: claves `['gastos', grupoId]` para la lista y
  `['gasto', grupoId, gastoId]` para el detalle.
- **Dependencias**: ninguna nueva. Los montos se manejan como cadenas y números
  del propio JavaScript; no se incorpora una librería de decimales (ver design).
- **Requisito previo**: `conectar-backend-real`, `add-frontend-grupos` y
  `add-frontend-miembros` aplicados.
