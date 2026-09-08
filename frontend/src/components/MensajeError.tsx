import { ApiError } from '../api/client'

/**
 * Error general de un formulario o pantalla.
 *
 * Los errores por campo los pinta cada Campo; acá va lo que no pertenece a ninguno:
 * el `message` del backend, o un texto genérico si el error no vino de la API.
 */
export function MensajeError({
  error,
  tono = 'error',
}: {
  error: unknown
  tono?: 'error' | 'aviso'
}) {
  if (!error) return null

  const mensaje =
    error instanceof ApiError
      ? error.message
      : typeof error === 'string'
        ? error
        : 'Ocurrió un error inesperado. Intentá de nuevo.'

  const estilos =
    tono === 'aviso'
      ? 'border-aviso-200 bg-aviso-50 text-aviso-900'
      : 'border-contra-200 bg-contra-50 text-contra-700'

  return (
    <p role="alert" className={`rounded-md border px-3 py-2 text-sm ${estilos}`}>
      {mensaje}
    </p>
  )
}
