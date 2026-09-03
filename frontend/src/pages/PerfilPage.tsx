import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent, type ReactNode } from 'react'
import { ApiError } from '../api/client'
import {
  actualizarPerfil,
  cambiarPassword,
  cambiarUsername,
  obtenerPerfil,
} from '../api/perfil'
import { Boton } from '../components/Boton'
import { Campo } from '../components/Campo'
import { MensajeError } from '../components/MensajeError'
import {
  soloErrores,
  validarApellido,
  validarConfirmacion,
  validarNombre,
  validarPassword,
  validarUsername,
} from '../lib/validacion'

const CLAVE_PERFIL = ['perfil']

function Seccion({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5">
      <h2 className="mb-4 font-semibold text-slate-900">{titulo}</h2>
      {children}
    </section>
  )
}

function Exito({ mensaje }: { mensaje: string | null }) {
  if (!mensaje) return null
  return (
    <p role="status" className="rounded-md border border-emerald-300 bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
      {mensaje}
    </p>
  )
}

export function PerfilPage() {
  const queryClient = useQueryClient()
  const consulta = useQuery({ queryKey: CLAVE_PERFIL, queryFn: obtenerPerfil })
  const perfil = consulta.data

  if (consulta.isPending) {
    return <p className="text-slate-500">Cargando tu perfil…</p>
  }

  if (consulta.isError) {
    return (
      <div className="flex flex-col items-start gap-3">
        <MensajeError error={consulta.error} />
        <Boton variante="secundario" onClick={() => consulta.refetch()}>
          Reintentar
        </Boton>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Mi perfil</h1>
        <p className="text-sm text-slate-600">
          Cuenta creada el {new Date(perfil!.createdAt).toLocaleDateString('es')}
        </p>
      </div>

      <DatosPersonales perfil={perfil!} queryClient={queryClient} />
      <CambioDeUsername perfil={perfil!} queryClient={queryClient} />
      <CambioDePassword />
    </div>
  )
}

// --- Datos personales ---

type ClienteQuery = ReturnType<typeof useQueryClient>
type Perfil = NonNullable<ReturnType<typeof obtenerPerfil> extends Promise<infer T> ? T : never>

function DatosPersonales({ perfil, queryClient }: { perfil: Perfil; queryClient: ClienteQuery }) {
  // El borrador solo existe mientras se edita. Fuera de edición es null y los campos
  // se derivan del perfil, así que lo guardado siempre se refleja sin sincronizar nada.
  const [borrador, setBorrador] = useState<{ nombre: string; apellido: string } | null>(null)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [exito, setExito] = useState<string | null>(null)

  const editando = borrador !== null
  const nombre = borrador?.nombre ?? perfil.nombre
  const apellido = borrador?.apellido ?? perfil.apellido

  const setNombre = (valor: string) =>
    setBorrador((b) => ({ nombre: valor, apellido: b?.apellido ?? perfil.apellido }))
  const setApellido = (valor: string) =>
    setBorrador((b) => ({ nombre: b?.nombre ?? perfil.nombre, apellido: valor }))

  const mutacion = useMutation({
    mutationFn: actualizarPerfil,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CLAVE_PERFIL })
      setBorrador(null)
      setExito('Datos actualizados')
    },
  })

  function cancelar() {
    // Descartar el borrador basta: los campos vuelven solos a lo guardado.
    setBorrador(null)
    setErrores({})
  }

  function enviar(e: FormEvent) {
    e.preventDefault()
    setExito(null)

    const encontrados = soloErrores({
      nombre: validarNombre(nombre),
      apellido: validarApellido(apellido),
    })
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) return

    mutacion.mutate({ nombre, apellido })
  }

  const erroresBackend = mutacion.error instanceof ApiError ? (mutacion.error.errors ?? {}) : {}

  return (
    <Seccion titulo="Datos personales">
      <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
        <Campo
          id="nombre"
          etiqueta="Nombre"
          valor={nombre}
          onChange={setNombre}
          error={errores.nombre ?? erroresBackend.nombre}
          soloLectura={!editando}
        />
        <Campo
          id="apellido"
          etiqueta="Apellido"
          valor={apellido}
          onChange={setApellido}
          error={errores.apellido ?? erroresBackend.apellido}
          soloLectura={!editando}
        />
        {/* El CI se muestra pero no se edita: el backend ignora cualquier ci recibido. */}
        <Campo
          id="ci"
          etiqueta="CI"
          valor={perfil.ci}
          onChange={() => {}}
          soloLectura
          ayuda="El CI no se puede modificar"
        />

        <Exito mensaje={exito} />
        {!erroresBackend.nombre && !erroresBackend.apellido ? (
          <MensajeError error={mutacion.error} />
        ) : null}

        <div className="flex gap-2">
          {editando ? (
            <>
              <Boton type="submit" enCurso={mutacion.isPending}>
                Guardar
              </Boton>
              <Boton variante="secundario" onClick={cancelar}>
                Cancelar
              </Boton>
            </>
          ) : (
            <Boton
              variante="secundario"
              onClick={() => {
                setExito(null)
                setBorrador({ nombre: perfil.nombre, apellido: perfil.apellido })
              }}
            >
              Editar
            </Boton>
          )}
        </div>
      </form>
    </Seccion>
  )
}

// --- Username ---

function CambioDeUsername({ perfil, queryClient }: { perfil: Perfil; queryClient: ClienteQuery }) {
  // Igual que arriba: null significa "mostrá el username vigente del perfil".
  const [borrador, setBorrador] = useState<string | null>(null)
  const [error, setError] = useState<string | undefined>()
  const [exito, setExito] = useState<string | null>(null)

  const username = borrador ?? perfil.username

  const mutacion = useMutation({
    mutationFn: cambiarUsername,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CLAVE_PERFIL })
      setBorrador(null)
      setExito('Username actualizado')
    },
    // Ante un 409 se descarta el borrador: el mostrado vuelve a ser el vigente.
    onError: () => setBorrador(null),
  })

  function enviar(e: FormEvent) {
    e.preventDefault()
    setExito(null)

    const mensaje = validarUsername(username)
    setError(mensaje ?? undefined)
    if (mensaje) return

    mutacion.mutate({ username })
  }

  return (
    <Seccion titulo="Username">
      <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
        <Campo
          id="nuevo-username"
          etiqueta="Username"
          valor={username}
          onChange={setBorrador}
          error={error}
          ayuda="Entre 3 y 50 caracteres"
        />
        <Exito mensaje={exito} />
        <MensajeError error={mutacion.error} />
        <div>
          <Boton type="submit" enCurso={mutacion.isPending}>
            Cambiar username
          </Boton>
        </div>
      </form>
    </Seccion>
  )
}

// --- Contraseña ---

function CambioDePassword() {
  const [password, setPassword] = useState('')
  const [confirmacion, setConfirmacion] = useState('')
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [exito, setExito] = useState<string | null>(null)

  const mutacion = useMutation({
    mutationFn: cambiarPassword,
    onSuccess: () => {
      // Los campos no se conservan después de la operación.
      setPassword('')
      setConfirmacion('')
      setExito('Contraseña actualizada')
    },
  })

  function enviar(e: FormEvent) {
    e.preventDefault()
    setExito(null)

    const encontrados = soloErrores({
      password: validarPassword(password),
      confirmacion: validarConfirmacion(password, confirmacion),
    })
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) return

    mutacion.mutate({ password })
  }

  return (
    <Seccion titulo="Contraseña">
      <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
        <Campo
          id="nueva-password"
          etiqueta="Nueva contraseña"
          tipo="password"
          valor={password}
          onChange={setPassword}
          error={errores.password}
          autoComplete="new-password"
          ayuda="Mínimo 8 caracteres"
        />
        <Campo
          id="confirmar-password"
          etiqueta="Confirmar contraseña"
          tipo="password"
          valor={confirmacion}
          onChange={setConfirmacion}
          error={errores.confirmacion}
          autoComplete="new-password"
        />
        <Exito mensaje={exito} />
        <MensajeError error={mutacion.error} />
        <div>
          <Boton type="submit" enCurso={mutacion.isPending}>
            Cambiar contraseña
          </Boton>
        </div>
      </form>
    </Seccion>
  )
}
