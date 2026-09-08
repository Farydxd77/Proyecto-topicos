## Why

Tres huecos de infraestructura que no son bugs de funcionalidad pero limitan el
proyecto:

1. **La suite de tests no corre sin Docker.** De los tests del backend, todos los de
   integración exigen un PostgreSQL escuchando en `localhost:5432`. Quien clona el
   repo y hace `mvnw test` sin haber levantado `docker compose up -d` ve fallar
   decenas de tests con un error de contexto de Spring que no dice nada sobre la
   causa real. Eso hace que correr los tests sea un ritual en vez de un reflejo.
2. **Si CriptoYa se cae, no se puede registrar ningún gasto en moneda fiat.** Cada
   alta o edición hace una o dos llamadas HTTP sincrónicas, sin caché y sin ninguna
   red de contención. La respuesta `503` es correcta, pero deja la aplicación
   inutilizable para todo lo que no sea USDT mientras dure la caída de un servicio
   de terceros. Además, registrar cinco gastos seguidos en bolivianos consulta la
   misma cotización cinco veces.
3. **No hay nada preparado para desplegar.** No existe `CorsConfig`, así que el día
   que el frontend y el backend se sirvan desde dominios distintos el navegador
   bloquea todas las peticiones; y no hay imágenes de contenedor para ninguno de los
   dos módulos.

## What Changes

- **Tests sin Docker**: se agrega H2 en modo de compatibilidad PostgreSQL con
  alcance `test`, y un `src/test/resources/application.properties` que los tests
  toman por precedencia de classpath. `mvnw test` pasa a correr en una base en
  memoria, sin Docker y sin tocar la base de desarrollo. Ninguna clase de test
  cambia: no hace falta anotar nada.
- **Caché de cotizaciones con degradación elegante** en `CriptoYaClient`:
  - Una cotización recién consultada se reutiliza durante `criptoya.cache-ttl`
    (60 segundos por defecto). Registrar varios gastos seguidos en la misma moneda
    pasa a hacer una sola llamada externa.
  - **BREAKING** (de contrato de error): si CriptoYa falla y hay una cotización
    cacheada de hasta `criptoya.cache-max-stale` (24 horas por defecto), el sistema
    la usa en lugar de responder `503`. El `503` queda para cuando no hay ninguna
    cotización previa de ese par. La tasa que se persiste en el gasto es la que
    realmente se aplicó, así que el registro sigue siendo fiel.
- **CORS configurable**: nuevo `config/CorsConfig` gobernado por la propiedad
  `cors.allowed-origins`. Vacía por defecto, que es el caso de desarrollo: el proxy
  de Vite hace que no haya petición cruzada y no se registra ninguna configuración.
  Con uno o más orígenes, se habilitan `GET/POST/PUT/DELETE/OPTIONS` y las cabeceras
  `Authorization` y `Content-Type` para esos orígenes.
- **Imágenes de contenedor**: `backend/Dockerfile` multi-etapa (Maven + JRE 21) y
  `frontend/Dockerfile` multi-etapa (Node + nginx), con sus `.dockerignore`.

## Capabilities

### New Capabilities

- `infraestructura`: cómo se ejecutan los tests sin dependencias externas, cómo se
  configura CORS para producción sin afectar el desarrollo, y qué garantiza cada
  imagen de contenedor.

### Modified Capabilities

- `cambio-moneda`: la conversión pasa a apoyarse en una caché con TTL, y la caída de
  CriptoYa deja de ser fatal cuando existe una cotización reciente del mismo par.

## Impact

- **Backend nuevo**: `config/CorsConfig`, `backend/Dockerfile`,
  `backend/.dockerignore`, `backend/src/test/resources/application.properties`.
- **Backend modificado**: `pom.xml` (dependencia H2 con alcance `test`),
  `client/CriptoYaClient` (caché y degradación), `application.properties`
  (propiedades `criptoya.cache-ttl`, `criptoya.cache-max-stale`,
  `cors.allowed-origins`).
- **Frontend nuevo**: `frontend/Dockerfile`, `frontend/nginx.conf`,
  `frontend/.dockerignore`.
- **Contrato de la API**: la única diferencia observable es que una petición que hoy
  devuelve `503` puede pasar a devolver `201`/`200` con una tasa cacheada.
- **Sin cambios de base de datos** ni de esquema.

## Non-Goals

- No se migra a Testcontainers: el objetivo es justamente poder correr los tests
  **sin** Docker.
- No se cambia la base de desarrollo ni la de producción: siguen siendo PostgreSQL
  17. H2 se usa exclusivamente en la fase de test.
- No se agrega una tasa de cambio fija de respaldo ni codificada en el proyecto: si
  no hay cotización cacheada de ese par, el sistema sigue respondiendo `503` antes
  que inventar un número.
- No se persiste la caché: vive en memoria y se pierde al reiniciar el backend. Un
  reinicio con CriptoYa caído vuelve a responder `503`, y eso es aceptable.
- No se agrega un orquestador de despliegue (Kubernetes, Compose de producción) ni
  un pipeline de CI: solo las imágenes.
- No se habilita CORS por defecto: sin `cors.allowed-origins` configurado, el
  comportamiento en desarrollo es idéntico al de hoy.
- No se cambian los timeouts del cliente HTTP ni el manejo de monedas soportadas.
