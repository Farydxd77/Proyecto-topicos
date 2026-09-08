import { SeccionGastos } from '../components/SeccionGastos'
import { useGrupo } from './GrupoLayout'

export function GrupoGastosPage() {
  const { grupo } = useGrupo()
  return <SeccionGastos grupo={grupo} />
}
