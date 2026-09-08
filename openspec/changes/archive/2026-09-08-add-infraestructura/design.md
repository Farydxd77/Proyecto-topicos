## Decisión 1: H2 vía `src/test/resources/application.properties`, sin anotar los tests

Spring Boot resuelve `application.properties` por precedencia de classpath, y el
classpath de test va **antes** que el de main. Poner el archivo ahí reemplaza por
completo la configuración de la aplicación durante los tests, sin tocar ninguna
clase.

**Alternativa descartada: un perfil `test` con `@ActiveProfiles("test")`.** Obliga a
anotar las catorce clases de test que existen hoy y a acordarse de anotar cada una
que se agregue. Un test nuevo sin la anotación se conectaría a la base de
desarrollo y la ensuciaría en silencio, que es exactamente el fallo que no se quiere
poder cometer.

**Alternativa descartada: Testcontainers.** Da fidelidad total con PostgreSQL, pero
exige Docker levantado, que es la dependencia que se está eliminando.

El archivo de test declara `MODE=PostgreSQL` en la URL de H2 y
`DB_CLOSE_DELAY=-1` para que la base sobreviva entre contextos de Spring dentro de
la misma JVM. El dialecto pasa a `H2Dialect`: dejar `PostgreSQLDialect` sobre H2
genera SQL que H2 no entiende.

**Riesgo asumido:** H2 no es PostgreSQL. Un test podría pasar en H2 y fallar contra
la base real. Se acota manteniendo el modo de compatibilidad y `ddl-auto=create-drop`
sobre el mismo mapeo JPA que produce el esquema real, y verificando que la suite
completa dé el mismo resultado en las dos bases antes de dar el cambio por bueno.

## Decisión 2: la caché vive dentro de `CriptoYaClient`

Un `ConcurrentHashMap<String, Cotizacion>` donde la clave es la ruta consultada
(`/USDT/BOB/1`) y el valor es el `bid` con el instante en que se obtuvo.

**Alternativa descartada: `spring-boot-starter-cache` con Caffeine y `@Cacheable`.**
Es lo idiomático para una caché con TTL, pero `@Cacheable` no sabe servir una entrada
vencida cuando el método falla, que es justamente la mitad valiosa de este cambio.
Habría que combinarlo con `@CacheEvict` y un manejo de excepciones que termina
siendo más código que el mapa, más una dependencia nueva.

**Alternativa descartada: cachear la `Conversion` ya calculada.** La conversión
depende del monto del gasto, así que la clave tendría que incluirlo y la caché no
acertaría casi nunca. Lo que se cachea es el `bid`, que es lo que no depende del
gasto.

## Decisión 3: dos ventanas de tiempo, no una

- `criptoya.cache-ttl` (60 s): mientras la entrada es más nueva que esto, se usa
  **sin** llamar a CriptoYa.
- `criptoya.cache-max-stale` (24 h): pasado el TTL se intenta la llamada real; si
  falla y la entrada es más nueva que esto, se usa igual.

Separarlas es lo que distingue «no hace falta preguntar» de «no se pudo preguntar y
esto es lo mejor que tengo». Con una sola ventana habría que elegir entre servir
datos viejos de rutina o no tener red de contención en una caída.

Los 60 segundos son cortos a propósito: la tasa que se persiste en el gasto es la que
se aplicó, y un usuario que registra un gasto espera que refleje el mercado del
momento, no el de hace media hora.

Las 24 horas de tolerancia son largas a propósito: cuando el servicio externo está
caído, una tasa de ayer es infinitamente mejor que no poder registrar el gasto. El
gasto queda con esa tasa y el usuario siempre puede editarlo después.

## Decisión 4: sin `cors.allowed-origins`, no se registra nada

`CorsConfig` expone un `WebMvcConfigurer` que solo agrega mapeos si la propiedad
trae al menos un origen. En desarrollo la propiedad está vacía y la configuración es
inerte, tal como describe `CLAUDE.md`: el proxy de Vite reenvía `/api` del lado del
servidor y el navegador solo ve un origen.

**Alternativa descartada: `allowedOrigins("*")` por defecto.** Con
`Authorization` en juego es un riesgo gratuito, y además Spring rechaza combinar el
comodín con credenciales.

**Alternativa descartada: `@CrossOrigin` en los controladores.** Dispersa la
política de seguridad en siete archivos y obliga a acordarse en cada controlador
nuevo.

## Decisión 5: las imágenes son multi-etapa

`backend/Dockerfile` compila con la imagen de Maven y copia solo el `.jar` a un JRE
21 sin herramientas de compilación. `frontend/Dockerfile` construye con Node y sirve
el `dist/` estático con nginx, que además necesita una regla para que las rutas de
`react-router` (modo declarativo, sin servidor) devuelvan `index.html` en lugar de
`404`.

El `nginx.conf` **no** incluye un `proxy_pass` hacia el backend: en producción esa
decisión depende de la topología del despliegue, y meterla en la imagen la ataría a
una suposición. Por eso el cambio incluye `CorsConfig`: es la alternativa que no
supone nada.

## Riesgos

- Servir una tasa vencida cambia una respuesta de error por una exitosa. Un cliente
  que hoy trata el `503` como «reintentar más tarde» pasará a recibir un gasto
  registrado. Es la intención, y queda registrado como MODIFIED en el delta de
  `cambio-moneda`.
- La caché es por instancia. Con varias instancias del backend, cada una mantiene la
  suya y pueden servir tasas distintas dentro de la misma ventana. Para el alcance
  actual del proyecto (una instancia) no es un problema.
