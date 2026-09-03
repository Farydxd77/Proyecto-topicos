# CLAUDE.md — Cuentas Claras

## Contexto del proyecto
App web para dividir gastos entre participantes de un viaje o evento.
Monorepo: backend (Spring Boot + PostgreSQL) y frontend (React + Vite).
Fase actual: backend completo (auth, perfil, gestión general, grupos, gastos con
conversión de moneda, balances y liquidación) y frontend conectado a él.
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
- PK compuesta: (gasto_id, participante_id)

## Convenciones
- Tablas: snake_case plural (usuarios, participantes)
- Columnas: snake_case (nombre_completo, created_at)
- Clases Java: PascalCase (UsuarioEntity, ParticipanteService)
- Endpoints REST: kebab-case plural (/api/grupos-viaje)
- Un usuario siempre tiene exactamente un participante (1 a 1)
- El pagador de un gasto absorbe los centavos sobrantes del redondeo

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
- Solo será necesario si en producción el frontend y el backend se sirven desde
  dominios distintos. En ese caso: `CorsConfig` en `config/`, con el origen de
  producción, métodos GET/POST/PUT/DELETE/OPTIONS y headers Authorization y
  Content-Type

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

## Fuera de alcance
- Email, notificaciones, invitaciones a grupos
- Pagos reales y registro de que una transferencia de la liquidación ya se pagó
- Roles de administrador global
- Excluir participantes de un gasto o repartir en proporciones desiguales: el
  backend reparte siempre entre todos los miembros del grupo
- Abandonar un grupo por cuenta propia y transferir el rol de creador