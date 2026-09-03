## 1. Habilitar la fase 2 y preparar el terreno

- [x] 1.1 Actualizar `CLAUDE.md`: reemplazar "Fase actual: backend únicamente. No tocar la carpeta `frontend/`" por la declaración de fase 2 activa, y documentar el stack del frontend (React 19, Vite, TypeScript, react-router, TanStack Query, Tailwind 4, MSW) junto con la estructura de carpetas de `src/` definida en `design.md`. Verificar que ya no queda ninguna prohibición de tocar `frontend/` (`grep -n "No tocar" CLAUDE.md` no devuelve nada).
- [x] 1.2 Actualizar el bloque `context` de `openspec/config.yaml` con el mismo criterio, y revisar que la guía `No crear archivos en frontend/` de `operations.apply.guidance` quede eliminada. Verificar con `openspec validate init-frontend --strict` que la configuración sigue siendo válida.
- [x] 1.3 Instalar las dependencias en `frontend/`: `react-router`, `@tanstack/react-query`, `tailwindcss`, `@tailwindcss/vite` y `msw` (esta última como dependencia de desarrollo). Verificar que `npm install` termina sin errores y que las cinco aparecen en `package.json`.
- [x] 1.4 Renombrar el paquete de `dashboard` a `cuentas-claras-frontend` en `frontend/package.json`. Verificar que `npm run build` sigue pasando sobre el template intacto — es el chequeo de que el stack recién instalado compila antes de escribir una sola pantalla.

## 2. Configuración del proyecto

- [x] 2.1 Configurar el proxy en `vite.config.ts`: reenviar `/api` a `http://localhost:8080` y registrar el plugin de Tailwind. Verificar que `npm run dev` arranca sin errores; el proxy queda configurado pero dormido mientras los mocks estén activos.
- [x] 2.2 Reemplazar el contenido de `src/index.css` por el import de Tailwind y los estilos base mínimos. Verificar que una clase de utilidad de Tailwind aplicada a un elemento de prueba tiene efecto visible en `npm run dev`.
- [x] 2.3 Actualizar `frontend/index.html`: `lang="es"` y título "Cuentas Claras". Verificar en el navegador que la pestaña muestra el título y que `document.documentElement.lang` es `es`.
- [x] 2.4 Eliminar los restos del template: `src/App.css`, `src/assets/hero.png`, `src/assets/react.svg` y `src/assets/vite.svg`. Verificar que `npm run build` pasa, confirmando que ningún módulo los seguía importando.

## 3. Contratos y API simulada

- [x] 3.1 Crear `src/api/types.ts` con los tipos de los contratos del backend, leídos de los DTOs reales: `LoginRequest`, `LoginResponse`, `RegisterRequest`, `RegisterResponse`, `UsuarioDto`, `ParticipanteDto`, `PerfilResponse`, `ActualizarPerfilRequest`, `CambiarUsernameRequest`, `CambiarPasswordRequest` y la forma del error estándar. Es la única definición de los contratos y la comparten los mocks y el código de producción. Verificar con `npx tsc --noEmit` que compila.
- [x] 3.2 Crear `src/mocks/db.ts`: almacén en memoria de usuarios y participantes, con un usuario semilla documentado para entrar sin registrarse. Incluye la emisión de JWT estructuralmente válidos (tres segmentos base64url con un `exp` real y firma de mentira). Verificar con `npx tsc --noEmit` y comprobando en consola que el token emitido se parte en tres y su payload tiene un `exp` futuro.
- [x] 3.3 Crear `src/mocks/handlers.ts` con los 6 endpoints, reproduciendo los contratos reales leídos de `AuthService`, `PerfilService` y `GlobalExceptionHandler`: el formato de error estándar, el mapa `errors` por campo en los 400, el 409 de username duplicado, el 401 de credenciales inválidas, el 401 de token ausente o vencido, y el 200 con cuerpo vacío de `PUT /api/perfil/me/password`. Verificar que cada endpoint responde el código y la forma esperados.
- [x] 3.4 Crear `src/mocks/browser.ts` con el setup del worker y generar el service worker con `npx msw init public/`. Arrancarlo solo bajo la bandera de desarrollo de Vite, nunca en producción. Verificar que en `npm run dev` la consola anuncia que el worker está activo y que las peticiones a `/api` las responde el mock.
- [x] 3.5 Verificar que el almacén reacciona de verdad: registrar una cuenta e iniciar sesión con ella, editar el perfil y comprobar que la consulta siguiente devuelve los datos nuevos, y cambiar la contraseña y comprobar que la anterior deja de servir.

## 4. Capa de API

- [x] 4.1 Crear `src/api/client.ts`: función `apiFetch` que antepone `/api`, inyecta `Authorization: Bearer {token}` cuando hay sesión y la omite en las rutas `/auth/**`, y traduce toda respuesta no exitosa a `ApiError` (`status`, `message`, `errors?`). Debe normalizar cuerpo vacío, cuerpo no-JSON y fallo de red, y tratar una respuesta 200 sin cuerpo como éxito. Verificar con `npx tsc --noEmit` y comprobando contra los mocks que un 409 de username duplicado llega como `ApiError` con `status: 409` y el `message` correcto.
- [x] 4.2 Añadir en `src/api/client.ts` el disparo de cierre de sesión ante un 401, exceptuando las llamadas a `/auth/**`. Verificar con los mocks: un token vencido en cualquier acción redirige a `/login`, y un login con contraseña incorrecta muestra el error en el formulario sin redirigir.
- [x] 4.3 Crear `src/api/auth.ts` con las funciones `login` y `registrar` sobre `apiFetch`. Verificar con `npx tsc --noEmit` y comprobando que un registro exitoso devuelve `token` y `participante`.
- [x] 4.4 Crear `src/api/perfil.ts` con `obtenerPerfil`, `actualizarPerfil`, `cambiarUsername` y `cambiarPassword`. Verificar con `npx tsc --noEmit` y comprobando que `cambiarPassword` resuelve sin error pese a la respuesta 200 con cuerpo vacío.

## 5. Sesión y rutas protegidas

- [x] 5.1 Crear `src/auth/token.ts`: leer, guardar y borrar el token en `localStorage`, y una función que decodifique el `exp` del payload del JWT para detectar vencimiento. Debe devolver "sin sesión" ante contenido ilegible en lugar de lanzar. Verificar con `npx tsc --noEmit` y probando con un token basura, un token vencido emitido por los mocks y uno válido.
- [x] 5.2 Crear `src/auth/AuthContext.tsx` con el `AuthProvider`: expone el token, `iniciarSesion(token)`, `cerrarSesion()` y un indicador de "restaurando" mientras lee el almacenamiento al arrancar. Al cerrar sesión debe limpiar también la caché de TanStack Query. Verificar con `npx tsc --noEmit`.
- [x] 5.3 Crear `src/auth/useAuth.ts`, el hook de consumo del contexto, que lanza un error claro si se usa fuera del provider. Verificar con `npx tsc --noEmit`.
- [x] 5.4 Crear `src/auth/RutaProtegida.tsx`: muestra un estado de carga mientras se restaura la sesión, redirige a `/login` sin sesión recordando la ruta pretendida, y renderiza la ruta cuando hay sesión. Verificar navegando directo a `/perfil` sin sesión (redirige a login) y recargando `/perfil` con sesión (permanece, sin parpadeo de la pantalla de login).

## 6. Componentes compartidos

- [x] 6.1 Crear `src/lib/validacion.ts` con las reglas espejadas del backend: username 3–50, password mínimo 8, nombre y apellido máximo 100, CI máximo 20, y obligatoriedad de todos. Verificar con `npx tsc --noEmit` que cada regla se exporta y devuelve el mensaje de error en español o `null`.
- [x] 6.2 Crear `src/components/Campo.tsx`, un campo de formulario con etiqueta, soporte de tipo `password` enmascarado y renderizado del mensaje de error asociado. Verificar con `npx tsc --noEmit`.
- [x] 6.3 Crear `src/components/Boton.tsx` con estado de envío en curso que se deshabilita mientras la operación está pendiente. Verificar con `npx tsc --noEmit`.
- [x] 6.4 Crear `src/components/MensajeError.tsx` para el error general de un formulario o pantalla a partir de un `ApiError`. Verificar con `npx tsc --noEmit`.
- [x] 6.5 Crear `src/components/Navegacion.tsx`: muestra el username leído del perfil cacheado, la acción de cerrar sesión, y los destinos Grupos y Gastos visiblemente deshabilitados y no navegables. Verificar en el navegador que hacer clic en Grupos o Gastos no cambia la ruta.
- [x] 6.6 Crear `src/components/Layout.tsx`, el marco común de las pantallas privadas que incluye la navegación y renderiza la ruta hija. Verificar que la pantalla de perfil aparece dentro del marco y que login y registro no lo llevan.

## 7. Pantallas

- [ ] 7.1 Crear `src/pages/LoginPage.tsx`: formulario de username y contraseña, validación previa, estado de envío, mensaje de credenciales inválidas ante 401 sin distinguir qué campo falló, y redirección a la ruta pretendida tras el éxito. Verificar contra los mocks los tres casos: usuario semilla con credenciales correctas, credenciales incorrectas y campos vacíos.
- [ ] 7.2 Crear `src/pages/RegistroPage.tsx`: formulario de username, contraseña, nombre, apellido y CI, con validación espejada, inicio de sesión inmediato con el token devuelto, y manejo del 409 conservando los datos ya cargados. Verificar contra los mocks un registro exitoso y un registro con el username del usuario semilla.
- [ ] 7.3 Crear `src/pages/PerfilPage.tsx` con la consulta del perfil vía TanStack Query bajo la clave `['perfil']`, estados de carga y de error con reintento, y el CI mostrado como solo lectura. Verificar que los datos se muestran, que el estado de carga aparece y que forzando un fallo en el handler se ve el error con opción de reintentar.
- [ ] 7.4 Añadir a `PerfilPage` la edición de nombre y apellido como mutación que invalida `['perfil']`, con opción de cancelar que restaura los valores guardados. Verificar que tras guardar la pantalla muestra los datos nuevos sin recargar y que cancelar descarta los cambios.
- [ ] 7.5 Añadir a `PerfilPage` el cambio de username como mutación separada que invalida `['perfil']`. Verificar que un username disponible se refleja en la pantalla y en la navegación sin cerrar la sesión, que el username del usuario semilla devuelve 409 conservando el anterior, y que reenviar el username propio sin cambios funciona.
- [ ] 7.6 Añadir a `PerfilPage` el cambio de contraseña con confirmación, comprobando la coincidencia antes de enviar y vaciando ambos campos tras el éxito. Verificar que contraseñas distintas no envían petición, que una de menos de 8 caracteres se rechaza en el cliente, y que tras un cambio exitoso se puede iniciar sesión con la contraseña nueva y ya no con la vieja.
- [ ] 7.7 Crear `src/pages/NoEncontradaPage.tsx` con un mensaje y un enlace de regreso. Verificar que navegar a `/cualquier-cosa` muestra esta página y no una pantalla en blanco.

## 8. Ensamblado

- [ ] 8.1 Crear `src/router.tsx` con el mapa de rutas: `/login` y `/registro` públicas con redirección a `/perfil` si ya hay sesión, `/perfil` protegida dentro del layout, `/` redirigiendo según haya sesión o no, y el comodín a la página de no encontrada. Verificar cada redirección navegando por URL con y sin sesión.
- [x] 8.2 Reescribir `src/App.tsx` reemplazando el template por el `QueryClientProvider`, el `AuthProvider`, el `BrowserRouter` y el router, arrancando el worker de mocks solo en desarrollo. Verificar que `npm run build` pasa y que ya no queda ninguna referencia al contador de ejemplo.
- [x] 8.3 Verificar que el worker de MSW no llega al build de producción: ejecutar `npm run build` y comprobar que `msw` no aparece en el bundle generado en `dist/`.
- [x] 8.4 Escribir `frontend/README.md`: arrancar es solo `npm install` y `npm run dev`, sin backend y sin PostgreSQL. Documentar las credenciales del usuario semilla, que el almacén se reinicia al recargar, y que conectar el backend real es el cambio posterior `conectar-backend-real`. Verificar siguiendo el README desde cero en una terminal limpia.

## 9. Verificación integral

- [x] 9.1 Ejecutar `npm run lint` y `npm run build` en `frontend/` y verificar que ambos pasan sin errores ni advertencias nuevas.
- [ ] 9.2 Recorrer el flujo completo contra los mocks: registrarse, cerrar sesión, iniciar sesión, editar nombre y apellido, cambiar username, cambiar contraseña, recargar la página en cada pantalla y volver a iniciar sesión con la contraseña nueva. Verificar que la sesión sobrevive a cada recarga y que ningún paso deja la interfaz en estado de carga permanente.
- [ ] 9.3 Verificar el manejo de sesión expirada: usar un token vencido emitido por los mocks, ejecutar una acción y comprobar que la aplicación cierra la sesión, avisa que expiró y redirige a `/login`.
- [ ] 9.4 Verificar los casos de error que los mocks permiten disparar a voluntad: 409 de username duplicado, 400 de validación con mapa de errores por campo, respuesta de error sin cuerpo interpretable y fallo de red. Comprobar que cada uno muestra el mensaje correcto y deja el formulario utilizable.
- [x] 9.5 Verificar que `backend/` no tiene ninguna modificación (`git status backend/` sale limpio) y que ninguna pantalla expone la contraseña ni su hash.
