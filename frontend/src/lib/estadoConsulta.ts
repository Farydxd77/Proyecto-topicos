import type { UseQueryResult } from '@tanstack/react-query'
import { ApiError } from '../api/client'

/**
 * Normaliza el estado de una consulta a tres casos: cargando, error o datos.
 *
 * Existe por un comportamiento de TanStack Query que rompe el requisito de «nunca
 * quedar en carga permanente»: cuando su heurística de conexión decide que no hay
 * red, la consulta no falla sino que queda en `fetchStatus: 'paused'` con
 * `status: 'pending'` de forma indefinida. La pantalla se congela en «Cargando…»,
 * sin error, sin reintento y sin salida.
 *
 * Se reprodujo con el backend detenido y `navigator.onLine === true`, y persiste con
 * `networkMode: 'always'` y con `onlineManager.setOnline(true)`. En lugar de seguir
 * peleando con esa heurística, acá una consulta pausada se trata como lo que es para
 * quien mira la pantalla: un fallo del que se puede reintentar.
 *
 * Todas las pantallas pasan por esta función para que la regla viva en un solo sitio.
 */
export interface EstadoConsulta<T> {
  cargando: boolean
  error: unknown
  datos: T | undefined
}

const ERROR_PAUSADA = new ApiError(
  0,
  'El servidor no está respondiendo. Revisá que el backend esté corriendo e intentá de nuevo.',
)

export function estadoDe<T>(consulta: UseQueryResult<T>): EstadoConsulta<T> {
  const pausada = consulta.fetchStatus === 'paused'

  if (pausada && consulta.data === undefined) {
    // `failureReason` conserva el error real del ultimo intento; si no lo hay, se usa
    // el mensaje generico de backend no disponible.
    return { cargando: false, error: consulta.failureReason ?? ERROR_PAUSADA, datos: undefined }
  }

  return {
    cargando: consulta.isPending,
    error: consulta.isError ? consulta.error : null,
    datos: consulta.data,
  }
}
