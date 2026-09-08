## 1. Borrado en cascada de un grupo

- [x] 1.1 Agregar `void deleteByGrupoId(Long grupoId)` a `repository/GastoRepository.java`
  y a `repository/PagoRepository.java` (Query Method, sin `@Query`). Verificar con
  `cd backend && mvnw.cmd -q test-compile`.
- [x] 1.2 En `service/GrupoService.java`, inyectar `GastoRepository`,
  `GastoParticipanteRepository` y `PagoRepository`, y reescribir `eliminar(Long
  grupoId)` `@Transactional` para que, tras `grupoDondeEsCreador`, borre en orden:
  la división de cada gasto del grupo (`deleteByGastoId` por cada gasto de
  `findByGrupoIdOrderByFechaDesc`), luego `gastoRepository.deleteByGrupoId`, luego
  `pagoRepository.deleteByGrupoId`, y por último `grupoRepository.delete(grupo)`.
  Intercalar `flush()` antes del borrado del grupo para que el orden llegue a la
  base tal cual. Verificar con `mvnw.cmd -q test-compile`.

## 2. Rango del monto de un pago

- [x] 2.1 Cambiar `@Digits(integer = 10, fraction = 2)` por `@Digits(integer = 8,
  fraction = 2)` en `dto/request/RegistrarPagoRequest.java`, ajustando el mensaje a
  «El monto admite hasta 8 enteros y 2 decimales». Verificar con `mvnw.cmd -q
  test-compile`.
- [x] 2.2 Aplicar el mismo cambio en `dto/request/ActualizarPagoRequest.java`.
  Verificar con `mvnw.cmd -q test-compile`.

## 3. Manejo de errores uniforme

- [x] 3.1 En `exception/GlobalExceptionHandler.java`, agregar un logger
  (`LoggerFactory.getLogger(GlobalExceptionHandler.class)`) y el handler de
  `HttpMessageNotReadableException` → `400` con `message` «El cuerpo de la petición
  es inválido». Verificar con `mvnw.cmd -q test-compile`.
- [x] 3.2 Agregar el handler de `MethodArgumentTypeMismatchException` → `400` con
  `message` que nombre el parámetro (`ex.getName()`). Verificar con `mvnw.cmd -q
  test-compile`.
- [x] 3.3 Agregar el handler de `HttpRequestMethodNotSupportedException` → `405`.
  Verificar con `mvnw.cmd -q test-compile`.
- [x] 3.4 Agregar el handler de `NoResourceFoundException` (paquete
  `org.springframework.web.servlet.resource`) → `404`. Verificar con `mvnw.cmd -q
  test-compile`.
- [x] 3.5 Agregar el handler de `DataIntegrityViolationException` → `409` con
  `message` genérico «La operación viola una restricción de integridad de datos», y
  registrar la excepción en el log a nivel `WARN`. Verificar con `mvnw.cmd -q
  test-compile`.
- [x] 3.6 Agregar el handler de `AccessDeniedException`
  (`org.springframework.security.access.AccessDeniedException`) → `403`. Verificar
  con `mvnw.cmd -q test-compile`.
- [x] 3.7 Agregar el handler catch-all de `Exception` → `500` con `message` fijo «Ha
  ocurrido un error inesperado», registrando `ex` completo a nivel `ERROR`. No
  incluir el mensaje ni la traza en la respuesta. Verificar con `mvnw.cmd -q
  test-compile`.
- [x] 3.8 En `application.properties`, fijar
  `spring.mvc.throw-exception-if-no-handler-found=true` y
  `spring.web.resources.add-mappings=false` para que una ruta inexistente llegue al
  advice en vez de resolverse como recurso estático. Verificar con `mvnw.cmd -q
  test-compile`.

## 4. Búsqueda de participantes por CI

- [x] 4.1 En `repository/ParticipanteRepository.java`, reemplazar
  `Optional<Participante> findByCi(String ci)` por `List<Participante>
  findAllByCi(String ci)`. Verificar con `mvnw.cmd -q test-compile`.
- [x] 4.2 En `service/ParticipanteService.java`, cambiar la rama de `ci` de
  `findByCi(ci).map(List::of).orElseGet(List::of)` a `findAllByCi(ci)`. Verificar
  con `mvnw.cmd -q test-compile`.

## 5. Documentación

- [x] 5.1 En `CLAUDE.md`: quitar de *Fuera de alcance* la línea «Pagos reales y
  registro de que una transferencia de la liquidación ya se pagó» y reemplazarla por
  una que aclare que sigue fuera de alcance **verificar** el pago contra una
  blockchain o una pasarela real, pero no registrarlo.
- [x] 5.2 En `CLAUDE.md`: agregar `pagos` a la descripción de la fase actual en
  *Contexto del proyecto* y una sección *Pagos* con las reglas de permiso (el
  pagador es siempre el del token; solo él edita y borra; monto siempre en USDT).

## 6. Tests

- [x] 6.1 En `grupos/GrupoControllerTest.java`, agregar
  `eliminarGrupo_conGastos_devuelve204YBorraGastosYDivision()`: crea grupo con 2
  miembros, registra un gasto, borra el grupo, verifica `204`, que
  `GET /api/grupos/{id}` da `404` y que no quedan filas en `gastos` ni
  `gasto_participantes` para ese grupo. Verificar con `mvnw.cmd -q test`.
- [x] 6.2 En `grupos/GrupoControllerTest.java`, agregar
  `eliminarGrupo_conPagos_devuelve204YBorraPagos()`: mismo montaje pero registrando
  un pago; verifica `204` y que no quedan filas en `pagos`. Verificar con `mvnw.cmd
  -q test`.
- [x] 6.3 En `grupos/GrupoControllerTest.java`, agregar
  `eliminarGrupo_conGastosYPagos_devuelve204YLosParticipantesSobreviven()`: verifica
  el borrado combinado y que los `Participante` involucrados siguen existiendo.
  Verificar con `mvnw.cmd -q test`.
- [x] 6.4 En `pagos/PagoControllerTest.java`, agregar
  `registrarPago_montoEnElLimite_devuelve201()` con `99999999.99` y
  `registrarPago_montoConNueveEnteros_devuelve400()` con `100000000.00`. Verificar
  con `mvnw.cmd -q test`.
- [x] 6.5 En `pagos/PagoControllerTest.java`, agregar
  `actualizarPago_montoFueraDeRango_devuelve400YNoCambiaElPago()`. Verificar con
  `mvnw.cmd -q test`.
- [x] 6.6 En `gestiongeneral/ParticipanteControllerTest.java`, agregar
  `buscarPorCi_dosParticipantesConElMismoCi_devuelve200ConAmbos()`. Verificar con
  `mvnw.cmd -q test`.
- [x] 6.7 Crear `errores/ManejoErroresIntegrationTest.java` con el montaje habitual
  (`@SpringBootTest`, `@Transactional`, `@TestPropertySource` con `jwt.secret`) y un
  helper que verifique los cinco campos del formato estándar. Casos:
  `jsonMalformado_devuelve400Estandar()`, `fechaInvalida_devuelve400Estandar()`,
  `idNoNumerico_devuelve400EstandarConNombreDeParametro()`,
  `metodoNoPermitido_devuelve405Estandar()`,
  `rutaInexistente_devuelve404Estandar()` y
  `errorDeNegocio_devuelve404Estandar()` como control. Verificar con `mvnw.cmd -q
  test`.

## 7. Verificación final

- [x] 7.1 Correr la suite completa con `cd backend && mvnw.cmd -q test` y confirmar
  que no hay fallos ni errores.
- [x] 7.2 Correr `openspec validate 2026-09-08-fix-robustez-backend --strict` y
  confirmar que pasa.
