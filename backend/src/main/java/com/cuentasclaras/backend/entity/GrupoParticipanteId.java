package com.cuentasclaras.backend.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class GrupoParticipanteId implements Serializable {

    private Long grupoId;
    private Long participanteId;

    public GrupoParticipanteId() {
    }

    public GrupoParticipanteId(Long grupoId, Long participanteId) {
        this.grupoId = grupoId;
        this.participanteId = participanteId;
    }

    public Long getGrupoId() {
        return grupoId;
    }

    public void setGrupoId(Long grupoId) {
        this.grupoId = grupoId;
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
        if (!(o instanceof GrupoParticipanteId that)) {
            return false;
        }
        return Objects.equals(grupoId, that.grupoId) && Objects.equals(participanteId, that.participanteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(grupoId, participanteId);
    }
}
