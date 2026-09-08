## 1. Tests sin Docker

- [x] 1.1 En `backend/pom.xml`, agregar la dependencia `com.h2database:h2` con
  `<scope>test</scope>`. Verificar con `cd backend && mvnw.cmd -q test-compile`.
- [x] 1.2 Crear `backend/src/test/resources/application.properties` con: URL de H2
  en memoria con `MODE=PostgreSQL` y `DB_CLOSE_DELAY=-1`, driver `org.h2.Driver`,
  `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect`,
  `ddl-auto=create-drop`, `show-sql=false`, el `jwt.secret` de test y las
  propiedades de manejo de errores. Comentar por qué el archivo se llama igual que
  el de `main` (precedencia de classpath) y por qué eso es deliberado.
- [x] 1.3 Correr `mvnw.cmd test` con el contenedor de PostgreSQL **detenido** y
  confirmar que la suite completa pasa.
- [x] 1.4 Volver a levantar PostgreSQL y confirmar que la base de desarrollo
  conserva sus datos (los tests no la tocaron).

## 2. Caché de cotizaciones

- [x] 2.1 En `application.properties`, agregar `criptoya.cache-ttl=60s` y
  `criptoya.cache-max-stale=24h`, con un comentario sobre para qué sirve cada una.
- [x] 2.2 En `client/CriptoYaClient.java`, agregar un
  `ConcurrentHashMap<String, Cotizacion>` (record con `BigDecimal bid` e `Instant
  obtenida`) y leer las dos duraciones por constructor con `@Value`. Verificar con
  `mvnw.cmd -q test-compile`.
- [x] 2.3 Reescribir `precio(path)` como `bid(path)` con la lógica del design:
  entrada fresca → se devuelve sin llamar; si no, se llama a CriptoYa y se refresca;
  si la llamada falla y hay entrada dentro de `cache-max-stale` → se devuelve esa;
  si no → `ServicioExternoNoDisponibleException`. Verificar con `mvnw.cmd -q
  test-compile`.
- [x] 2.4 Exponer un método para vaciar la caché, usable desde los tests, y
  documentar que existe por eso. Verificar con `mvnw.cmd -q test-compile`.

## 3. CORS

- [x] 3.1 Crear `config/CorsConfig.java` que implemente `WebMvcConfigurer`, lea
  `cors.allowed-origins` (lista vacía por defecto) y solo registre el mapeo cuando
  hay al menos un origen, con métodos `GET/POST/PUT/DELETE/OPTIONS`, cabeceras
  `Authorization` y `Content-Type`, y sin comodín. Verificar con `mvnw.cmd -q
  test-compile`.
- [x] 3.2 En `application.properties`, agregar `cors.allowed-origins=` vacía con el
  comentario de que en desarrollo no hace falta.

## 4. Imágenes de contenedor

- [x] 4.1 Crear `backend/Dockerfile` multi-etapa: etapa de compilación con Maven y
  JDK 21 que cachee las dependencias antes de copiar el código, y etapa final con
  JRE 21 que solo copie el `.jar` y exponga el puerto 8080.
- [x] 4.2 Crear `backend/.dockerignore` (`target/`, `.git`, `*.md`).
- [x] 4.3 Crear `frontend/nginx.conf` con `try_files $uri $uri/ /index.html;` para
  que las rutas de react-router no devuelvan 404, y sin `proxy_pass`.
- [x] 4.4 Crear `frontend/Dockerfile` multi-etapa: build con Node y `npm ci && npm
  run build`, y etapa final con nginx sirviendo `dist/` con ese `nginx.conf`.
- [x] 4.5 Crear `frontend/.dockerignore` (`node_modules/`, `dist/`, `.git`).

## 5. Tests

- [x] 5.1 En `cambiomoneda/CriptoYaClientTest.java`, agregar los casos de la caché:
  `dosConversionesSeguidas_consultanUnaSolaVez()`,
  `monedasDistintas_consultanCadaPar()`,
  `falloConCotizacionCacheada_usaLaCacheada()`,
  `falloSinCotizacionCacheada_lanza503()` y
  `falloConCotizacionDemasiadoVieja_lanza503()`. Verificar con `mvnw.cmd -q test`.
- [x] 5.2 Correr la suite completa con `mvnw.cmd -q test`.

## 6. Documentación

- [x] 6.1 En `CLAUDE.md`, actualizar la sección de CORS para que refleje que
  `CorsConfig` ya existe y se activa con `cors.allowed-origins`, agregar la caché de
  CriptoYa a la sección de cambio de moneda, y aclarar que los tests corren con H2 y
  no necesitan Docker.
- [x] 6.2 En `backend/README.md`, documentar que `mvnw test` no requiere PostgreSQL,
  y cómo construir y correr la imagen.

## 7. Verificación final

- [x] 7.1 `cd backend && mvnw.cmd test` sin PostgreSQL levantado: sin fallos.
- [x] 7.2 `cd frontend && npx tsc --noEmit && npx oxlint src && npm run build` sin
  errores.
- [x] 7.3 `openspec validate 2026-09-08-add-infraestructura --strict` pasa.
