import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { registrar } from '../api/auth'
import { useAuth } from '../auth/useAuth'
import { Boton } from '../components/Boton'
import { Campo } from '../components/Campo'
import { MensajeError } from '../components/MensajeError'
import {
  soloErrores,
  validarApellido,
  validarCi,
  validarNombre,
  validarPassword,
  validarUsername,
} from '../lib/validacion'

export function RegistroPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [nombre, setNombre] = useState('')
  const [apellido, setApellido] = useState('')
  const [ci, setCi] = useState('')
  const [errores, setErrores] = useState<Record<string, string>>({})

  const { iniciarSesion } = useAuth()
  const navigate = useNavigate()

  const mutacion = useMutation({
    mutationFn: registrar,
    onSuccess: (respuesta) => {
      // Un registro exitoso inicia sesión de inmediato: no se pide entrar otra vez.
      iniciarSesion(respuesta.token)
      navigate('/perfil', { replace: true })
    },
  })

  // Los campos conservan lo cargado ante un 409, para corregir solo el username.
  const erroresDelBackend =
    mutacion.error instanceof ApiError ? (mutacion.error.errors ?? {}) : {}
  const errorDe = (campo: string) => errores[campo] ?? erroresDelBackend[campo]

  function enviar(e: FormEvent) {
    e.preventDefault()

    const encontrados = soloErrores({
      username: validarUsername(username),
      password: validarPassword(password),
      nombre: validarNombre(nombre),
      apellido: validarApellido(apellido),
      ci: validarCi(ci),
    })
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) return

    mutacion.mutate({ username, password, nombre, apellido, ci })
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-8">
      <div className="w-full max-w-sm">
        <h1 className="mb-1 text-2xl font-semibold text-slate-900">Crear cuenta</h1>
        <p className="mb-6 text-sm text-slate-600">
          Tus datos de participante en Cuentas Claras
        </p>

        <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
          <Campo
            id="username"
            etiqueta="Username"
            valor={username}
            onChange={setUsername}
            error={errorDe('username')}
            autoComplete="username"
            ayuda="Entre 3 y 50 caracteres"
          />
          <Campo
            id="password"
            etiqueta="Contraseña"
            tipo="password"
            valor={password}
            onChange={setPassword}
            error={errorDe('password')}
            autoComplete="new-password"
            ayuda="Mínimo 8 caracteres"
          />
          <Campo
            id="nombre"
            etiqueta="Nombre"
            valor={nombre}
            onChange={setNombre}
            error={errorDe('nombre')}
            autoComplete="given-name"
          />
          <Campo
            id="apellido"
            etiqueta="Apellido"
            valor={apellido}
            onChange={setApellido}
            error={errorDe('apellido')}
            autoComplete="family-name"
          />
          <Campo
            id="ci"
            etiqueta="CI"
            valor={ci}
            onChange={setCi}
            error={errorDe('ci')}
            ayuda="No vas a poder cambiarlo después"
          />

          {/* Los 400 se pintan por campo; el 409 y el resto van acá. */}
          {mutacion.error instanceof ApiError && mutacion.error.errors ? null : (
            <MensajeError error={mutacion.error} />
          )}

          <Boton type="submit" enCurso={mutacion.isPending}>
            Crear cuenta
          </Boton>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          ¿Ya tenés cuenta?{' '}
          <Link to="/login" className="font-medium text-emerald-700 hover:underline">
            Iniciá sesión
          </Link>
        </p>
      </div>
    </div>
  )
}
