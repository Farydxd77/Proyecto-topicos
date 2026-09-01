# CLAUDE.md — Cuentas Claras

## Contexto del proyecto
App web para dividir gastos entre participantes de un viaje o evento.
Monorepo: backend (Spring Boot + PostgreSQL) y frontend (React + Vite).
Fase actual: backend únicamente. No tocar la carpeta `frontend/`.

## Stack backend
- Spring Boot 4.1.1, Java 21, Maven
- PostgreSQL 17, Spring Data JPA, Hibernate (ddl-auto=update)
- Spring Security + JWT (jjwt 0.12.6)
- Lombok, Validation, Actuator
- Paquete base: com.cuentasclaras.backend

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
- monto DECIMAL(10,2) NOT NULL CHECK > 0
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

## Idioma
Todos los artefactos de OpenSpec (proposal.md, spec.md, design.md, tasks.md) 
deben escribirse en español.

## Fuera de alcance
- Frontend (fase 2)
- Email, notificaciones
- Múltiples monedas
- Pagos reales
- Roles de administrador global