# Cuentas Claras — Frontend

Interfaz web de Cuentas Claras. React 19 + Vite + TypeScript.

## Arrancar

```bash
npm install
npm run dev
```

Y listo: <http://localhost:5173>

**No hace falta el backend ni PostgreSQL.** La API está simulada con
[MSW](https://mswjs.io), que intercepta las peticiones a `/api` en el navegador.

## Entrar

Hay un usuario semilla cargado, para no tener que registrarse primero:

| Username | Contraseña |
| -------- | ---------- |
| `demo`   | `demo1234` |

También podés crear una cuenta desde **Registrate**: queda disponible para iniciar
sesión de inmediato.

> Los datos viven en memoria y **se reinician con cada recarga de la página**. Es a
> propósito: cada prueba arranca desde un estado conocido.

## Qué hay

- **Login** y **Registro**, con la sesión JWT persistida entre recargas
- **Perfil**: ver y editar nombre y apellido, cambiar username, cambiar contraseña
- **Grupos** y **Gastos** aparecen en la navegación pero están deshabilitados: su
  backend todavía no existe

## Comandos

| Comando         | Qué hace                                  |
| --------------- | ----------------------------------------- |
| `npm run dev`   | Servidor de desarrollo con la API simulada |
| `npm run build` | Build de producción (sin MSW)              |
| `npm run lint`  | oxlint                                     |
| `npm run preview` | Sirve el build de producción             |

## Cómo está armado

```
src/
├── api/          client.ts (fetch + token + ApiError), auth.ts, perfil.ts, types.ts
├── auth/         AuthContext.tsx, useAuth.ts, RutaProtegida.tsx, token.ts
├── components/   Layout, Navegacion, Campo, Boton, MensajeError
├── pages/        LoginPage, RegistroPage, PerfilPage, NoEncontradaPage
├── lib/          validacion.ts
├── mocks/        db.ts (almacén en memoria), handlers.ts, browser.ts
└── router.tsx
```

Dos reglas que conviene respetar al agregar código:

1. **Toda llamada al backend pasa por `apiFetch`**, nunca `fetch` directo. Ahí viven la
   inyección del token, la traducción de errores y el cierre de sesión ante un 401.
2. **El token es lo único que vive en `AuthContext`.** Todo lo que viene del backend lo
   posee TanStack Query. El username de la barra de navegación se lee del perfil
   cacheado, no del contexto — por eso cambiarlo actualiza la barra solo.

## Conectar el backend real

Cuando el backend esté corriendo en `localhost:8080`, apagar los mocks es dejar de
arrancar el worker: la llamada a `iniciarMocks()` de `src/main.tsx` está detrás de
`import.meta.env.DEV` y el proxy de `/api` hacia el 8080 ya está configurado en
`vite.config.ts`. El código de producción no cambia.

Eso es trabajo del cambio `conectar-backend-real`, que además tiene que reverificar
todos los flujos contra PostgreSQL y resolver las divergencias que aparezcan entre la
simulación y el backend de verdad.
