import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { actualizarGrupo, eliminarGrupo, obtenerGrupo } from '../api/grupos'
import { obtenerPerfil } from '../api/perfil'
import type { GrupoResponse } from '../api/types'
import { Boton } from '../components/Boton'
import { Campo } from '../components/Campo'
import { GestionMiembros } from '../components/GestionMiembros'
import { MensajeError } from '../components/MensajeError'
import { SeccionBalances } from '../components/SeccionBalances'
import { SeccionGastos } from '../components/SeccionGastos'
import { CLAVE_GRUPOS, CLAVE_PERFIL, claveGrupo } from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { soloErrores, validarNombreGrupo } from '../lib/validacion'

/**
 * Un 404 y un 403 son situaciones distintas para quien las vive: "este grupo no
 * existe" frente a "existe pero no es tuyo". Se explican por separado, y con una
 * salida a mano en lugar de redirigir sin avisar.
 */
function SinAcceso({ error }: { error: unknown }) {
  const status = error instanceof ApiError ? error.status : 0

  const texto =
    status === 404
      ? 'Este grupo no existe. Puede que su creador lo haya eliminado.'
      : status === 403
        ? 'No tenés acceso a este grupo porque no sos miembro.'
        : null

  return (
    <div className="flex flex-col items-start gap-3">
      {texto ? (
        <p role="alert" className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {texto}
        </p>
      ) : (
        <MensajeError error={error} />
      )}
      <Link to="/grupos" className="text-sm font-medium text-emerald-700 hover:underline">
        Volver a mis grupos
      </Link>
    </div>
  )
}

function FormularioEditar({
  grupo,
  onListo,
}: {
  grupo: GrupoResponse
  onListo: () => void
}) {
  const queryClient = useQueryClient()
  const [nombre, setNombre] = useState(grupo.nombre)
  const [descripcion, setDescripcion] = useState(grupo.descripcion ?? '')
  const [errores, setErrores] = useState<Record<string, string>>({})

  const mutacion = useMutation({
    mutationFn: (datos: { nombre: string; descripcion?: string }) =>
      actualizarGrupo(grupo.id, datos),
    onSuccess: () => {
      // El nombre se ve en las dos pantallas: invalidar solo el detalle dejaría la
      // lista mostrando el nombre viejo.
      queryClient.invalidateQueries({ queryKey: claveGrupo(grupo.id) })
      queryClient.invalidateQueries({ queryKey: CLAVE_GRUPOS })
      onListo()
    },
  })

  function enviar(e: FormEvent) {
    e.preventDefault()
    const encontrados = soloErrores({ nombre: validarNombreGrupo(nombre) })
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) return

    mutacion.mutate({
      nombre: nombre.trim(),
      descripcion: descripcion.trim() || undefined,
    })
  }

  return (
    <form onSubmit={enviar} className="flex flex-col gap-4">
      <Campo
        id="editar-nombre"
        etiqueta="Nombre del grupo"
        valor={nombre}
        onChange={setNombre}
        error={errores.nombre}
      />
      <Campo
        id="editar-descripcion"
        etiqueta="Descripción (opcional)"
        valor={descripcion}
        onChange={setDescripcion}
      />

      <MensajeError error={mutacion.error} />

      <div className="flex gap-3">
        <Boton type="submit" enCurso={mutacion.isPending}>
          Guardar cambios
        </Boton>
        {/* Cancelar descarta y restaura: el estado local se descarta al desmontar. */}
        <Boton variante="secundario" onClick={onListo}>
          Cancelar
        </Boton>
      </div>
    </form>
  )
}

function ConfirmarEliminar({
  grupo,
  onCancelar,
}: {
  grupo: GrupoResponse
  onCancelar: () => void
}) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const mutacion = useMutation({
    mutationFn: () => eliminarGrupo(grupo.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CLAVE_GRUPOS })
      queryClient.removeQueries({ queryKey: claveGrupo(grupo.id) })
      navigate('/grupos', { replace: true })
    },
  })

  return (
    <div className="rounded-lg border border-red-300 bg-red-50 p-5">
      <p className="font-medium text-red-900">
        ¿Eliminar «{grupo.nombre}»?
      </p>
      <p className="mt-1 text-sm text-red-800">
        Se eliminará el grupo y a todos sus miembros. Esta acción no se puede deshacer.
      </p>

      <div className="mt-3">
        <MensajeError error={mutacion.error} />
      </div>

      <div className="mt-4 flex gap-3">
        <Boton onClick={() => mutacion.mutate()} enCurso={mutacion.isPending}>
          Sí, eliminar
        </Boton>
        <Boton variante="secundario" onClick={onCancelar}>
          Cancelar
        </Boton>
      </div>
    </div>
  )
}

export function GrupoDetallePage() {
  const { id } = useParams()
  const grupoId = Number(id)
  const [editando, setEditando] = useState(false)
  const [confirmando, setConfirmando] = useState(false)

  const consulta = useQuery({
    queryKey: claveGrupo(grupoId),
    queryFn: () => obtenerGrupo(grupoId),
    // Un 403 o un 404 no se arreglan reintentando: son respuestas, no fallos.
    retry: (intentos, error) =>
      error instanceof ApiError && (error.status === 403 || error.status === 404)
        ? false
        : intentos < 2,
  })

  // El id del participante propio sale del perfil ya cacheado. La convención del
  // proyecto es que AuthContext guarda solo el token y nada que venga del backend.
  const { data: perfil } = useQuery({ queryKey: CLAVE_PERFIL, queryFn: obtenerPerfil })

  const estado = estadoDe(consulta)

  if (estado.cargando) {
    return <p className="text-slate-500">Cargando el grupo…</p>
  }

  if (estado.error || !estado.datos) {
    return <SinAcceso error={estado.error} />
  }

  const grupo = estado.datos
  // Mientras el perfil no esté disponible se trata como no creador: es preferible que
  // una acción aparezca tarde a que aparezca y desaparezca.
  const esCreador = perfil != null && perfil.id === grupo.creador.id

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Link to="/grupos" className="text-sm text-slate-500 hover:text-slate-800">
          ← Mis grupos
        </Link>
      </div>

      {editando ? (
        <section className="rounded-lg border border-slate-200 bg-white p-5">
          <h2 className="mb-4 font-semibold text-slate-900">Editar grupo</h2>
          <FormularioEditar grupo={grupo} onListo={() => setEditando(false)} />
        </section>
      ) : (
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">{grupo.nombre}</h1>
            {grupo.descripcion ? (
              <p className="mt-1 text-slate-600">{grupo.descripcion}</p>
            ) : null}
            <p className="mt-2 text-sm text-slate-500">
              Creado por {grupo.creador.nombre} {grupo.creador.apellido}
            </p>
          </div>

          {esCreador && !confirmando ? (
            <div className="flex shrink-0 gap-2">
              <Boton variante="secundario" onClick={() => setEditando(true)}>
                Editar
              </Boton>
              <Boton variante="secundario" onClick={() => setConfirmando(true)}>
                Eliminar
              </Boton>
            </div>
          ) : null}
        </div>
      )}

      {confirmando ? (
        <ConfirmarEliminar grupo={grupo} onCancelar={() => setConfirmando(false)} />
      ) : null}

      <GestionMiembros grupo={grupo} esCreador={esCreador} />

      <SeccionGastos grupo={grupo} />

      <SeccionBalances grupo={grupo} />
    </div>
  )
}
