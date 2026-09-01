package com.cuentasclaras.backend.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class GastoParticipanteId implements Serializable {

    private Long gastoId;
    private Long participanteId;

    public GastoParticipanteId() {
    }

    public GastoParticipanteId(Long gastoId, Long participanteId) {
        this.gastoId = gastoId;
        this.participanteId = participanteId;
    }

    public Long getGastoId() {
        return gastoId;
    }

    public void setGastoId(Long gastoId) {
        this.gastoId = gastoId;
    }

    public Long getParticipanteId() {
        return participanteId;
    }

    public void setParticipanteId(Long participanteId) {
        this.participanteId = participanteId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GastoParticipanteId that)) {
            return false;
        }
        return Objects.equals(gastoId, that.gastoId) && Objects.equals(participanteId, that.participanteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gastoId, participanteId);
    }
}
