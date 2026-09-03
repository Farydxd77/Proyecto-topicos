## Why

El backend ya registra grupos y sus gastos con la división por participante, pero
no hay forma de responder la pregunta central del producto: *"¿quién le debe a
quién y cuánto?"*. Falta agregar los gastos en un balance por participante y
derivar la lista mínima de transferencias para dejar el grupo a mano. Existe un
borrador de requisitos escrito a mano en `openspec/specs/balances/spec.md`; esta
tarea lo formaliza e implementa. Es la última pieza del núcleo backend antes de
conectar el frontend real.

## What Changes

- Nuevo endpoint `GET /api/grupos/{id}/balances`: para un miembro del grupo,
  devuelve `200` con el balance de cada participante con actividad en el grupo.
  Balance positivo = le deben; negativo = debe. La suma de todos los balances es
  **exactamente 0**. Grupo sin gastos → todos los balances en `0`.
- Nuevo endpoint `GET /api/grupos/{id}/liquidacion`: para un miembro del grupo,
  devuelve `200` con la lista mínima de transferencias
  (`{ "de", "deId", "para", "paraId", "monto" }`) que lleva todos los balances a
  `0`. Sin deudas pendientes → `[]`.
- Nuevo `BalanceService` que orquesta el cálculo leyendo `GastoRepository`,
  `GastoParticipanteRepository`, `GrupoParticipanteRepository` y
  `ParticipanteRepository` (más `GrupoRepository` y `UsuarioRepository` para el
  `404` de grupo inexistente y para resolver al solicitante del token).
- Nuevo `util/BalanceUtil` con la lógica pura de cálculo: agregación de balances y
  algoritmo greedy de minimización de transferencias (mayor deudor paga al mayor
  acreedor). Sin dependencias de Spring ni de JPA.
- Nuevo `BalanceController` bajo `/api/grupos/{id}` con los dos `GET`.
- Nuevos DTOs de salida (`BalanceDto`, `TransferenciaDto`). Se reutiliza
  `dto/response/ParticipanteDto` para identificar a cada participante en el
  balance.
- Se reutilizan las excepciones `ForbiddenOperationException` (→ `403`) y
  `ResourceNotFoundException` (→ `404`), ya mapeadas al formato de error estándar
  en `GlobalExceptionHandler`. No se crean excepciones nuevas.
- Todos los endpoints requieren JWT válido; sin cambios en `SecurityConfig`
  (`anyRequest().authenticated()` ya cubre `/api/grupos/**`).

## Non-Goals

- No se registran pagos reales entre participantes ni se marca ninguna
  transferencia como "hecha".
- No se marca el grupo como saldado ni se archiva automáticamente.
- No se implementan múltiples monedas.
- La liquidación usa el algoritmo greedy del borrador (mayor deudor → mayor
  acreedor); no se busca el óptimo global de número de transferencias (es
  NP-difícil y el borrador no lo pide).
- Endpoints de solo lectura: no se modifica ningún gasto, división ni membresía.
- No se modifica ninguna entidad JPA ni el esquema de base de datos.
- No se toca `frontend/` (balances y liquidación siguen fuera de alcance según
  CLAUDE.md; el frontend continúa contra MSW).
- No se añaden anotaciones `@PreAuthorize` ni se toca `SecurityConfig`.

## Capabilities

### New Capabilities

- `balances`: cálculo del estado de cuentas de un grupo por parte de sus
  miembros — el balance neto de cada participante con actividad (lo que pagó menos
  lo que le corresponde adeudar, con la suma siempre en `0`) y la lista mínima de
  transferencias que salda el grupo. Solo lectura; no registra pagos.

### Modified Capabilities

<!-- Ninguna: no cambian los requisitos de capacidades existentes. -->

## Impact

- **Código nuevo**:
  - `util/BalanceUtil`, `util/package-info.java`.
  - `service/BalanceService`.
  - `controller/BalanceController`.
  - `dto/response/BalanceDto`, `dto/response/TransferenciaDto`.
  - `test/.../balances/BalanceControllerTest`, `test/.../balances/BalanceUtilTest`.
- **Código modificado**: ninguno. Se reutilizan `GastoRepository`,
  `GastoParticipanteRepository`, `GrupoRepository`, `GrupoParticipanteRepository`,
  `ParticipanteRepository`, `UsuarioRepository`, las excepciones de negocio,
  `GlobalExceptionHandler` y `dto/response/ParticipanteDto` tal cual.
- **APIs**: dos rutas nuevas de solo lectura bajo `/api/grupos/{id}/balances` y
  `/api/grupos/{id}/liquidacion`, protegidas por JWT sin tocar `SecurityConfig`.
- **Base de datos**: sin cambios de esquema; solo consultas de lectura sobre
  `gastos` y `gasto_participantes`.
- **Dependencias**: ninguna nueva.
- **Specs**: al archivar el cambio, el delta reemplaza el borrador manual de
  `openspec/specs/balances/spec.md` por un spec en formato canónico.
