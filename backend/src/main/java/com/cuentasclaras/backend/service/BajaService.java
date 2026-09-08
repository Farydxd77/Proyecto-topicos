package com.cuentasclaras.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cuentasclaras.backend.dto.request.ResolverBajaRequest;
import com.cuentasclaras.backend.dto.response.BajaGrupoDto;
import com.cuentasclaras.backend.dto.response.BajaParticipanteDto;
import com.cuentasclaras.backend.dto.response.ParticipanteDto;
import com.cuentasclaras.backend.entity.BajaGrupo;
import com.cuentasclaras.backend.entity.BajaParticipante;
import com.cuentasclaras.backend.entity.BajaParticipanteId;
import com.cuentasclaras.backend.entity.EstadoBaja;
import com.cuentasclaras.backend.entity.Grupo;
import com.cuentasclaras.backend.entity.GrupoParticipante;
import com.cuentasclaras.backend.entity.Participante;
import com.cuentasclaras.backend.entity.Usuario;
import com.cuentasclaras.backend.exception.BadRequestException;
import com.cuentasclaras.backend.exception.ConflictException;
import com.cuentasclaras.backend.exception.ForbiddenOperationException;
import com.cuentasclaras.backend.exception.ResourceNotFoundException;
import com.cuentasclaras.backend.repository.BajaGrupoRepository;
import com.cuentasclaras.backend.repository.BajaParticipanteRepository;
import com.cuentasclaras.backend.repository.GrupoParticipanteRepository;
import com.cuentasclaras.backend.repository.GrupoRepository;
import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;

/**
 * Bajas de participantes con saldo pendiente y la decisión del grupo sobre esa
 * deuda: o se hacen cargo entre todos, o queda como un tema abierto.
 */
@Service
public class BajaService {

    private final BajaGrupoRepository bajaRepository;
    private final BajaParticipanteRepository bajaParticipanteRepository;
    private final GrupoRepository grupoRepository;
    private final GrupoParticipanteRepository grupoParticipanteRepository;
    private final ParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;

    public BajaService(
            BajaGrupoRepository bajaRepository,
            BajaParticipanteRepository bajaParticipanteRepository,
            GrupoRepository grupoRepository,
            GrupoParticipanteRepository grupoParticipanteRepository,
            ParticipanteRepository participanteRepository,
            UsuarioRepository usuarioRepository) {
        this.bajaRepository = bajaRepository;
        this.bajaParticipanteRepository = bajaParticipanteRepository;
        this.grupoRepository = grupoRepository;
        this.grupoParticipanteRepository = grupoParticipanteRepository;
        this.participanteRepository = participanteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra la baja de quien acaba de salir del grupo, si dejó saldo.
     *
     * <p>Lo llama {@code GrupoService} dentro de su propia transacción, después de
     * haber autorizado la operación: por eso acá no hay guardas propias.
     */
    @Transactional
    public void registrar(Grupo grupo, Participante saliente, BigDecimal saldo) {
        if (saldo.signum() == 0) {
            // Salió a mano: no hay nada que decidir.
            return;
        }
        bajaRepository.save(BajaGrupo.builder()
                .grupo(grupo)
                .participante(saliente)
                .saldo(saldo.setScale(2, RoundingMode.HALF_UP))
                .estado(EstadoBaja.PENDIENTE)
                .fecha(LocalDate.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<BajaGrupoDto> listar(Long grupoId) {
        grupoDondeEsMiembro(grupoId, participanteActual());
        return bajaRepository.findByGrupoIdOrderByFechaDesc(grupoId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * El creador decide si el grupo asume la deuda. Asumirla reparte el saldo en
     * partes iguales entre los miembros actuales y congela ese reparto; no asumirla
     * no toca ningún balance, solo cierra la decisión.
     */
    @Transactional
    public BajaGrupoDto resolver(Long grupoId, Long bajaId, ResolverBajaRequest req) {
        grupoDondeEsCreador(grupoId, participanteActual());
        BajaGrupo baja = bajaRepository.findByIdAndGrupoId(bajaId, grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Baja no encontrada: " + bajaId));

        if (baja.getEstado() != EstadoBaja.PENDIENTE) {
            // No es un dato inválido: es una operación correcta sobre un recurso que
            // ya está en otro estado.
            throw new ConflictException("Esta baja ya fue resuelta");
        }

        if (!req.asumir()) {
            baja.setEstado(EstadoBaja.NO_ASUMIDA);
            return toResponse(bajaRepository.save(baja));
        }

        List<Participante> absorbentes = grupoParticipanteRepository.findByGrupoId(grupoId).stream()
                .map(GrupoParticipante::getParticipante)
                .toList();
        if (absorbentes.isEmpty()) {
            throw new BadRequestException(
                    "No queda ningún miembro en el grupo que pueda asumir la deuda");
        }

        baja.setEstado(EstadoBaja.ASUMIDA);
        baja = bajaRepository.save(baja);
        bajaParticipanteRepository.saveAll(repartir(baja, absorbentes));

        return toResponse(baja);
    }

    /** Borra las bajas del grupo con sus repartos. Lo usa el borrado de un grupo. */
    @Transactional
    public void eliminarDelGrupo(Long grupoId) {
        for (BajaGrupo baja : bajaRepository.findByGrupoIdOrderByFechaDesc(grupoId)) {
            bajaParticipanteRepository.deleteByBajaId(baja.getId());
        }
        bajaParticipanteRepository.flush();
        bajaRepository.deleteByGrupoId(grupoId);
        bajaRepository.flush();
    }

    /**
     * Reparte el saldo en partes iguales. El último absorbe la diferencia, con la
     * misma técnica que la división de un gasto: calcular el resto por resta es lo
     * que garantiza que la suma sea exactamente el saldo, sin importar el redondeo.
     */
    private List<BajaParticipante> repartir(BajaGrupo baja, List<Participante> absorbentes) {
        int n = absorbentes.size();
        BigDecimal saldo = baja.getSaldo();
        BigDecimal porCabeza = saldo.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);

        List<BajaParticipante> reparto = new ArrayList<>(n);
        BigDecimal repartido = BigDecimal.ZERO;

        for (int i = 0; i < n; i++) {
            Participante p = absorbentes.get(i);
            boolean ultimo = i == n - 1;
            BigDecimal monto = ultimo ? saldo.subtract(repartido) : porCabeza;
            if (!ultimo) {
                repartido = repartido.add(porCabeza);
            }

            BajaParticipante fila = new BajaParticipante();
            fila.setId(new BajaParticipanteId(baja.getId(), p.getId()));
            fila.setBaja(baja);
            fila.setParticipante(p);
            fila.setMonto(monto);
            reparto.add(fila);
        }
        return reparto;
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

    private Grupo grupoPorId(Long grupoId) {
        return grupoRepository.findById(grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + grupoId));
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
                    "Solo el creador del grupo puede resolver una baja");
        }
        return grupo;
    }

    // --- Mapeo a DTO ---------------------------------------------------

    private BajaGrupoDto toResponse(BajaGrupo baja) {
        List<BajaParticipanteDto> reparto = baja.getEstado() == EstadoBaja.ASUMIDA
                ? bajaParticipanteRepository.findByBajaId(baja.getId()).stream()
                        .map(bp -> new BajaParticipanteDto(
                                toParticipanteDto(bp.getParticipante()), bp.getMonto()))
                        .toList()
                : List.of();

        return new BajaGrupoDto(
                baja.getId(),
                baja.getGrupo().getId(),
                toParticipanteDto(baja.getParticipante()),
                baja.getSaldo(),
                baja.getEstado().name(),
                baja.getFecha(),
                reparto);
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
