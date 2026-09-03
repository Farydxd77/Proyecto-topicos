## Why

El backend ya expone una superficie de identidad completa y probada (registro, login
con JWT y gestión del perfil propio), pero no existe ninguna forma de usarla que no
sea un cliente HTTP manual. La carpeta `frontend/` sigue conteniendo el template por
defecto de Vite: un contador de ejemplo, assets sin usar y el título "dashboard".

Arrancar ahora la fase 2 con el vertical slice de identidad valida el flujo JWT de
punta a punta en un navegador real y deja montado el esqueleto (enrutado, cliente
HTTP, sesión, estilos) sobre el que las próximas features de backend —grupos,
gastos, balances— van a entrar como pantallas y no como infraestructura.

## What Changes

- **BREAKING (proceso)**: se habilita la fase 2. `CLAUDE.md` y `openspec/config.yaml`
  declaran hoy *"Fase actual: backend únicamente. No tocar la carpeta `frontend/`"*;
  ambos se actualizan para permitir trabajo en `frontend/`. Sin esto, las sesiones
  futuras se bloquean solas.
- Se elimina el template por defecto de Vite: el contador de `App.tsx`, los estilos
  de ejemplo y los assets sin usar (`hero.png`, `react.svg`, `vite.svg`); el título
  de `index.html` pasa de "dashboard" a "Cuentas Claras" y el idioma a `es`.
- Se agregan tres dependencias al frontend, que hoy solo tiene `react` y `react-dom`:
  `react-router` (enrutado), `@tanstack/react-query` (estado de servidor, caché e
  invalidación) y `tailwindcss` (estilos).
- Se configura un proxy de desarrollo en `vite.config.ts` que reenvía `/api` a
  `http://localhost:8080`. Esto resuelve el mismo-origen en desarrollo **sin tocar el
  backend**, donde `CorsConfig` todavía no existe.
- Nueva capa `src/mocks/`: la API se simula con Mock Service Worker, que intercepta las
  peticiones en la frontera de red. Esto permite construir y verificar el frontend
  completo **sin levantar el backend ni PostgreSQL**. Los mocks reproducen fielmente los
  contratos reales —incluidos el 409 de username duplicado, el 401 de credenciales
  inválidas, el 401 de token vencido y el 200 con cuerpo vacío del cambio de
  contraseña— sobre un almacén en memoria que reacciona: registrarse crea una cuenta
  con la que después se puede iniciar sesión, y editar el perfil cambia lo que devuelve
  la consulta siguiente.
- Nueva capa `src/api/`: un cliente `fetch` que inyecta `Authorization: Bearer {token}`
  y traduce el formato de error estándar del `GlobalExceptionHandler`
  (`{timestamp, status, error, message, path}`, más el mapa `errors` por campo en los
  400 de validación) a un tipo de error tipado y consumible por la UI.
- Nuevo contexto de sesión: token persistido en `localStorage`, expuesto por un
  `AuthProvider`, con cierre de sesión automático ante cualquier 401 del backend.
- Nuevas pantallas: **Login**, **Registro** y **Perfil** (ver y editar nombre y
  apellido, cambiar username, cambiar contraseña).
- Nuevo layout con navegación. Los enlaces a **Grupos** y **Gastos** se muestran
  deshabilitados, señalando explícitamente que su backend todavía no existe.

## Capabilities

### New Capabilities

- `frontend-bootstrap`: cimientos de la aplicación React — limpieza del template,
  dependencias, proxy de desarrollo hacia el backend, enrutado, layout con
  navegación y la capa de cliente HTTP que traduce los errores estándar del backend.
- `frontend-auth`: registro e inicio de sesión desde el navegador, persistencia de la
  sesión JWT entre recargas, protección de rutas privadas y cierre de sesión (manual
  y automático ante 401).
- `frontend-perfil`: pantalla de cuenta propia — consulta y edición de los datos del
  participante, cambio de username y cambio de contraseña contra `/api/perfil/me`.

### Modified Capabilities

<!-- Ninguna: no cambian los requisitos de ninguna capacidad de backend existente.
     La actualización de CLAUDE.md y openspec/config.yaml es documental, no altera
     el comportamiento especificado de ninguna capacidad. -->

## Non-Goals

- **Grupos, gastos, balances y liquidación**: el backend todavía no expone esos
  endpoints. Sus enlaces aparecen deshabilitados, sin pantallas ni llamadas.
- **`CorsConfig` en el backend**: el proxy de Vite lo hace innecesario en desarrollo.
  Se resolverá cuando exista despliegue.
- **Cualquier modificación dentro de `backend/`**: este cambio no toca una sola línea
  de Java.
- **Despliegue, build de producción y variables de entorno por ambiente.**
- **Tests E2E** (Playwright, Cypress) y tests de componentes.
- **Refresh tokens, "recordarme" y recuperación de contraseña**: el backend emite un
  único token de 24 h y no tiene endpoints para nada de eso.
- **Diseño visual acabado**: el objetivo es una interfaz limpia y funcional, no una
  identidad de marca.
- **Conectar contra el backend real**: este cambio se completa y se archiva contra los
  mocks. Apagar el worker, verificar todo contra el backend y PostgreSQL y resolver las
  divergencias que aparezcan es trabajo de un cambio posterior,
  `conectar-backend-real`, que no se crea todavía.

## Impact

- **Código nuevo**: `src/api/` (cliente HTTP y tipos de error), `src/auth/`
  (`AuthProvider`, hook de sesión, ruta protegida), `src/pages/`
  (Login, Registro, Perfil), `src/components/` (layout y navegación), `src/router.tsx`.
- **Código modificado**: `frontend/index.html` (título e idioma), `src/App.tsx`
  (se reemplaza el template por los providers y el router), `src/main.tsx`,
  `src/index.css` y `src/App.css` (Tailwind), `vite.config.ts` (proxy),
  `frontend/package.json` (dependencias y nombre del paquete).
- **Código eliminado**: `src/assets/hero.png`, `src/assets/react.svg`,
  `src/assets/vite.svg` y los estilos del template.
- **Documentación modificada**: `CLAUDE.md` y `openspec/config.yaml` — se levanta la
  restricción de no tocar `frontend/` y se documenta el stack de la fase 2.
- **APIs**: ninguna nueva; se consumen los 6 endpoints ya existentes de
  `/api/auth/**` y `/api/perfil/**`.
- **Base de datos**: sin cambios.
- **Backend**: sin cambios.
- **Dependencia operativa**: ninguna. El frontend se desarrolla y se verifica por
  completo con `npm run dev`, sin backend y sin PostgreSQL, gracias a los mocks.
