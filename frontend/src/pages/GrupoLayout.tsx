import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, Outlet, useNavigate, useOutletContext, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { listarBajas } from '../api/bajas'
import { actualizarGrupo, eliminarGrupo, obtenerGrupo } from '../api/grupos'
import { obtenerPerfil } from '../api/perfil'
import type { GrupoResponse } from '../api/types'
import { Boton } from '../components/Boton'
import { Campo } from '../components/Campo'
import { Card } from '../components/Card'
import { MensajeError } from '../components/MensajeError'
import { Pestanas } from '../components/Pestanas'
import { CLAVE_GRUPOS, CLAVE_PERFIL, claveBajas, claveGrupo } from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { soloErrores, validarNombreGrupo } from '../lib/validacion'

/**
 * Lo que el layout le pasa a cada pestaña por el `Outlet`.
 *
 * El grupo se consulta UNA vez acá, no en cada pantalla: como las pestañas son rutas
 * hijas, cambiar de pestaña no desmonta el layout y la consulta no se repite.
 */
export interface ContextoGrupo {
  grupo: GrupoResponse
  esCreador: boolean
  /** El participante propio, o null mientras el perfil no esté disponible. */
  participanteId: number | null
}

export function useGrupo(): ContextoGrupo {
  return useOutletContext<ContextoGrupo>()
}

/**
 * Un 404 y un 403 son situaciones distintas para quien las vive: «este grupo no
 * existe» frente a «existe pero no es tuyo». Se explican por separado, y con una
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
    <div className="mx-auto max-w-md py-16 text-center">
      {texto ? (
        <p role="alert" className="text-tinta-700">
          {texto}
        </p>
      ) : (
        <MensajeError error={error} />
      )}
      <div className="mt-4">
        <Link
          to="/grupos"
          className="text-sm font-medium text-marca-700 hover:text-marca-800 hover:underline"
        >
          ← Volver a mis grupos
        </Link>
      </div>
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
    <Card tono="aviso">
      <p className="font-medium text-tinta-900">¿Eliminar «{grupo.nombre}»?</p>
      <p className="mt-1 text-sm text-tinta-700">
        Se borran el grupo, sus miembros, sus gastos y sus pagos. No se puede deshacer.
      </p>

      <div className="mt-3">
        <MensajeError error={mutacion.error} />
      </div>

      <div className="mt-4 flex gap-3">
        <Boton variante="peligro" onClick={() => mutacion.mutate()} enCurso={mutacion.isPending}>
          Sí, eliminar
        </Boton>
        <Boton variante="secundario" onClick={onCancelar}>
          Cancelar
        </Boton>
      </div>
    </Card>
  )
}

export function GrupoLayout() {
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

  // Solo para el punto de atención en la pestaña de miembros. Es barata y la pestaña
  // de miembros la reusa del caché.
  const { data: bajas } = useQuery({
    queryKey: claveBajas(grupoId),
    queryFn: () => listarBajas(grupoId),
    enabled: consulta.isSuccess,
  })

  const estado = estadoDe(consulta)

  if (estado.cargando) {
    return <p className="py-16 text-center text-tinta-500">Cargando el grupo…</p>
  }
  if (estado.error || !estado.datos) {
    return <SinAcceso error={estado.error} />
  }

  const grupo = estado.datos
  // Mientras el perfil no esté disponible se trata como no creador: es preferible que
  // una acción aparezca tarde a que aparezca y desaparezca.
  const esCreador = perfil != null && perfil.id === grupo.creador.id
  const hayBajasPendientes = (bajas ?? []).some((b) => b.estado === 'PENDIENTE')

  const contexto: ContextoGrupo = {
    grupo,
    esCreador,
    participanteId: perfil?.id ?? null,
  }

  const base = `/grupos/${grupo.id}`

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Link
          to="/grupos"
          className="text-sm text-tinta-500 transition-colors hover:text-tinta-800"
        >
          ← Mis grupos
        </Link>
      </div>

      {editando ? (
        <Card titulo="Editar grupo">
          <FormularioEditar grupo={grupo} onListo={() => setEditando(false)} />
        </Card>
      ) : (
        <header className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0">
            <h1 className="truncate text-2xl font-semibold text-tinta-900">
              {grupo.nombre}
            </h1>
            {grupo.descripcion ? (
              <p className="mt-1 text-tinta-600">{grupo.descripcion}</p>
            ) : null}
            <p className="mt-1.5 text-sm text-tinta-500">
              {grupo.miembros.length}{' '}
              {grupo.miembros.length === 1 ? 'integrante' : 'integrantes'} · creado por{' '}
              {grupo.creador.nombre} {grupo.creador.apellido}
            </p>
          </div>

          {esCreador && !confirmando ? (
            <div className="flex shrink-0 gap-2">
              <Boton variante="secundario" tamano="chico" onClick={() => setEditando(true)}>
                Editar
              </Boton>
              <Boton variante="secundario" tamano="chico" onClick={() => setConfirmando(true)}>
                Eliminar
              </Boton>
            </div>
          ) : null}
        </header>
      )}

      {confirmando ? (
        <ConfirmarEliminar grupo={grupo} onCancelar={() => setConfirmando(false)} />
      ) : null}

      <Pestanas
        pestanas={[
          { to: base, texto: 'Resumen', exacta: true },
          { to: `${base}/gastos`, texto: 'Gastos' },
          { to: `${base}/pagos`, texto: 'Pagos' },
          { to: `${base}/balances`, texto: 'Balances' },
          { to: `${base}/miembros`, texto: 'Miembros', alerta: hayBajasPendientes },
        ]}
      />

      <Outlet context={contexto} />
    </div>
  )
}
