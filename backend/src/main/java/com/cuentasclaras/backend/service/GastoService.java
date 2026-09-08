package com.cuentasclaras.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.cuentasclaras.backend.client.Conversion;
import com.cuentasclaras.backend.client.CriptoYaClient;
import com.cuentasclaras.backend.dto.request.ActualizarGastoRequest;
import com.cuentasclaras.backend.dto.request.DivisionParticipanteRequest;
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
import com.cuentasclaras.backend.util.MonedasSoportadas;

@Service
public class GastoService {

    private final GastoRepository gastoRepository;
    private final GastoParticipanteRepository gastoParticipanteRepository;
    private final GrupoRepository grupoRepository;
    private final GrupoParticipanteRepository grupoParticipanteRepository;
    private final ParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CriptoYaClient criptoYaClient;

    public GastoService(
            GastoRepository gastoRepository,
            GastoParticipanteRepository gastoParticipanteRepository,
            GrupoRepository grupoRepository,
            GrupoParticipanteRepository grupoParticipanteRepository,
            ParticipanteRepository participanteRepository,
            UsuarioRepository usuarioRepository,
            CriptoYaClient criptoYaClient) {
        this.gastoRepository = gastoRepository;
        this.gastoParticipanteRepository = gastoParticipanteRepository;
        this.grupoRepository = grupoRepository;
        this.grupoParticipanteRepository = grupoParticipanteRepository;
        this.participanteRepository = participanteRepository;
        this.usuarioRepository = usuarioRepository;
        this.criptoYaClient = criptoYaClient;
    }

    @Transactional
    public GastoResponse registrar(Long grupoId, RegistrarGastoRequest req) {
        Grupo grupo = grupoDondeEsMiembro(grupoId, participanteActual());
        Participante pagador = pagadorMiembro(grupoId, req.pagadorId());
        // Se valida la división ANTES de convertir: no tiene sentido pegarle a
        // CriptoYa para después rechazar la petición por un participante repetido.
        List<Parte> partes = resolverPartes(grupoId, req.division());
        ResultadoConversion conv = resolver(req.moneda(), req.monedaNombre(), req.monto());

        Gasto gasto = Gasto.builder()
                .grupo(grupo)
                .descripcion(req.descripcion())
                .monto(req.monto())
                .moneda(conv.moneda())
                .monedaNombre(conv.monedaNombre())
                .montoUsdt(conv.montoUsdt())
                .tasaCambio(conv.tasaCambio())
                .pagador(pagador)
                .fecha(req.fecha())
                .build();
        gasto = gastoRepository.save(gasto);

        List<GastoParticipante> division = calcularDivision(
                gasto, montoADividir(conv), partes, pagador);
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
        List<Parte> partes = resolverPartes(grupoId, req.division());
        ResultadoConversion conv = resolver(req.moneda(), req.monedaNombre(), req.monto());

        gasto.setDescripcion(req.descripcion());
        gasto.setMonto(req.monto());
        gasto.setMoneda(conv.moneda());
        gasto.setMonedaNombre(conv.monedaNombre());
        gasto.setMontoUsdt(conv.montoUsdt());
        gasto.setTasaCambio(conv.tasaCambio());
        gasto.setPagador(pagador);
        gasto.setFecha(req.fecha());
        gasto = gastoRepository.save(gasto);

        // Descarta la división anterior antes de recalcular. El flush es
        // obligatorio: sin él, la reinserción de un miembro que permanece
        // chocaría con la PK compuesta (gasto_id, participante_id).
        gastoParticipanteRepository.deleteByGastoId(gastoId);
        gastoParticipanteRepository.flush();

        List<GastoParticipante> division = calcularDivision(
                gasto, montoADividir(conv), partes, pagador);
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

    // --- Conversión de moneda a USDT ------------------------------------

    private record ResultadoConversion(
            String moneda, String monedaNombre, BigDecimal montoUsdt, BigDecimal tasaCambio) {
    }

    /**
     * Resuelve la moneda del gasto y su conversión a USDT. Moneda ausente o
     * {@code USDT} → sin llamada externa (tasa 1). Moneda soportada distinta de
     * USDT → consulta CriptoYa. Moneda no soportada → {@link BadRequestException}
     * (antes de cualquier llamada externa).
     */
    private ResultadoConversion resolver(String monedaReq, String monedaNombreReq, BigDecimal montoOriginal) {
        String moneda = StringUtils.hasText(monedaReq)
                ? monedaReq.trim().toUpperCase()
                : "USDT";

        BigDecimal montoUsdt;
        BigDecimal tasaCambio;
        if (MonedasSoportadas.esUsdt(moneda)) {
            montoUsdt = montoOriginal.setScale(6, RoundingMode.HALF_UP);
            tasaCambio = BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);
        } else if (!MonedasSoportadas.esSoportada(moneda)) {
            throw new BadRequestException("Moneda no soportada: " + moneda);
        } else {
            Conversion c = MonedasSoportadas.esFiat(moneda)
                    ? criptoYaClient.convertirFiatAUsdt(moneda, montoOriginal)
                    : criptoYaClient.convertirCriptoAUsdt(moneda, montoOriginal);
            montoUsdt = c.montoUsdt();
            tasaCambio = c.tasaCambio();
        }

        String monedaNombre;
        if (StringUtils.hasText(monedaNombreReq)) {
            monedaNombre = monedaNombreReq.trim();
        } else if (MonedasSoportadas.esUsdt(moneda)) {
            monedaNombre = "Tether";
        } else {
            monedaNombre = moneda;
        }

        return new ResultadoConversion(moneda, monedaNombre, montoUsdt, tasaCambio);
    }

    /** El reparto en {@code gasto_participantes} se hace sobre el monto en USDT a 2 decimales. */
    private BigDecimal montoADividir(ResultadoConversion conv) {
        return conv.montoUsdt().setScale(2, RoundingMode.HALF_UP);
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

    // --- Resolución y validación de la división ---------------------------

    /** Un participante del reparto con las partes que le tocan. */
    private record Parte(Participante participante, int peso) {
    }

    /**
     * Resuelve entre quiénes se reparte el gasto y con qué peso.
     *
     * <p>Sin {@code division} explícita: todos los miembros actuales con peso 1, que
     * es el reparto equitativo de siempre. Con {@code division}: solo los listados,
     * con su peso. La diferencia entre «campo ausente» y «lista vacía» es
     * deliberada: lo primero es el valor por defecto, lo segundo un error.
     */
    private List<Parte> resolverPartes(Long grupoId, List<DivisionParticipanteRequest> division) {
        List<Participante> miembros = miembrosActuales(grupoId);
        if (division == null) {
            return miembros.stream().map(m -> new Parte(m, 1)).toList();
        }
        if (division.isEmpty()) {
            throw new BadRequestException("La división debe incluir al menos un participante");
        }

        Map<Long, Participante> miembrosPorId = miembros.stream()
                .collect(Collectors.toMap(Participante::getId, p -> p));

        Set<Long> vistos = new LinkedHashSet<>();
        List<Parte> partes = new ArrayList<>(division.size());
        for (DivisionParticipanteRequest entrada : division) {
            if (!vistos.add(entrada.participanteId())) {
                throw new BadRequestException(
                        "La división repite al participante " + entrada.participanteId());
            }
            Participante participante = miembrosPorId.get(entrada.participanteId());
            if (participante == null) {
                throw new BadRequestException(
                        "El participante " + entrada.participanteId()
                                + " de la división no es miembro del grupo");
            }
            partes.add(new Parte(participante, entrada.peso()));
        }
        return partes;
    }

    // --- Cálculo de la división ------------------------------------------

    /**
     * Reparte {@code monto} en proporción al peso de cada parte. El absorbente se
     * calcula por resta y no con su propia división: es lo que garantiza que la suma
     * de lo adeudado sea exactamente el monto del gasto, sin depender de cómo caigan
     * los redondeos.
     */
    private List<GastoParticipante> calcularDivision(
            Gasto gasto, BigDecimal monto, List<Parte> partes, Participante pagador) {

        Parte absorbente = elegirAbsorbente(partes, pagador);
        Long idAbsorbente = absorbente.participante().getId();
        BigDecimal pesoTotal = BigDecimal.valueOf(
                partes.stream().mapToLong(Parte::peso).sum());

        List<GastoParticipante> division = new ArrayList<>(partes.size());
        BigDecimal repartido = BigDecimal.ZERO;

        for (Parte parte : partes) {
            if (parte.participante().getId().equals(idAbsorbente)) {
                continue;
            }
            BigDecimal monto_i = monto
                    .multiply(BigDecimal.valueOf(parte.peso()))
                    .divide(pesoTotal, 2, RoundingMode.HALF_UP);
            repartido = repartido.add(monto_i);
            division.add(fila(gasto, parte, monto_i));
        }

        division.add(fila(gasto, absorbente, monto.subtract(repartido)));
        return division;
    }

    /**
     * Quién se queda con el sobrante del redondeo: el pagador si participa del gasto
     * (la regla histórica), y si no participa, el de mayor peso; a igualdad de peso,
     * el de menor id. El criterio tiene que ser determinista: sin él, dos ediciones
     * idénticas podrían darle el centavo a personas distintas.
     */
    private Parte elegirAbsorbente(List<Parte> partes, Participante pagador) {
        return partes.stream()
                .filter(p -> p.participante().getId().equals(pagador.getId()))
                .findFirst()
                .orElseGet(() -> partes.stream()
                        .max(Comparator.comparingInt(Parte::peso)
                                .thenComparing(p -> p.participante().getId(),
                                        Comparator.reverseOrder()))
                        .orElseThrow());
    }

    private GastoParticipante fila(Gasto gasto, Parte parte, BigDecimal montoAdeudado) {
        GastoParticipante fila = new GastoParticipante();
        fila.setId(new GastoParticipanteId(gasto.getId(), parte.participante().getId()));
        fila.setGasto(gasto);
        fila.setParticipante(parte.participante());
        fila.setMontoAdeudado(montoAdeudado);
        fila.setPeso(parte.peso());
        return fila;
    }

    // --- Mapeo a DTO ---------------------------------------------------

    private GastoResumenDto toResumen(Gasto g) {
        return new GastoResumenDto(
                g.getId(),
                g.getDescripcion(),
                g.getMonto(),
                g.getMoneda(),
                g.getMonedaNombre(),
                g.getMontoUsdt(),
                toParticipanteDto(g.getPagador()),
                g.getFecha());
    }

    private GastoResponse toResponse(Gasto g, List<GastoParticipante> division) {
        List<GastoParticipanteDto> divisionDto = division.stream()
                .map(gp -> new GastoParticipanteDto(
                        toParticipanteDto(gp.getParticipante()),
                        gp.getMontoAdeudado(),
                        gp.getPeso()))
                .toList();
        return new GastoResponse(
                g.getId(),
                g.getGrupo().getId(),
                g.getDescripcion(),
                g.getMonto(),
                g.getMoneda(),
                g.getMonedaNombre(),
                g.getMontoUsdt(),
                g.getTasaCambio(),
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
