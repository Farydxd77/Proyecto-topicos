import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { crearGrupo, listarGrupos } from '../api/grupos'
import { Boton } from '../components/Boton'
import { Campo } from '../components/Campo'
import { Card } from '../components/Card'
import { EstadoVacio } from '../components/EstadoVacio'
import { MensajeError } from '../components/MensajeError'
import { CLAVE_GRUPOS } from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { soloErrores, validarNombreGrupo } from '../lib/validacion'

function FormularioNuevoGrupo({ onListo }: { onListo: () => void }) {
  const queryClient = useQueryClient()
  const [nombre, setNombre] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [errores, setErrores] = useState<Record<string, string>>({})

  const mutacion = useMutation({
    mutationFn: crearGrupo,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CLAVE_GRUPOS })
      // Solo se limpia al tener éxito: si el backend rechaza, lo escrito se conserva.
      setNombre('')
      setDescripcion('')
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
      // Una descripción en blanco no es una descripción: se omite el campo.
      descripcion: descripcion.trim() || undefined,
    })
  }

  return (
    <form onSubmit={enviar} className="flex flex-col gap-4">
      <Campo
        id="nombre-grupo"
        etiqueta="Nombre del grupo"
        valor={nombre}
        onChange={setNombre}
        error={errores.nombre}
        ayuda="Por ejemplo: Viaje a Samaipata"
      />
      <Campo
        id="descripcion-grupo"
        etiqueta="Descripción (opcional)"
        valor={descripcion}
        onChange={setDescripcion}
      />

      <MensajeError error={mutacion.error} />

      <div className="flex gap-3">
        <Boton type="submit" enCurso={mutacion.isPending}>
          Crear grupo
        </Boton>
        <Boton variante="secundario" onClick={onListo}>
          Cancelar
        </Boton>
      </div>
    </form>
  )
}

export function GruposPage() {
  const [creando, setCreando] = useState(false)
  const consulta = useQuery({ queryKey: CLAVE_GRUPOS, queryFn: listarGrupos })
  const estado = estadoDe(consulta)

  if (estado.cargando) {
    return <p className="text-tinta-500">Cargando tus grupos…</p>
  }

  if (estado.error) {
    return (
      <div className="flex flex-col items-start gap-3">
        <MensajeError error={estado.error} />
        <Boton variante="secundario" onClick={() => consulta.refetch()}>
          Reintentar
        </Boton>
      </div>
    )
  }

  const grupos = estado.datos ?? []
  const vacio = grupos.length === 0

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-tinta-900">Mis grupos</h1>
          <p className="text-sm text-tinta-600">
            Los grupos de los que formás parte.
          </p>
        </div>
        {!creando && !vacio ? (
          <Boton onClick={() => setCreando(true)}>Nuevo grupo</Boton>
        ) : null}
      </div>

      {creando ? (
        <Card titulo="Nuevo grupo">
          <FormularioNuevoGrupo onListo={() => setCreando(false)} />
        </Card>
      ) : null}

      {vacio && !creando ? (
        // Una lista vacía sin contexto se lee como un error. Se explica y se ofrece salida.
        <EstadoVacio
          titulo="Todavía no tenés grupos"
          descripcion="Un grupo es un viaje, una salida o cualquier plan donde varios ponen plata. Creá el primero y empezá a repartir gastos con tu gente."
          accion={<Boton onClick={() => setCreando(true)}>Crear mi primer grupo</Boton>}
        />
      ) : null}

      {!vacio ? (
        <ul className="grid gap-3 sm:grid-cols-2">
          {grupos.map((grupo) => (
            <li key={grupo.id}>
              <Link
                to={`/grupos/${grupo.id}`}
                className="flex h-full flex-col rounded-xl border border-tinta-200 bg-white p-4 shadow-tarjeta transition-colors duration-150 hover:border-marca-300 hover:bg-marca-50/40"
              >
                <p className="font-medium text-tinta-900">{grupo.nombre}</p>
                {grupo.descripcion ? (
                  <p className="mt-0.5 line-clamp-2 text-sm text-tinta-600">
                    {grupo.descripcion}
                  </p>
                ) : null}
                <p className="mt-auto pt-3 text-xs text-tinta-500">
                  Creado por {grupo.creador.nombre} {grupo.creador.apellido}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}
