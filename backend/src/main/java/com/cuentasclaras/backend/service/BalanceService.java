package com.cuentasclaras.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cuentasclaras.backend.dto.response.BalanceDto;
import com.cuentasclaras.backend.dto.response.ParticipanteDto;
import com.cuentasclaras.backend.dto.response.ResumenGrupoDto;
import com.cuentasclaras.backend.dto.response.TransferenciaDto;
import com.cuentasclaras.backend.entity.BajaGrupo;
import com.cuentasclaras.backend.entity.BajaParticipante;
import com.cuentasclaras.backend.entity.EstadoBaja;
import com.cuentasclaras.backend.entity.Gasto;
import com.cuentasclaras.backend.entity.GastoParticipante;
import com.cuentasclaras.backend.entity.Grupo;
import com.cuentasclaras.backend.entity.GrupoParticipante;
import com.cuentasclaras.backend.entity.Pago;
import com.cuentasclaras.backend.entity.Participante;
import com.cuentasclaras.backend.entity.Usuario;
import com.cuentasclaras.backend.exception.ForbiddenOperationException;
import com.cuentasclaras.backend.exception.ResourceNotFoundException;
import com.cuentasclaras.backend.repository.BajaGrupoRepository;
import com.cuentasclaras.backend.repository.BajaParticipanteRepository;
import com.cuentasclaras.backend.repository.GastoParticipanteRepository;
import com.cuentasclaras.backend.repository.GastoRepository;
import com.cuentasclaras.backend.repository.GrupoParticipanteRepository;
import com.cuentasclaras.backend.repository.GrupoRepository;
import com.cuentasclaras.backend.repository.PagoRepository;
import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;
import com.cuentasclaras.backend.util.BalanceUtil;
import com.cuentasclaras.backend.util.BalanceUtil.Movimiento;

@Service
public class BalanceService {

    private final GastoRepository gastoRepository;
    private final GastoParticipanteRepository gastoParticipanteRepository;
    private final PagoRepository pagoRepository;
    private final BajaGrupoRepository bajaRepository;
    private final BajaParticipanteRepository bajaParticipanteRepository;
    private final GrupoRepository grupoRepository;
    private final GrupoParticipanteRepository grupoParticipanteRepository;
    private final ParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;

    public BalanceService(
            GastoRepository gastoRepository,
            GastoParticipanteRepository gastoParticipanteRepository,
            PagoRepository pagoRepository,
            BajaGrupoRepository bajaRepository,
            BajaParticipanteRepository bajaParticipanteRepository,
            GrupoRepository grupoRepository,
            GrupoParticipanteRepository grupoParticipanteRepository,
            ParticipanteRepository participanteRepository,
            UsuarioRepository usuarioRepository) {
        this.gastoRepository = gastoRepository;
        this.gastoParticipanteRepository = gastoParticipanteRepository;
        this.pagoRepository = pagoRepository;
        this.bajaRepository = bajaRepository;
        this.bajaParticipanteRepository = bajaParticipanteRepository;
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
                ctx.participantes().keySet(), ctx.pagadoPorId(), ctx.adeudadoPorId(),
                ctx.pagosRealizadosPorId(), ctx.pagosRecibidosPorId(),
                ctx.ajusteBajasPorId());

        return balances.entrySet().stream()
                .map(e -> new BalanceDto(
                        toParticipanteDto(ctx.participantes().get(e.getKey())),
                        e.getValue(),
                        ctx.miembrosActuales().contains(e.getKey())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransferenciaDto> calcularLiquidacion(Long grupoId) {
        grupoDondeEsMiembro(grupoId, participanteActual());
        Contexto ctx = cargarContexto(grupoId);

        Map<Long, BigDecimal> balances = BalanceUtil.calcularBalances(
                ctx.participantes().keySet(), ctx.pagadoPorId(), ctx.adeudadoPorId(),
                ctx.pagosRealizadosPorId(), ctx.pagosRecibidosPorId(),
                ctx.ajusteBajasPorId());

        return BalanceUtil.minimizarTransferencias(balances).stream()
                .map(m -> new TransferenciaDto(
                        nombreDe(ctx, m.deId()), m.deId(),
                        nombreDe(ctx, m.paraId()), m.paraId(),
                        m.monto()))
                .toList();
    }

    /**
     * Totales agregados del grupo. Se apoya en el mismo {@code Contexto} que ya usan
     * los balances y la liquidación: no agrega ni una consulta.
     */
    @Transactional(readOnly = true)
    public ResumenGrupoDto resumen(Long grupoId) {
        Participante solicitante = participanteActual();
        grupoDondeEsMiembro(grupoId, solicitante);
        Contexto ctx = cargarContexto(grupoId);

        Map<Long, BigDecimal> balances = BalanceUtil.calcularBalances(
                ctx.participantes().keySet(), ctx.pagadoPorId(), ctx.adeudadoPorId(),
                ctx.pagosRealizadosPorId(), ctx.pagosRecibidosPorId(),
                ctx.ajusteBajasPorId());

        // Lo que todavía hay que transferir para que todos queden a mano. Se suma el
        // lado positivo —lo que alguien tiene que RECIBIR—; da lo mismo que el
        // negativo en valor absoluto porque la suma de balances es exactamente cero.
        BigDecimal pendiente = balances.values().stream()
                .filter(b -> b.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPagado = ctx.pagosRealizadosPorId().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal miParte = ctx.adeudadoPorId()
                .getOrDefault(solicitante.getId(), BigDecimal.ZERO);

        return new ResumenGrupoDto(
                dosDecimales(ctx.totalGastadoUsdt()),
                dosDecimales(totalPagado),
                dosDecimales(pendiente),
                dosDecimales(miParte),
                ctx.cantidadGastos(),
                ctx.cantidadPagos());
    }

    private BigDecimal dosDecimales(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Balance de un participante concreto, SIN guarda de autorización.
     *
     * <p>Es de uso interno: lo llama {@code GrupoService} justo después de sacar a
     * alguien del grupo, para saber con cuánto saldo se fue. Quien lo llama ya
     * autorizó la operación, así que repetir la guarda acá solo estorbaría —el
     * solicitante puede no ser el participante consultado—.
     */
    @Transactional(readOnly = true)
    public BigDecimal saldoDe(Long grupoId, Long participanteId) {
        Contexto ctx = cargarContexto(grupoId);
        Map<Long, BigDecimal> balances = BalanceUtil.calcularBalances(
                ctx.participantes().keySet(), ctx.pagadoPorId(), ctx.adeudadoPorId(),
                ctx.pagosRealizadosPorId(), ctx.pagosRecibidosPorId(),
                ctx.ajusteBajasPorId());
        return balances.getOrDefault(participanteId, BigDecimal.ZERO.setScale(2));
    }

    // --- Carga del contexto de cálculo ---------------------------------

    private Contexto cargarContexto(Long grupoId) {
        Map<Long, Participante> participantes = new HashMap<>();
        Map<Long, BigDecimal> pagado = new HashMap<>();
        Map<Long, BigDecimal> adeudado = new HashMap<>();
        Map<Long, BigDecimal> pagosRealizados = new HashMap<>();
        Map<Long, BigDecimal> pagosRecibidos = new HashMap<>();
        // Se guarda aparte de `participantes` porque ese mapa también acumula a los
        // que ya no son miembros pero conservan actividad en gastos o pagos.
        Set<Long> miembrosActuales = new HashSet<>();
        // Se acumula en la escala original (6 decimales) y se redondea una sola vez
        // al final: redondear gasto por gasto acumularía el error.
        BigDecimal totalGastadoUsdt = BigDecimal.ZERO;
        int cantidadGastos = 0;
        int cantidadPagos = 0;
        // Un solo número con signo por participante: lo que las bajas asumidas le
        // suman o le restan. Suma cero por baja, así que no rompe la invariante.
        Map<Long, BigDecimal> ajusteBajas = new HashMap<>();

        for (GrupoParticipante gp : grupoParticipanteRepository.findByGrupoId(grupoId)) {
            Participante p = gp.getParticipante();
            participantes.putIfAbsent(p.getId(), p);
            miembrosActuales.add(p.getId());
        }

        for (Gasto gasto : gastoRepository.findByGrupoIdOrderByFechaDesc(grupoId)) {
            totalGastadoUsdt = totalGastadoUsdt.add(gasto.getMontoUsdt());
            cantidadGastos++;
            Participante pagador = gasto.getPagador();
            if (pagador != null) {
                participantes.putIfAbsent(pagador.getId(), pagador);
                // El balance se lleva en USDT: lo pagado es el monto convertido
                // redondeado a 2 decimales, igual escala que `monto_adeudado`.
                BigDecimal pagadoUsdt = gasto.getMontoUsdt().setScale(2, RoundingMode.HALF_UP);
                pagado.merge(pagador.getId(), pagadoUsdt, BigDecimal::add);
            }
            for (GastoParticipante fila : gastoParticipanteRepository.findByGastoId(gasto.getId())) {
                Participante p = fila.getParticipante();
                participantes.putIfAbsent(p.getId(), p);
                adeudado.merge(p.getId(), fila.getMontoAdeudado(), BigDecimal::add);
            }
        }

        // Los pagos ya están en USDT con escala 2 (garantizado por PagoService).
        for (Pago pago : pagoRepository.findByGrupoIdOrderByFechaDesc(grupoId)) {
            cantidadPagos++;
            Participante pagador = pago.getPagador();
            Participante receptor = pago.getReceptor();
            participantes.putIfAbsent(pagador.getId(), pagador);
            participantes.putIfAbsent(receptor.getId(), receptor);
            pagosRealizados.merge(pagador.getId(), pago.getMonto(), BigDecimal::add);
            pagosRecibidos.merge(receptor.getId(), pago.getMonto(), BigDecimal::add);
        }

        // Solo las bajas ASUMIDA mueven balances. Una PENDIENTE o una NO_ASUMIDA
        // dejan el saldo del que se fue exactamente donde estaba.
        for (BajaGrupo baja : bajaRepository.findByGrupoIdAndEstado(grupoId, EstadoBaja.ASUMIDA)) {
            Participante saliente = baja.getParticipante();
            participantes.putIfAbsent(saliente.getId(), saliente);
            // Al que se fue se le condona su saldo, lo que lo lleva exactamente a
            // cero: si debía 300, recibe +300.
            ajusteBajas.merge(saliente.getId(), baja.getSaldo().negate(), BigDecimal::add);

            for (BajaParticipante fila : bajaParticipanteRepository.findByBajaId(baja.getId())) {
                Participante p = fila.getParticipante();
                participantes.putIfAbsent(p.getId(), p);
                // El monto guardado ya es el delta con signo de ese participante.
                ajusteBajas.merge(p.getId(), fila.getMonto(), BigDecimal::add);
            }
        }

        return new Contexto(participantes, pagado, adeudado, pagosRealizados, pagosRecibidos,
                miembrosActuales, totalGastadoUsdt, cantidadGastos, cantidadPagos,
                ajusteBajas);
    }

    private record Contexto(
            Map<Long, Participante> participantes,
            Map<Long, BigDecimal> pagadoPorId,
            Map<Long, BigDecimal> adeudadoPorId,
            Map<Long, BigDecimal> pagosRealizadosPorId,
            Map<Long, BigDecimal> pagosRecibidosPorId,
            Set<Long> miembrosActuales,
            BigDecimal totalGastadoUsdt,
            int cantidadGastos,
            int cantidadPagos,
            Map<Long, BigDecimal> ajusteBajasPorId) {
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
