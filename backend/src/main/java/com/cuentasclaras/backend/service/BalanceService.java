package com.cuentasclaras.backend.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cuentasclaras.backend.dto.response.BalanceDto;
import com.cuentasclaras.backend.dto.response.ParticipanteDto;
import com.cuentasclaras.backend.dto.response.TransferenciaDto;
import com.cuentasclaras.backend.entity.Gasto;
import com.cuentasclaras.backend.entity.GastoParticipante;
import com.cuentasclaras.backend.entity.Grupo;
import com.cuentasclaras.backend.entity.GrupoParticipante;
import com.cuentasclaras.backend.entity.Participante;
import com.cuentasclaras.backend.entity.Usuario;
import com.cuentasclaras.backend.exception.ForbiddenOperationException;
import com.cuentasclaras.backend.exception.ResourceNotFoundException;
import com.cuentasclaras.backend.repository.GastoParticipanteRepository;
import com.cuentasclaras.backend.repository.GastoRepository;
import com.cuentasclaras.backend.repository.GrupoParticipanteRepository;
import com.cuentasclaras.backend.repository.GrupoRepository;
import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;
import com.cuentasclaras.backend.util.BalanceUtil;
import com.cuentasclaras.backend.util.BalanceUtil.Movimiento;

@Service
public class BalanceService {

    private final GastoRepository gastoRepository;
    private final GastoParticipanteRepository gastoParticipanteRepository;
    private final GrupoRepository grupoRepository;
    private final GrupoParticipanteRepository grupoParticipanteRepository;
    private final ParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;

    public BalanceService(
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

    @Transactional(readOnly = true)
    public List<BalanceDto> calcularBalances(Long grupoId) {
        grupoDondeEsMiembro(grupoId, participanteActual());
        Contexto ctx = cargarContexto(grupoId);

        Map<Long, BigDecimal> balances = BalanceUtil.calcularBalances(
                ctx.participantes().keySet(), ctx.pagadoPorId(), ctx.adeudadoPorId());

        return balances.entrySet().stream()
                .map(e -> new BalanceDto(
                        toParticipanteDto(ctx.participantes().get(e.getKey())), e.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransferenciaDto> calcularLiquidacion(Long grupoId) {
        grupoDondeEsMiembro(grupoId, participanteActual());
        Contexto ctx = cargarContexto(grupoId);

        Map<Long, BigDecimal> balances = BalanceUtil.calcularBalances(
                ctx.participantes().keySet(), ctx.pagadoPorId(), ctx.adeudadoPorId());

        return BalanceUtil.minimizarTransferencias(balances).stream()
                .map(m -> new TransferenciaDto(
                        nombreDe(ctx, m.deId()), m.deId(),
                        nombreDe(ctx, m.paraId()), m.paraId(),
                        m.monto()))
                .toList();
    }

    // --- Carga del contexto de cálculo ---------------------------------

    private Contexto cargarContexto(Long grupoId) {
        Map<Long, Participante> participantes = new HashMap<>();
        Map<Long, BigDecimal> pagado = new HashMap<>();
        Map<Long, BigDecimal> adeudado = new HashMap<>();

        for (GrupoParticipante gp : grupoParticipanteRepository.findByGrupoId(grupoId)) {
            Participante p = gp.getParticipante();
            participantes.putIfAbsent(p.getId(), p);
        }

        for (Gasto gasto : gastoRepository.findByGrupoIdOrderByFechaDesc(grupoId)) {
            Participante pagador = gasto.getPagador();
            if (pagador != null) {
                participantes.putIfAbsent(pagador.getId(), pagador);
                pagado.merge(pagador.getId(), gasto.getMonto(), BigDecimal::add);
            }
            for (GastoParticipante fila : gastoParticipanteRepository.findByGastoId(gasto.getId())) {
                Participante p = fila.getParticipante();
                participantes.putIfAbsent(p.getId(), p);
                adeudado.merge(p.getId(), fila.getMontoAdeudado(), BigDecimal::add);
            }
        }

        return new Contexto(participantes, pagado, adeudado);
    }

    private record Contexto(
            Map<Long, Participante> participantes,
            Map<Long, BigDecimal> pagadoPorId,
            Map<Long, BigDecimal> adeudadoPorId) {
    }

    private String nombreDe(Contexto ctx, Long participanteId) {
        return ctx.participantes().get(participanteId).getNombre();
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

    private ParticipanteDto toParticipanteDto(Participante p) {
        return new ParticipanteDto(
                p.getId(),
                p.getNombre(),
                p.getApellido(),
                p.getCi(),
                p.getUsuario().getUsername());
    }
}
