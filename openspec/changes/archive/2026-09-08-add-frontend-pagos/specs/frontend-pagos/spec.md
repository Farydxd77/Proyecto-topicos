## ADDED Requirements

### Requirement: Ver los pagos de un grupo

La aplicación SHALL mostrar, dentro del detalle del grupo, los pagos registrados en
una sección propia y separada de la de gastos. De cada pago SHALL mostrar quién pagó,
quién recibió, el monto en USDT, la fecha y, si lo tiene, el identificador de
transacción. La aplicación SHALL indicar mientras carga que está consultando, y SHALL
ofrecer reintentar si la consulta falla, sin quedar en carga permanente. Cuando el
grupo no tiene pagos, la aplicación SHALL explicarlo en lugar de mostrar una lista
vacía.

#### Scenario: Un grupo con pagos registrados

- **WHEN** un miembro abre el detalle de un grupo que tiene pagos
- **THEN** ve la sección de pagos con una entrada por pago
- **AND** de cada una entiende quién le pagó a quién, cuánto y cuándo

#### Scenario: Un grupo sin pagos

- **WHEN** un miembro abre el detalle de un grupo sin pagos registrados
- **THEN** la sección explica que todavía no se registró ningún pago

#### Scenario: Un pago con identificador de transacción

- **WHEN** un pago tiene un `txId` cargado
- **THEN** la aplicación lo muestra junto al pago
- **AND** un identificador largo no rompe el ancho de la pantalla

#### Scenario: La consulta falla

- **WHEN** la consulta de pagos falla porque el backend no responde
- **THEN** la aplicación muestra el error y ofrece reintentar
- **AND** no queda en estado de carga permanente

### Requirement: Registrar un pago

La aplicación SHALL permitir a cualquier miembro del grupo registrar un pago que él
mismo realizó. El formulario MUST NOT ofrecer elegir quién paga: el pagador es
siempre quien está usando la aplicación, y la interfaz SHALL decirlo explícitamente
en vez de simular una elección. El receptor SHALL elegirse entre los demás miembros
actuales del grupo, sin poder escribirlo a mano y sin poder elegirse a uno mismo. El
monto se expresa siempre en USDT, sin selector de moneda. El identificador de
transacción es opcional.

La aplicación SHALL validar antes de enviar que haya un receptor elegido, que el
monto sea mayor que cero y de hasta 8 dígitos enteros y 2 decimales —el límite que
admite la columna del backend— y que el identificador de transacción, si se carga, no
supere los 100 caracteres. Tras registrarlo con éxito, el pago SHALL aparecer en la
lista y los balances y la liquidación del grupo SHALL quedar al día, sin recargar la
página.

#### Scenario: Registro con datos válidos

- **WHEN** un miembro registra un pago eligiendo receptor, monto positivo y fecha
- **THEN** el pago aparece en la lista sin recargar
- **AND** los balances y la liquidación del grupo reflejan el pago

#### Scenario: El pagador no se elige

- **WHEN** un miembro abre el formulario de pago
- **THEN** la aplicación indica que el pago se registra a su nombre
- **AND** no hay ningún control para elegir a otra persona como pagador

#### Scenario: El receptor se elige entre los demás miembros

- **WHEN** un miembro abre el formulario de pago
- **THEN** puede elegir como receptor a cualquier otro integrante actual del grupo
- **AND** no puede elegirse a sí mismo ni a alguien que no pertenece al grupo

#### Scenario: Monto no positivo

- **WHEN** un miembro intenta registrar un pago con monto cero o negativo
- **THEN** la aplicación se lo indica y no envía la petición

#### Scenario: Monto fuera del rango que admite el backend

- **WHEN** un miembro intenta registrar un pago con un monto de más de 8 dígitos
  enteros
- **THEN** la aplicación se lo indica y no envía la petición

#### Scenario: El backend rechaza el registro

- **WHEN** el backend responde con un error al registrar el pago
- **THEN** la aplicación muestra el mensaje recibido
- **AND** conserva lo que ya se había cargado en el formulario

### Requirement: Editar y eliminar solo los pagos propios

La aplicación SHALL ofrecer editar y eliminar un pago únicamente a quien lo registró,
que es lo único que el backend permite. Sobre los pagos de otras personas MUST NOT
mostrar esas acciones. La eliminación SHALL pedir confirmación explícita. Tras editar
o eliminar, la lista, los balances y la liquidación SHALL quedar al día sin recargar
la página. Si una de esas operaciones llega igualmente al backend y este responde que
no hay permiso, la aplicación SHALL mostrar el mensaje sin romperse.

#### Scenario: Un miembro mira la lista de pagos

- **WHEN** un miembro ve la lista de pagos del grupo
- **THEN** junto a los pagos que él registró aparecen las acciones de editar y
  eliminar
- **AND** junto a los pagos de otras personas no aparecen

#### Scenario: Editar un pago propio

- **WHEN** quien registró un pago cambia su monto y confirma
- **THEN** la lista muestra el nuevo monto sin recargar
- **AND** los balances y la liquidación reflejan el cambio

#### Scenario: Eliminar un pago propio

- **WHEN** quien registró un pago confirma que quiere eliminarlo
- **THEN** el pago desaparece de la lista
- **AND** los balances y la liquidación vuelven a reflejar la deuda que ese pago
  saldaba

#### Scenario: Se cancela la confirmación de eliminar

- **WHEN** quien registró un pago abre la confirmación de eliminar y la cancela
- **THEN** el pago sigue en la lista
- **AND** no se envió ninguna petición al backend

#### Scenario: El backend niega el permiso

- **WHEN** una edición o eliminación llega al backend y este responde que no hay
  permiso
- **THEN** la aplicación muestra el mensaje de permiso denegado
- **AND** el pago no cambia

### Requirement: Registrar un pago desde la liquidación sugerida

La aplicación SHALL ofrecer, en cada transferencia de la liquidación en la que quien
mira es el deudor, una acción para registrarla como pago. Esa acción SHALL abrir el
formulario de pago con el receptor y el monto **precargados** a partir de la
transferencia sugerida, dejando que el usuario los modifique antes de confirmar,
porque el backend admite pagar de más o de menos. La aplicación MUST NOT ofrecer esa
acción en las transferencias donde quien mira no es el deudor.

#### Scenario: El deudor registra la transferencia sugerida

- **WHEN** un miembro ve en la liquidación que le toca pagarle a otra persona y elige
  registrarlo
- **THEN** se abre el formulario de pago con esa persona como receptor y ese monto
  cargado
- **AND** al confirmar, el pago queda registrado y la liquidación se recalcula

#### Scenario: Los valores precargados se pueden cambiar

- **WHEN** un miembro abre el formulario desde una transferencia sugerida y cambia el
  monto antes de confirmar
- **THEN** el pago se registra con el monto que él indicó

#### Scenario: Una transferencia entre otras personas

- **WHEN** un miembro ve en la liquidación una transferencia en la que no es el
  deudor
- **THEN** no se le ofrece registrarla como pago

#### Scenario: Sin deudas pendientes

- **WHEN** la liquidación del grupo está vacía porque nadie debe nada
- **THEN** no se ofrece ninguna acción de registrar pago en esa sección
