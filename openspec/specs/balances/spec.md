# Balances y Liquidación

## Objetivo
Calcular cuánto debe o le deben a cada participante del grupo,
y generar la lista mínima de transferencias para que todos queden a mano.

## Comportamiento esperado

### Dado que un miembro envía GET /api/grupos/{id}/balances
Cuando el grupo existe y el usuario es miembro,
Entonces devuelve 200 OK con el balance de cada participante del grupo.
Balance positivo significa que le deben dinero.
Balance negativo significa que debe dinero.
La suma de todos los balances siempre es exactamente 0.

### Dado que un miembro envía GET /api/grupos/{id}/balances
Cuando el grupo no tiene gastos registrados,
Entonces devuelve 200 OK con todos los balances en 0.

### Dado que un miembro envía GET /api/grupos/{id}/liquidacion
Cuando el grupo existe y el usuario es miembro,
Entonces devuelve 200 OK con la lista mínima de transferencias
para que todos los balances queden en 0.
Formato: { "de": "Diego", "deId": 1, "para": "Ana", "paraId": 2, "monto": 200.00 }

### Dado que un miembro envía GET /api/grupos/{id}/liquidacion
Cuando no hay deudas pendientes,
Entonces devuelve 200 OK con lista vacía [].

### Dado que un usuario no miembro envía GET /api/grupos/{id}/balances
Cuando el usuario no pertenece al grupo,
Entonces devuelve 403 Forbidden con el formato de error estándar.

### Dado que un usuario no miembro envía GET /api/grupos/{id}/liquidacion
Cuando el usuario no pertenece al grupo,
Entonces devuelve 403 Forbidden con el formato de error estándar.

## Casos límite
- Grupo sin gastos → todos los balances en 0, liquidación vacía []
- Suma de todos los balances siempre debe ser exactamente 0
- Si el monto no divide exactamente, el pagador absorbe el centavo sobrante
- Usuario no miembro → 403 Forbidden
- Grupo no existe → 404 Not Found

## Algoritmo de liquidación
Algoritmo greedy de mínimas transferencias:
1. Calcular balance neto de cada participante
   balance = suma de gastos que pagó - suma de monto_adeudado de todos sus gastos
2. Separar en acreedores (balance > 0) y deudores (balance < 0)
3. Ordenar ambas listas por valor absoluto descendente
4. El mayor deudor paga al mayor acreedor lo que pueda
5. Repetir hasta que todos los balances sean 0
6. Resultado: lista mínima de transferencias

## Repositorios utilizados

### GastoRepository
- findByGrupoId(Long grupoId)

### GastoParticipanteRepository
- findByGastoId(Long gastoId)

### GrupoParticipanteRepository
- findByGrupoId(Long grupoId)
- findByGrupoIdAndParticipanteId(Long grupoId, Long participanteId)

### ParticipanteRepository
- findByUsuarioId(Long usuarioId)

## Servicios
- BalanceService usa GastoRepository, GastoParticipanteRepository,
  GrupoParticipanteRepository y ParticipanteRepository
- El cálculo de balances y liquidación vive en BalanceService
- La lógica pura de cálculo vive en util/BalanceUtil
- El participante del usuario autenticado se resuelve desde el token JWT

## Criterios de aceptación
- [ ] GET /api/grupos/{id}/balances devuelve balance de cada participante
- [ ] La suma de todos los balances es exactamente 0 siempre
- [ ] Grupo sin gastos devuelve todos los balances en 0
- [ ] GET /api/grupos/{id}/liquidacion devuelve lista mínima de transferencias
- [ ] Sin deudas pendientes devuelve []
- [ ] Con el escenario Samaipata: Ana +600, Beto -200, Carla -200, Diego -200
- [ ] Liquidación Samaipata: 3 transferencias de Bs. 200 hacia Ana
- [ ] Usuario no miembro devuelve 403
- [ ] Grupo no existe devuelve 404
- [ ] La app compila sin errores
- [ ] mvnw test pasa

## Fuera de alcance
- No se registran pagos reales entre participantes
- No se marca el grupo como saldado automáticamente
- No se implementan múltiples monedas