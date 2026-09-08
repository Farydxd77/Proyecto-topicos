## 1. Modelo

- [x] 1.1 Crear `entity/EstadoBaja.java`: enum con `PENDIENTE`, `ASUMIDA`,
  `NO_ASUMIDA`. Verificar con `cd backend && mvnw.cmd -q test-compile`.
- [x] 1.2 Crear `entity/BajaGrupo.java` (extiende `BaseEntity`; `@Getter @Setter
  @SuperBuilder @NoArgsConstructor @AllArgsConstructor`; `id` IDENTITY;
  `@ManyToOne(LAZY)` a `grupo` y `participante`, ambos `nullable = false`; `saldo`
  `precision = 10, scale = 2` `nullable = false`; `estado`
  `@Enumerated(EnumType.STRING)` `length = 20` `nullable = false`; `fecha`
  `LocalDate` `nullable = false`). Verificar con `mvnw.cmd -q test-compile`.
- [x] 1.3 Crear `entity/BajaParticipanteId.java` (`@Embeddable`, `Serializable`,
  `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode`, con
  `bajaId` y `participanteId`), siguiendo la convención de `GastoParticipanteId`.
  Verificar con `mvnw.cmd -q test-compile`.
- [x] 1.4 Crear `entity/BajaParticipante.java` (`@EmbeddedId`, `@MapsId` a `baja` y
  `participante`, `monto` `precision = 10, scale = 2` `nullable = false`,
  documentando que admite negativos porque cubre también el caso del que se va con
  saldo a favor). Verificar con `mvnw.cmd -q test-compile`.
- [x] 1.5 Crear `repository/BajaGrupoRepository.java` (`List<BajaGrupo>
  findByGrupoIdOrderByFechaDesc`, `Optional<BajaGrupo> findByIdAndGrupoId`,
  `List<BajaGrupo> findByGrupoIdAndEstado`, `void deleteByGrupoId`) y
  `repository/BajaParticipanteRepository.java` (`List<BajaParticipante>
  findByBajaId`, `void deleteByBajaId`), con Query Methods. Verificar con `mvnw.cmd
  -q test-compile`.

## 2. Contratos

- [x] 2.1 Crear `dto/request/ResolverBajaRequest.java` (record con `Boolean asumir`
  `@NotNull`). Verificar con `mvnw.cmd -q test-compile`.
- [x] 2.2 Crear `dto/response/BajaParticipanteDto.java` (record `ParticipanteDto
  participante`, `BigDecimal monto`) y `dto/response/BajaGrupoDto.java` (record `Long
  id`, `Long grupoId`, `ParticipanteDto participante`, `BigDecimal saldo`, `String
  estado`, `LocalDate fecha`, `List<BajaParticipanteDto> reparto`). Verificar con
  `mvnw.cmd -q test-compile`.

## 3. Servicio de bajas

- [x] 3.1 Crear `service/BajaService.java` con los helpers habituales
  (`participanteActual`, `grupoDondeEsMiembro`, `grupoDondeEsCreador`,
  `bajaDelGrupo`) y `toResponse`. Verificar con `mvnw.cmd -q test-compile`.
- [x] 3.2 Implementar `registrar(Grupo grupo, Participante saliente, BigDecimal
  saldo)` (llamado desde `GrupoService`, sin guardas de autorización propias porque
  ya las hizo quien lo llama): si el saldo es `0.00` no crea nada; si no, persiste la
  baja en `PENDIENTE`. Verificar con `mvnw.cmd -q test-compile`.
- [x] 3.3 Implementar `listar(Long grupoId)` `@Transactional(readOnly = true)` con la
  guarda de membresía. Verificar con `mvnw.cmd -q test-compile`.
- [x] 3.4 Implementar `resolver(Long grupoId, Long bajaId, ResolverBajaRequest req)`
  `@Transactional` con el orden de comprobaciones del design: 404 grupo → 403 no
  creador → 404 baja → 409 ya resuelta → 400 sin miembros que absorban. Si
  `asumir`, reparte el saldo en partes iguales entre los miembros actuales con la
  misma técnica de resto por diferencia que `GastoService` (el último absorbe la
  diferencia para que la suma sea exacta) y persiste el reparto. Verificar con
  `mvnw.cmd -q test-compile`.
- [x] 3.5 Crear `controller/BajaController.java` con `GET /api/grupos/{grupoId}/bajas`
  y `PUT /api/grupos/{grupoId}/bajas/{bajaId}`. Verificar con `mvnw.cmd -q
  test-compile`.

## 4. Integración con el resto

- [x] 4.1 En `service/BalanceService.java`, exponer `saldoDe(Long grupoId, Long
  participanteId)` sin guarda de autorización (documentando que es interno y que
  quien lo llama ya autorizó), para que `GrupoService` calcule el saldo al momento de
  la baja. Verificar con `mvnw.cmd -q test-compile`.
- [x] 4.2 En `service/GrupoService.java`, tras borrar la membresía en
  `quitarMiembro`, calcular el saldo del que sale y llamar a `BajaService.registrar`.
  Verificar con `mvnw.cmd -q test-compile`.
- [x] 4.3 En `util/BalanceUtil.java`, agregar la sobrecarga de `calcularBalances` que
  recibe `condonadoPorId` y `absorbidoPorId`, delegando las anteriores con `Map.of()`.
  Verificar con `mvnw.cmd -q test-compile`.
- [x] 4.4 En `service/BalanceService.java`, cargar en el `Contexto` las bajas
  `ASUMIDA` del grupo: `condonado[saliente] += -saldo` y `absorbido[m] += monto` por
  cada fila del reparto, e incluir a esos participantes en el mapa de participantes.
  Verificar con `mvnw.cmd -q test-compile`.
- [x] 4.5 En `service/GrupoService.eliminar`, borrar también las bajas y sus repartos
  antes de borrar el grupo, para no dejar la FK colgada. Verificar con `mvnw.cmd -q
  test-compile`.

## 5. Tests de backend

- [x] 5.1 Crear `bajas/BajaControllerTest.java`: registro automático de la baja
  (`salirConSaldo_registraBajaPendiente()`, `salirSinSaldo_noRegistraBaja()`,
  `saldoQuedaCongelado()`), listado (`listar_devuelveLasBajasDelGrupo()`,
  `listar_noMiembro_devuelve403()`, `listar_sinToken_devuelve401()`) y resolución
  (`resolver_asumir_repartAEntreLosMiembrosActuales()`,
  `resolver_noAsumir_noCambiaNingunBalance()`,
  `resolver_miembroNoCreador_devuelve403()`,
  `resolver_bajaYaResuelta_devuelve409()`, `resolver_bajaInexistente_devuelve404()`,
  `resolver_sinCampoAsumir_devuelve400()`). Verificar con `mvnw.cmd -q test`.
- [x] 5.2 En `balances/BalanceControllerTest.java`, agregar el efecto sobre los
  balances: `bajaPendiente_noAlteraNingunBalance()`,
  `bajaAsumida_llevaACeroAlQueSeFueYReparte()`,
  `bajaAsumidaDeAcreedor_subeElBalanceDeLosDemas()`,
  `bajaNoAsumida_dejaLosBalancesIntactos()`,
  `bajaAsumidaNoDivisible_sumaSigueEnCero()` y
  `bajasEnLosTresEstados_sumaSigueEnCero()`. Verificar con `mvnw.cmd -q test`.
- [x] 5.3 En `bajas/BajaControllerTest.java`, agregar
  `repartoCongelado_miembroNuevoNoAbsorbe()`. Verificar con `mvnw.cmd -q test`.
- [x] 5.4 En `grupos/GrupoControllerTest.java`, agregar
  `eliminarGrupo_conBajas_devuelve204YBorraLasBajas()`. Verificar con `mvnw.cmd -q
  test`.
- [x] 5.5 Correr la suite completa con `mvnw.cmd -q test`.

## 6. Documentación del modelo

- [x] 6.1 En `CLAUDE.md`, agregar las tablas `bajas_grupo` y `baja_participantes` al
  modelo de datos, una sección *Bajas con deuda* con las reglas, y quitar de *Fuera
  de alcance* lo que este cambio ya cubre.
- [x] 6.2 En `openspec/specs/data-model.md`, reflejar las dos tablas nuevas.

## 7. Frontend

- [x] 7.1 En `frontend/src/api/types.ts`, agregar `EstadoBaja`,
  `BajaParticipanteDto`, `BajaGrupoDto` y `ResolverBajaRequest`. Verificar con `cd
  frontend && npx tsc --noEmit`.
- [x] 7.2 Crear `frontend/src/api/bajas.ts` con `listarBajas` y `resolverBaja`.
  Verificar con `npx tsc --noEmit`.
- [x] 7.3 En `frontend/src/lib/claves.ts`, agregar `claveBajas(grupoId)` y sumarla a
  `clavesDerivadasDelGrupo`. Verificar con `npx tsc --noEmit`.
- [x] 7.4 Crear `components/SeccionBajas.tsx` con las tres partes: bajas pendientes
  (con las dos acciones solo para el creador, y la previsualización del reparto antes
  de asumir), el apartado «Deudas sin resolver» (oculto si está vacío) y las bajas ya
  asumidas con su reparto. Verificar con `npx tsc --noEmit` y `npx oxlint src`.
- [x] 7.5 En `pages/GrupoDetallePage.tsx`, montar `SeccionBajas` entre
  `GestionMiembros` y `SeccionGastos`. Verificar con `npx tsc --noEmit` y `npx oxlint
  src`.

## 8. Verificación final

- [x] 8.1 `cd backend && mvnw.cmd -q test` sin fallos.
- [x] 8.2 `cd frontend && npx tsc --noEmit && npx oxlint src && npm run build` sin
  errores.
- [x] 8.3 `openspec validate 2026-09-08-add-baja-con-deuda --strict` pasa.
