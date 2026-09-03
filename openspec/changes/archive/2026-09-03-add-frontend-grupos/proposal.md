## Why

El backend expone los siete endpoints de `/api/grupos` desde el cambio
`add-gestion-grupos`, pero el frontend no tiene ninguna pantalla que los use: hoy
una persona puede registrarse y editar su perfil, y nada más. El grupo es además
la puerta de entrada de todo lo que viene después —gastos y balances cuelgan de
`/api/grupos/{id}/…`—, así que sin estas pantallas no hay dónde colgar el resto de
la aplicación.

## What Changes

- Nueva pantalla de lista de grupos en `/grupos`: muestra los grupos donde la
  persona es miembro, con estado vacío cuando no pertenece a ninguno.
- Nueva pantalla de detalle en `/grupos/:id`: nombre, descripción, creador y lista
  de miembros, con las acciones de administración visibles solo para el creador.
- Creación de grupo desde la lista, con nombre obligatorio y descripción opcional.
- Edición de nombre y descripción, reservada al creador.
- Eliminación del grupo con confirmación previa, reservada al creador.
- Nuevo módulo `src/api/grupos.ts` con las cinco llamadas, todas vía `apiFetch`.
- Nuevos contratos en `src/api/types.ts`: `CrearGrupoRequest`,
  `ActualizarGrupoRequest`, `GrupoResumenDto` y `GrupoResponse`.
- Nuevas rutas protegidas en `src/router.tsx` y enlace a Grupos en la navegación.
- Los mocks de MSW se amplían con los cinco endpoints, para que el modo simulado
  siga sirviendo como alternativa sin base de datos.

## Non-Goals

- No se gestionan miembros: agregar y quitar es el cambio `add-frontend-miembros`.
  El detalle **muestra** la lista de miembros, pero no la modifica.
- No se muestran gastos ni balances del grupo: son cambios posteriores.
- No hay búsqueda, filtrado, ordenamiento ni paginación de grupos: el backend
  devuelve la lista completa y la pantalla la muestra completa.
- No se implementa abandonar un grupo por cuenta propia: el backend no lo permite.
- No se implementa transferir el rol de creador: el backend no lo expone.
- No se toca `backend/`.

## Capabilities

### New Capabilities

- `frontend-grupos`: las pantallas con las que una persona autenticada ve los
  grupos a los que pertenece, crea grupos nuevos, consulta el detalle de uno con
  sus miembros, y —cuando es su creador— edita sus datos o lo elimina. Incluye
  cómo se distingue en la interfaz el rol de creador del de miembro y cómo se
  presenta cada error del backend.

### Modified Capabilities

<!-- Ninguna. -->

## Impact

- **Código nuevo**: `src/api/grupos.ts`, `src/pages/GruposPage.tsx`,
  `src/pages/GrupoDetallePage.tsx`, y los componentes de formulario y
  confirmación que esas pantallas necesiten.
- **Código modificado**: `src/api/types.ts` (cuatro contratos nuevos),
  `src/router.tsx` (dos rutas protegidas), `src/components/Navegacion.tsx`
  (enlace a Grupos), `src/mocks/db.ts` y `src/mocks/handlers.ts` (cinco endpoints
  simulados).
- **APIs consumidas**: `POST /api/grupos`, `GET /api/grupos`,
  `GET /api/grupos/{id}`, `PUT /api/grupos/{id}`, `DELETE /api/grupos/{id}`.
- **Estado de servidor**: claves de TanStack Query `['grupos']` para la lista y
  `['grupo', id]` para el detalle.
- **Dependencias**: ninguna nueva.
- **Requisito previo**: el cambio `conectar-backend-real` debe estar aplicado,
  porque estas pantallas se construyen y verifican contra el backend real.
