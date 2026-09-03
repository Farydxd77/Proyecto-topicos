## Context

Ver `proposal.md` — Why. Estado actual relevante:

- Las entidades de gastos ya están mapeadas y **no las usa nadie**:
  `Gasto` (tabla `gastos`: `id`, `grupo_id` FK a `grupos`, `descripcion`, `monto`
  `DECIMAL(10,2)` con `@Check("monto > 0")`, `pagador_id` FK a `participantes`
  `nullable = true`, `fecha` `DATE`, hereda `created_at`/`updated_at`),
  `GastoParticipante` (tabla `gasto_participantes`, `@EmbeddedId`
  `GastoParticipanteId(gastoId, participanteId)`, `@MapsId` hacia `Gasto` y
  `Participante`, `monto_adeudado` `DECIMAL(10,2)`). No existen `GastoRepository`
  ni `GastoParticipanteRepository`.
- La capacidad `grupos` ya está implementada: `GrupoRepository`,
  `GrupoParticipanteRepository`
  (`findByGrupoIdAndParticipanteId` → `Optional`, `findByGrupoId` → `List`),
  `GrupoService`, `GrupoController` bajo `/api/grupos`. `GrupoService` resuelve al
  solicitante con un privado `participanteActual()` y valida si es miembro//autoría con
  los privados `grupoDondeEsMiembro` / `grupoDondeEsCreador` (ambos `private`, no
  reutilizables desde fuera).
- `ParticipanteRepository.findByUsuarioId(Long)` ya existe.
- `GlobalExceptionHandler` produce el formato de error estándar y ya mapea
  `MethodArgumentNotValidException` → `400`, `ResourceNotFoundException` → `404`,
  `ForbiddenOperationException` → `403`, `ConflictException` → `409`,
  `BadRequestException` → `400`, `BadCredentialsException` → `401`.
  `JwtAuthenticationEntryPoint` devuelve el `401` con el formato estándar.
- `SecurityConfig` aplica `anyRequest().authenticated()` salvo `/api/auth/**` y
  `/actuator/health/**`; `/api/grupos/**` (y por tanto `/api/grupos/{id}/gastos`)
  ya queda protegido.
- `dto/response/ParticipanteDto(id, nombre, apellido, ci, username)` ya existe y no
  contiene contraseña.
- Ver modelo de datos completo y convenciones en CLAUDE.md.

## Goals / Non-Goals

**Goals:**

- Implementar los cinco endpoints del spec delta reutilizando los patrones ya
  establecidos por `grupos`: controller delgado, `@Service` con constructor
  injection, Query Methods JPA sin `@Query`, errores centralizados en
  `GlobalExceptionHandler`, mapeo entidad→DTO a mano dentro de la transacción.
- Concentrar el cálculo de la división equitativa en un único método puro y
  testeable de `GastoService`, con el invariante "suma de adeudos == monto"
  garantizado por construcción.
- No modificar ninguna entidad JPA ni el esquema de base de datos.

**Non-Goals (nivel diseño):**

- No se introduce un mapper genérico ni MapStruct.
- No se refactoriza `GrupoService` para exponer sus guardas privadas (ver
  decisión 3); esta tarea no toca código que ya funciona.
- No se añade una colección `@OneToMany` de líneas de división en `Gasto` (ver
  decisión 4): el spec nombra explícitamente `GastoParticipanteRepository`.
- No se usan `@PreAuthorize` ni expresiones SpEL de seguridad.
- No se añade paginación, filtros ni `@EntityGraph`.

## Decisions

### 1. Dos repositorios nuevos con los Query Methods que nombra el spec

`GastoRepository extends JpaRepository<Gasto, Long>`:

- `List<Gasto> findByGrupoIdOrderByFechaDesc(Long grupoId)` — resuelve
  `GET /api/grupos/{id}/gastos` ordenado en una consulta derivada.
- `Optional<Gasto> findByIdAndGrupoId(Long id, Long grupoId)` — resuelve el detalle,
  la edición y el borrado, y hace que un `gastoId` de otro grupo dé `404` sin un
  chequeo extra.

`GastoParticipanteRepository extends JpaRepository<GastoParticipante, GastoParticipanteId>`:

- `List<GastoParticipante> findByGastoId(Long gastoId)` — carga la división
  registrada para el detalle y el listado.
- `void deleteByGastoId(Long gastoId)` — borra la división en el `DELETE` y antes
  de recalcular en el `PUT`.

- **Por qué:** son exactamente los accesos que el servicio necesita, todos
  derivables del nombre, sin `@Query` manual (prohibido por CLAUDE.md). Coincide
  con la lista "Repositorios utilizados" del borrador.
- **`deleteByGastoId` es un derived delete**: Spring Data lo ejecuta dentro de la
  transacción del servicio; carga las filas y las elimina. El volumen (miembros de
  un grupo) es bajo, así que no se justifica un `@Modifying @Query` de borrado
  masivo (que además chocaría con la regla de "sin `@Query`").
- **Alternativa descartada:** `deleteAllByGastoIdInBatch` o `@Modifying` — más
  rápido pero innecesario y fuera de convención.

### 2. `GastoService` con seis repositorios inyectados

`GastoService` inyecta `GastoRepository`, `GastoParticipanteRepository`,
`GrupoRepository`, `GrupoParticipanteRepository`, `ParticipanteRepository` y
`UsuarioRepository`. Métodos públicos (todos `@Transactional`; lecturas
`readOnly = true`):

- `GastoResponse registrar(Long grupoId, RegistrarGastoRequest req)`
- `List<GastoResumenDto> listar(Long grupoId)`
- `GastoResponse obtenerDetalle(Long grupoId, Long gastoId)`
- `GastoResponse actualizar(Long grupoId, Long gastoId, ActualizarGastoRequest req)`
- `void eliminar(Long grupoId, Long gastoId)`

Privados: `participanteActual()` (idéntico al de `GrupoService` /
`PerfilService`), `grupoDondeEsMiembro(Long grupoId, Participante solicitante)`,
`calcularDivision(...)` (decisión 5) y los mapeadores a DTO.

- **Por qué:** una sola clase para toda la capacidad, como `GrupoService`. El
  mapeo entidad→DTO se hace **dentro** de la transacción porque `Gasto.grupo`,
  `Gasto.pagador`, `GastoParticipante.participante` y `Participante.usuario` son
  LAZY (mismo motivo que en `GrupoService`/`ParticipanteService`).

### 3. La verificación de miembro del grupo se reimplementa localmente en `GastoService`

`grupoDondeEsMiembro(grupoId, solicitante)` en `GastoService`:
`grupoRepository.findById(grupoId)` vacío →
`ResourceNotFoundException("Grupo no encontrado: " + grupoId)`; luego
`grupoParticipanteRepository.findByGrupoIdAndParticipanteId(grupoId, solicitante.getId())`
vacío → `ForbiddenOperationException("No eres miembro de este grupo")`. El `404` se
evalúa **siempre antes** que el `403`.

- **Por qué:** es la misma regla que `GrupoService.grupoDondeEsMiembro`, pero ese
  método es `private`. Copiar ~4 líneas sobre repositorios que ya existen tiene
  menos riesgo que refactorizar `GrupoService` (cambiar la visibilidad o extraer
  un colaborador) y volver a validar toda la capacidad `grupos`, que ya está
  archivada y probada.
- **Alternativa descartada (recomendada para una tarea futura):** extraer un
  `GrupoGuard`/`GrupoValidacionService` que devuelva el `Grupo` validando existencia
  y membresía, e inyectarlo tanto en `GrupoService` como en `GastoService` (y
  luego en `BalanceService`). Se deja anotado; no se hace aquí para no ampliar el
  alcance.

### 4. La división se persiste vía `GastoParticipanteRepository`, sin colección en `Gasto`

Registrar: se construye una lista de `GastoParticipante` (cada uno con su
`GastoParticipanteId(gastoId, participanteId)`, su `@MapsId` a `Gasto` y a
`Participante`, y su `montoAdeudado`) y se guarda con
`gastoParticipanteRepository.saveAll(...)` después de persistir el `Gasto`.
Editar: `deleteByGastoId(gastoId)` + `flush()` y luego `saveAll(...)` de la nueva
división. Eliminar: `deleteByGastoId(gastoId)` y después
`gastoRepository.delete(gasto)`.

- **Por qué:** el spec nombra `GastoParticipanteRepository.findByGastoId` y
  `deleteByGastoId` como la vía de acceso. No se añade `@OneToMany` a `Gasto` (a
  diferencia de lo que se hizo con `Grupo.miembros`) porque aquí no hace falta un
  Query Method inverso ni una cascada: el borrado de la división es explícito en
  dos únicos puntos (editar y eliminar), y el proposal se compromete a no tocar
  entidades.
- **El `flush()` entre `deleteByGastoId` y `saveAll` en la edición es obligatorio**:
  sin él, Hibernate puede intentar insertar la fila nueva de un miembro que
  permanece antes de haber ejecutado el `DELETE`, violando la PK compuesta
  `(gasto_id, participante_id)`.
- **Alternativa descartada:** `@OneToMany(cascade = ALL, orphanRemoval = true)` en
  `Gasto` con `getDivision().clear()` / `addAll(...)` — funciona, pero modifica la
  entidad (fuera del alcance declarado) y el `clear()` + `add()` de un
  `@EmbeddedId` con `orphanRemoval` tiene el mismo problema de orden de `flush`.

### 5. Cálculo de la división: un método puro con el pagador absorbiendo el resto

```
calcularDivision(BigDecimal monto, List<Participante> miembros, Participante pagador):
  n = miembros.size()
  porPersona = monto.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP)
  para cada miembro m distinto del pagador:  montoAdeudado(m) = porPersona
  montoAdeudado(pagador) = monto.subtract(porPersona.multiply(BigDecimal.valueOf(n - 1)))
```

- **Por qué:** es la fórmula del borrador. Al fijar el adeudo del pagador como el
  residuo (`monto - porPersona*(n-1)`), la suma de todos los adeudos es
  **exactamente** `monto` por construcción, para cualquier `monto` y cualquier `n`,
  sin necesidad de repartir centavos uno a uno. Con `n = 1` el pagador adeuda el
  `monto` completo (`porPersona*0 = 0`).
- **Modo de redondeo `HALF_UP`:** el borrador dice "redondeado a 2 decimales" sin
  precisar el modo; `HALF_UP` es el convenio habitual para dinero. El invariante de
  la suma se cumple con cualquier modo, así que esta elección solo fija el reparto
  exacto de los centavos entre los no pagadores.
- **`miembros` = todos los miembros actuales** obtenidos con
  `grupoParticipanteRepository.findByGrupoId(grupoId)` en el momento de registrar o
  editar. Como la división queda persistida en `gasto_participantes`, un alta o
  baja posterior de miembros no toca gastos ya registrados (requisito del spec);
  no hace falta código extra para lograrlo.
- **Alternativa descartada:** repartir el sobrante centavo a centavo empezando por
  el pagador — mismo resultado en la práctica, más código y más difícil de razonar.

### 6. Tres DTOs de salida; validación de entrada con anotaciones Jakarta

- `RegistrarGastoRequest(@NotBlank @Size(max = 255) String descripcion,
  @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal monto,
  @NotNull Long pagadorId, @NotNull LocalDate fecha)`.
- `ActualizarGastoRequest` — mismo `record`, mismas validaciones (dos tipos
  distintos por claridad y coherencia con `CrearGrupoRequest`/`ActualizarGrupoRequest`).
- `GastoResumenDto(Long id, String descripcion, BigDecimal monto,
  ParticipanteDto pagador, LocalDate fecha)` para `GET .../gastos`.
- `GastoParticipanteDto(ParticipanteDto participante, BigDecimal montoAdeudado)`.
- `GastoResponse(Long id, Long grupoId, String descripcion, BigDecimal monto,
  ParticipanteDto pagador, LocalDate fecha, List<GastoParticipanteDto> division)`
  para `POST`, `GET .../{gastoId}` y `PUT`.

- **Por qué:** el spec pide la `division` solo en el detalle y en las respuestas de
  registro/edición; el listado no la necesita y cargarla para cada gasto sería N
  consultas extra. Reutilizar `ParticipanteDto` mantiene una única forma de
  "participante" y garantiza que no se filtre la contraseña. `@Positive` sobre
  `BigDecimal` cubre el caso `monto <= 0` como `400` con `errors`; el `@Check` de
  la entidad queda como segunda línea de defensa. `@Digits(integer = 8)` refleja
  `DECIMAL(10,2)`.
- **`pagadorId` no miembro se valida en el servicio**, no con anotaciones: se busca
  `grupoParticipanteRepository.findByGrupoIdAndParticipanteId(grupoId, pagadorId)`;
  si está vacío → `BadRequestException("El pagador no es miembro del grupo")`
  (`400`). Ese lookup también entrega la entidad `Participante` del pagador (vía
  `getParticipante()`) para asignarla a `Gasto.pagador`, sin una consulta aparte.
- **Alternativa descartada:** un único DTO de entrada compartido — se prefiere la
  simetría con la capacidad `grupos`.

### 7. Orden de comprobaciones por endpoint

1. Validación de `@Valid @RequestBody` (solo `POST`/`PUT`) → `400` con `errors` si
   falla. Ocurre en el borde del controller, antes del servicio (igual que en
   `grupos`); un no miembro que además manda un cuerpo inválido recibe `400`, no
   `403`. Ningún escenario del spec combina ambos, así que es aceptable.
2. `participanteActual()` → resuelve el solicitante desde el token.
3. `grupoDondeEsMiembro(grupoId, solicitante)` → `404` si el grupo no existe,
   `403` si existe y no es miembro.
4. Para `GET/{gastoId}`, `PUT`, `DELETE`: `findByIdAndGrupoId(gastoId, grupoId)`
   vacío → `404`.
5. Reglas de negocio del cuerpo: `pagadorId` no miembro → `400`. (`monto <= 0` ya
   lo atrapó el paso 1.)

- **Por qué:** reproduce el orden `404` antes que `403` que exige el spec y
  reutiliza la misma mecánica que `grupos`.

### 8. Controller anidado bajo `/api/grupos/{grupoId}/gastos`

`GastoController` (`@RestController`,
`@RequestMapping("/api/grupos/{grupoId}/gastos")`, constructor injection de
`GastoService`): `@PostMapping` + `@ResponseStatus(CREATED)`; `@GetMapping`;
`@GetMapping("/{gastoId}")`; `@PutMapping("/{gastoId}")`;
`@DeleteMapping("/{gastoId}")` + `@ResponseStatus(NO_CONTENT)`. `grupoId` y
`gastoId` como `@PathVariable Long`.

- **Por qué:** clase separada de `GrupoController` (recurso REST distinto, y
  `GrupoController` ya tiene siete endpoints). El prefijo con `{grupoId}` mantiene
  la ruta jerárquica que usa el spec.
- **Alternativa descartada:** meter los cinco endpoints en `GrupoController` —
  mezcla dos recursos y engorda una clase ya grande.

### 9. `gastos` como spec canónico nuevo

El delta usa `## ADDED Requirements` con `## Purpose`. Al archivar, OpenSpec
generará `openspec/specs/gastos/spec.md` en formato canónico, sustituyendo el
borrador manual. Mismo camino que siguieron `gestion-general` y `grupos`.

### 10. Pruebas: integración con MockMvc, dos archivos

`src/test/java/com/cuentasclaras/backend/gastos/GastoControllerTest.java` (los
cinco endpoints, códigos `201/200/204/400/403/404/401`, formato de error) y
`GastoDivisionTest.java` (la aritmética del reparto: división no exacta, división
exacta, grupo de un miembro, invariante suma == monto, y el aislamiento de gastos
frente a altas/bajas de miembros). Ambos siguen el patrón de
`grupos/GrupoControllerTest`: `@SpringBootTest`, `@Transactional`,
`@TestPropertySource` con `jwt.secret` de prueba, `MockMvcBuilders` con
`springSecurity()`, usuarios vía `POST /api/auth/register` con sufijo
`System.nanoTime()`, grupos vía `POST /api/grupos` y miembros vía
`POST /api/grupos/{id}/miembros`.

- **Por qué:** casi todo lo observable de esta capacidad (códigos de error, forma
  del cuerpo, y sobre todo los centavos del reparto) solo se ve en el borde HTTP;
  un test unitario con mocks no cubriría el `403` vs `404` ni el JSON de la
  `division`. Separar la aritmética en su propio archivo evita un test enorme.

### 11. Corrección incidental de `GrupoService.quitarMiembro` (fuera del alcance original)

Al ejecutar la suite completa (task 9.1) falló un test **preexistente** de `grupos`
(`GrupoMiembrosControllerTest.quitarMiembro_creadorQuitaAOtro_devuelve204YDejaDeVerElGrupo`),
que también falla en `main` sin ninguno de los cambios de esta tarea. Causa:
`quitarMiembro` quitaba la membresía solo de `grupo.getMiembros()` y llamaba a
`grupoRepository.save(grupo)`. Sobre una entidad ya gestionada ese `save()` es un
`merge()`, y con `Grupo.miembros` mapeado como bag
(`@OneToMany(mappedBy = "grupo", cascade = ALL, orphanRemoval = true)` sin
`@OrderColumn`) el `merge` no propaga la eliminación de huérfanos: el `DELETE FROM
grupo_participantes` nunca se emite (verificado en el log SQL) y la fila queda en la
base, así que el miembro "expulsado" sigue viendo el grupo en `GET /api/grupos`.

**Corrección aplicada** (mínima, aprobada durante la implementación): en
`quitarMiembro` se resuelve la membresía con
`grupoParticipanteRepository.findByGrupoIdAndParticipanteId(...)` (que ahora produce
el `404` "El participante no es miembro del grupo"), se quita de la colección en
memoria para mantener el agregado coherente dentro de la transacción, y se borra la
fila con `grupoParticipanteRepository.delete(membresia)` en vez de
`grupoRepository.save(grupo)`. No cambia ningún contrato de endpoint de `grupos`.

- **Por qué se hace aquí y no en un cambio aparte:** el criterio de aceptación de
  `gastos` exige que `mvnw.cmd test` pase con toda la suite; sin esta corrección no
  se puede cerrar la tarea. El arreglo es de una sola función y está cubierto por el
  test preexistente que ahora pasa.
- **Alternativas descartadas:** (a) quitar los `grupoRepository.save(grupo)` de
  `actualizar`/`agregarMiembro`/`quitarMiembro` y confiar en el dirty-checking —
  más limpio pero toca tres métodos de una capacidad archivada; (b) `@OrderColumn`
  en `miembros` para convertir el bag en lista indexada — cambio de esquema
  innecesario. La `gastos` no sufre este problema porque gestiona
  `gasto_participantes` con su repositorio (decisión 4), no con `@OneToMany`.

## Risks / Trade-offs

- **`PUT` recalcula con los miembros actuales, que pueden diferir de los del
  registro** → un gasto editado "pierde" la foto original de miembros. Mitigación:
  es exactamente lo que pide el spec ("recalcula la división entre todos los
  miembros actuales del grupo"); queda documentado en el requisito de edición.
- **La validación de `@Valid` corre antes que la verificación de miembro del grupo** → un no
  miembro con cuerpo inválido recibe `400` en vez de `403`. Mitigación: ningún
  escenario del spec combina ambos; es el mismo comportamiento que `grupos`.
- **Borrar un grupo que ya tiene gastos falla por FK** (`gasto_participantes` /
  `gastos` referencian `grupos`, y `Grupo.miembros` tiene `cascade = ALL` pero no
  cubre `gastos`) → hoy `DELETE /api/grupos/{id}` de un grupo con gastos lanzaría
  una violación de integridad. Mitigación: está fuera del alcance de esta tarea
  (no se toca el borrado de grupos); cuando se aborde, será un cambio local a
  `GrupoService` (bloquear con `409` o cascada explícita). Se anota aquí para que
  no sorprenda.
- **`deleteByGastoId` como derived delete** carga las filas antes de borrarlas
  (N+1 con divisiones grandes) → Mitigación: volumen bajo en esta fase.
- **Falta el `flush()` entre borrar y reinsertar la división en el `PUT`** →
  violación de la PK compuesta. Mitigación: es un punto explícito en la tarea del
  `actualizar` y queda cubierto por el test de edición.
- **Ordenar solo por `fecha` desc deja indefinido el orden entre gastos del mismo
  día** → Mitigación: el spec solo exige orden por fecha; los tests que dependan
  del orden usan fechas distintas.
- **Redondeo `HALF_UP` asumido** (el borrador no lo fija) → Mitigación: solo afecta
  el reparto exacto de centavos entre no pagadores; la suma total siempre cuadra.
  Documentado en la decisión 5 y verificado por `GastoDivisionTest`.
- **`ddl-auto=update`**: las tablas `gastos` y `gasto_participantes` ya se generan
  desde entidades existentes; no hay cambios de esquema ni riesgo de migración.

## Migration Plan

No hay migración de datos ni de esquema: no se crean, renombran ni borran columnas
o tablas, no se modifica ninguna entidad, y todos los endpoints son nuevos. El
despliegue es el de siempre (`mvnw.cmd test` y arrancar la app). El rollback es
revertir el commit; no queda estado que limpiar salvo las filas de `gastos` /
`gasto_participantes` creadas durante el uso.
