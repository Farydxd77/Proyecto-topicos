## Why

Toda la funcionalidad del grupo vive apilada en una sola pantalla. `GrupoDetallePage`
renderiza hoy, una debajo de otra: el panel de totales, la gestión de miembros, las
bajas, los gastos, los pagos, los balances y la liquidación. Son siete bloques y más
de 2000 píxeles de scroll para encontrar cualquier cosa.

El costo no es solo estético. Cada visita dispara **seis consultas al backend** aunque
quien entra solo quiera ver cuánto lleva gastado. No hay forma de compartir un enlace
a los balances de un grupo. Y no hay jerarquía: el total del viaje pesa lo mismo que
el buscador para agregar un miembro.

Además, la interfaz nunca tuvo una pasada de diseño. Funciona y es legible, pero no
tiene sistema: los tamaños de texto, los grises y los espaciados se eligieron caso por
caso, y los montos —que son lo único que la aplicación existe para mostrar— se ven
igual que cualquier otro texto.

## What Changes

### Navegación: el grupo pasa a ser una sección con pestañas

Cada capacidad tiene su pantalla y su URL propia:

| Ruta | Pantalla |
|------|----------|
| `/grupos` | Mis grupos |
| `/grupos/:id` | **Resumen** del grupo |
| `/grupos/:id/gastos` | Gastos |
| `/grupos/:id/gastos/:gastoId` | Detalle de un gasto |
| `/grupos/:id/pagos` | Pagos |
| `/grupos/:id/balances` | Balances y liquidación |
| `/grupos/:id/miembros` | Miembros y bajas |
| `/perfil` | Perfil |

Un `GrupoLayout` (ruta anidada) sostiene la cabecera del grupo y las pestañas, y las
pantallas se montan en su `Outlet`. La consulta del grupo vive en el layout: cambiar
de pestaña no la vuelve a pedir.

**El Resumen es una pantalla nueva**, no un contenedor de las demás: el total del
viaje en grande, la situación de quien mira, el avance de la liquidación, los avisos
que requieren acción (bajas pendientes de decidir) y accesos a las demás pestañas con
su recuento. No trae las listas de gastos ni de pagos: eso costaría dos consultas y
desharía la razón de separar las pantallas.

### Diseño: un sistema, no una colección de decisiones sueltas

- **Tokens de color, tipografía y elevación** definidos con `@theme` de Tailwind 4,
  en lugar de elegir un gris distinto en cada archivo.
- **Los montos son ciudadanos de primera**: escala tipográfica propia, cifras
  tabulares y color semántico consistente (a favor / en contra / a mano).
- **Superficies con profundidad** en lugar de solo bordes de 1px.
- **Estados vacíos con intención**: explican qué falta y ofrecen la acción, en vez de
  una línea de texto gris.
- **Login y registro** dejan de ser un formulario centrado sobre fondo plano y pasan
  a una composición partida con identidad de marca.
- **Componentes nuevos**: `Card`, `Pestanas`, `EstadoVacio`, `Monto`, `Etiqueta`.
  `Boton` y `Campo` se amplían con variantes y tamaños.

## Capabilities

### New Capabilities

- `navegacion`: la estructura de rutas de la aplicación, la sección del grupo con sus
  pestañas, qué vive en cada pantalla y cómo se comportan los enlaces profundos y el
  estado activo.
- `diseno`: el sistema visual —tokens, jerarquía tipográfica, tratamiento de los
  montos, superficies, estados vacíos y de carga, comportamiento responsive y
  requisitos de accesibilidad— que todas las pantallas comparten.

### Modified Capabilities

- `frontend-balances`: el panel de totales deja de estar «antes del detalle por
  persona» en la misma pantalla y pasa a la pantalla de Resumen, con los balances en
  su propia pestaña.

## Impact

- **Frontend nuevo**: `pages/GrupoLayout.tsx`, `pages/GrupoResumenPage.tsx`,
  `pages/GrupoGastosPage.tsx`, `pages/GrupoPagosPage.tsx`,
  `pages/GrupoBalancesPage.tsx`, `pages/GrupoMiembrosPage.tsx`,
  `components/Card.tsx`, `components/Pestanas.tsx`, `components/EstadoVacio.tsx`,
  `components/Monto.tsx`, `components/Etiqueta.tsx`.
- **Frontend modificado**: `router.tsx` (rutas anidadas), `index.css` (tokens),
  `components/Layout.tsx`, `components/Navegacion.tsx`, `components/Boton.tsx`,
  `components/Campo.tsx`, las seis secciones existentes (pierden su cabecera propia,
  que ahora la pone la pantalla), `pages/GruposPage.tsx`, `pages/LoginPage.tsx`,
  `pages/RegistroPage.tsx`, `pages/PerfilPage.tsx`, `pages/GastoDetallePage.tsx`.
- **Frontend eliminado**: `pages/GrupoDetallePage.tsx`, reemplazada por el layout más
  las cinco pantallas.
- **Sin cambios de backend**: ni un endpoint, ni un contrato, ni el modelo de datos.
  Es exclusivamente reorganización e interfaz.

## Non-Goals

- No se agrega ninguna funcionalidad nueva: todo lo que se puede hacer hoy se sigue
  pudiendo hacer, en la misma cantidad de pasos o menos.
- No se cambia ningún contrato de la API ni se agregan endpoints.
- No se implementa modo oscuro: los tokens quedan preparados para soportarlo, pero
  este cambio no lo entrega.
- No se agrega una biblioteca de componentes (shadcn, Radix, MUI): se sigue con
  Tailwind y componentes propios, que es lo que el proyecto ya tiene.
- No se agregan animaciones más allá de transiciones de estado de 150–200 ms.
- No se traduce ni se agrega i18n: los textos siguen siendo literales en español.
- No se agrega una pantalla de configuración del grupo separada de Miembros.
