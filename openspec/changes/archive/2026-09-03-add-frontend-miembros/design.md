## Context

Ver `proposal.md` — Why. Estado relevante que aporta el backend:

- `POST /api/grupos/{id}/miembros` recibe `{ participanteId }` y devuelve `201`
  con el `GrupoResponse` **completo**, ya con la lista de miembros actualizada.
- `DELETE /api/grupos/{id}/miembros/{participanteId}` devuelve `204` sin cuerpo.
- Códigos que puede devolver la gestión: `403` si quien pide no es el creador,
  `409` si el participante ya es miembro, `404` si el participante no existe o —al
  quitar— si no es miembro del grupo, y `400` si el creador intenta quitarse a sí
  mismo o si falta `participanteId`.
- `GET /api/participantes` acepta `ci`, `nombre` y `apellido` como parámetros
  opcionales, aplica **un solo criterio** con precedencia `ci` > `nombre` >
  `apellido`, y sin ninguno devuelve la lista completa. `ci` es exacto; `nombre` y
  `apellido` son parciales e insensibles a mayúsculas. Devuelve siempre un array,
  vacío si no hay coincidencias.
- Cualquier usuario autenticado puede consultar `/api/participantes`: no está
  restringido a creadores ni a miembros.
- `add-frontend-grupos` ya dejó la consulta `['grupo', id]`, el cálculo de si
  quien mira es el creador, y la lista de miembros renderizada en el detalle.

## Goals / Non-Goals

**Goals:**

- Que nadie tenga que conocer ni escribir un `participanteId`.
- Prevenir en la interfaz los dos errores evitables (`409` de miembro repetido y
  `400` de auto-baja del creador) sin dejar de manejarlos si ocurren igual.
- Reutilizar la consulta `['grupo', id]` que ya existe, en lugar de introducir un
  estado paralelo de miembros.

**Non-Goals (nivel diseño):**

- No se introduce un componente de autocompletado de terceros.
- No se cachea el directorio completo de participantes.
- No se añade paginación a la búsqueda.

## Decisions

### 1. La búsqueda usa un criterio a la vez, coherente con la precedencia del backend

La interfaz ofrece elegir el criterio —CI, nombre o apellido— y un único campo de
texto, y envía solo ese parámetro.

- **Por qué:** el backend aplica un solo criterio con precedencia fija; ofrecer
  tres campos simultáneos daría a entender que se combinan con AND, y el resultado
  contradiría lo que la persona ve escrito. Un selector explícito hace visible la
  regla real.
- **Por qué CI exacto va aparte:** buscar «123» por CI no encuentra «1234567»,
  mientras que por nombre sí encuentra parciales. Si el criterio no fuera
  explícito, esa asimetría parecería un error de la aplicación.
- **Alternativa descartada:** un único campo que adivine el criterio según parezca
  número o texto — falla con CIs alfanuméricos y con nombres que empiezan por
  dígito, y vuelve impredecible qué se está buscando.

### 2. La búsqueda es explícita y no se dispara al teclear

Los resultados se piden al confirmar la búsqueda, no en cada pulsación.

- **Por qué:** sin debounce, un autocompletado dispara una petición por tecla
  contra un endpoint sin paginación que puede devolver el directorio entero. Con
  debounce hace falta elegir un retardo y manejar respuestas fuera de orden. Para
  una acción puntual —agregar a alguien a un grupo— la búsqueda explícita es más
  simple y no tiene ninguno de los dos problemas.
- **Alternativa descartada:** autocompletado con debounce — más vistoso, pero
  añade complejidad real a cambio de comodidad en una acción que se hace pocas
  veces.

### 3. La clave de búsqueda incluye el criterio y el valor

Se usa `['participantes', criterio, valor]`, habilitada solo cuando hay un valor.

- **Por qué:** buscar «Ana» por nombre y «Ana» por apellido son consultas
  distintas con resultados distintos; una clave que solo llevara el valor
  devolvería el resultado cacheado del criterio equivocado.

### 4. Los miembros actuales se marcan cruzando los resultados con `['grupo', id]`

Cada resultado de la búsqueda se compara por `id` contra `grupo.miembros`, y los
que ya están se muestran señalados y sin acción de agregar.

- **Por qué:** evita el `409` antes de que ocurra, y sobre todo evita la confusión
  de "lo agregué y no pasó nada". El dato ya está en el caché del detalle, así que
  no cuesta ninguna petición extra.
- **Por qué igualmente se maneja el `409`:** entre que se cargan los resultados y
  se pulsa agregar, otra sesión pudo haber agregado a esa persona. La prevención
  en la interfaz reduce el caso, no lo elimina.

### 5. Las dos mutaciones invalidan `['grupo', id]`; agregar además siembra el caché

`agregarMiembro` devuelve el `GrupoResponse` completo, así que su respuesta se
escribe directamente en `['grupo', id]` además de invalidar. `quitarMiembro`
devuelve `204` sin cuerpo, así que solo invalida.

- **Por qué sembrar en el alta:** el backend ya devolvió el estado nuevo y
  completo; descartarlo para volver a pedirlo es un viaje de más y un parpadeo
  visible en la lista.
- **Por qué no hacer lo mismo al quitar:** con `204` no hay estado nuevo que
  sembrar. Se podría filtrar la lista en memoria, pero eso reimplementaría en el
  cliente una regla que el backend ya aplicó; invalidar es más honesto.
- **Qué más se invalida:** `['grupos']` de quien ejecuta no cambia —sigue siendo
  miembro—, así que no se toca. La lista de la persona afectada sí cambia, pero
  vive en otra sesión y se resolverá en su próxima consulta.

### 6. La auto-baja del creador se oculta comparando con el creador del grupo

La acción de quitar no se renderiza en la fila cuyo `id` coincide con
`grupo.creador.id`.

- **Por qué así y no comparando con el participante propio:** son equivalentes
  mientras solo el creador administre, pero la regla del backend está enunciada
  sobre el creador del grupo, no sobre quien pide. Espejar la regla real evita que
  la interfaz quede mal si algún día se permite administrar a otros.

### 7. Quitar pide confirmación; agregar no

La baja usa un diálogo propio que nombra a la persona. El alta se ejecuta directo
desde el resultado de la búsqueda.

- **Por qué la asimetría:** quitar a alguien de un grupo con gastos ya cargados
  tiene consecuencias sobre los balances y no se deshace solo; agregar es
  reversible con la acción opuesta. Pedir confirmación para todo entrena a la
  gente a confirmar sin leer.

## Risks / Trade-offs

- **`GET /api/participantes` sin filtro devuelve el directorio completo del
  sistema**, y el endpoint no está restringido → Mitigación: la pantalla siempre
  parte de una búsqueda con criterio y valor, y no ofrece «ver todos». La
  restricción por roles es una decisión pendiente del backend, anotada como tal en
  el proposal de `add-gestion-general`.
- **Se puede agregar al grupo a cualquier persona registrada, sin que ella lo
  acepte** → Mitigación: es el comportamiento que el backend implementa hoy; no
  hay capacidad de invitación ni de notificación. Queda documentado como
  limitación conocida, no como olvido.
- **Quitar a un miembro que ya tiene gastos** puede alterar los balances del
  grupo de forma poco evidente → Mitigación: la confirmación advierte que la
  persona dejará de ver el grupo; el efecto exacto sobre los balances se hará
  visible en la pantalla de balances, que es donde se puede explicar bien.
- **La búsqueda no distingue homónimos más allá del CI y el username** →
  Mitigación: los resultados muestran los cuatro datos, y el CI es el
  desempate natural.
- **Sin paginación, una búsqueda muy general puede devolver una lista larga** →
  Mitigación: aceptable para el volumen de esta fase; la búsqueda por CI es la
  ruta rápida cuando se conoce a la persona.
