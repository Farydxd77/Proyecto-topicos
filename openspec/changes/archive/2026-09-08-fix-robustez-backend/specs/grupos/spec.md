## MODIFIED Requirements

### Requirement: Eliminar un grupo

El sistema SHALL exponer `DELETE /api/grupos/{id}` reservado al creador del grupo.
Cuando el solicitante es el creador, el sistema SHALL eliminar el grupo junto con
todos sus registros de membresía, todos sus gastos (incluida la división de cada
gasto en `gasto_participantes`) y todos sus pagos, y SHALL responder `204 No
Content` sin cuerpo. La eliminación SHALL ser atómica: si alguna parte falla, no se
borra nada. Tras la eliminación, el grupo MUST NOT aparecer en `GET /api/grupos` de
ninguno de sus antiguos miembros y `GET /api/grupos/{id}` SHALL responder `404 Not
Found`. El sistema SHALL responder `403 Forbidden` cuando el solicitante es miembro
pero no creador, o cuando no es miembro, y `404 Not Found` cuando el grupo no
existe.

#### Scenario: El creador elimina el grupo

- **WHEN** el creador envía `DELETE /api/grupos/{id}`
- **THEN** el sistema responde `204 No Content` sin cuerpo
- **AND** se eliminan también todos los registros de membresía del grupo
- **AND** una consulta posterior a `GET /api/grupos/{id}` responde `404 Not Found`

#### Scenario: El creador elimina un grupo que tiene gastos

- **WHEN** el creador envía `DELETE /api/grupos/{id}` sobre un grupo con al menos
  un gasto registrado
- **THEN** el sistema responde `204 No Content` sin cuerpo
- **AND** se eliminan los gastos del grupo y la división de cada uno
- **AND** una consulta posterior a `GET /api/grupos/{id}` responde `404 Not Found`

#### Scenario: El creador elimina un grupo que tiene pagos

- **WHEN** el creador envía `DELETE /api/grupos/{id}` sobre un grupo con al menos
  un pago registrado
- **THEN** el sistema responde `204 No Content` sin cuerpo
- **AND** se eliminan los pagos del grupo

#### Scenario: El creador elimina un grupo con gastos y pagos a la vez

- **WHEN** el creador envía `DELETE /api/grupos/{id}` sobre un grupo que tiene
  gastos con su división y también pagos entre sus miembros
- **THEN** el sistema responde `204 No Content` sin cuerpo
- **AND** no queda ningún gasto, división de gasto, pago ni membresía asociado a
  ese grupo
- **AND** los participantes involucrados siguen existiendo y conservan sus otros
  grupos

#### Scenario: Un miembro no creador intenta eliminar

- **WHEN** un miembro que no es el creador envía `DELETE /api/grupos/{id}`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar
- **AND** el grupo sigue existiendo

#### Scenario: El grupo no existe

- **WHEN** un usuario autenticado envía `DELETE /api/grupos/{id}` con un `id`
  inexistente
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar
