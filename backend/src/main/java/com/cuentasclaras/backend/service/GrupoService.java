package com.cuentasclaras.backend.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cuentasclaras.backend.dto.request.ActualizarGrupoRequest;
import com.cuentasclaras.backend.dto.request.AgregarMiembroRequest;
import com.cuentasclaras.backend.dto.request.CrearGrupoRequest;
import com.cuentasclaras.backend.dto.request.TransferirCreadorRequest;
import com.cuentasclaras.backend.dto.response.GrupoResponse;
import com.cuentasclaras.backend.dto.response.GrupoResumenDto;
import com.cuentasclaras.backend.dto.response.ParticipanteDto;
import com.cuentasclaras.backend.entity.Gasto;
import com.cuentasclaras.backend.entity.Grupo;
import com.cuentasclaras.backend.entity.GrupoParticipante;
import com.cuentasclaras.backend.entity.GrupoParticipanteId;
import com.cuentasclaras.backend.entity.Participante;
import com.cuentasclaras.backend.entity.Usuario;
import com.cuentasclaras.backend.exception.BadRequestException;
import com.cuentasclaras.backend.exception.ConflictException;
import com.cuentasclaras.backend.exception.ForbiddenOperationException;
import com.cuentasclaras.backend.exception.ResourceNotFoundException;
import com.cuentasclaras.backend.repository.GastoParticipanteRepository;
import com.cuentasclaras.backend.repository.GastoRepository;
import com.cuentasclaras.backend.repository.GrupoParticipanteRepository;
import com.cuentasclaras.backend.repository.GrupoRepository;
import com.cuentasclaras.backend.repository.PagoRepository;
import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;

@Service
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final GrupoParticipanteRepository grupoParticipanteRepository;
    private final GastoRepository gastoRepository;
    private final GastoParticipanteRepository gastoParticipanteRepository;
    private final PagoRepository pagoRepository;
    private final ParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final BajaService bajaService;
    private final BalanceService balanceService;

    public GrupoService(
            GrupoRepository grupoRepository,
            GrupoParticipanteRepository grupoParticipanteRepository,
            GastoRepository gastoRepository,
            GastoParticipanteRepository gastoParticipanteRepository,
            PagoRepository pagoRepository,
            ParticipanteRepository participanteRepository,
            UsuarioRepository usuarioRepository,
            BajaService bajaService,
            BalanceService balanceService) {
        this.bajaService = bajaService;
        this.balanceService = balanceService;
        this.grupoRepository = grupoRepository;
        this.grupoParticipanteRepository = grupoParticipanteRepository;
        this.gastoRepository = gastoRepository;
        this.gastoParticipanteRepository = gastoParticipanteRepository;
        this.pagoRepository = pagoRepository;
        this.participanteRepository = participanteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public GrupoResponse crear(CrearGrupoRequest req) {
        Participante creador = participanteActual();

        Grupo grupo = Grupo.builder()
                .nombre(req.nombre())
                .descripcion(req.descripcion())
                .creador(creador)
                .build();
        grupo.getMiembros().add(nuevaMembresia(grupo, creador));

        return toResponse(grupoRepository.save(grupo));
    }

    @Transactional(readOnly = true)
    public List<GrupoResumenDto> listarMisGrupos() {
        Participante solicitante = participanteActual();
        return grupoRepository.findByMiembrosParticipanteId(solicitante.getId())
                .stream()
                .map(this::toResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public GrupoResponse obtenerDetalle(Long grupoId) {
        return toResponse(grupoDondeEsMiembro(grupoId, participanteActual()));
    }

    @Transactional
    public GrupoResponse actualizar(Long grupoId, ActualizarGrupoRequest req) {
        Grupo grupo = grupoDondeEsCreador(grupoId, participanteActual());
        grupo.setNombre(req.nombre());
        grupo.setDescripcion(req.descripcion());
        return toResponse(grupoRepository.save(grupo));
    }

    /**
     * Borra el grupo con todo su historial. El orden importa: primero la división
     * de cada gasto, después los gastos y los pagos, y recién entonces el grupo,
     * porque `gastos.grupo_id` y `pagos.grupo_id` son claves foráneas NOT NULL que
     * bloquearían el DELETE de la fila del grupo. Las membresías se van por la
     * cascada JPA sobre `miembros`. Todo dentro de la misma transacción: o se borra
     * todo o no se borra nada.
     */
    @Transactional
    public void eliminar(Long grupoId) {
        Grupo grupo = grupoDondeEsCreador(grupoId, participanteActual());

        for (Gasto gasto : gastoRepository.findByGrupoIdOrderByFechaDesc(grupoId)) {
            gastoParticipanteRepository.deleteByGastoId(gasto.getId());
        }
        gastoParticipanteRepository.flush();

        gastoRepository.deleteByGrupoId(grupoId);
        pagoRepository.deleteByGrupoId(grupoId);
        gastoRepository.flush();
        pagoRepository.flush();

        bajaService.eliminarDelGrupo(grupoId);

        grupoRepository.delete(grupo);
    }

    @Transactional
    public GrupoResponse agregarMiembro(Long grupoId, AgregarMiembroRequest req) {
        Grupo grupo = grupoDondeEsCreador(grupoId, participanteActual());

        Participante nuevo = participanteRepository.findById(req.participanteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Participante no encontrado: " + req.participanteId()));

        grupoParticipanteRepository
                .findByGrupoIdAndParticipanteId(grupo.getId(), nuevo.getId())
                .ifPresent(existente -> {
                    throw new ConflictException("El participante ya es miembro del grupo");
                });

        grupo.getMiembros().add(nuevaMembresia(grupo, nuevo));
        return toResponse(grupoRepository.save(grupo));
    }

    /**
     * Transfiere el rol de creador a otro miembro del grupo. El creador saliente
     * conserva su membresía: solo pierde los privilegios.
     */
    @Transactional
    public GrupoResponse transferirCreador(Long grupoId, TransferirCreadorRequest req) {
        Grupo grupo = grupoDondeEsCreador(grupoId, participanteActual());

        if (grupo.getCreador().getId().equals(req.participanteId())) {
            throw new BadRequestException("El participante ya es el creador del grupo");
        }

        Participante nuevoCreador = grupoParticipanteRepository
                .findByGrupoIdAndParticipanteId(grupoId, req.participanteId())
                .map(GrupoParticipante::getParticipante)
                .orElseThrow(() -> new BadRequestException(
                        "El nuevo creador debe ser miembro del grupo"));

        grupo.setCreador(nuevoCreador);
        return toResponse(grupoRepository.save(grupo));
    }

    /**
     * Cubre dos operaciones sobre la misma ruta: expulsar a otro (reservado al
     * creador) y abandonar el grupo (cualquier miembro que no sea el creador). El
     * orden de las guardas importa: primero se establece que el solicitante
     * pertenece al grupo, y recién después se distingue de cuál de las dos se trata.
     */
    @Transactional
    public void quitarMiembro(Long grupoId, Long participanteId) {
        Participante solicitante = participanteActual();
        Grupo grupo = grupoDondeEsMiembro(grupoId, solicitante);

        if (grupo.getCreador() != null && grupo.getCreador().getId().equals(participanteId)) {
            throw new BadRequestException(
                    "El creador no puede salir del grupo: primero transfiere el rol "
                            + "a otro miembro, o elimina el grupo");
        }

        boolean seQuitaASiMismo = solicitante.getId().equals(participanteId);
        if (!seQuitaASiMismo && !esCreador(grupo, solicitante)) {
            throw new ForbiddenOperationException(
                    "Solo el creador del grupo puede quitar a otro miembro");
        }

        GrupoParticipante membresia = grupoParticipanteRepository
                .findByGrupoIdAndParticipanteId(grupoId, participanteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El participante no es miembro del grupo"));

        // Se quita también de la colección en memoria para que el agregado quede
        // coherente dentro de la transacción, pero el borrado real de la fila lo
        // hace el repositorio: `orphanRemoval` sobre este bag no programa el DELETE
        // cuando la entidad ya está gestionada y se pasa por `save()`/`merge()`.
        Participante saliente = membresia.getParticipante();
        grupo.getMiembros().removeIf(m -> m.getParticipante().getId().equals(participanteId));
        grupoParticipanteRepository.delete(membresia);
        grupoParticipanteRepository.flush();

        // Si se va debiendo (o dejando saldo a favor), el grupo tiene una decisión
        // pendiente: hacerse cargo entre todos, o dejarlo como tema abierto. El saldo
        // se lee DESPUÉS de sacar la membresía, que es el estado del que se fue.
        bajaService.registrar(grupo, saliente, balanceService.saldoDe(grupoId, participanteId));
    }

    private GrupoParticipante nuevaMembresia(Grupo grupo, Participante participante) {
        GrupoParticipante membresia = new GrupoParticipante();
        membresia.setId(new GrupoParticipanteId(grupo.getId(), participante.getId()));
        membresia.setGrupo(grupo);
        membresia.setParticipante(participante);
        return membresia;
    }

    private Participante participanteActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("No hay un usuario autenticado");
        }
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado: " + username));
        return participanteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario no tiene un participante vinculado"));
    }

    private Grupo grupoDondeEsMiembro(Long grupoId, Participante solicitante) {
        Grupo grupo = grupoPorId(grupoId);
        if (grupoParticipanteRepository
                .findByGrupoIdAndParticipanteId(grupoId, solicitante.getId())
                .isEmpty()) {
            throw new ForbiddenOperationException("No eres miembro de este grupo");
        }
        return grupo;
    }

    private Grupo grupoDondeEsCreador(Long grupoId, Participante solicitante) {
        Grupo grupo = grupoPorId(grupoId);
        if (!esCreador(grupo, solicitante)) {
            throw new ForbiddenOperationException(
                    "Solo el creador del grupo puede realizar esta operación");
        }
        return grupo;
    }

    private boolean esCreador(Grupo grupo, Participante participante) {
        Participante creador = grupo.getCreador();
        return creador != null && creador.getId().equals(participante.getId());
    }

    private Grupo grupoPorId(Long grupoId) {
        return grupoRepository.findById(grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + grupoId));
    }

    private GrupoResumenDto toResumen(Grupo grupo) {
        return new GrupoResumenDto(
                grupo.getId(),
                grupo.getNombre(),
                grupo.getDescripcion(),
                toParticipanteDto(grupo.getCreador()));
    }

    private GrupoResponse toResponse(Grupo grupo) {
        List<ParticipanteDto> miembros = grupo.getMiembros().stream()
                .map(m -> toParticipanteDto(m.getParticipante()))
                .toList();
        return new GrupoResponse(
                grupo.getId(),
                grupo.getNombre(),
                grupo.getDescripcion(),
                toParticipanteDto(grupo.getCreador()),
                miembros);
    }

    private ParticipanteDto toParticipanteDto(Participante p) {
        if (p == null) {
            return null;
        }
        return new ParticipanteDto(
                p.getId(),
                p.getNombre(),
                p.getApellido(),
                p.getCi(),
                p.getUsuario().getUsername());
    }
}
