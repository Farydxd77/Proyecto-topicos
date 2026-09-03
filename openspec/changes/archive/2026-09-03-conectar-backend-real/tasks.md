## 1. Levantar el sistema completo

- [x] 1.1 Arrancar PostgreSQL en `localhost:5432` con la base `cuentas_claras`
  accesible con las credenciales de `application.properties`
  (`postgres`/`admin`, o las que definan `DB_USER`/`DB_PASS`). Verificar que el
  puerto responde antes de seguir.
- [x] 1.2 Ejecutar `mvnw.cmd test` desde `backend/` y comprobar que las 14 clases
  de test pasan. Esta es la primera corrida completa de la suite: deja constancia
  de que grupos, gastos, moneda y balances funcionan de verdad antes de que el
  frontend dependa de ellos. Si algo falla, resolverlo antes de continuar.
- [x] 1.3 Arrancar el backend con `mvnw.cmd spring-boot:run` desde `backend/` y
  verificar que responde: `GET http://localhost:8080/actuator/health` devuelve
  estado `UP`, y `GET http://localhost:8080/api/grupos` sin token devuelve `401`
  con el formato de error estándar. Confirmar que Hibernate no reporta errores de
  esquema en el arranque.

## 2. Apagar los mocks por defecto

- [x] 2.1 Eliminar MSW del proyecto: borrar `frontend/src/mocks/`,
  `frontend/public/mockServiceWorker.js`, la dependencia `msw` y su bloque
  `workerDirectory` de `package.json`, y simplificar `frontend/src/main.tsx` para
  que monte la aplicacion sin ningun arranque condicional. Verificar que
  `grep -r "msw" src/ package.json` no devuelve nada.
- [x] 2.2 Simplificar `frontend/vite.config.ts`: quitar el plugin que borraba el
  service worker de MSW del build, y dejar documentado en el comentario del proxy
  por que CORS no hace falta en desarrollo. Verificar que `npm run build` sigue
  funcionando.
- [x] 2.3 Verificar que la aplicacion no depende de ninguna variable de entorno
  para elegir origen de datos: arrancar `npm run dev` sin `.env` y comprobar que
  las peticiones llegan al backend real.
- [x] 2.4 Verificar que el build de produccion no arrastra nada de MSW: ejecutar
  `npm run build` y comprobar que `dist/` no contiene `mockServiceWorker.js` ni
  codigo de `msw` en los bundles.

## 3. Verificar autenticación contra el backend real

- [x] 3.1 Con los tres procesos arriba y sin la bandera de mocks, registrar una
  cuenta nueva desde `/registro` usando un username con sufijo variable. Verificar
  que la aplicación inicia sesión sola tras el registro, y que la fila existe en
  la base (`SELECT username FROM usuarios ORDER BY id DESC LIMIT 1`).
- [x] 3.2 Verificar el inicio de sesión: cerrar sesión, entrar por `/login` con
  esas credenciales, y comprobar que el acceso funciona. Verificar también que
  unas credenciales incorrectas muestran el mensaje de credenciales inválidas sin
  revelar cuál de los dos campos falló.
- [x] 3.3 Verificar la persistencia de la sesión: estando autenticado, recargar la
  página en cada pantalla y comprobar que la sesión sobrevive y que ninguna
  pantalla queda en estado de carga permanente.
- [x] 3.4 Verificar el registro con username duplicado: intentar registrar el
  username creado en 3.1. Debe mostrar el mensaje de conflicto del backend (`409`)
  y conservar los datos ya cargados en el formulario.
- [x] 3.5 Verificar el cierre de sesión: comprobar que limpia el token, redirige a
  `/login` y que volver atrás en el navegador no devuelve a una pantalla protegida
  con datos visibles.

## 4. Verificar el perfil contra el backend real

- [x] 4.1 Verificar la consulta: entrar a `/perfil` y comprobar que muestra
  nombre, apellido, CI y username reales de la base, con el CI en solo lectura.
  Contrastar el `createdAt` que devuelve el backend con lo que muestra la pantalla
  y confirmar que la fecha no se desplaza de día (Jackson serializa
  `LocalDateTime` sin zona horaria; los mocks emitían ISO con `Z`).
- [x] 4.2 Verificar la edición de nombre y apellido: guardar cambios, comprobar
  que la pantalla los refleja sin recargar, recargar y comprobar que persisten, y
  que cancelar restaura los valores guardados.
- [x] 4.3 Verificar el cambio de username: cambiarlo por uno disponible y
  comprobar que se refleja en la pantalla y en la navegación sin cerrar la sesión;
  intentar cambiarlo al de otra cuenta y comprobar el `409` conservando el
  anterior; reenviar el propio sin cambios y comprobar que funciona.
- [x] 4.4 Verificar el cambio de contraseña: comprobar que el backend responde
  `200` con **cuerpo vacío** y que la aplicación lo trata como éxito y no como
  error de parseo; que ambos campos se vacían tras el éxito; y que después se
  puede iniciar sesión con la contraseña nueva y ya no con la vieja.
- [x] 4.5 Verificar el mapa de errores por campo: enviar el formulario de registro
  con una contraseña de menos de 8 caracteres saltándose la validación del cliente
  (desde las herramientas del navegador) y comprobar que el `400` del backend trae
  el mapa `errors` y que la pantalla lo muestra junto al campo correspondiente,
  con los nombres de campo coincidiendo con los del formulario.

## 5. Verificar los fallos de conexión

- [x] 5.1 Verificar el backend caído: con la aplicación abierta y autenticada,
  detener el backend y ejecutar una acción que pida datos. Comprobar que aparece
  el mensaje de que no se pudo conectar, que se ofrece reintentar, que la interfaz
  sigue utilizable y que **no** se cierra la sesión (un fallo de red no es un token
  rechazado).
- [x] 5.2 Verificar la recuperación: con el mensaje de error en pantalla, volver a
  arrancar el backend y pulsar reintentar. Los datos deben cargarse sin recargar
  la página.
- [x] 5.3 Verificar la sesión expirada contra el backend real: alterar el token
  guardado en el navegador por uno inválido, ejecutar una acción sobre una ruta
  protegida y comprobar que la aplicación cierra la sesión, avisa que expiró y
  redirige a `/login`. Confirmar que el `401` de
  `JwtAuthenticationEntryPoint` se distingue del `401` de credenciales de
  `/api/auth/login`, que debe seguir mostrándose dentro del formulario.

## 6. Resolver divergencias y documentar

- [x] 6.1 Dejar constancia de las divergencias detectadas entre lo que asumian los
  mocks y lo que hace el backend real. Al eliminarse MSW no hay simulacion que
  ajustar; lo que si debe corregirse es `src/api/types.ts` si algun contrato no
  coincide. Divergencia encontrada: `createdAt` de `PerfilResponse` llega sin `Z`
  (Jackson serializa `LocalDateTime` sin zona horaria) mientras los mocks emitian
  ISO con `Z`; no afecta al contrato en TypeScript, que ya lo declara `string`.
- [x] 6.2 Actualizar `frontend/README.md`: seccion de puesta en marcha con los tres
  procesos (PostgreSQL `5432` via `docker compose`, backend `8080`, frontend
  `5173`) y el orden de arranque, y quitar toda mencion a MSW y al modo simulado.
  Verificar siguiendo la seccion desde cero en una terminal limpia.
- [x] 6.3 Actualizar `CLAUDE.md`: reemplazar que el frontend se desarrolla contra
  MSW por que se desarrolla contra el backend real, y quitar MSW del stack; quitar
  «Grupos, gastos, balances y liquidacion en el frontend» de «Fuera de alcance», ya
  que los cambios siguientes los implementan; actualizar la estructura de carpetas
  del frontend; y aclarar en la seccion de CORS que en desarrollo el proxy de Vite
  lo hace innecesario y que solo sera necesario si en produccion se sirven desde
  dominios distintos. Verificar releyendo el archivo completo en busca de
  afirmaciones que hayan quedado obsoletas.

## 7. Verificación final

- [x] 7.1 Recorrer el flujo completo de punta a punta contra el backend real, en
  una sesión de navegador limpia: registrarse, cerrar sesión, iniciar sesión,
  editar nombre y apellido, cambiar username, cambiar contraseña, volver a iniciar
  sesión con la contraseña nueva, y recargar la página en cada pantalla.
  Comprobar que todos los datos persisten en PostgreSQL y que ningún paso deja la
  interfaz colgada.
- [x] 7.2 Ejecutar `npx oxlint` desde `frontend/` y comprobar que no hay errores
  nuevos introducidos por este cambio.
