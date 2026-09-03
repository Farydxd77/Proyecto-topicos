package com.cuentasclaras.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "gastos")
@Check(constraints = "monto > 0")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Gasto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    /**
     * Monto ORIGINAL del gasto, expresado en la moneda del campo {@link #moneda}.
     * NO está en USDT: el valor convertido a USDT es {@link #montoUsdt}.
     */
    @Column(name = "monto", nullable = false, precision = 20, scale = 8)
    private BigDecimal monto;

    /** Símbolo de la moneda original del gasto (p. ej. {@code USDT}, {@code BOB}, {@code BTC}). */
    @Column(name = "moneda", nullable = false, length = 10)
    @ColumnDefault("'USDT'")
    private String moneda;

    /** Nombre de la moneda tal como lo envía el cliente (p. ej. {@code Boliviano}). */
    @Column(name = "moneda_nombre", nullable = false, length = 50)
    @ColumnDefault("'Tether'")
    private String monedaNombre;

    /** Monto del gasto convertido a USDT al momento de registrarlo o editarlo. */
    @Column(name = "monto_usdt", nullable = false, precision = 20, scale = 6)
    @ColumnDefault("0")
    private BigDecimal montoUsdt;

    /**
     * Tasa aplicada: {@code montoUsdt = monto * tasaCambio}. {@code 1} cuando la
     * moneda es USDT. La parte entera admite tasas cripto altas (p. ej. BTC/USDT).
     */
    @Column(name = "tasa_cambio", nullable = false, precision = 20, scale = 6)
    @ColumnDefault("1")
    private BigDecimal tasaCambio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagador_id", nullable = true)
    private Participante pagador;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;
}
