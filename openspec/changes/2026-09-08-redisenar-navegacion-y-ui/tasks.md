## 1. Sistema de diseño

- [x] 1.1 En `frontend/src/index.css`, declarar los tokens con `@theme`: la escala
  `marca` (verde de marca), `tinta` (neutro cálido para texto y bordes), `lienzo`
  (fondo), y los semánticos `favor` y `contra`. Ajustar `@layer base` para usarlos.
  Verificar con `cd frontend && npm run build`.
- [x] 1.2 Crear `components/Card.tsx`: superficie común con `titulo` opcional,
  `descripcion`, `accion` (nodo a la derecha del título) y `children`. Verificar con
  `npx tsc --noEmit`.
- [x] 1.3 Crear `components/Monto.tsx`: renderiza un monto con cifras tabulares,
  tamaño de una escala fija (`heroe` | `destacado` | `normal` | `chico`) y color
  semántico opcional según el signo. Sin aritmética. Verificar con `npx tsc --noEmit`.
- [x] 1.4 Crear `components/Etiqueta.tsx`: la píldora que hoy se repite a mano
  (Creador, Vos, Ya no integra el grupo, pagó), con tonos `neutro` | `marca` | `aviso`
  | `contra`. Verificar con `npx tsc --noEmit`.
- [x] 1.5 Crear `components/EstadoVacio.tsx`: título, explicación y acción opcional.
  Verificar con `npx tsc --noEmit`.
- [x] 1.6 Ampliar `components/Boton.tsx` con variantes `primario` | `secundario` |
  `fantasma` | `peligro` y tamaños `normal` | `chico`, manteniendo la API actual.
  Verificar con `npx tsc --noEmit` y `npx oxlint src`.
- [x] 1.7 Ajustar `components/Campo.tsx` a los tokens nuevos, conservando la
  asociación de error y ayuda por `aria-describedby`. Verificar con `npx tsc --noEmit`.

## 2. Navegación

- [x] 2.1 Crear `components/Pestanas.tsx`: lista de `NavLink` con estado activo
  visible, scroll horizontal en pantalla chica y `aria-current`. Verificar con `npx
  tsc --noEmit` y `npx oxlint src`.
- [x] 2.2 Crear `pages/GrupoLayout.tsx`: consulta del grupo (la única de la sección),
  manejo de 403/404 con la distinción actual, cabecera con nombre, descripción y
  acciones del creador (editar, eliminar), las pestañas, y `<Outlet />` con el
  contexto del grupo y del participante propio. Verificar con `npx tsc --noEmit`.
- [x] 2.3 En `router.tsx`, reemplazar la ruta plana por rutas anidadas: `/grupos/:id`
  con `GrupoLayout` e hijas `index` (resumen), `gastos`, `gastos/:gastoId`, `pagos`,
  `balances`, `miembros`. Verificar con `npx tsc --noEmit`.
- [x] 2.4 Actualizar `components/Layout.tsx` y `components/Navegacion.tsx` al sistema
  nuevo: barra con la marca, ancho de contenido mayor y menú de usuario. Verificar con
  `npx tsc --noEmit` y `npx oxlint src`.

## 3. Las secciones pierden su cabecera

- [x] 3.1 Quitar el `<section>` con `<h2>` y descripción propios de
  `components/PanelTotales.tsx`, `SeccionGastos.tsx`, `SeccionPagos.tsx`,
  `SeccionBalances.tsx`, `SeccionBajas.tsx` y `GestionMiembros.tsx`, dejando que cada
  uno entregue solo su contenido. Verificar con `npx tsc --noEmit`.

## 4. Pantallas del grupo

- [x] 4.1 Crear `pages/GrupoResumenPage.tsx`: el total del viaje como única cifra
  héroe, la situación de quien mira (su balance en palabras y su parte), el avance de
  la liquidación, los avisos de bajas pendientes con enlace a Miembros, los últimos
  movimientos y accesos a las demás pestañas. Estado inicial propio para un grupo
  recién creado. Verificar con `npx tsc --noEmit` y `npx oxlint src`.
- [x] 4.2 Crear `pages/GrupoGastosPage.tsx` con `SeccionGastos`. Verificar con `npx
  tsc --noEmit`.
- [x] 4.3 Crear `pages/GrupoPagosPage.tsx` con `SeccionPagos`. Verificar con `npx tsc
  --noEmit`.
- [x] 4.4 Crear `pages/GrupoBalancesPage.tsx` con `SeccionBalances`, incluido el
  atajo de la liquidación, que ahora navega a la pestaña de pagos con la precarga.
  Verificar con `npx tsc --noEmit`.
- [x] 4.5 Crear `pages/GrupoMiembrosPage.tsx` con `GestionMiembros` y `SeccionBajas`.
  Verificar con `npx tsc --noEmit`.
- [x] 4.6 Eliminar `pages/GrupoDetallePage.tsx`. Verificar con `npx tsc --noEmit`.

## 5. Resto de pantallas

- [x] 5.1 Rediseñar `pages/LoginPage.tsx` y `pages/RegistroPage.tsx` con la
  composición partida: panel de marca y formulario, colapsando a cabecera en pantalla
  chica. Verificar con `npx tsc --noEmit` y `npx oxlint src`.
- [x] 5.2 Rediseñar `pages/GruposPage.tsx`: tarjetas con jerarquía, estado vacío con
  acción, y el alta de grupo. Verificar con `npx tsc --noEmit` y `npx oxlint src`.
- [x] 5.3 Ajustar `pages/PerfilPage.tsx` y `pages/GastoDetallePage.tsx` al sistema
  nuevo, y que el detalle del gasto vuelva a la pestaña de gastos. Verificar con `npx
  tsc --noEmit` y `npx oxlint src`.
- [x] 5.4 Ajustar `pages/NoEncontradaPage.tsx`. Verificar con `npx tsc --noEmit`.

## 6. Verificación

- [x] 6.1 `cd frontend && npx tsc --noEmit && npx oxlint src && npm run build` sin
  errores.
- [x] 6.2 `cd backend && mvnw.cmd -q test` sigue sin fallos (este cambio no toca el
  backend, se confirma que nada se rompió).
- [x] 6.3 Recorrer en el navegador las ocho pantallas con datos reales: login, grupos,
  resumen, gastos, detalle de gasto, pagos, balances y miembros. Verificar que ninguna
  funcionalidad se perdió.
- [ ] 6.4 PENDIENTE — Verificar en ancho de teléfono que no hay scroll horizontal y que las
  pestañas siguen siendo alcanzables.
- [x] 6.5 `openspec validate 2026-09-08-redisenar-navegacion-y-ui --strict` pasa.
