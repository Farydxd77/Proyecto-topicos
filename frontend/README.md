# Cuentas Claras — Frontend

Interfaz web de Cuentas Claras. React 19 + Vite + TypeScript.

Habla siempre con el backend real: **no hay API simulada**. Las peticiones salen a
`/api` relativo y el proxy de Vite las reenvía a `localhost:8080`.

## Arrancar

Hacen falta **tres procesos**, en este orden:

```bash
# 1. Base de datos (desde la raíz del repo)
docker compose up -d

# 2. Backend (desde backend/)
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run

# 3. Frontend (desde frontend/)
npm install
npm run dev
```

| Proceso    | Puerto | Depende de           |
| ---------- | ------ | -------------------- |
| PostgreSQL | `5432` | Docker               |
| Backend    | `8080` | PostgreSQL           |
| Frontend   | `5173` | Backend (vía proxy)  |

Y listo: <http://localhost:5173>

Si el backend no está levantado, la aplicación no se cuelga: cada pantalla muestra
«El servidor no está respondiendo» con un botón de reintentar.

> **Por qué no hace falta configurar CORS.** El proxy reenvía las peticiones del lado
> del servidor, así que el navegador solo ve un origen (`localhost:5173`) y no hay
> petición cruzada que negociar. Solo será necesario si en producción el frontend y el
> backend se sirven desde dominios distintos.

## Entrar

No hay usuarios de prueba: las cuentas se crean desde **Registrate** y quedan
guardadas en PostgreSQL. Los datos persisten entre recargas y entre reinicios.

## Qué hay

- **Login** y **Registro**, con la sesión JWT persistida entre recargas
- **Perfil**: ver y editar nombre y apellido, cambiar username, cambiar contraseña
- **Grupos**: lista, creación, detalle, edición y eliminación (solo el creador
  administra)
- **Miembros**: buscar personas por CI, nombre o apellido y agregarlas o quitarlas
- **Gastos**: registrar, editar y eliminar, en cualquiera de las 48 monedas soportadas;
  el backend convierte a USDT contra CriptoYa y reparte entre todos los miembros
- **Balances y liquidación**: cuánto debe o le deben a cada uno, y la lista mínima de
  transferencias para quedar a mano

## Comandos

| Comando           | Qué hace                     |
| ----------------- | ---------------------------- |
| `npm run dev`     | Servidor de desarrollo       |
| `npm run build`   | Build de producción          |
| `npm run lint`    | oxlint                       |
| `npm run preview` | Sirve el build de producción |

## Cómo está armado

```
src/
├── api/          client.ts (fetch + token + ApiError), auth.ts, perfil.ts,
│                 grupos.ts, participantes.ts, gastos.ts, balances.ts, types.ts
├── auth/         AuthContext.tsx, useAuth.ts, RutaProtegida.tsx, token.ts
├── components/   Layout, Navegacion, Campo, Boton, MensajeError,
│                 GestionMiembros, FormularioGasto, SeccionGastos, SeccionBalances
├── pages/        LoginPage, RegistroPage, PerfilPage, GruposPage,
│                 GrupoDetallePage, GastoDetallePage, NoEncontradaPage
├── lib/          validacion.ts, claves.ts, formato.ts, monedas.ts, estadoConsulta.ts
└── router.tsx
```

Cinco reglas que conviene respetar al agregar código:

1. **Toda llamada al backend pasa por `apiFetch`**, nunca `fetch` directo. Ahí viven la
   inyección del token, la traducción de errores y el cierre de sesión ante un 401.
2. **El token es lo único que vive en `AuthContext`.** Todo lo que viene del backend lo
   posee TanStack Query. El username de la barra se lee del perfil cacheado, no del
   contexto — por eso cambiarlo actualiza la barra solo.
3. **Toda pantalla que consulta pasa por `estadoDe(consulta)`** de
   `lib/estadoConsulta.ts`. TanStack Query puede dejar una consulta en
   `fetchStatus: 'paused'` indefinidamente cuando su heurística de red decide que no
   hay conexión; ese helper la trata como error para que ninguna pantalla se quede
   colgada en «Cargando…».
4. **Las claves de consulta viven en `lib/claves.ts`.** Las mutaciones de una capacidad
   invalidan consultas de otra —un gasto cambia los balances, agregar un miembro cambia
   el reparto—, así que tenerlas juntas evita invalidaciones que no coinciden.
5. **Los montos se formatean, nunca se operan.** Llegan como números JSON con hasta 8
   decimales y el backend ya calculó conversión y reparto; hacer aritmética en coma
   flotante solo degradaría esa precisión.
