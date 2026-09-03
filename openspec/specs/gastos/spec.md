# Gastos — Registro y gestión de gastos de un grupo

## Objetivo
Permitir que los miembros de un grupo registren, editen, eliminen y consulten
gastos. El monto siempre se divide equitativamente entre todos los miembros
del grupo al momento de registrar el gasto.

## Comportamiento esperado

### Dado que un miembro envía POST /api/grupos/{id}/gastos con datos válidos
Cuando el monto es mayor a 0 y el pagador es miembro del grupo,
Entonces se registra el gasto y se divide el monto equitativamente
entre todos los miembros del grupo en ese momento,
el pagador absorbe los centavos sobrantes del redondeo,
y devuelve 201 Created con el gasto y la división calculada.

### Dado que un miembro envía GET /api/grupos/{id}/gastos
Cuando el grupo existe y el usuario es miembro,
Entonces devuelve 200 OK con la lista de gastos del grupo
ordenados por fecha descendente.
Si no hay gastos devuelve 200 con [].

### Dado que un miembro envía GET /api/grupos/{id}/gastos/{gastoId}
Cuando el gasto existe y pertenece al grupo,
Entonces devuelve 200 OK con los datos del gasto y su división.

### Dado que un miembro envía PUT /api/grupos/{id}/gastos/{gastoId} con datos válidos
Cuando el gasto existe y pertenece al grupo,
Entonces actualiza descripcion, monto, pagador y/o fecha,
recalcula la división entre todos los miembros actuales del grupo,
y devuelve 200 OK con el gasto actualizado.

### Dado que un miembro envía DELETE /api/grupos/{id}/gastos/{gastoId}
Cuando el gasto existe y pertenece al grupo,
Entonces elimina el gasto y sus registros en gasto_participantes
y devuelve 204 No Content.

## Casos límite
- Monto <= 0 → 400 Bad Request
- El pagador no es miembro del grupo → 400 Bad Request
- Usuario no miembro del grupo → 403 Forbidden
- Gasto no existe → 404 Not Found
- Grupo no existe → 404 Not Found
- Al dividir con decimales periódicos el pagador absorbe el centavo sobrante
- La suma de monto_adeudado siempre debe ser igual al monto total del gasto
- Si se agrega un nuevo miembro al grupo después de registrar un gasto,
  ese miembro NO afecta la división de gastos anteriores

## Repositorios utilizados

### GastoRepository
- findByGrupoIdOrderByFechaDesc(Long grupoId)
- findByIdAndGrupoId(Long id, Long grupoId)

### GastoParticipanteRepository
- findByGastoId(Long gastoId)
- deleteByGastoId(Long gastoId)

### GrupoParticipanteRepository
- findByGrupoId(Long grupoId)
- findByGrupoIdAndParticipanteId(Long grupoId, Long participanteId)

### ParticipanteRepository
- findByUsuarioId(Long usuarioId)

## Servicios
- GastoService usa GastoRepository, GastoParticipanteRepository,
  GrupoParticipanteRepository y ParticipanteRepository
- La división del monto se calcula en GastoService:
  monto_por_persona = monto / cantidad_miembros (redondeado a 2 decimales)
  el pagador absorbe la diferencia: su monto_adeudado = monto - (monto_por_persona * (n-1))
- El participante del usuario autenticado se resuelve desde el token JWT via findByUsuarioId
- Verificar que el usuario es miembro del grupo antes de cualquier operación

## Criterios de aceptación
- [ ] POST /api/grupos/{id}/gastos registra el gasto y divide entre todos los miembros
- [ ] El pagador absorbe los centavos sobrantes del redondeo
- [ ] La suma de monto_adeudado == monto total del gasto siempre
- [ ] GET /api/grupos/{id}/gastos devuelve lista ordenada por fecha descendente
- [ ] GET /api/grupos/{id}/gastos/{gastoId} devuelve el gasto con su división
- [ ] PUT /api/grupos/{id}/gastos/{gastoId} actualiza y recalcula la división
- [ ] DELETE /api/grupos/{id}/gastos/{gastoId} elimina el gasto y su división
- [ ] Monto <= 0 devuelve 400
- [ ] Pagador no miembro del grupo devuelve 400
- [ ] Usuario no miembro devuelve 403
- [ ] Gasto no existe devuelve 404
- [ ] La app compila sin errores
- [ ] mvnw test pasa

## Fuera de alcance
- No se implementan balances ni liquidación (siguiente tarea)
- No se implementan pagos entre participantes
- No hay límite de gastos por grupo
- La división siempre es equitativa — no se puede personalizar por participante