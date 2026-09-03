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
      ? 'border-amber-300 bg-amber-50 text-amber-800'
      : 'border-red-300 bg-red-50 text-red-700'

  return (
    <p role="alert" className={`rounded-md border px-3 py-2 text-sm ${estilos}`}>
      {mensaje}
    </p>
  )
}
