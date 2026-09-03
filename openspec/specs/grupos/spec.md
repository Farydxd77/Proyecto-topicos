# Grupos — Gestión de grupos de viaje

## Objetivo
Permitir que usuarios autenticados creen grupos de viaje, gestionen sus miembros
y consulten la información del grupo. Solo el creador tiene permisos de
administración (editar, eliminar, agregar y quitar miembros).

## Comportamiento esperado

### Dado que un usuario autenticado envía POST /api/grupos con datos válidos
Cuando el nombre no está vacío,
Entonces se crea el grupo con el usuario autenticado como creador,
se agrega automáticamente el creador como primer miembro,
y devuelve 201 Created con los datos del grupo y la lista de miembros.

### Dado que un usuario autenticado envía GET /api/grupos
Cuando el token JWT es válido,
Entonces devuelve 200 OK con la lista de grupos donde el usuario es miembro.
Si no pertenece a ningún grupo devuelve 200 con [].

### Dado que un usuario autenticado envía GET /api/grupos/{id}
Cuando el grupo existe y el usuario es miembro,
Entonces devuelve 200 OK con nombre, descripcion, creador y lista de miembros.

### Dado que un usuario autenticado envía GET /api/grupos/{id}
Cuando el usuario no es miembro del grupo,
Entonces devuelve 403 Forbidden con el formato de error estándar.

### Dado que el creador envía PUT /api/grupos/{id} con datos válidos
Cuando el grupo existe y el usuario es el creador,
Entonces actualiza nombre y/o descripcion
y devuelve 200 OK con los datos actualizados.

### Dado que un no creador envía PUT /api/grupos/{id}
Cuando el usuario es miembro pero no el creador,
Entonces devuelve 403 Forbidden con el formato de error estándar.

### Dado que el creador envía DELETE /api/grupos/{id}
Cuando el grupo existe y el usuario es el creador,
Entonces elimina el grupo y todos sus registros en grupo_participantes
y devuelve 204 No Content.

### Dado que un no creador envía DELETE /api/grupos/{id}
Cuando el usuario es miembro pero no el creador,
Entonces devuelve 403 Forbidden con el formato de error estándar.

### Dado que el creador envía POST /api/grupos/{id}/miembros con un participanteId válido
Cuando el participante existe y no es ya miembro del grupo,
Entonces se agrega el participante al grupo
y devuelve 201 Created con la lista actualizada de miembros.

### Dado que el creador intenta agregar un participante que ya es miembro
Cuando el participante ya pertenece al grupo,
Entonces devuelve 409 Conflict con el formato de error estándar.

### Dado que el creador envía DELETE /api/grupos/{id}/miembros/{participanteId}
Cuando el participante es miembro del grupo y no es el creador,
Entonces elimina al participante del grupo
y devuelve 204 No Content.

### Dado que el creador intenta eliminarse a sí mismo del grupo
Cuando el participanteId corresponde al creador,
Entonces devuelve 400 Bad Request — el creador no puede quitarse a sí mismo.

## Casos límite
- Nombre del grupo vacío → 400 Bad Request
- Grupo no existe → 404 Not Found
- Usuario no miembro intenta ver el grupo → 403 Forbidden
- No creador intenta editar, eliminar o gestionar miembros → 403 Forbidden
- Participante ya es miembro → 409 Conflict
- Creador intenta eliminarse a sí mismo → 400 Bad Request
- Eliminar grupo elimina en cascada sus registros en grupo_participantes

## Repositorios utilizados

### GrupoRepository
- findById(Long id)
- findByMiembrosParticipanteId(Long participanteId)

### GrupoParticipanteRepository
- findByGrupoIdAndParticipanteId(Long grupoId, Long participanteId)
- findByGrupoId(Long grupoId)

### ParticipanteRepository
- findById(Long id)
- findByUsuarioId(Long usuarioId)

## Servicios
- GrupoService usa GrupoRepository, GrupoParticipanteRepository y ParticipanteRepository
- El participante del usuario autenticado se resuelve desde el token JWT via findByUsuarioId
- Verificar membresía antes de cualquier operación sobre el grupo
- Verificar que es creador antes de editar, eliminar o gestionar miembros

## Criterios de aceptación
- [ ] POST /api/grupos crea el grupo y agrega al creador como primer miembro
- [ ] GET /api/grupos devuelve solo los grupos donde el usuario es miembro
- [ ] GET /api/grupos/{id} devuelve 403 si el usuario no es miembro
- [ ] GET /api/grupos/{id} devuelve 404 si el grupo no existe
- [ ] PUT /api/grupos/{id} solo lo puede hacer el creador
- [ ] PUT /api/grupos/{id} devuelve 403 si no es el creador
- [ ] DELETE /api/grupos/{id} solo lo puede hacer el creador
- [ ] DELETE /api/grupos/{id} devuelve 403 si no es el creador
- [ ] POST /api/grupos/{id}/miembros agrega el participante correctamente
- [ ] POST /api/grupos/{id}/miembros devuelve 409 si ya es miembro
- [ ] DELETE /api/grupos/{id}/miembros/{participanteId} elimina al miembro
- [ ] DELETE /api/grupos/{id}/miembros/{participanteId} devuelve 400 si es el creador
- [ ] La app compila sin errores
- [ ] mvnw test pasa

## Fuera de alcance
- No se implementan gastos (siguiente tarea)
- No se implementan balances ni liquidación
- Un miembro no puede abandonar el grupo por su cuenta
- No se implementan roles dentro del grupo (solo creador vs miembro)
- No se implementa transferencia de rol de creador a otro miembro