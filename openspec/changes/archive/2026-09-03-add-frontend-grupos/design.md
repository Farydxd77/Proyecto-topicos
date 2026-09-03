## Context

Ver `proposal.md` — Why. Estado actual relevante:

- `src/api/client.ts` expone `apiFetch<T>(ruta, { method, body })`, normaliza todo
  fallo a `ApiError` con `status` y `message`, y ya cierra la sesión ante un `401`
  en ruta protegida. Ninguna pantalla debe usar `fetch` directo.
- `src/api/types.ts` es la única definición de contratos y ya tiene
  `ParticipanteDto(id, nombre, apellido, ci, username)`, que es exactamente la
  forma con la que el backend representa al creador y a cada miembro.
- El backend devuelve dos formas distintas: `GrupoResumenDto(id, nombre,
  descripcion, creador)` en la lista y `GrupoResponse(id, nombre, descripcion,
  creador, miembros)` en el resto de operaciones.
- Autorización del backend: ver el detalle exige ser miembro (`403` si no lo es);
  editar, eliminar y gestionar miembros exigen ser el creador (`403` si no). El
  `404` se evalúa **antes** que el `403`.
- `PerfilPage` ya establece el patrón de TanStack Query en este proyecto: consulta
  con clave estable, mutación que invalida esa clave, estados de carga y error con
  reintento.
- `AuthContext` guarda únicamente el token; todo lo que viene del backend lo posee
  TanStack Query.

## Goals / Non-Goals

**Goals:**

- Que la interfaz nunca ofrezca una acción que el backend vaya a rechazar por
  permisos, sin por eso confiar en la interfaz como mecanismo de seguridad.
- Que cada uno de los cuatro códigos de error del backend (`400`, `403`, `404`,
  `401`) llegue a la persona como una frase que se entiende, no como un número.
- Dejar establecido el patrón de pantalla con el que se construirán gastos y
  balances, que cuelgan de la misma jerarquía de rutas.

**Non-Goals (nivel diseño):**

- No se introduce una librería de formularios ni de componentes: se sigue con los
  `Campo` y `Boton` que ya existen.
- No se introduce gestión de estado global: TanStack Query es el dueño del estado
  de servidor.
- No se optimiza con actualizaciones optimistas.

## Decisions

### 1. Dos claves de consulta: `['grupos']` para la lista y `['grupo', id]` para el detalle

La lista y el detalle son consultas independientes con formas distintas
(`GrupoResumenDto[]` frente a `GrupoResponse`). Cada mutación invalida lo que
realmente cambió:

| Mutación | Invalida |
|---|---|
| Crear | `['grupos']` |
| Editar | `['grupo', id]` y `['grupos']` (el nombre se ve en la lista) |
| Eliminar | `['grupos']`, y elimina del caché `['grupo', id]` |

- **Por qué dos claves:** meter el detalle dentro de la lista obligaría a que
  abrir un grupo refrescara todos, y a inventar una forma intermedia que el
  backend no devuelve. Con dos claves cada pantalla pide exactamente lo que
  muestra.
- **Por qué editar invalida las dos:** el nombre aparece en ambas pantallas; si
  solo se invalidara el detalle, volver a la lista mostraría el nombre viejo.
- **Alternativa descartada:** sembrar el caché del detalle desde la lista
  (`setQueryData`) para evitar el segundo viaje — la lista no trae `miembros`,
  así que sembraría un detalle incompleto que la pantalla mostraría como si
  estuviera completo.

### 2. El rol de creador se deriva comparando el creador con el participante propio

La pantalla de detalle decide si mostrar las acciones de administración comparando
`grupo.creador.id` con el `id` del participante de la persona autenticada, que se
lee del perfil ya cacheado bajo `['perfil']`.

- **Por qué desde el perfil cacheado:** `PerfilResponse` ya trae el `id` del
  participante y la convención del proyecto dice explícitamente que el Context
  guarda solo el token y que nada que venga del backend se duplica en él. La
  consulta `['perfil']` ya está en caché en cualquier pantalla protegida.
- **Por qué no comparar por `username`:** el username es editable desde el perfil;
  comparar por un campo mutable es frágil. El `id` del participante no cambia.
- **Esto es presentación, no seguridad:** el backend verifica igual en cada
  petición. Ocultar el botón evita el error del usuario; no evita el ataque. Por
  eso el spec exige además manejar el `403` si la acción se envía de todos modos.
- **Alternativa descartada:** que el backend devuelva un booleano `esCreador` en
  `GrupoResponse` — obligaría a cambiar un DTO ya implementado y probado, y la
  información ya está en la respuesta.

### 3. `404` y `403` del detalle se resuelven en la pantalla, no con una redirección

Ante un `404` o un `403` al abrir el detalle, la pantalla muestra un mensaje
explicativo con un enlace a la lista, en lugar de redirigir automáticamente.

- **Por qué:** una redirección automática deja a la persona en la lista sin saber
  qué pasó, y con una URL que ya no es la que escribió. Es especialmente confuso
  en el caso real de "el creador eliminó el grupo mientras lo tenías abierto".
- **Por qué distinguir los dos mensajes:** `404` es "este grupo no existe" y `403`
  es "existe pero no es tuyo". Son situaciones distintas para quien las vive.
- **Alternativa descartada:** un único mensaje genérico para ambos — más simple de
  implementar, pero convierte dos diagnósticos claros en uno ambiguo.

### 4. La eliminación pide confirmación en la interfaz, no con `window.confirm`

Se usa un diálogo propio dentro de la pantalla.

- **Por qué:** `window.confirm` bloquea el hilo, no se puede estilar, y en algunos
  navegadores queda suprimido si la persona marca "no volver a mostrar". Para una
  acción irreversible conviene un control propio que nombre el grupo que se va a
  borrar.
- **Alternativa descartada:** eliminar sin confirmación con opción de deshacer — el
  backend no expone forma de restaurar un grupo eliminado, así que no hay nada que
  deshacer.

### 5. Un módulo `src/api/grupos.ts` por recurso, espejando la convención existente

Igual que `api/auth.ts` y `api/perfil.ts`, se crea `api/grupos.ts` con una función
por endpoint, todas sobre `apiFetch` y con rutas relativas. Las pantallas no
conocen rutas ni verbos HTTP.

- **Por qué:** mantiene un único lugar donde cambiar si el backend cambia, y deja
  las pantallas legibles. Es la convención ya establecida en el proyecto.

### 6. Los contratos nuevos se añaden a `types.ts` espejando los records del backend

Se añaden `CrearGrupoRequest`, `ActualizarGrupoRequest`, `GrupoResumenDto` y
`GrupoResponse`, con `descripcion` como `string | null` en las respuestas
—Jackson omite o envía `null` cuando la columna es nula— y como `string` opcional
en las peticiones.

- **Por qué `string | null` y no `string | undefined`:** el backend envía la clave
  con valor nulo, no la omite; declararla opcional escondería que hay que
  contemplar el caso nulo al mostrarla.

### 7. Las rutas nuevas son `/grupos` y `/grupos/:id`, ambas protegidas

Se añaden dentro del layout, junto a `/perfil`, y se agrega el enlace en
`Navegacion`.

- **Por qué esta forma:** espeja la jerarquía del backend
  (`/api/grupos/{id}/gastos`, `/api/grupos/{id}/balances`), de modo que gastos y
  balances puedan colgar de `/grupos/:id/…` sin reorganizar el mapa de rutas.

## Risks / Trade-offs

- **La comparación de creador depende de que `['perfil']` esté en caché.** Si el
  detalle se abre por URL directa antes de que el perfil cargue, las acciones
  podrían no aparecer al principio → Mitigación: mientras el perfil no esté
  disponible se tratan como no disponibles y aparecen al resolverse; nunca al
  revés, para no mostrar una acción que luego desaparece.
- **La lista no muestra cuántos miembros tiene cada grupo**, porque
  `GrupoResumenDto` no lo trae → Mitigación: se acepta; pedir el detalle de cada
  grupo para contar miembros multiplicaría las peticiones. Si hace falta, es un
  campo aditivo en el DTO del backend.
- **Dos personas editando el mismo grupo**: la última en guardar pisa a la otra,
  sin aviso → Mitigación: el backend no expone control de concurrencia; queda
  fuera de alcance y se documenta como limitación conocida.
- **Los mocks de MSW deben mantenerse en paralelo** y pueden divergir del backend
  → Mitigación: `types.ts` es compartido, así que un cambio de contrato rompe
  ambos lados a la vez en TypeScript; y la verificación de cada tarea se hace
  contra el backend real, no contra los mocks.
