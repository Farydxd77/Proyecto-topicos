## Why

La membresía de un grupo hoy tiene tres callejones sin salida:

1. **El grupo se queda huérfano.** El creador no puede quitarse a sí mismo (400) y
   no hay forma de pasarle el rol a otro. Si esa persona abandona el proyecto, el
   grupo queda congelado: nadie puede editarlo, borrarlo, ni agregar o quitar
   miembros, porque las cuatro operaciones están reservadas al creador.
2. **Nadie se puede ir por su cuenta.** Un miembro que quiere salir depende de que
   el creador lo saque. Si el creador no responde, queda atrapado en el grupo.
3. **Un ex-miembro con saldo es indistinguible de un miembro activo.** Los balances
   incluyen, correctamente, a quien ya no es miembro pero tiene actividad en gastos
   o pagos. La respuesta no dice cuál es cuál, así que la interfaz muestra a los dos
   igual y quien mira no entiende por qué aparece alguien que no está en la lista de
   integrantes.

## What Changes

- **Nuevo endpoint `PUT /api/grupos/{id}/creador`**, reservado al creador actual,
  que transfiere el rol a otro miembro del grupo. El cuerpo lleva
  `participanteId`. El creador saliente sigue siendo miembro común. Responde `200
  OK` con el grupo actualizado.
- **`DELETE /api/grupos/{id}/miembros/{participanteId}` amplía su significado**: un
  miembro cualquiera PUEDE quitarse a sí mismo (abandonar el grupo). El creador
  sigue sin poder quitarse: primero tiene que transferir el rol, o borrar el grupo
  si es el único miembro. Un miembro no creador que intente quitar a **otro** sigue
  recibiendo `403`.
  - **BREAKING** (de contrato de error): el escenario que hoy devuelve `403` cuando
    un miembro no creador se quita a sí mismo pasa a devolver `204`.
- **`GET /api/grupos/{id}/balances` agrega el campo `esMiembroActual`** a cada
  entrada, para que el cliente distinga a un integrante del grupo de un ex-miembro
  que quedó con saldo. La liquidación no cambia: sigue devolviendo transferencias
  entre ids, sin importar la membresía.
- **Frontend**: en el detalle del grupo aparecen dos acciones nuevas —«Transferir
  rol de creador» (solo la ve el creador, elige entre los demás miembros) y
  «Abandonar grupo» (la ven los miembros no creadores)—, ambas con confirmación. En
  la sección de balances, un ex-miembro se muestra con una marca que lo identifica
  como tal.

## Capabilities

### Modified Capabilities

- `grupos`: nueva operación de transferencia del rol de creador, y la salida
  voluntaria de un miembro pasa a ser una operación permitida sobre el endpoint de
  membresías que ya existe.
- `balances`: cada entrada de `GET /api/grupos/{id}/balances` indica si el
  participante es miembro actual del grupo.
- `frontend-miembros`: la pantalla de gestión de miembros suma transferir el rol de
  creador y abandonar el grupo.
- `frontend-balances`: la lista de balances distingue visualmente a un ex-miembro.

## Impact

- **Backend nuevo**: `dto/request/TransferirCreadorRequest`.
- **Backend modificado**: `service/GrupoService` (`transferirCreador`, y
  `quitarMiembro` con las nuevas reglas), `controller/GrupoController` (endpoint
  nuevo), `dto/response/BalanceDto` (campo `esMiembroActual`),
  `service/BalanceService` (calcular ese campo con las membresías que ya carga).
- **Frontend modificado**: `api/types.ts` (`TransferirCreadorRequest`,
  `BalanceDto.esMiembroActual`), `api/grupos.ts` (`transferirCreador`,
  `abandonarGrupo`), `components/GestionMiembros.tsx` (las dos acciones),
  `components/SeccionBalances.tsx` (marca de ex-miembro),
  `pages/GrupoDetallePage.tsx` (navegar a `/grupos` tras abandonar).
- **Base de datos**: sin cambios de esquema. La transferencia solo actualiza
  `grupos.creador_id`.
- **Contrato de la API**: `BalanceDto` gana un campo (aditivo, no rompe clientes
  que lo ignoren). El único cambio incompatible es el `403` → `204` descrito arriba.

## Non-Goals

- No se agrega un rol de coadministrador ni permisos por miembro: el grupo sigue
  teniendo exactamente un creador con todos los privilegios.
- No se bloquea la salida de un miembro que tiene saldo pendiente. Se permite y su
  balance sigue visible; forzar la liquidación antes de salir es una decisión de
  producto distinta.
- No se borra ni se reasigna el historial del que se va: sus gastos y pagos quedan
  tal como están, y su división no se recalcula.
- No se notifica a nadie de la transferencia ni de la salida (sigue sin haber email
  ni notificaciones).
- No se permite que el creador abandone el grupo sin transferir primero, ni que
  transfiera el rol a alguien que no es miembro.
- No se agrega una vista de «ex-miembros» ni un historial de membresía: la única
  señal es el campo `esMiembroActual` en los balances.
- Sin cambios en gastos, pagos ni en la conversión de moneda.
