## 1. Contratos y capa de API

- [x] 1.1 En `src/api/types.ts` añadir `AgregarMiembroRequest { participanteId: number }`
  y un tipo para el criterio de búsqueda (`'ci' | 'nombre' | 'apellido'`),
  documentando en comentario que el backend aplica un solo criterio con precedencia
  `ci` > `nombre` > `apellido` y que `ci` es exacto mientras los otros dos son
  parciales e insensibles a mayúsculas. Verificar con `npx tsc --noEmit`.
- [x] 1.2 Crear `src/api/participantes.ts` con
  `buscarParticipantes(criterio, valor)` sobre `apiFetch`, que envía únicamente el
  parámetro correspondiente al criterio elegido y devuelve `ParticipanteDto[]`.
  Verificar con `npx tsc --noEmit`.
- [x] 1.3 En `src/api/grupos.ts` añadir `agregarMiembro(grupoId, body)` —que
  devuelve el `GrupoResponse` completo— y `quitarMiembro(grupoId, participanteId)`
  —que no devuelve cuerpo—. Verificar con `npx tsc --noEmit`.

## 2. Sin API simulada

- [x] 2.1 No se crean handlers simulados para la busqueda de participantes ni para
  la gestion de miembros: MSW se elimina del proyecto. Las reglas del backend
  (409, 404, 400, 403) se verifican contra el backend real. Verificar que
  `src/mocks/` no existe.

## 3. Buscador de participantes

- [x] 3.1 Crear el componente de búsqueda con un selector de criterio (CI, nombre,
  apellido) y un campo de texto, que dispara la consulta al confirmar y **no** al
  teclear, bajo la clave `['participantes', criterio, valor]` habilitada solo
  cuando hay valor. Verificar en la pestaña de red que escribir no genera
  peticiones y que confirmar genera exactamente una.
- [x] 3.2 Mostrar cada resultado con nombre, apellido, CI y username. Verificar
  contra el backend real que los cuatro datos aparecen y que ninguna respuesta
  incluye contraseña.
- [x] 3.3 Verificar la búsqueda parcial e insensible a mayúsculas por nombre y por
  apellido contra el backend real, y la búsqueda exacta por CI, comprobando que un
  CI parcial no encuentra a nadie.
- [x] 3.4 Añadir el estado sin resultados con un mensaje explicativo, y el estado
  de error con reintento que conserve lo escrito. Verificar ambos: buscando algo
  inexistente, y deteniendo el backend antes de confirmar la búsqueda.

## 4. Agregar miembros

- [x] 4.1 Integrar el buscador en `GrupoDetallePage`, visible solo para el creador,
  y añadir la acción de agregar sobre cada resultado como mutación que escribe la
  respuesta en `['grupo', id]` y además invalida esa clave. Verificar contra el
  backend real que la persona aparece en la lista de miembros sin recargar y sin
  parpadeo.
- [x] 4.2 Marcar en los resultados a quienes ya integran el grupo, cruzando por
  `id` contra `grupo.miembros`, y no ofrecerles la acción de agregar. Verificar
  buscando a alguien que ya es miembro: aparece señalado y sin acción.
- [x] 4.3 Verificar con dos cuentas que tras agregar a la cuenta B, el grupo
  aparece en su `GET /api/grupos` y puede abrir el detalle sin recibir `403`.
- [x] 4.4 Verificar el `409`: provocar la petición de agregar a un miembro actual
  saltándose la interfaz y comprobar que se explica que ya es miembro y que la
  lista no lo duplica.
- [x] 4.5 Verificar el `404` de participante inexistente enviando un
  `participanteId` que no existe, y comprobar que se muestra el mensaje del backend
  sin alterar la lista.

## 5. Quitar miembros

- [x] 5.1 Añadir la acción de quitar en cada fila de la lista de miembros, visible
  solo para el creador, con un diálogo de confirmación propio que nombre a la
  persona. Verificar que cancelar no genera ninguna petición de red y que la
  persona sigue siendo miembro.
- [x] 5.2 Al confirmar, ejecutar la mutación e invalidar `['grupo', id]`. Verificar
  contra el backend real que la persona desaparece de la lista sin recargar y que
  la fila correspondiente ya no está en `grupo_participantes`.
- [x] 5.3 Verificar con dos cuentas que tras quitar a la cuenta B, el grupo deja de
  aparecer en su lista y su detalle le responde `403`.
- [x] 5.4 Verificar el `404` al quitar a quien ya no es miembro: enviar la petición
  dos veces seguidas y comprobar que la segunda muestra que no es miembro del grupo
  y que la lista queda al día con el backend.

## 6. Reglas de rol y de auto-baja

- [x] 6.1 No renderizar la acción de quitar en la fila cuyo `id` coincida con
  `grupo.creador.id`. Verificar que en un grupo de tres miembros la acción aparece
  en dos filas y no en la del creador.
- [x] 6.2 Verificar el `400` de auto-baja: enviar la petición de quitar al creador
  saltándose la interfaz y comprobar que se explica que el creador no puede
  quitarse a sí mismo y que la composición del grupo no cambia.
- [x] 6.3 Verificar que un miembro no creador ve la lista completa de integrantes
  pero no ve el buscador ni ninguna acción de quitar. Comprobarlo con la cuenta B
  dentro de un grupo creado por la cuenta A.
- [x] 6.4 Verificar el `403` de gestión: enviar desde la cuenta B —miembro no
  creadora— una petición de agregar y otra de quitar, y comprobar que ambas
  muestran el mensaje de permiso denegado y que la composición del grupo no cambia.

## 7. Verificación final

- [x] 7.1 Recorrer el flujo completo contra el backend real con tres cuentas: la
  cuenta A crea un grupo, busca y agrega a B y a C, C es quitada, y se comprueba en
  cada paso qué ve cada cuenta en su lista y en el detalle. Confirmar el estado
  final en PostgreSQL consultando `grupo_participantes`.
- [x] 7.3 Ejecutar `npx tsc --noEmit` y `npx oxlint` desde `frontend/` y comprobar
  que no hay errores.
