/**
 * Catálogo de monedas soportadas.
 *
 * Espeja `util/MonedasSoportadas.java` del backend, que solo conoce símbolos. Los
 * nombres legibles los aporta el frontend porque `monedaNombre` es un dato que el
 * cliente ENVÍA al registrar un gasto, no algo que el backend calcule.
 *
 * Si el backend suma una moneda y este catálogo se queda corto, la interfaz
 * simplemente no la ofrece; y si aun así llegara una no soportada, el backend
 * responde 400 «Moneda no soportada: X», que la pantalla ya maneja.
 */

export interface Moneda {
  simbolo: string
  nombre: string
}

/** USDT es la unidad de reparto: tasa 1 y sin consulta al servicio externo. */
export const MONEDA_POR_DEFECTO = 'USDT'

export const FIATS: Moneda[] = [
  { simbolo: 'BOB', nombre: 'Boliviano' },
  { simbolo: 'ARS', nombre: 'Peso argentino' },
  { simbolo: 'BRL', nombre: 'Real brasileño' },
  { simbolo: 'CLP', nombre: 'Peso chileno' },
  { simbolo: 'COP', nombre: 'Peso colombiano' },
  { simbolo: 'MXN', nombre: 'Peso mexicano' },
  { simbolo: 'PEN', nombre: 'Sol peruano' },
  { simbolo: 'UYU', nombre: 'Peso uruguayo' },
  { simbolo: 'PYG', nombre: 'Guaraní' },
  { simbolo: 'VES', nombre: 'Bolívar venezolano' },
  { simbolo: 'DOP', nombre: 'Peso dominicano' },
  { simbolo: 'USD', nombre: 'Dólar estadounidense' },
  { simbolo: 'EUR', nombre: 'Euro' },
]

export const CRIPTOS: Moneda[] = [
  { simbolo: 'USDT', nombre: 'Tether' },
  { simbolo: 'USDC', nombre: 'USD Coin' },
  { simbolo: 'DAI', nombre: 'Dai' },
  { simbolo: 'BTC', nombre: 'Bitcoin' },
  { simbolo: 'ETH', nombre: 'Ethereum' },
  { simbolo: 'BNB', nombre: 'BNB' },
  { simbolo: 'SOL', nombre: 'Solana' },
  { simbolo: 'XRP', nombre: 'XRP' },
  { simbolo: 'ADA', nombre: 'Cardano' },
  { simbolo: 'AVAX', nombre: 'Avalanche' },
  { simbolo: 'DOGE', nombre: 'Dogecoin' },
  { simbolo: 'TRX', nombre: 'TRON' },
  { simbolo: 'LINK', nombre: 'Chainlink' },
  { simbolo: 'DOT', nombre: 'Polkadot' },
  { simbolo: 'MATIC', nombre: 'Polygon' },
  { simbolo: 'SHIB', nombre: 'Shiba Inu' },
  { simbolo: 'LTC', nombre: 'Litecoin' },
  { simbolo: 'BCH', nombre: 'Bitcoin Cash' },
  { simbolo: 'EOS', nombre: 'EOS' },
  { simbolo: 'XLM', nombre: 'Stellar' },
  { simbolo: 'FTM', nombre: 'Fantom' },
  { simbolo: 'AAVE', nombre: 'Aave' },
  { simbolo: 'UNI', nombre: 'Uniswap' },
  { simbolo: 'ALGO', nombre: 'Algorand' },
  { simbolo: 'BAT', nombre: 'Basic Attention Token' },
  { simbolo: 'PAXG', nombre: 'PAX Gold' },
  { simbolo: 'CAKE', nombre: 'PancakeSwap' },
  { simbolo: 'AXS', nombre: 'Axie Infinity' },
  { simbolo: 'SLP', nombre: 'Smooth Love Potion' },
  { simbolo: 'MANA', nombre: 'Decentraland' },
  { simbolo: 'SAND', nombre: 'The Sandbox' },
  { simbolo: 'CHZ', nombre: 'Chiliz' },
  { simbolo: 'UXD', nombre: 'UXD Stablecoin' },
  { simbolo: 'USDP', nombre: 'Pax Dollar' },
  { simbolo: 'WLD', nombre: 'Worldcoin' },
]

export function nombreDe(simbolo: string): string {
  const encontrada = [...FIATS, ...CRIPTOS].find((m) => m.simbolo === simbolo)
  return encontrada?.nombre ?? simbolo
}

export function esUsdt(simbolo: string): boolean {
  return simbolo === MONEDA_POR_DEFECTO
}
