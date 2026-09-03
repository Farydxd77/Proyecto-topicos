## 1. Excepciones y formato de error

- [x] 1.1 Crear `exception/ForbiddenOperationException.java`: `RuntimeException`
  pública con constructor `(String message)` que delega en `super(message)`. Mismo
  estilo que `ResourceNotFoundException`. Verificar con `mvnw.cmd compile`.
- [x] 1.2 Crear `exception/ConflictException.java` con la misma forma.
  Verificar con `mvnw.cmd compile`.
- [x] 1.3 Crear `exception/BadRequestException.java` con la misma forma.
  Verificar con `mvnw.cmd compile`.
- [x] 1.4 En `exception/GlobalExceptionHandler.java` añadir tres
  `@ExceptionHandler` que reutilicen el helper `standardBody(...)`:
  `ForbiddenOperationException` → `403 Forbidden`, `ConflictException` →
  `409 Conflict`, `BadRequestException` → `400 Bad Request`. Verificar con
  `mvnw.cmd compile`.

## 2. Entidad y repositorios

- [ ] 2.1 En `entity/Grupo.java` añadir la colección inversa
  `@OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)`
  `@Builder.Default private List<GrupoParticipante> miembros = new ArrayList<>();`.
  No añadir `@Data` ni `toString` sobre la colección. Verificar con
  `mvnw.cmd compile` y arrancando la app: Hibernate (`ddl-auto=update`) no debe
  emitir ningún `alter table`.
- [ ] 2.2 Crear `repository/GrupoRepository.java`:
  `interface GrupoRepository extends JpaRepository<Grupo, Long>` con
  `List<Grupo> findByMiembrosParticipanteId(Long participanteId)`. Verificar con
  `mvnw.cmd compile` y con el arranque del contexto Spring (`mvnw.cmd test` sobre
  `BackendApplicationTests`), que falla si el Query Method no es derivable.
- [ ] 2.3 Crear `repository/GrupoParticipanteRepository.java`:
  `interface GrupoParticipanteRepository extends JpaRepository<GrupoParticipante, GrupoParticipanteId>`
  con `Optional<GrupoParticipante> findByGrupoIdAndParticipanteId(Long grupoId, Long participanteId)`
  y `List<GrupoParticipante> findByGrupoId(Long grupoId)`. Verificar igual que 2.2.

## 3. DTOs

- [x] 3.1 Crear `dto/request/CrearGrupoRequest.java` como `record` con
  `@NotBlank @Size(max = 100) String nombre` y `String descripcion` (sin
  validación). Verificar con `mvnw.cmd compile`.
- [x] 3.2 Crear `dto/request/ActualizarGrupoRequest.java` como `record` con los
  mismos campos y las mismas validaciones que 3.1. Verificar con `mvnw.cmd compile`.
- [x] 3.3 Crear `dto/request/AgregarMiembroRequest.java` como `record` con
  `@NotNull Long participanteId`. Verificar con `mvnw.cmd compile`.
- [x] 3.4 Crear `dto/response/GrupoResumenDto.java` como
  `record GrupoResumenDto(Long id, String nombre, String descripcion, ParticipanteDto creador)`.
  Verificar con `mvnw.cmd compile`.
- [x] 3.5 Crear `dto/response/GrupoResponse.java` como
  `record GrupoResponse(Long id, String nombre, String descripcion, ParticipanteDto creador, List<ParticipanteDto> miembros)`.
  Verificar con `mvnw.cmd compile`.

## 4. Servicio: base y guardas de autorización

- [x] 4.1 Crear `service/GrupoService.java` (`@Service`, constructor injection de
  `GrupoRepository`, `GrupoParticipanteRepository`, `ParticipanteRepository` y
  `UsuarioRepository`) con los privados de mapeo:
  `ParticipanteDto toParticipanteDto(Participante p)` (lee
  `p.getUsuario().getUsername()`), `GrupoResumenDto toResumen(Grupo g)` y
  `GrupoResponse toResponse(Grupo g)` (mapea `g.getMiembros()` a la lista de
  `ParticipanteDto`, tolerando `creador` nulo). Verificar con `mvnw.cmd compile`.
- [x] 4.2 Añadir el privado `Participante participanteActual()`: lee el `username`
  desde `SecurityContextHolder.getContext().getAuthentication()`, resuelve el
  `Usuario` con `usuarioRepository.findByUsername` y el participante con
  `participanteRepository.findByUsuarioId`, lanzando `ResourceNotFoundException`
  si falta alguno. Mismo patrón que `PerfilService.usuarioActual()`. Verificar con
  `mvnw.cmd compile`.
- [x] 4.3 Añadir el privado `Grupo grupoDondeEsMiembro(Long grupoId, Participante solicitante)`:
  `grupoRepository.findById` → `ResourceNotFoundException("Grupo no encontrado: " + grupoId)`;
  luego `grupoParticipanteRepository.findByGrupoIdAndParticipanteId` vacío →
  `ForbiddenOperationException("No eres miembro de este grupo")`. El `404` se
  evalúa siempre antes que el `403`. Verificar con `mvnw.cmd compile`.
- [x] 4.4 Añadir el privado `Grupo grupoDondeEsCreador(Long grupoId, Participante solicitante)`:
  `findById` → `404`; si `grupo.getCreador()` es nulo o su id no coincide con el
  del solicitante → `ForbiddenOperationException("Solo el creador del grupo puede realizar esta operación")`.
  Verificar con `mvnw.cmd compile`.

## 5. Servicio: operaciones sobre el grupo

- [x] 5.1 Añadir `GrupoResponse crear(CrearGrupoRequest req)` (`@Transactional`):
  resuelve `participanteActual()`, construye el `Grupo` con `nombre`,
  `descripcion` y ese participante como `creador`, crea el `GrupoParticipante` del
  creador (con su `GrupoParticipanteId`) y lo añade a `grupo.getMiembros()`,
  guarda con `grupoRepository.save` y devuelve `toResponse`. Verificar con
  `mvnw.cmd compile`; el comportamiento lo cubre la tarea 8.1.
- [x] 5.2 Añadir `List<GrupoResumenDto> listarMisGrupos()`
  (`@Transactional(readOnly = true)`): `participanteActual()` →
  `grupoRepository.findByMiembrosParticipanteId(id)` → mapea con `toResumen`;
  devuelve lista vacía si no hay resultados. Verificar con `mvnw.cmd compile`;
  comportamiento cubierto por la tarea 8.2.
- [x] 5.3 Añadir `GrupoResponse obtenerDetalle(Long grupoId)`
  (`@Transactional(readOnly = true)`): usa `grupoDondeEsMiembro` y devuelve
  `toResponse`. Verificar con `mvnw.cmd compile`; comportamiento cubierto por 8.3.
- [x] 5.4 Añadir `GrupoResponse actualizar(Long grupoId, ActualizarGrupoRequest req)`
  (`@Transactional`): usa `grupoDondeEsCreador`, asigna `nombre` y `descripcion`,
  guarda y devuelve `toResponse`. No toca `creador` ni `miembros`. Verificar con
  `mvnw.cmd compile`; comportamiento cubierto por 8.4.
- [x] 5.5 Añadir `void eliminar(Long grupoId)` (`@Transactional`): usa
  `grupoDondeEsCreador` y `grupoRepository.delete(grupo)`, dejando que la cascada
  borre `grupo_participantes`. Verificar con `mvnw.cmd compile`; comportamiento
  cubierto por 8.5.

## 6. Servicio: miembros

- [x] 6.1 Añadir `GrupoResponse agregarMiembro(Long grupoId, AgregarMiembroRequest req)`
  (`@Transactional`): `grupoDondeEsCreador`; `participanteRepository.findById` →
  `ResourceNotFoundException("Participante no encontrado: " + id)`;
  `findByGrupoIdAndParticipanteId` presente →
  `ConflictException("El participante ya es miembro del grupo")`; si no, añade el
  `GrupoParticipante` a `grupo.getMiembros()`, guarda y devuelve `toResponse`.
  Verificar con `mvnw.cmd compile`; comportamiento cubierto por 9.1.
- [x] 6.2 Añadir `void quitarMiembro(Long grupoId, Long participanteId)`
  (`@Transactional`): `grupoDondeEsCreador`; si `participanteId` es el del creador
  → `BadRequestException("El creador no puede quitarse a sí mismo del grupo")`;
  si `findByGrupoIdAndParticipanteId` está vacío →
  `ResourceNotFoundException("El participante no es miembro del grupo")`; si no,
  elimina la fila de `grupo.getMiembros()` (para que `orphanRemoval` la borre) y
  guarda. Verificar con `mvnw.cmd compile`; comportamiento cubierto por 9.2 y 9.3.

## 7. Controller

- [x] 7.1 Crear `controller/GrupoController.java` (`@RestController`,
  `@RequestMapping("/api/grupos")`, constructor injection de `GrupoService`) con
  los endpoints de grupo: `@PostMapping` + `@ResponseStatus(CREATED)` →
  `crear(@Valid @RequestBody CrearGrupoRequest)`; `@GetMapping` →
  `listarMisGrupos()`; `@GetMapping("/{id}")` → `obtenerDetalle(id)`;
  `@PutMapping("/{id}")` → `actualizar(id, @Valid @RequestBody ActualizarGrupoRequest)`;
  `@DeleteMapping("/{id}")` + `@ResponseStatus(NO_CONTENT)` → `eliminar(id)`.
  Verificar con `mvnw.cmd compile`.
- [x] 7.2 En `controller/GrupoController.java` añadir los endpoints de miembros:
  `@PostMapping("/{id}/miembros")` + `@ResponseStatus(CREATED)` →
  `agregarMiembro(id, @Valid @RequestBody AgregarMiembroRequest)`;
  `@DeleteMapping("/{id}/miembros/{participanteId}")` +
  `@ResponseStatus(NO_CONTENT)` → `quitarMiembro(id, participanteId)`.
  Verificar con `mvnw.cmd compile`.
- [ ] 7.3 Confirmar que `/api/grupos/**` queda cubierto por
  `anyRequest().authenticated()` sin tocar `SecurityConfig`: arrancar la app y
  comprobar que `GET /api/grupos` sin token devuelve `401` con el formato de error
  estándar.

## 8. Pruebas de grupo

- [ ] 8.1 Crear `src/test/java/com/cuentasclaras/backend/grupos/GrupoControllerTest.java`
  siguiendo el patrón de `gestiongeneral/ParticipanteControllerTest`
  (`@SpringBootTest`, `@Transactional`, `@TestPropertySource` con `jwt.secret`,
  `MockMvcBuilders` + `springSecurity()`, usuarios registrados vía
  `POST /api/auth/register` con sufijo `System.nanoTime()`), con los casos de
  creación: datos válidos → `201` con `creador` correcto y `miembros` de un solo
  elemento; sin `descripcion` → `201` con `descripcion` nula; `nombre` vacío o en
  blanco → `400` con formato de error estándar. Verificar con `mvnw.cmd test`.
- [ ] 8.2 Añadir a `GrupoControllerTest` los casos de listado: usuario miembro de
  dos grupos → `200` con ambos y campos `id`, `nombre`, `descripcion`, `creador`;
  usuario sin grupos → `200` con `[]`; los grupos de otro usuario no aparecen en
  la lista propia. Verificar con `mvnw.cmd test`.
- [ ] 8.3 Añadir a `GrupoControllerTest` los casos de detalle: miembro → `200` con
  `nombre`, `descripcion`, `creador` y `miembros` (incluido el creador) y sin
  ningún campo de contraseña; no miembro → `403`; id inexistente → `404`. Ambos
  errores con formato de error estándar. Verificar con `mvnw.cmd test`.
- [ ] 8.4 Añadir a `GrupoControllerTest` los casos de edición: creador con datos
  válidos → `200` con los valores actualizados y `miembros`/`creador` intactos;
  miembro no creador → `403` y grupo sin cambios; no miembro → `403`; `nombre`
  vacío → `400`; id inexistente → `404`. Verificar con `mvnw.cmd test`.
- [ ] 8.5 Añadir a `GrupoControllerTest` los casos de eliminación: creador →
  `204` sin cuerpo y `GET /api/grupos/{id}` posterior → `404`; miembro no creador
  → `403` y el grupo sigue existiendo; id inexistente → `404`. Verificar con
  `mvnw.cmd test`.
- [ ] 8.6 Añadir a `GrupoControllerTest` el caso de autenticación: `POST`, `GET`,
  `PUT` y `DELETE` sobre `/api/grupos` sin token válido devuelven `401` con el
  formato de error estándar. Verificar con `mvnw.cmd test`.

## 9. Pruebas de miembros

- [ ] 9.1 Crear `src/test/java/com/cuentasclaras/backend/grupos/GrupoMiembrosControllerTest.java`
  con el mismo montaje que 8.1 y los casos de alta: creador agrega un participante
  nuevo → `201` con la lista de miembros actualizada que lo incluye, y ese
  participante ve el grupo en su `GET /api/grupos`; participante ya miembro →
  `409` sin cambiar la membresía; `participanteId` inexistente → `404`; cuerpo sin
  `participanteId` → `400`; miembro no creador → `403`. Verificar con
  `mvnw.cmd test`.
- [ ] 9.2 Añadir a `GrupoMiembrosControllerTest` los casos de baja: creador quita a
  otro miembro → `204` sin cuerpo, el participante desaparece de los miembros del
  grupo y deja de ver el grupo en su `GET /api/grupos`; participante que no es
  miembro → `404`. Verificar con `mvnw.cmd test`.
- [ ] 9.3 Añadir a `GrupoMiembrosControllerTest` los casos de autorización de la
  baja: creador con su propio `participanteId` → `400` y membresía intacta;
  miembro no creador quitando a otro → `403`; miembro no creador intentando
  quitarse a sí mismo (abandonar el grupo) → `403` y sigue siendo miembro.
  Verificar con `mvnw.cmd test`.

## 10. Verificación final

- [ ] 10.1 Ejecutar `mvnw.cmd test` y comprobar que toda la suite pasa (incluidos
  los tests preexistentes de auth, perfil, seguridad y gestión general) y que la
  app compila sin errores.
- [ ] 10.2 Marcar los criterios de aceptación del spec delta recorriendo los siete
  endpoints con la app levantada y un token real, confirmando además que Hibernate
  no generó cambios de esquema al arrancar.
