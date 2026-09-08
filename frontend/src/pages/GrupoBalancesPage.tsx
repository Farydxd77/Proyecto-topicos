import { useNavigate } from 'react-router'
import { SeccionBalances } from '../components/SeccionBalances'
import { useGrupo } from './GrupoLayout'

export function GrupoBalancesPage() {
  const { grupo, participanteId } = useGrupo()
  const navigate = useNavigate()

  return (
    <SeccionBalances
      grupo={grupo}
      participanteId={participanteId}
      // El atajo cruza de pestaña: lleva a Pagos con el receptor y el monto en la
      // URL, que es lo que hace que la precarga sobreviva a la navegación.
      onRegistrarPago={({ receptorId, monto }) =>
        navigate(`/grupos/${grupo.id}/pagos?receptor=${receptorId}&monto=${monto}`)
      }
    />
  )
}
