## Why

Con `add-frontend-grupos` una persona puede crear un grupo y verlo, pero queda
sola dentro: el detalle **muestra** la lista de miembros y no la modifica. Un
grupo de viaje con un solo integrante no sirve para dividir nada, así que sin esta
pantalla el resto de la aplicación —gastos y balances— no tiene sobre quién
repartir.

El backend ya expone las dos operaciones (`POST` y `DELETE` sobre
`/api/grupos/{id}/miembros`), pero pide un `participanteId`, un número que nadie
conoce de memoria. Hace falta además una forma de encontrar a la persona, y para
eso está `GET /api/participantes` con sus filtros por CI, nombre y apellido.

## What Changes

- El detalle del grupo gana la gestión de miembros, visible solo para el creador.
- Nuevo buscador de participantes para agregar a alguien: se busca por CI exacto,
  o por nombre o apellido de forma parcial, y se elige de los resultados. La
  persona nunca escribe un identificador a mano.
- El buscador señala a quienes ya son miembros del grupo y no permite elegirlos,
  para evitar de antemano el conflicto que devolvería el backend.
- Quitar un miembro desde la lista, con confirmación previa.
- El creador no puede quitarse a sí mismo: la interfaz no ofrece esa acción sobre
  él y explica por qué si se intenta igual.
- Nuevo módulo `src/api/participantes.ts` con la búsqueda, y dos funciones nuevas
  en `src/api/grupos.ts` para agregar y quitar miembros.
- Nuevos contratos en `src/api/types.ts`: `AgregarMiembroRequest` y los parámetros
  de búsqueda de participantes.
- Los mocks de MSW se amplían con los tres endpoints implicados.

## Non-Goals

- No se invita por correo ni se generan enlaces de invitación: el backend agrega
  participantes que ya existen en el sistema, y no hay capacidad de email.
- No se agregan varios miembros de una vez: el backend acepta uno por petición.
- No se implementa abandonar el grupo por cuenta propia: el backend lo rechaza con
  `403` por diseño, y el spec de grupos lo declara fuera de alcance.
- No se implementa transferir el rol de creador.
- No se listan todos los participantes del sistema sin filtro como forma normal de
  uso: aunque el backend lo permite, la pantalla parte de una búsqueda.
- No se toca `backend/`.

## Capabilities

### New Capabilities

- `frontend-miembros`: la gestión de quiénes integran un grupo desde la interfaz.
  Cubre cómo el creador encuentra a una persona del sistema y la agrega, cómo la
  quita, qué ve cada rol, y cómo se presentan las tres reglas que el backend
  impone: solo el creador administra, no se puede agregar a quien ya es miembro, y
  el creador no puede quitarse a sí mismo.

### Modified Capabilities

<!-- Ninguna. La pantalla de detalle de `frontend-grupos` se amplía, pero los
     requisitos ya especificados allí (mostrar los miembros, señalar al creador,
     ocultar acciones a quien no es creador) no cambian de comportamiento. -->

## Impact

- **Código nuevo**: `src/api/participantes.ts`, y los componentes de buscador de
  participantes y de confirmación de baja que la pantalla necesite.
- **Código modificado**: `src/api/grupos.ts` (`agregarMiembro`, `quitarMiembro`),
  `src/api/types.ts` (`AgregarMiembroRequest`), `src/pages/GrupoDetallePage.tsx`
  (sección de gestión), `src/mocks/db.ts` y `src/mocks/handlers.ts`.
- **APIs consumidas**: `POST /api/grupos/{id}/miembros`,
  `DELETE /api/grupos/{id}/miembros/{participanteId}`, y
  `GET /api/participantes` con los filtros `ci`, `nombre` y `apellido`.
- **Estado de servidor**: se reutiliza `['grupo', id]`, que las dos mutaciones
  invalidan; la búsqueda usa `['participantes', filtro, valor]`.
- **Dependencias**: ninguna nueva.
- **Requisito previo**: `conectar-backend-real` y `add-frontend-grupos` aplicados.
