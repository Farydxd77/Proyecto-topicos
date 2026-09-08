package com.cuentasclaras.backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuánto absorbió cada miembro de una baja asumida. Congela el reparto igual que
 * {@code gasto_participantes} congela la división de un gasto: si después entra o
 * sale gente, este reparto no se recalcula.
 */
@Entity
@Table(name = "baja_participantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BajaParticipante {

    @EmbeddedId
    private BajaParticipanteId id = new BajaParticipanteId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("bajaId")
    @JoinColumn(name = "baja_id")
    private BajaGrupo baja;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("participanteId")
    @JoinColumn(name = "participante_id")
    private Participante participante;

    /**
     * Lo que esta baja le suma o le resta al balance de este participante, en USDT.
     * Es el delta con signo, ya listo para sumar: no hace falta interpretarlo.
     *
     * <p>Si el que se fue debía 300 y lo absorben 2, cada uno guarda {@code -150.00}
     * y su balance baja. Si le debían 300 a él, cada uno guarda {@code +150.00} y su
     * balance sube, porque dejan de deberle. Un solo número con signo cubre las dos
     * direcciones, y la suma de todas las filas es exactamente el saldo de la baja.
     */
    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
}
