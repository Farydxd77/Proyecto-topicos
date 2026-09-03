package com.cuentasclaras.backend.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cuentasclaras.backend.dto.request.ActualizarGrupoRequest;
import com.cuentasclaras.backend.dto.request.AgregarMiembroRequest;
import com.cuentasclaras.backend.dto.request.CrearGrupoRequest;
import com.cuentasclaras.backend.dto.response.GrupoResponse;
import com.cuentasclaras.backend.dto.response.GrupoResumenDto;
import com.cuentasclaras.backend.dto.response.ParticipanteDto;
import com.cuentasclaras.backend.entity.Grupo;
import com.cuentasclaras.backend.entity.GrupoParticipante;
import com.cuentasclaras.backend.entity.GrupoParticipanteId;
import com.cuentasclaras.backend.entity.Participante;
import com.cuentasclaras.backend.entity.Usuario;
import com.cuentasclaras.backend.exception.BadRequestException;
import com.cuentasclaras.backend.exception.ConflictException;
import com.cuentasclaras.backend.exception.ForbiddenOperationException;
import com.cuentasclaras.backend.exception.ResourceNotFoundException;
import com.cuentasclaras.backend.repository.GrupoParticipanteRepository;
import com.cuentasclaras.backend.repository.GrupoRepository;
import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;

@Service
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final GrupoParticipanteRepository grupoParticipanteRepository;
    private final ParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;

    public GrupoService(
            GrupoRepository grupoRepository,
            GrupoParticipanteRepository grupoParticipanteRepository,
            ParticipanteRepository participanteRepository,
            UsuarioRepository usuarioRepository) {
        this.grupoRepository = grupoRepository;
        this.grupoParticipanteRepository = grupoParticipanteRepository;
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

    @Transactional
    public void eliminar(Long grupoId) {
        Grupo grupo = grupoDondeEsCreador(grupoId, participanteActual());
        // La cascada de `miembros` borra también las filas de grupo_participantes.
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

    @Transactional
    public void quitarMiembro(Long grupoId, Long participanteId) {
        Grupo grupo = grupoDondeEsCreador(grupoId, participanteActual());

        if (grupo.getCreador() != null && grupo.getCreador().getId().equals(participanteId)) {
            throw new BadRequestException("El creador no puede quitarse a sí mismo del grupo");
        }

        boolean eraMiembro = grupo.getMiembros().removeIf(
                m -> m.getParticipante().getId().equals(participanteId));
        if (!eraMiembro) {
            throw new ResourceNotFoundException("El participante no es miembro del grupo");
        }

        grupoRepository.save(grupo);
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
        Participante creador = grupo.getCreador();
        if (creador == null || !creador.getId().equals(solicitante.getId())) {
            throw new ForbiddenOperationException(
                    "Solo el creador del grupo puede realizar esta operación");
        }
        return grupo;
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
