package com.cuentasclaras.backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gasto_participantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GastoParticipante {

    @EmbeddedId
    private GastoParticipanteId id = new GastoParticipanteId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gastoId")
    @JoinColumn(name = "gasto_id")
    private Gasto gasto;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("participanteId")
    @JoinColumn(name = "participante_id")
    private Participante participante;

    @Column(name = "monto_adeudado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoAdeudado;
}
