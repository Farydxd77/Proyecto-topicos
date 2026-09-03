# Cuentas Claras

App web para dividir gastos entre participantes de un viaje o evento.

## Estructura del proyecto
cuentas-claras/
├── backend/ → Spring Boot 4.1.1 + Java 21 + PostgreSQL
├── frontend/ → React + Vite + TypeScript (fase 2)
├── openspec/ → Specs y cambios OpenSpec
└── CLAUDE.md → Reglas y contexto para la IA

## Requisitos previos

- Java 21
- Maven
- PostgreSQL 17
- Node.js 22+
- OpenSpec CLI: `npm install -g @fission-ai/openspec@latest`
- Claude Code: `npm install -g @anthropic-ai/claude-code`

## Configuración inicial

### 1. Crear la base de datos
```sql
CREATE DATABASE cuentas_claras;
```

### 2. Configurar variables de entorno
El backend usa estas variables con valores por defecto para desarrollo local:
DB_URL=jdbc:postgresql://localhost:5432/cuentas_claras
DB_USER=postgres
DB_PASS=postgres
JWT_SECRET=clave-secreta-local-solo-para-desarrollo-no-usar-en-prod

### 3. Correr el backend
```bash
cd backend
./mvnw spring-boot:run
```

### 4. Verificar que funciona
GET http://localhost:8080/actuator/health → { "status": "UP" }

## Estado actual del backend

| Tarea | Estado |
|-------|--------|
| Proyecto Spring Boot base | ✅ Completo |
| Entidades JPA | ✅ Completo |
| Seguridad JWT | ✅ Completo |
| Auth (registro y login) | ✅ Completo |
| Perfil | ✅ Completo |
| Gestión general (usuarios y participantes) | ✅ Completo |
| Grupos | ⬜ Pendiente |
| Gastos | ⬜ Pendiente |
| Balances y liquidación | ⬜ Pendiente |
| CORS | ⬜ Pendiente |

## Endpoints disponibles

### Públicos (sin token)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | /api/auth/register | Registro de usuario |
| POST | /api/auth/login | Login |

### Protegidos (requieren JWT)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | /api/perfil/me | Ver mi perfil |
| PUT | /api/perfil/me | Editar nombre y apellido |
| PUT | /api/perfil/me/username | Cambiar username |
| PUT | /api/perfil/me/password | Cambiar password |
| GET | /api/usuarios | Listar usuarios |
| GET | /api/usuarios/{id} | Ver usuario por id |
| GET | /api/usuarios?username= | Buscar usuarios |
| GET | /api/usuarios/{id}/participante | Ver participante de un usuario |
| GET | /api/participantes | Listar participantes |
| GET | /api/participantes/{id} | Ver participante por id |
| GET | /api/participantes?nombre= | Buscar por nombre |
| GET | /api/participantes?apellido= | Buscar por apellido |
| GET | /api/participantes?ci= | Buscar por CI |

## Cómo continuar el desarrollo con OpenSpec

### Flujo de trabajo
1. Leer el spec en `openspec/specs/{tarea}/spec.md`
2. Abrir Claude Code en la raíz del proyecto: `claude`
3. Proponer la tarea
4. Revisar los artefactos generados
5. Aplicar: `/opsx:apply`
6. Verificar que los tests pasan
7. Archivar: `/opsx:archive`

### Tareas pendientes en orden

**1. Grupos**
/opsx:propose "implementar gestión de grupos: crear grupo, listar mis grupos, ver detalle, editar, eliminar, agregar y quitar miembros siguiendo openspec/specs/grupos/spec.md"

**2. Gastos** (solo después de archivar grupos)
/opsx:propose "implementar gestión de gastos: registrar, editar, eliminar y listar gastos de un grupo siguiendo openspec/specs/gastos/spec.md"

**3. Balances y liquidación** (solo después de archivar gastos)
/opsx:propose "implementar balances y liquidación: calcular balance de cada participante y lista mínima de transferencias siguiendo openspec/specs/balances/spec.md"

**4. CORS** (después de balances)
/opsx:propose "configurar CORS para permitir conexión desde el frontend React en localhost:5173"

## Convenciones importantes

- PKs: BIGINT autoincremental
- Contraseñas: BCrypt, nunca en texto plano
- Respuestas de error: siempre formato estándar con timestamp, status, error, message, path
- Repositorios: Query Methods JPA, sin @Query manual
- Lombok: @SuperBuilder en entidades con herencia, nunca @Data
- Ver CLAUDE.md para el modelo de datos completo y todas las convenciones
## Funcionalidades pendientes para después

| Funcionalidad | Descripción |
|---------------|-------------|
| Eliminación lógica | Agregar deleted_at a todas las entidades en lugar de borrado físico |
| Pagos entre participantes | Registrar cuando un participante le paga a otro para saldar deuda |
| Cambio de moneda | Cada gasto puede tener su propia moneda (BOB, USD, CLP, USDT, etc.) con conversión via API externa |
| Swagger/OpenAPI | Documentación interactiva de la API en /swagger-ui |
| Roles y permisos | Roles admin para gestión avanzada de usuarios y grupos |
| Marcar grupo como saldado | Cuando todos los balances lleguen a 0 |