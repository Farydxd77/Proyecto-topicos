package com.cuentasclaras.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cuentasclaras.backend.dto.request.ActualizarGastoRequest;
import com.cuentasclaras.backend.dto.request.RegistrarGastoRequest;
import com.cuentasclaras.backend.dto.response.GastoParticipanteDto;
import com.cuentasclaras.backend.dto.response.GastoResponse;
import com.cuentasclaras.backend.dto.response.GastoResumenDto;
import com.cuentasclaras.backend.dto.response.ParticipanteDto;
import com.cuentasclaras.backend.entity.Gasto;
import com.cuentasclaras.backend.entity.GastoParticipante;
import com.cuentasclaras.backend.entity.GastoParticipanteId;
import com.cuentasclaras.backend.entity.Grupo;
import com.cuentasclaras.backend.entity.GrupoParticipante;
import com.cuentasclaras.backend.entity.Participante;
import com.cuentasclaras.backend.entity.Usuario;
import com.cuentasclaras.backend.exception.BadRequestException;
import com.cuentasclaras.backend.exception.ForbiddenOperationException;
import com.cuentasclaras.backend.exception.ResourceNotFoundException;
import com.cuentasclaras.backend.repository.GastoParticipanteRepository;
import com.cuentasclaras.backend.repository.GastoRepository;
import com.cuentasclaras.backend.repository.GrupoParticipanteRepository;
import com.cuentasclaras.backend.repository.GrupoRepository;
import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;

@Service
public class GastoService {

    private final GastoRepository gastoRepository;
    private final GastoParticipanteRepository gastoParticipanteRepository;
    private final GrupoRepository grupoRepository;
    private final GrupoParticipanteRepository grupoParticipanteRepository;
    private final ParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;

    public GastoService(
            GastoRepository gastoRepository,
            GastoParticipanteRepository gastoParticipanteRepository,
            GrupoRepository grupoRepository,
            GrupoParticipanteRepository grupoParticipanteRepository,
            ParticipanteRepository participanteRepository,
            UsuarioRepository usuarioRepository) {
        this.gastoRepository = gastoRepository;
        this.gastoParticipanteRepository = gastoParticipanteRepository;
        this.grupoRepository = grupoRepository;
        this.grupoParticipanteRepository = grupoParticipanteRepository;
        this.participanteRepository = participanteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public GastoResponse registrar(Long grupoId, RegistrarGastoRequest req) {
        Grupo grupo = grupoDondeEsMiembro(grupoId, participanteActual());
        Participante pagador = pagadorMiembro(grupoId, req.pagadorId());

        Gasto gasto = Gasto.builder()
                .grupo(grupo)
                .descripcion(req.descripcion())
                .monto(req.monto())
                .pagador(pagador)
                .fecha(req.fecha())
                .build();
        gasto = gastoRepository.save(gasto);

        List<GastoParticipante> division = calcularDivision(
                gasto, req.monto(), miembrosActuales(grupoId), pagador);
        gastoParticipanteRepository.saveAll(division);

        return toResponse(gasto, division);
    }

    @Transactional(readOnly = true)
    public List<GastoResumenDto> listar(Long grupoId) {
        grupoDondeEsMiembro(grupoId, participanteActual());
        return gastoRepository.findByGrupoIdOrderByFechaDesc(grupoId).stream()
                .map(this::toResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public GastoResponse obtenerDetalle(Long grupoId, Long gastoId) {
        grupoDondeEsMiembro(grupoId, participanteActual());
        Gasto gasto = gastoDelGrupo(grupoId, gastoId);
        return toResponse(gasto, gastoParticipanteRepository.findByGastoId(gastoId));
    }

    @Transactional
    public GastoResponse actualizar(Long grupoId, Long gastoId, ActualizarGastoRequest req) {
        grupoDondeEsMiembro(grupoId, participanteActual());
        Gasto gasto = gastoDelGrupo(grupoId, gastoId);
        Participante pagador = pagadorMiembro(grupoId, req.pagadorId());

        gasto.setDescripcion(req.descripcion());
        gasto.setMonto(req.monto());
        gasto.setPagador(pagador);
        gasto.setFecha(req.fecha());
        gasto = gastoRepository.save(gasto);

        // Descarta la división anterior antes de recalcular. El flush es
        // obligatorio: sin él, la reinserción de un miembro que permanece
        // chocaría con la PK compuesta (gasto_id, participante_id).
        gastoParticipanteRepository.deleteByGastoId(gastoId);
        gastoParticipanteRepository.flush();

        List<GastoParticipante> division = calcularDivision(
                gasto, req.monto(), miembrosActuales(grupoId), pagador);
        gastoParticipanteRepository.saveAll(division);

        return toResponse(gasto, division);
    }

    @Transactional
    public void eliminar(Long grupoId, Long gastoId) {
        grupoDondeEsMiembro(grupoId, participanteActual());
        Gasto gasto = gastoDelGrupo(grupoId, gastoId);
        gastoParticipanteRepository.deleteByGastoId(gastoId);
        gastoRepository.delete(gasto);
    }

    // --- Guardas y resolución de identidad --------------------------------

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

    private Gasto gastoDelGrupo(Long grupoId, Long gastoId) {
        return gastoRepository.findByIdAndGrupoId(gastoId, grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado: " + gastoId));
    }

    private Participante pagadorMiembro(Long grupoId, Long pagadorId) {
        return grupoParticipanteRepository
                .findByGrupoIdAndParticipanteId(grupoId, pagadorId)
                .map(GrupoParticipante::getParticipante)
                .orElseThrow(() -> new BadRequestException("El pagador no es miembro del grupo"));
    }

    private List<Participante> miembrosActuales(Long grupoId) {
        return grupoParticipanteRepository.findByGrupoId(grupoId).stream()
                .map(GrupoParticipante::getParticipante)
                .toList();
    }

    // --- Cálculo de la división ------------------------------------------

    private List<GastoParticipante> calcularDivision(
            Gasto gasto, BigDecimal monto, List<Participante> miembros, Participante pagador) {

        int n = miembros.size();
        BigDecimal porPersona = monto.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        BigDecimal montoPagador = monto.subtract(porPersona.multiply(BigDecimal.valueOf(n - 1L)));

        List<GastoParticipante> division = new ArrayList<>(n);
        for (Participante miembro : miembros) {
            boolean esPagador = miembro.getId().equals(pagador.getId());
            GastoParticipante fila = new GastoParticipante();
            fila.setId(new GastoParticipanteId(gasto.getId(), miembro.getId()));
            fila.setGasto(gasto);
            fila.setParticipante(miembro);
            fila.setMontoAdeudado(esPagador ? montoPagador : porPersona);
            division.add(fila);
        }
        return division;
    }

    // --- Mapeo a DTO ---------------------------------------------------

    private GastoResumenDto toResumen(Gasto g) {
        return new GastoResumenDto(
                g.getId(),
                g.getDescripcion(),
                g.getMonto(),
                toParticipanteDto(g.getPagador()),
                g.getFecha());
    }

    private GastoResponse toResponse(Gasto g, List<GastoParticipante> division) {
        List<GastoParticipanteDto> divisionDto = division.stream()
                .map(gp -> new GastoParticipanteDto(
                        toParticipanteDto(gp.getParticipante()), gp.getMontoAdeudado()))
                .toList();
        return new GastoResponse(
                g.getId(),
                g.getGrupo().getId(),
                g.getDescripcion(),
                g.getMonto(),
                toParticipanteDto(g.getPagador()),
                g.getFecha(),
                divisionDto);
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
