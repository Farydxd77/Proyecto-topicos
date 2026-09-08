import { GestionMiembros } from '../components/GestionMiembros'
import { SeccionBajas } from '../components/SeccionBajas'
import { useGrupo } from './GrupoLayout'

export function GrupoMiembrosPage() {
  const { grupo, esCreador, participanteId } = useGrupo()

  return (
    <div className="flex flex-col gap-4">
      {/* Las bajas van primero: son lo que puede requerir una decisión. */}
      <SeccionBajas grupo={grupo} esCreador={esCreador} />
      <GestionMiembros
        grupo={grupo}
        esCreador={esCreador}
        participanteId={participanteId}
      />
    </div>
  )
}
