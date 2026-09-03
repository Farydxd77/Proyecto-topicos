## 1. Contratos y capa de API

- [x] 1.1 En `src/api/types.ts` añadir los contratos de grupos espejando los
  records del backend: `CrearGrupoRequest { nombre: string; descripcion?: string }`,
  `ActualizarGrupoRequest` con la misma forma,
  `GrupoResumenDto { id: number; nombre: string; descripcion: string | null; creador: ParticipanteDto }`
  y `GrupoResponse` igual más `miembros: ParticipanteDto[]`. Documentar en
  comentario las validaciones del backend (`nombre` `@NotBlank @Size(max = 100)`).
  Verificar con `npx tsc --noEmit` que compila.
- [x] 1.2 Crear `src/api/grupos.ts` con las cinco funciones sobre `apiFetch`, todas
  con rutas relativas: `listarGrupos()`, `obtenerGrupo(id)`, `crearGrupo(body)`,
  `actualizarGrupo(id, body)` y `eliminarGrupo(id)`. Ninguna usa `fetch` directo ni
  URL absoluta. Verificar con `npx tsc --noEmit`.

## 2. Sin API simulada

- [x] 2.1 No se crean handlers simulados para los endpoints de grupos: MSW se
  elimina del proyecto en `conectar-backend-real` y el frontend trabaja solo contra
  el backend real. Verificar que `src/mocks/` no existe y que `grep -r msw src/`
  no devuelve nada.

## 3. Pantalla de lista de grupos

- [x] 3.1 Crear `src/pages/GruposPage.tsx` con la consulta `['grupos']` vía
  TanStack Query, mostrando nombre, descripción y creador de cada grupo, y estado
  de carga mientras resuelve. Verificar contra el backend real con una cuenta que
  sea miembro de dos grupos: aparecen los dos y ninguno ajeno.
- [x] 3.2 Añadir el estado vacío: cuando la lista viene vacía, mostrar un mensaje
  que explique que todavía no tiene grupos y ofrecer crear el primero. Verificar
  con una cuenta recién registrada que no ve una lista vacía sin contexto.
- [x] 3.3 Añadir el estado de error con reintento: si la consulta falla, mostrar el
  mensaje del `ApiError` y un botón de reintentar. Verificar deteniendo el backend,
  recargando la pantalla, y comprobando que reintentar carga los datos tras volver
  a arrancarlo, sin recargar la página.
- [x] 3.4 Hacer que cada grupo de la lista lleve a su detalle en `/grupos/:id`.
  Verificar que la navegación funciona y que la URL es compartible (abrirla
  directamente en una pestaña nueva muestra el mismo grupo).

## 4. Creación de grupo

- [x] 4.1 Añadir a `GruposPage` el formulario de creación con nombre y descripción,
  como mutación que invalida `['grupos']`. Verificar contra el backend real que el
  grupo creado aparece en la lista sin recargar y que en la base queda una fila en
  `grupos` y otra en `grupo_participantes` con el creador.
- [x] 4.2 Añadir la validación previa del nombre en `src/lib/validacion.ts`
  (obligatorio, no solo espacios, máximo 100 caracteres) y usarla antes de enviar.
  Verificar que con el nombre vacío o con solo espacios no sale ninguna petición a
  la red, y que con 101 caracteres se avisa antes de enviar.
- [x] 4.3 Verificar la creación solo con nombre: el grupo se crea y la pantalla no
  muestra la descripción nula como un dato faltante ni como el texto «null».
- [x] 4.4 Verificar el manejo de error del backend: provocar un `400` enviando el
  nombre vacío desde las herramientas del navegador y comprobar que se muestra el
  mensaje recibido y que el formulario conserva lo escrito.

## 5. Pantalla de detalle

- [x] 5.1 Crear `src/pages/GrupoDetallePage.tsx` con la consulta `['grupo', id]`,
  mostrando nombre, descripción, creador y la lista completa de miembros con
  nombre, apellido y username de cada uno. Verificar contra el backend real con un
  grupo de tres miembros que aparecen los tres.
- [x] 5.2 Señalar en la lista de miembros cuál es el creador. Verificar que el
  distintivo aparece exactamente en una persona y que coincide con el creador que
  devuelve el backend.
- [x] 5.3 Manejar el `404`: mostrar que el grupo no existe y ofrecer volver a la
  lista, sin redirigir automáticamente. Verificar navegando a `/grupos/999999`.
- [x] 5.4 Manejar el `403`: mostrar que no tiene acceso a ese grupo y ofrecer
  volver a la lista, con un mensaje distinto del de `404`. Verificar con dos
  cuentas: crear un grupo con la cuenta A y abrir su URL con la cuenta B.
- [x] 5.5 Verificar el caso del grupo eliminado mientras estaba abierto: con el
  detalle abierto en la cuenta A, eliminar el grupo desde otra sesión del creador,
  volver a consultar y comprobar que la pantalla explica que ya no existe en lugar
  de seguir mostrando los datos viejos.

## 6. Rol de creador en la interfaz

- [x] 6.1 En `GrupoDetallePage` derivar si quien mira es el creador comparando
  `grupo.creador.id` con el `id` del participante leído de la consulta `['perfil']`
  ya cacheada, sin duplicar ese dato en `AuthContext`. Mientras el perfil no esté
  disponible, tratar a la persona como no creadora. Verificar con `npx tsc --noEmit`
  y comprobando que abrir el detalle por URL directa nunca muestra un botón que
  luego desaparezca.
- [x] 6.2 Mostrar las acciones de editar y eliminar solo al creador. Verificar con
  dos cuentas en el mismo grupo: el creador ve ambas acciones y el miembro no ve
  ninguna, pero sí ve toda la información del grupo.

## 7. Edición del grupo

- [x] 7.1 Añadir a `GrupoDetallePage` la edición de nombre y descripción como
  mutación que invalida `['grupo', id]` y `['grupos']`, partiendo de los valores
  actuales y reutilizando la validación de 4.2. Verificar que tras guardar la
  pantalla muestra los valores nuevos sin recargar y que al volver a la lista el
  nombre también aparece actualizado.
- [x] 7.2 Añadir la opción de cancelar, que descarta los cambios y restaura los
  valores guardados sin enviar ninguna petición. Verificar observando la pestaña de
  red que cancelar no genera tráfico.
- [x] 7.3 Verificar que la edición no altera la lista de miembros ni el creador:
  editar un grupo de tres miembros y comprobar que siguen siendo los mismos tres.
- [x] 7.4 Verificar el `403` al editar: enviar la edición desde una cuenta miembro
  no creadora saltándose la interfaz y comprobar que se muestra el mensaje de
  permiso denegado y que la pantalla queda coherente, sin datos a medio actualizar.

## 8. Eliminación del grupo

- [x] 8.1 Añadir la eliminación con un diálogo de confirmación propio —no
  `window.confirm`— que nombre el grupo a eliminar y advierta que no se puede
  deshacer. Verificar que cancelar no envía ninguna petición y que el grupo sigue
  existiendo.
- [x] 8.2 Al confirmar, ejecutar la mutación, invalidar `['grupos']`, quitar
  `['grupo', id]` del caché y navegar a la lista. Verificar contra el backend real
  que el grupo desaparece de la lista y que `GET /api/grupos/{id}` responde `404`.
- [x] 8.3 Verificar que el grupo eliminado desaparece también para los demás
  miembros: con un grupo de dos cuentas, eliminarlo desde el creador y comprobar
  que deja de aparecer en la lista de la otra cuenta.

## 9. Rutas y navegación

- [x] 9.1 En `src/router.tsx` añadir `/grupos` y `/grupos/:id` como rutas
  protegidas dentro del layout. Verificar que sin sesión ambas redirigen a
  `/login`, y que tras iniciar sesión se vuelve a la ruta pretendida.
- [x] 9.2 En `src/components/Navegacion.tsx` añadir el enlace a Grupos, marcándolo
  como activo cuando la ruta actual cuelga de `/grupos`. Verificar navegando entre
  Perfil y Grupos que el estado activo es correcto en ambas.

## 10. Verificación final

- [x] 10.1 Recorrer el flujo completo contra el backend real con dos cuentas:
  crear un grupo, abrirlo, editarlo, comprobar desde la segunda cuenta que no lo ve
  en su lista y que su URL le da acceso denegado, y finalmente eliminarlo.
  Comprobar en PostgreSQL que las filas de `grupos` y `grupo_participantes`
  aparecen y desaparecen como corresponde.
- [x] 10.3 Ejecutar `npx tsc --noEmit` y `npx oxlint` desde `frontend/` y comprobar
  que no hay errores.
