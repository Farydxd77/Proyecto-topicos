# CLAUDE.md — Cuentas Claras

## Contexto del proyecto
App web para dividir gastos entre participantes de un viaje o evento.
Monorepo: backend (Spring Boot + PostgreSQL) y frontend (React + Vite).
Fase actual: backend completo (auth, perfil, gestión general, grupos, gastos con
conversión de moneda, balances y liquidación, y registro de pagos entre
participantes) y frontend conectado a él.
El frontend habla SIEMPRE con el backend real: no hay API simulada. Las peticiones
salen a `/api` relativo y el proxy de Vite las reenvía a `localhost:8080`.
Para trabajar hacen falta los tres procesos: PostgreSQL (`docker compose up -d`),
backend (`mvnw.cmd spring-boot:run`) y frontend (`npm run dev`).

## Stack backend
- Spring Boot 4.1.1, Java 21, Maven
- PostgreSQL 17, Spring Data JPA, Hibernate (ddl-auto=update)
- Spring Security + JWT (jjwt 0.12.6)
- Lombok, Validation, Actuator
- Paquete base: com.cuentasclaras.backend

## Stack frontend
- React 19, Vite 8, TypeScript 6
- react-router (modo declarativo, no framework)
- @tanstack/react-query (estado de servidor: posee todo lo que viene del backend)
- Tailwind 4 (vía @tailwindcss/vite, sin archivo de configuración)
- oxlint

## Estructura de carpetas del frontend
frontend/src/
├── api/ # client.ts (fetch + token + ApiError), auth.ts, perfil.ts,
│ # grupos.ts, participantes.ts, gastos.ts, balances.ts, types.ts
├── auth/ # AuthContext.tsx, useAuth.ts, RutaProtegida.tsx, token.ts
├── components/ # Layout, Navegacion, Campo, Boton, MensajeError,
│ # GestionMiembros, FormularioGasto, SeccionGastos, SeccionBalances
├── pages/ # LoginPage, RegistroPage, PerfilPage, GruposPage,
│ # GrupoDetallePage, GastoDetallePage, NoEncontradaPage
├── lib/ # validacion.ts, claves.ts, formato.ts, monedas.ts, estadoConsulta.ts
└── router.tsx

### Convenciones del frontend
- El token de sesión vive en `AuthContext`; todo lo demás lo posee TanStack Query
- Nunca duplicar en el Context datos que ya vienen del backend (el username se lee
  del perfil cacheado, no del Context)
- Toda llamada al backend pasa por `apiFetch`; ninguna pantalla usa `fetch` directo
- Rutas siempre relativas (`/api/...`), nunca URLs absolutas
- `src/api/types.ts` es la única definición de los contratos y espeja los `record`
  de `com.cuentasclaras.backend.dto`
- Toda pantalla que consulte pasa por `estadoDe(consulta)` de `lib/estadoConsulta.ts`
  para no quedarse nunca en carga permanente
- Las claves de TanStack Query viven en `lib/claves.ts`, porque las mutaciones de una
  capacidad invalidan consultas de otra (un gasto cambia los balances)
- Los montos se formatean, nunca se operan: el backend ya calculó conversión y reparto
- Textos de interfaz en español, literales (sin i18n)

## Estructura de paquetes
backend/src/main/java/com/cuentasclaras/backend/
├── config/ # SecurityConfig, CorsConfig
├── controller/ # REST controllers
├── service/ # Lógica de negocio
├── repository/ # Spring Data JPA repositories
├── entity/ # Entidades JPA
├── dto/
│ ├── request/ # DTOs de entrada
│ └── response/ # DTOs de salida
├── exception/ # Excepciones custom + GlobalExceptionHandler
├── security/ # JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
└── util/ # BalanceUtil

## Modelo de datos
PKs: BIGINT autoincremental (@GeneratedValue(strategy = GenerationType.IDENTITY))
Todas las tablas tienen created_at y updated_at (TIMESTAMP NOT NULL)

### usuarios
- id BIGSERIAL PK
- username VARCHAR(50) NOT NULL UNIQUE
- password VARCHAR(255) NOT NULL (BCrypt)
- created_at, updated_at

### participantes
- id BIGSERIAL PK
- usuario_id BIGINT NOT NULL UNIQUE FK→usuarios.id
- nombre VARCHAR(100) NOT NULL
- apellido VARCHAR(100) NOT NULL
- ci VARCHAR(20) NOT NULL
- created_at, updated_at

### grupos
- id BIGSERIAL PK
- nombre VARCHAR(100) NOT NULL
- descripcion TEXT nullable
- creador_id BIGINT FK→participantes.id
- created_at, updated_at

### grupo_participantes
- grupo_id BIGINT FK→grupos.id
- participante_id BIGINT FK→participantes.id
- joined_at TIMESTAMP NOT NULL
- PK compuesta: (grupo_id, participante_id)

### gastos
- id BIGSERIAL PK
- grupo_id BIGINT FK→grupos.id
- descripcion VARCHAR(255) NOT NULL
- monto DECIMAL(10,2) NOT NULL CHECK > 0 — monto original en la moneda del gasto, NO es USDT
- moneda VARCHAR(10) NOT NULL DEFAULT 'USDT' — símbolo (BOB, USD, BTC, etc.)
- moneda_nombre VARCHAR(50) NOT NULL DEFAULT 'Tether' — nombre completo (Boliviano, etc.)
- monto_usdt DECIMAL(10,6) NOT NULL DEFAULT 0 — monto convertido a USDT
- tasa_cambio DECIMAL(10,6) NOT NULL DEFAULT 1 — tasa usada al registrar/editar
- pagador_id BIGINT FK→participantes.id
- fecha DATE NOT NULL
- created_at, updated_at

### gasto_participantes
- gasto_id BIGINT FK→gastos.id
- participante_id BIGINT FK→participantes.id
- monto_adeudado DECIMAL(10,2) NOT NULL
- peso INTEGER NOT NULL DEFAULT 1 — partes que le tocaron en el reparto
- PK compuesta: (gasto_id, participante_id)

### pagos
- id BIGSERIAL PK
- grupo_id BIGINT NOT NULL FK→grupos.id
- pagador_id BIGINT NOT NULL FK→participantes.id
- receptor_id BIGINT NOT NULL FK→participantes.id
- monto DECIMAL(10,2) NOT NULL — siempre en USDT
- fecha DATE NOT NULL
- tx_id VARCHAR(100) nullable — hash de transacción blockchain, opcional
- created_at, updated_at

### bajas_grupo
- id BIGSERIAL PK
- grupo_id BIGINT NOT NULL FK→grupos.id
- participante_id BIGINT NOT NULL FK→participantes.id — el que se fue
- saldo DECIMAL(10,2) NOT NULL — su balance al salir, CONGELADO. Negativo = debía
- estado VARCHAR(20) NOT NULL — PENDIENTE | ASUMIDA | NO_ASUMIDA
- fecha DATE NOT NULL
- created_at, updated_at

### baja_participantes
- baja_id BIGINT FK→bajas_grupo.id
- participante_id BIGINT FK→participantes.id
- monto DECIMAL(10,2) NOT NULL — el delta CON SIGNO que la baja le aplica al balance
- PK compuesta: (baja_id, participante_id)

## Convenciones
- Tablas: snake_case plural (usuarios, participantes)
- Columnas: snake_case (nombre_completo, created_at)
- Clases Java: PascalCase (UsuarioEntity, ParticipanteService)
- Endpoints REST: kebab-case plural (/api/grupos-viaje)
- Un usuario siempre tiene exactamente un participante (1 a 1)
- Del reparto de un gasto absorbe los centavos sobrantes del redondeo el pagador si
  participa del gasto; si no participa, el de mayor peso, y a igualdad de peso el
  de menor id

## Seguridad
- Todos los endpoints excepto /api/auth/** requieren JWT válido
- JWT expira en 24 horas (86400000 ms)
- Contraseñas hasheadas con BCrypt — nunca en texto plano
- Un usuario solo accede a recursos de sus propios grupos
- El token va en el header: Authorization: Bearer {token}

## Respuestas HTTP estándar
| Código | Cuándo usarlo |
|--------|--------------|
| 200 | Consulta exitosa |
| 201 | Recurso creado (incluir recurso en body) |
| 204 | Eliminación exitosa (sin body) |
| 400 | Validación fallida |
| 401 | Sin token o token inválido |
| 403 | Token válido pero sin permisos |
| 404 | Recurso no existe |
| 409 | Conflicto (username duplicado, miembro ya existe) |

## Formato de error estándar
Siempre este JSON para cualquier error:
```json
{
  "timestamp": "2025-09-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "El username ya está en uso",
  "path": "/api/auth/register"
}
```

## Lombok en entidades
- Usar @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
- NUNCA @Data en entidades JPA con relaciones (causa StackOverflow)
- Para herencia usar @SuperBuilder en lugar de @Builder

## Lombok en clases @Embeddable
- Usar @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
- Implementar Serializable obligatoriamente
- NO usar @SuperBuilder (no tienen herencia)
- NO usar @Data (aunque @EqualsAndHashCode está permitido aquí porque no tienen relaciones JPA)

## Lombok en entidades con @EmbeddedId
- Usar @Getter @Setter @NoArgsConstructor @AllArgsConstructor
- NO usar @SuperBuilder (no heredan de BaseEntity)
- NO usar @Builder normal tampoco (no tienen herencia pero tampoco la necesitan por ahora)
- NO usar @Data (tienen relaciones JPA)

## Repositorios
- Usar Query Methods de JPA para todas las consultas (sin @Query manual)
- Retornar Optional<T> para consultas de un solo resultado
- Retornar List<T> para consultas de múltiples resultados
- Nunca retornar null — usar Optional<T> o List<T> vacío
- Nombrar los métodos siguiendo la convención de Spring Data JPA:
  findBy{Campo}(valor) → Optional<T>
  findAllBy{Campo}(valor) → List<T>
  existsBy{Campo}(valor) → boolean
  deleteBy{Campo}(valor)

## Idioma
Todos los artefactos de OpenSpec (proposal.md, spec.md, design.md, tasks.md) 
deben escribirse en español.

## CORS (no hace falta en desarrollo)
- El proxy de Vite reenvía `/api` a `localhost:8080` del lado del servidor, así que
  el navegador solo ve un origen (`localhost:5173`): no hay petición cruzada
- `config/CorsConfig` ya existe y se gobierna con la propiedad
  `cors.allowed-origins`. Vacía (el caso de desarrollo) → no registra nada
- En producción, con uno o más orígenes: métodos GET/POST/PUT/DELETE/OPTIONS y
  headers Authorization y Content-Type. Nunca comodín: estas peticiones llevan
  Authorization

## Tests
- `mvnw.cmd test` NO necesita Docker ni PostgreSQL: la fase de test corre sobre H2
  en memoria en modo de compatibilidad PostgreSQL
- La configuración vive en `src/test/resources/application.properties`, que se llama
  igual que el de `main` a propósito: el classpath de test tiene precedencia, así
  que ninguna clase de test necesita `@ActiveProfiles` ni anotación alguna
- `infraestructura/ConfiguracionDeTestTest` falla si alguien reintroduce PostgreSQL
  en los tests, para que nunca escriban en la base de desarrollo

## Cambio de moneda
- Cada gasto tiene su propia moneda (BOB, USD, BTC, etc.)
- monto en gastos = monto original en la moneda del gasto (NO es USDT)
- moneda = símbolo de la moneda (BOB, USD, BTC, etc.)
- moneda_nombre = nombre completo (Boliviano, Dólar estadounidense, Bitcoin, etc.)
- monto_usdt = monto convertido a USDT usando CriptoYa (Binance P2P)
- tasa_cambio = tasa usada al momento de registrar o editar el gasto
- monto_adeudado en gasto_participantes siempre en USDT
- API externa: https://criptoya.com/api/binancep2p/{coin}/{fiat}/1
- Fiat soportado: ARS, BRL, CLP, COP, MXN, PEN, VES, BOB, UYU, DOP, PYG, USD, EUR
- Cripto soportado: USDT, BTC, ETH, USDC, DAI, BNB, SOL, XRP, ADA, AVAX, DOGE y más
- USDT → tasa = 1.0, sin consulta externa
- `CriptoYaClient` cachea el `bid` de cada par en memoria, con dos ventanas:
  `criptoya.cache-ttl` (60s) → no se vuelve a consultar; `criptoya.cache-max-stale`
  (24h) → si CriptoYa falla, se usa la cotización cacheada en vez de responder 503
- Sin ninguna cotización previa del par, la caída de CriptoYa sigue siendo 503:
  nunca se inventa una tasa

## División de un gasto
- El alta y la edición de un gasto aceptan un campo OPCIONAL `division`: una lista
  de `{ participanteId, peso }`
- Sin `division` (campo ausente o null) → reparto equitativo entre todos los
  miembros actuales del grupo, todos con peso 1. Es el comportamiento por defecto
- Con `division` → solo los participantes listados, en proporción a su peso.
  Excluir a alguien es no listarlo; repartir desigual es darle un peso distinto
- `peso` es un entero de 1 a 1000. Lista vacía → 400
- Los participantes de la división deben ser miembros actuales del grupo, sin
  repetirse → 400
- El pagador debe ser miembro del grupo, pero NO tiene que participar del gasto:
  puede haber pagado algo que no consume
- Editar un gasto SIN `division` no conserva la división anterior: vuelve al
  reparto equitativo

## Pagos
- Un pago registra que un participante ya le transfirió dinero a otro para saldar
  su deuda. Cierra el ciclo que abre la liquidación
- El pagador es SIEMPRE el participante resuelto del token JWT: no viaja en el
  request y nadie puede registrar un pago a nombre de otro
- El receptor debe ser otro miembro del grupo, distinto del pagador
- Solo quien registró el pago puede editarlo o borrarlo (403 para los demás)
- El monto va siempre en USDT, sin conversión: no se llama a CriptoYa
- `pagos.monto` es DECIMAL(10,2) → máximo `99999999.99`; la validación usa
  `@Digits(integer = 8, fraction = 2)` para que un monto fuera de rango sea 400 y
  no 500
- `tx_id` es opcional y solo referencia: nunca se verifica contra ninguna red
- Los pagos entran en el balance: `balance = pagadoEnGastos − adeudado +
  pagosRealizados − pagosRecibidos`

## Bajas con deuda
- Si alguien deja el grupo con balance distinto de cero, se registra una baja
  PENDIENTE con ese saldo congelado. Si sale a mano, no se registra nada
- El saldo se congela porque el de un ex-miembro todavía puede moverse (un pago
  hacia él, la edición de un gasto viejo)
- La decisión la toma EL CREADOR, después de la baja: cuando alguien abandona por su
  cuenta no hay nadie más en pantalla para decidir en ese momento
- ASUMIDA → el saldo se reparte en partes iguales entre los miembros actuales, y ese
  reparto queda congelado. El que se fue pasa a 0.00
- NO_ASUMIDA → no cambia ningún balance. Solo cambia dónde se muestra: pasa a un
  apartado propio de deudas sin resolver
- `baja_participantes.monto` es el delta CON SIGNO listo para sumar. Si el que se fue
  debía, es negativo (los demás bajan); si le debían, positivo (los demás suben)
- Solo las bajas ASUMIDA entran en el cálculo de balances. La suma sigue dando 0.00

## Totales del grupo
- `GET /api/grupos/{id}/resumen`: totalGastado, totalPagado, pendientePorSaldar,
  miParte, cantidadGastos, cantidadPagos. Todo en USDT con 2 decimales
- OJO: `totalPagado` NO converge a `totalGastado` y no debe hacerlo. Quien paga un
  gasto cubre su propia parte y nunca se transfiere dinero a sí mismo. Lo que cierra
  es `totalPagado + pendientePorSaldar = deuda total del grupo`
- La barra de avance llega al 100% cuando `pendientePorSaldar` es 0, NO cuando
  `totalPagado` iguala a `totalGastado`

## Fuera de alcance
- Email, notificaciones, invitaciones a grupos
- Verificar un pago contra una blockchain o una pasarela real: el `tx_id` se guarda
  como referencia y nunca se comprueba. Registrar el pago sí está en alcance (ver
  la sección Pagos)
- Roles de administrador global
- Repartir un gasto por montos exactos o por porcentajes: la división se expresa
  siempre en partes enteras (ver la sección División de un gasto)
- Votación de los miembros para decidir sobre una baja: decide el creador
- Deshacer una baja ya resuelta: es un registro histórico