## Context

Ver `proposal.md` — Why para la motivación. Lo que condiciona el diseño:

- **El punto de partida es un template intacto.** `frontend/` tiene el andamiaje de
  Vite sin modificar: React 19.2, Vite 8.2, TypeScript 6.0, oxlint, y solo `react` y
  `react-dom` como dependencias. No hay enrutado, ni estilos, ni cliente HTTP.
- **El backend no tiene CORS.** `CorsConfig` figura en `CLAUDE.md` pero la clase no
  existe. Cualquier petición desde `localhost:5173` a `localhost:8080` sería
  bloqueada por el navegador.
- **La superficie de la API es chica y fija.** Seis endpoints (ver `proposal.md`), sin
  paginación, sin filtros y sin refresh token. El backend emite un único JWT de 24 h.
- **El backend ya define un formato de error único.** `GlobalExceptionHandler` produce
  siempre `{timestamp, status, error, message, path}`, y agrega un mapa `errors` por
  campo en los 400 de validación. El frontend puede confiar en esa forma.
- **`PUT /api/perfil/me/password` devuelve `void`**: responde 200 con cuerpo vacío, no
  204. El cliente HTTP debe tolerarlo sin intentar parsear JSON.
- **Las validaciones del backend son conocidas y estables** (`@NotBlank`, `@Size`),
  así que el frontend puede espejarlas sin adivinar.
- **El backend no va a estar corriendo durante este cambio.** Se construye y se
  verifica el frontend entero contra una API simulada; conectar el backend real es un
  cambio posterior. Esto no relaja los contratos: la API está definida por código Java
  ya escrito y leído, así que la simulación no inventa nada.

## Goals / Non-Goals

**Goals:**

- Un esqueleto donde agregar una feature nueva (grupos, gastos) sea agregar una
  carpeta y una ruta, sin volver a tocar infraestructura.
- Un único punto por el que pasa toda llamada al backend, para que la inyección del
  token, la traducción de errores y la reacción al 401 estén escritas una sola vez.
- No modificar `backend/` ni una línea.
- Espejar las validaciones del backend en el cliente para dar feedback inmediato, sin
  que el cliente sea la única defensa.

**Non-Goals (de diseño; ver también `proposal.md` — Non-Goals):**

- Abstraer sobre el cliente HTTP pensando en un backend futuro distinto.
- Un sistema de diseño con tokens, temas o librería de componentes.
- Internacionalización: la interfaz es solo en español, con textos literales.
- Modo oscuro.

## Decisions

### 1. Proxy de Vite en lugar de `CorsConfig` en el backend

`vite.config.ts` reenvía `/api` a `http://localhost:8080`. El navegador ve todo como
mismo origen y la cuestión de CORS desaparece en desarrollo.

*Alternativas descartadas:*

- **Agregar `CorsConfig` al backend ahora.** Rompe el objetivo de no tocar `backend/`,
  y obligaría a mantener una lista de orígenes permitidos que en desarrollo cambia
  seguido. Se hará cuando exista un despliegue real, que es cuando se sabrá el origen.
- **`@CrossOrigin` en los controllers.** Dispersa configuración de seguridad en las
  clases de negocio, y habría que repetirlo en cada controller nuevo.

*Consecuencia:* el código del frontend usa rutas relativas (`/api/...`), nunca URLs
absolutas. Eso lo deja listo para servirse detrás del mismo dominio en producción.

*Estado actual:* el proxy se configura igual, pero queda **dormido** mientras los mocks
estén activos: el service worker atiende `/api` antes de que la petición llegue a la
red, así que el proxy nunca se usa. Se deja configurado porque cuesta tres líneas y es
justo lo que hará falta el día que se apaguen los mocks.

### 1b. La API se simula con MSW, interceptando la red

Mock Service Worker registra un service worker que responde las peticiones a `/api`
antes de que salgan del navegador. La consecuencia que decide la elección: **el código
de producción no se entera**. `client.ts` hace un `fetch` de verdad, adjunta la cabecera
`Authorization` de verdad y traduce respuestas HTTP de verdad. Conectar el backend real
es dejar de arrancar el worker — cero cambios en código de producción.

*Alternativas descartadas:*

- **Un módulo `api/mock.ts` conmutado por `VITE_USE_MOCKS`.** Es más simple, pero
  cortocircuita justamente la capa que más importa probar: con los mocks encendidos, el
  cliente HTTP, la inyección del token, la traducción a `ApiError` y el manejo del 401
  no se ejecutan nunca. Los bugs de esa capa aparecerían todos juntos el día de
  conectar el backend, que es el peor momento.
- **Envolver `window.fetch` a mano.** Ejercita el cliente real igual que MSW, pero hay
  que escribir y mantener el emparejado de rutas, los códigos de estado y los retardos.
  Es reimplementar MSW peor.

*Ventaja adicional:* escenarios que contra Postgres son un engorro de reproducir —el 409
de username duplicado, el 401 de token vencido, el fallo de red, la respuesta de error
sin cuerpo interpretable— acá se disparan a voluntad. Varios escenarios de las specs
solo son verificables de forma determinista gracias a esto.

### 1c. Los datos simulados viven en un almacén en memoria que reacciona

`src/mocks/db.ts` mantiene usuarios y participantes en memoria, con un usuario semilla
para poder entrar sin registrarse primero. Registrarse agrega una cuenta con la que
después se puede iniciar sesión; editar el perfil cambia lo que devuelve la consulta
siguiente; cambiar la contraseña invalida la anterior en el login. El almacén se
reinicia al recargar la página, y eso se documenta.

Los tokens que emiten los mocks son JWT **estructuralmente** válidos —tres segmentos en
base64url con un `exp` real— aunque la firma sea de mentira. Así `auth/token.ts`
decodifica un `exp` de verdad y la lógica de vencimiento se ejercita en serio.

*Alternativa descartada:* respuestas fijas por endpoint. Se escriben en la mitad de
tiempo, pero entonces guardar el perfil no cambiaría nada en pantalla, y quedaría sin
verificar precisamente lo que hay que verificar: que una mutación invalida la caché de
TanStack Query y la vista se refresca. Con respuestas fijas, ese bug es invisible.

### 2. `react-router` en modo declarativo, no en modo framework

Se usa `react-router` con `BrowserRouter` y `Routes` en el código. No se adopta el
modo framework, con su propio build, sus loaders y SSR.

*Versión:* queda en la 7.18, no en la 8. La 8 exige `node >= 22.22.0` y la máquina de
desarrollo tiene v22.19, así que npm resuelve a la 7 por sí solo. Para lo que se usa
acá —`BrowserRouter`, `Routes`, `Route`, `Navigate`, `Outlet`, `useNavigate`,
`useLocation`, `Link`— la API es la misma en ambas, así que no cambia nada del diseño.
Subir de Node y de versión mayor es un cambio aparte.

*Alternativa descartada:* el modo framework de react-router aporta carga de datos por
ruta y SSR, pero se lleva puesto el build de Vite ya configurado y agrega un modelo
mental grande para tres pantallas. Con TanStack Query ya cubriendo la carga de datos,
duplicaría responsabilidades.

### 3. Dos estados separados: sesión en Context, datos del servidor en TanStack Query

- **`AuthContext`** guarda únicamente el **token** y deriva de él el estado de sesión.
  Es lo único que la aplicación posee de verdad.
- **TanStack Query** posee todo lo que viene del backend, empezando por el perfil bajo
  la clave `['perfil']`. Las mutaciones de perfil invalidan esa clave.

El username que muestra la navegación se lee del perfil cacheado, **no** del Context.
Así, cambiar el username invalida una sola clave y la navegación se actualiza sola,
sin sincronizar dos fuentes de verdad.

*Alternativa descartada:* guardar el usuario completo en el `AuthContext` junto al
token. Obliga a actualizarlo a mano tras cada mutación de perfil y crea dos copias del
mismo dato que se desincronizan — exactamente el bug que el cambio de username
provocaría.

### 4. El token vive en `localStorage`

Es la única opción compatible con el backend actual: el JWT viaja en la cabecera
`Authorization`, no en una cookie, así que el frontend necesita poder leerlo.

*Alternativas descartadas:*

- **Cookie `HttpOnly`.** Es más segura frente a XSS, pero requiere que el backend
  emita y lea cookies y maneje CSRF. Es un cambio de backend, fuera de alcance.
- **`sessionStorage`.** La sesión se perdería al cerrar la pestaña, contradiciendo el
  requisito de persistencia entre recargas.
- **Solo en memoria.** Se pierde en cada recarga (F5), inaceptable en desarrollo.

### 5. La expiración se detecta leyendo el `exp` del JWT, sin verificar la firma

Al arrancar, la aplicación decodifica el payload del token para descartar de entrada
uno ya vencido y evitar un parpadeo de pantalla privada seguido de un 401.

Esto es una **optimización de experiencia, no un control de seguridad**: la
verificación real es siempre la del backend. El cliente nunca decide si un token es
válido, solo si vale la pena intentarlo.

*Alternativa descartada:* llamar a `GET /api/perfil/me` al arrancar y decidir según la
respuesta. Es una ida y vuelta extra en cada carga; además la aplicación igual va a
pedir el perfil, así que el chequeo previo sale gratis.

### 6. El manejo del 401 vive en el cliente HTTP, con una excepción explícita

El cliente `fetch` detecta el 401, dispara el cierre de sesión y redirige. La excepción
son las llamadas a `/api/auth/**`: ahí un 401 significa "credenciales inválidas", no
"sesión expirada", y debe llegar al formulario como un error normal.

La distinción se hace por **ruta llamada**, no por el contenido del mensaje, para no
depender de textos que el backend puede cambiar.

### 7. Errores del backend como un tipo único, `ApiError`

El cliente traduce toda respuesta no exitosa a un `ApiError` con `status`, `message` y
un mapa opcional de errores por campo. Los casos límite se normalizan ahí: cuerpo
vacío, cuerpo que no es JSON, fallo de red. Las pantallas nunca ven una `Response`
cruda y nunca tienen que preguntarse qué forma tiene el error.

Los formularios pintan el error del campo junto a cada campo cuando viene, y el
`message` como error general cuando no.

### 8. Tailwind 4 vía plugin de Vite, sin archivo de configuración

Tailwind 4 se integra con `@tailwindcss/vite` y se activa con una sola línea de import
en el CSS. No necesita `tailwind.config.js` ni PostCSS.

*Alternativa descartada:* Tailwind 3 con PostCSS y globs de contenido. Es la vía
anterior: más piezas para configurar y compilación más lenta.

### 9. Validación de formularios escrita a mano

Cada formulario valida en su propio envío, con las reglas espejadas del backend
centralizadas en un módulo de validación compartido para no repetir "mínimo 8
caracteres" en tres lugares.

*Alternativa descartada:* React Hook Form + Zod. Para tres formularios de entre dos y
cinco campos, dos dependencias más no se pagan. Si aparecen los formularios de gastos
(montos, fechas, división entre participantes), conviene reevaluarlo — y ahí el módulo
de validación compartido es justamente el punto de entrada.

### 10. Estructura por tipo, no por feature

```
src/
├── api/          client.ts (fetch + token + ApiError), auth.ts, perfil.ts, types.ts
├── auth/         AuthContext.tsx, useAuth.ts, RutaProtegida.tsx, token.ts
├── components/   Layout.tsx, Navegacion.tsx, Campo.tsx, Boton.tsx, MensajeError.tsx
├── pages/        LoginPage.tsx, RegistroPage.tsx, PerfilPage.tsx, NoEncontradaPage.tsx
├── lib/          validacion.ts
├── mocks/        db.ts (almacén en memoria), handlers.ts (6 endpoints), browser.ts
├── router.tsx
├── App.tsx       providers + router
└── main.tsx
```

Con tres pantallas, agrupar por feature crearía carpetas de un solo archivo. Cuando
entren grupos y gastos conviene revisarlo, pero anticiparlo ahora es estructura vacía.

## Risks / Trade-offs

- **El token en `localStorage` es accesible desde JavaScript, y por lo tanto ante un
  XSS** → Mitigación: React escapa por defecto todo lo que renderiza y no se usa
  `dangerouslySetInnerHTML` en ninguna parte. La mitigación real (cookie `HttpOnly`) es
  un cambio de backend y queda anotada como deuda explícita para cuando haya despliegue.

- **`PUT /api/perfil/me/password` no pide la contraseña actual.** El backend la cambia
  solo con el token, así que quien tome prestada una sesión abierta puede apropiarse de
  la cuenta → Mitigación: **ninguna posible desde el frontend**. Pedir la contraseña
  actual en el formulario sería teatro, porque el backend no la verificaría. Es un
  hallazgo de backend y debe tratarse como un cambio aparte. Este diseño no lo disimula:
  el formulario pide contraseña nueva y confirmación, nada más.

- **El proxy de Vite solo existe en desarrollo.** Un build de producción servido desde
  otro origen fallaría por CORS → Mitigación: el frontend usa rutas relativas, así que
  servirlo detrás del mismo dominio que el backend funciona sin cambios. El caso de
  dominios distintos se resuelve con `CorsConfig`, cuando exista despliegue.

- **Sin refresh token, la sesión muere a las 24 h sin aviso previo** y la persona puede
  perder lo que estaba escribiendo → Mitigación: el 401 se maneja de forma limpia y
  avisa que la sesión expiró, en lugar de mostrar un error críptico. Un aviso anticipado
  calculado desde el `exp` es una mejora posterior.

- **Vite 8, React 19, TypeScript 6 y Tailwind 4 son versiones recientes y sus
  combinaciones pueden tener aristas** → Mitigación: la primera tarea de implementación
  instala las dependencias y verifica que el build pasa, antes de escribir cualquier
  pantalla. Si algo choca, se descubre en el paso 1 y no con tres pantallas ya escritas
  encima.

- **Los mocks pueden divergir del backend real, y la divergencia se descubre recién al
  conectar.** Es el costo genuino de esta estrategia: un frontend que anda perfecto
  contra la simulación puede romperse contra Postgres → Mitigación: los handlers se
  escriben leyendo los DTOs y el `GlobalExceptionHandler` reales, no de memoria, y
  `src/api/types.ts` es la única definición de los contratos, compartida por los mocks y
  por el código de producción — si un contrato cambia, TypeScript rompe ambos lados a la
  vez. Aun así, la divergencia no se elimina, solo se acota: cerrarla es el trabajo del
  cambio `conectar-backend-real`, y hay que asumir que ahí van a aparecer ajustes.

- **El worker de MSW podría llegar a un build de producción y simular la API en serio**
  → Mitigación: se arranca únicamente bajo la bandera de desarrollo de Vite, de modo que
  el bundler lo elimina del build. La tarea de ensamblado verifica que el build de
  producción no contiene el worker.

## Migration Plan

No hay datos ni usuarios que migrar; el frontend nunca estuvo en uso. La secuencia:

1. Instalar dependencias y verificar que el build pasa **antes** de escribir pantallas.
2. Levantar la restricción sobre `frontend/` en `CLAUDE.md` y `openspec/config.yaml`
   dentro del mismo cambio, para que el repositorio quede coherente en un solo commit.
3. Definir los contratos y montar los mocks antes que el cliente HTTP, para que cada
   pieza sea verificable en el momento en que se escribe.
4. Construir la infraestructura (proxy, cliente, sesión, router) antes que las
   pantallas.

Al terminar, el frontend queda completo y archivable contra los mocks. Apagar el worker
y reverificar contra el backend y PostgreSQL es el cambio posterior
`conectar-backend-real`.

**Rollback:** revertir el commit. No hay estado persistido fuera del `localStorage` del
navegador de quien haya probado, y ese token queda inerte por sí solo.
