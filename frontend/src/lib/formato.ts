/**
 * Formateo de montos para mostrar.
 *
 * Los montos llegan como números JSON —Jackson serializa los BigDecimal así— y acá
 * SOLO se formatean: nunca se suman, restan ni comparan. El backend ya calculó la
 * conversión y el reparto, así que el frontend no necesita aritmética, y evitarla
 * es lo que impide que la coma flotante degrade valores de hasta 8 decimales.
 */
export function formatearMonto(valor: number): string {
  return valor.toLocaleString('es', { maximumFractionDigits: 8 })
}
