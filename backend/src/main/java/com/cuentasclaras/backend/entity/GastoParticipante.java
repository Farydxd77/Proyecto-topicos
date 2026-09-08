package com.cuentasclaras.backend.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.ColumnDefault;

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

    /**
     * Partes que le tocaron a este participante en el reparto. {@code 1} para todos
     * en un reparto equitativo. Se persiste porque deducirlo de {@code montoAdeudado}
     * sería ambiguo: con un gasto de 100 repartido 33.33/33.33/33.34 no hay forma de
     * saber si el centavo de diferencia es redondeo o un peso distinto.
     *
     * <p>El {@code DEFAULT 1} hace que la columna se pueda agregar con
     * {@code ddl-auto=update} sin tocar las filas ya existentes: quedan en 1, que es
     * exactamente el reparto que ya tenían.
     */
    @Column(name = "peso", nullable = false)
    @ColumnDefault("1")
    private Integer peso;
}
