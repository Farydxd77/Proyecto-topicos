package com.cuentasclaras.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cuentasclaras.backend.dto.request.ActualizarPagoRequest;
import com.cuentasclaras.backend.dto.request.RegistrarPagoRequest;
import com.cuentasclaras.backend.dto.response.PagoResponse;
import com.cuentasclaras.backend.dto.response.ParticipanteDto;
import com.cuentasclaras.backend.entity.Grupo;
import com.cuentasclaras.backend.entity.GrupoParticipante;
import com.cuentasclaras.backend.entity.Pago;
import com.cuentasclaras.backend.entity.Participante;
import com.cuentasclaras.backend.entity.Usuario;
import com.cuentasclaras.backend.exception.BadRequestException;
import com.cuentasclaras.backend.exception.ForbiddenOperationException;
import com.cuentasclaras.backend.exception.ResourceNotFoundException;
import com.cuentasclaras.backend.repository.GrupoParticipanteRepository;
import com.cuentasclaras.backend.repository.GrupoRepository;
import com.cuentasclaras.backend.repository.PagoRepository;
import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final GrupoRepository grupoRepository;
    private final GrupoParticipanteRepository grupoParticipanteRepository;
    private final ParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;

    public PagoService(
            PagoRepository pagoRepository,
            GrupoRepository grupoRepository,
            GrupoParticipanteRepository grupoParticipanteRepository,
            ParticipanteRepository participanteRepository,
            UsuarioRepository usuarioRepository) {
        this.pagoRepository = pagoRepository;
        this.grupoRepository = grupoRepository;
        this.grupoParticipanteRepository = grupoParticipanteRepository;
        this.participanteRepository = participanteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public PagoResponse registrar(Long grupoId, RegistrarPagoRequest req) {
        Participante pagador = participanteActual();
        Grupo grupo = grupoDondeEsMiembro(grupoId, pagador);
        Participante receptor = receptorMiembro(grupoId, req.receptorId());
        validarDistintos(pagador.getId(), receptor.getId());

        Pago pago = Pago.builder()
                .grupo(grupo)
                .pagador(pagador)
                .receptor(receptor)
                .monto(req.monto().setScale(2, RoundingMode.HALF_UP))
                .fecha(req.fecha())
                .txId(req.txId())
                .build();
        pago = pagoRepository.save(pago);

        return toResponse(pago);
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> listar(Long grupoId) {
        grupoDondeEsMiembro(grupoId, participanteActual());
        return pagoRepository.findByGrupoIdOrderByFechaDesc(grupoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagoResponse obtenerDetalle(Long grupoId, Long pagoId) {
        grupoDondeEsMiembro(grupoId, participanteActual());
        return toResponse(pagoDelGrupo(grupoId, pagoId));
    }

    @Transactional
    public PagoResponse actualizar(Long grupoId, Long pagoId, ActualizarPagoRequest req) {
        Participante solicitante = participanteActual();
        grupoDondeEsMiembro(grupoId, solicitante);
        Pago pago = pagoDelGrupo(grupoId, pagoId);
        exigirPagador(pago, solicitante);

        Participante receptor = receptorMiembro(grupoId, req.receptorId());
        validarDistintos(pago.getPagador().getId(), receptor.getId());

        pago.setReceptor(receptor);
        pago.setMonto(req.monto().setScale(2, RoundingMode.HALF_UP));
        pago.setFecha(req.fecha());
        pago.setTxId(req.txId());
        pago = pagoRepository.save(pago);

        return toResponse(pago);
    }

    @Transactional
    public void eliminar(Long grupoId, Long pagoId) {
        Participante solicitante = participanteActual();
        grupoDondeEsMiembro(grupoId, solicitante);
        Pago pago = pagoDelGrupo(grupoId, pagoId);
        exigirPagador(pago, solicitante);
        pagoRepository.delete(pago);
    }

    // --- Guardas y resolución de identidad ----------------------------

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
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + grupoId));
        if (grupoParticipanteRepository
                .findByGrupoIdAndParticipanteId(grupoId, solicitante.getId())
                .isEmpty()) {
            throw new ForbiddenOperationException("No eres miembro de este grupo");
        }
        return grupo;
    }

    private Pago pagoDelGrupo(Long grupoId, Long pagoId) {
        return pagoRepository.findByIdAndGrupoId(pagoId, grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado: " + pagoId));
    }

    private Participante receptorMiembro(Long grupoId, Long receptorId) {
        return grupoParticipanteRepository
                .findByGrupoIdAndParticipanteId(grupoId, receptorId)
                .map(GrupoParticipante::getParticipante)
                .orElseThrow(() -> new BadRequestException("El receptor no es miembro del grupo"));
    }

    private void validarDistintos(Long pagadorId, Long receptorId) {
        if (pagadorId.equals(receptorId)) {
            throw new BadRequestException("El pagador y el receptor no pueden ser la misma persona");
        }
    }

    private void exigirPagador(Pago pago, Participante solicitante) {
        if (!pago.getPagador().getId().equals(solicitante.getId())) {
            throw new ForbiddenOperationException("Solo quien registró el pago puede modificarlo");
        }
    }

    // --- Mapeo a DTO ------------------------------------------------

    private PagoResponse toResponse(Pago p) {
        return new PagoResponse(
                p.getId(),
                p.getGrupo().getId(),
                toParticipanteDto(p.getPagador()),
                toParticipanteDto(p.getReceptor()),
                p.getMonto(),
                p.getFecha(),
                p.getTxId());
    }

    private ParticipanteDto toParticipanteDto(Participante p) {
        return new ParticipanteDto(
                p.getId(),
                p.getNombre(),
                p.getApellido(),
                p.getCi(),
                p.getUsuario().getUsername());
    }
}
