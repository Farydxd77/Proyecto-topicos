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

@Entity
@Table(name = "gasto_participantes")
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

    public GastoParticipanteId getId() {
        return id;
    }

    public void setId(GastoParticipanteId id) {
        this.id = id;
    }

    public Gasto getGasto() {
        return gasto;
    }

    public void setGasto(Gasto gasto) {
        this.gasto = gasto;
    }

    public Participante getParticipante() {
        return participante;
    }

    public void setParticipante(Participante participante) {
        this.participante = participante;
    }

    public BigDecimal getMontoAdeudado() {
        return montoAdeudado;
    }

    public void setMontoAdeudado(BigDecimal montoAdeudado) {
        this.montoAdeudado = montoAdeudado;
    }
}
