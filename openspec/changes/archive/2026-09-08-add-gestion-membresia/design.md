## Contexto

El modelo de membresía de `CLAUDE.md` es deliberadamente simple: `grupos.creador_id`
apunta a un `participantes.id`, y `grupo_participantes` es la tabla de unión con PK
compuesta `(grupo_id, participante_id)`. No hay columna de rol ni de estado. Este
cambio no toca el esquema: transferir el rol es un `UPDATE` de `creador_id`, y
abandonar es el `DELETE` de una fila de `grupo_participantes` que ya sabemos hacer.

Lo que cambia es **quién puede pedir qué**.

## Decisión 1: `PUT /api/grupos/{id}/creador`, no `POST /api/grupos/{id}/transferir-creador`

La convención de `CLAUDE.md` es endpoints en kebab-case plural sobre sustantivos. El
rol de creador es un atributo singular del grupo, así que se modela como un
subrecurso y se reemplaza con `PUT`. La operación es idempotente: transferirle el rol
dos veces a la misma persona deja el mismo estado.

**Alternativa descartada: `POST /api/grupos/{id}/transferir-creador`.** Mete un verbo
en la ruta, que es justo lo que la convención evita.

**Alternativa descartada: incluir `creadorId` en `ActualizarGrupoRequest`** y
resolverlo en el `PUT /api/grupos/{id}` que ya existe. Mezcla dos operaciones con
reglas de autorización distintas en un mismo endpoint, y obligaría a que cada edición
de nombre o descripción reenvíe el creador. Peor: un cliente que omitiera el campo
borraría el creador sin querer.

## Decisión 2: abandonar reutiliza el endpoint de membresía

`DELETE /api/grupos/{id}/miembros/{participanteId}` pasa a resolver la autorización
en dos ramas, en este orden:

1. `participanteId` **es** el solicitante → puede salir, salvo que sea el creador.
2. `participanteId` **no** es el solicitante → solo el creador puede quitarlo.

**Alternativa descartada: un endpoint `DELETE /api/grupos/{id}/miembros/me`.**
Duplica lógica y obliga al cliente a decidir entre dos rutas para la misma acción
según quién sea. El identificador propio ya lo tiene el frontend (viene del perfil
cacheado), así que no gana nada.

**Alternativa descartada: `POST /api/grupos/{id}/abandonar`.** Mismo problema de
verbo en la ruta que la Decisión 1, y además deja dos formas de borrar la misma fila.

El caso del creador que es el único miembro merece un mensaje propio: no puede
transferir (no hay a quién) ni salir. La respuesta es `400` diciéndole que borre el
grupo, que es la salida real.

## Decisión 3: `esMiembroActual` se calcula, no se persiste

`BalanceService.cargarContexto` ya recorre `grupoParticipanteRepository.findByGrupoId`
antes que los gastos y los pagos. Basta con guardar ese conjunto de ids y comparar.
No hay consulta extra ni columna nueva.

**Alternativa descartada: no devolver a los ex-miembros.** Rompería la invariante de
que la suma de balances es exactamente `0.00`, que es el corazón de la capacidad
`balances` y está especificada como requisito propio. Si alguien se va debiendo 200,
esos 200 tienen que seguir apareciendo en algún lado.

**Alternativa descartada: un campo `estado: 'MIEMBRO' | 'EX_MIEMBRO'`.** Un booleano
alcanza para la única distinción que existe hoy; un enum invita a inventar estados
que el modelo no tiene.

`TransferenciaDto` **no** gana el campo. La liquidación dice quién le paga a quién, y
eso no depende de la membresía: a un ex-miembro que debe plata hay que pagarle igual.
El cliente que quiera marcarlo cruza los ids con la respuesta de balances.

## Decisión 4: el creador saliente queda como miembro común

Tras transferir, la fila de `grupo_participantes` del creador anterior no se toca.
Sigue viendo el grupo, sus gastos y sus balances; lo único que pierde son los cuatro
privilegios de creador.

**Alternativa descartada: transferir y salir en una sola operación.** Son dos
intenciones distintas. Quien quiera hacer ambas encadena las dos llamadas, y el
frontend puede ofrecerlo como un flujo de dos pasos sin que el backend lo acople.

## Orden de comprobaciones

Se mantiene el criterio que ya usan gastos y pagos: **existencia antes que permiso**.

Para `PUT /api/grupos/{id}/creador`:

1. Grupo inexistente → `404`
2. Solicitante no es el creador → `403`
3. `participanteId` no es miembro del grupo → `400`
4. `participanteId` es el creador actual → `400` (no hay nada que transferir)

Para `DELETE /api/grupos/{id}/miembros/{participanteId}`:

1. Grupo inexistente → `404`
2. Solicitante no es miembro → `403`
3. Se quita a sí mismo y es el creador → `400`
4. Se quita a otro y no es el creador → `403`
5. El objetivo no es miembro → `404`

El paso 2 es nuevo: hoy `quitarMiembro` va directo a `grupoDondeEsCreador`, que
devuelve `403` tanto para un miembro como para un extraño. Con la salida voluntaria
hay que saber primero si el solicitante pertenece al grupo, así que la guarda pasa a
ser `grupoDondeEsMiembro` y el chequeo de creador se aplica solo en la rama que lo
necesita.

## Riesgos

- El cambio de `403` a `204` cuando un miembro se quita a sí mismo invalida un
  escenario ya archivado de `grupos` y su test. Es un cambio de contrato deliberado y
  queda registrado como MODIFIED en el delta.
- Transferir el rol es irreversible desde la perspectiva de quien lo entrega: si le
  pasa el grupo a la persona equivocada, ya no puede recuperarlo por su cuenta. El
  frontend lo pide con confirmación explícita nombrando a quién se lo transfiere.
