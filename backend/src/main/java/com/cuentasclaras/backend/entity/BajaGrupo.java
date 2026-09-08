package com.cuentasclaras.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * La salida de un participante que dejó saldo pendiente, y la decisión del grupo
 * sobre esa deuda.
 *
 * <p>Solo se registra cuando el balance del que sale es distinto de cero: si estaba
 * a mano no hay nada que decidir.
 */
@Entity
@Table(name = "bajas_grupo")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BajaGrupo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    /** Quien salió del grupo. Ya no figura en `grupo_participantes`. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participante_id", nullable = false)
    private Participante participante;

    /**
     * El balance que tenía al salir, en USDT. Negativo = debía; positivo = le debían.
     *
     * <p>Queda CONGELADO: el saldo de un ex-miembro todavía puede moverse (un pago
     * hacia él, la edición de un gasto viejo). Si el reparto se calculara sobre el
     * saldo vigente, la decisión que tomó el creador dejaría de corresponder con lo
     * que se repartió.
     */
    @Column(name = "saldo", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoBaja estado;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;
}
