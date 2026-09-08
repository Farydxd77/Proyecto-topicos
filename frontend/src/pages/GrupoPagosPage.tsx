import { useSearchParams } from 'react-router'
import { SeccionPagos, type PagoPrecargado } from '../components/SeccionPagos'
import { useGrupo } from './GrupoLayout'

/**
 * La precarga desde la liquidación llega por la URL, no por estado en memoria.
 *
 * Balances y Pagos son ahora pantallas distintas, así que el atajo tiene que
 * sobrevivir a una navegación. Por la URL sobrevive, y además el enlace resultante
 * se puede compartir y recargar.
 */
function leerPrecarga(params: URLSearchParams): PagoPrecargado | null {
  const receptor = Number(params.get('receptor'))
  const monto = params.get('monto')
  if (!receptor || !monto) return null
  return { receptorId: receptor, monto }
}

export function GrupoPagosPage() {
  const { grupo, participanteId } = useGrupo()
  const [params, setParams] = useSearchParams()

  return (
    <SeccionPagos
      grupo={grupo}
      participanteId={participanteId}
      precargado={leerPrecarga(params)}
      onPrecargadoConsumido={() => {
        // Se limpia la URL para que recargar no vuelva a abrir el formulario.
        params.delete('receptor')
        params.delete('monto')
        setParams(params, { replace: true })
      }}
    />
  )
}
