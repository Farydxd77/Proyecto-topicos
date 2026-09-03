import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { login } from '../api/auth'
import { useAuth } from '../auth/useAuth'
import { Boton } from '../components/Boton'
import { Campo } from '../components/Campo'
import { MensajeError } from '../components/MensajeError'
import { soloErrores, validarObligatorio } from '../lib/validacion'

export function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [errores, setErrores] = useState<Record<string, string>>({})

  const { iniciarSesion, expiro, limpiarAvisoExpiro } = useAuth()
  const navigate = useNavigate()
  const ubicacion = useLocation()

  // Ruta que se intentó visitar antes de ser redirigido acá.
  const destino = (ubicacion.state as { desde?: string } | null)?.desde ?? '/perfil'

  const mutacion = useMutation({
    mutationFn: login,
    onSuccess: (sesion) => {
      iniciarSesion(sesion.token)
      navigate(destino, { replace: true })
    },
  })

  function enviar(e: FormEvent) {
    e.preventDefault()

    const encontrados = soloErrores({
      username: validarObligatorio(username, 'El username'),
      password: validarObligatorio(password, 'La contraseña'),
    })
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) return

    limpiarAvisoExpiro()
    mutacion.mutate({ username, password })
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-1 text-2xl font-semibold text-slate-900">Cuentas Claras</h1>
        <p className="mb-6 text-sm text-slate-600">Iniciá sesión para continuar</p>

        {expiro ? (
          <div className="mb-4">
            <MensajeError error="Tu sesión expiró. Iniciá sesión de nuevo." tono="aviso" />
          </div>
        ) : null}

        <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
          <Campo
            id="username"
            etiqueta="Username"
            valor={username}
            onChange={setUsername}
            error={errores.username}
            autoComplete="username"
          />
          <Campo
            id="password"
            etiqueta="Contraseña"
            tipo="password"
            valor={password}
            onChange={setPassword}
            error={errores.password}
            autoComplete="current-password"
          />

          {/* El 401 del backend no distingue qué campo falló: se muestra como error
              general, nunca junto a un campo. */}
          <MensajeError error={mutacion.error} />

          <Boton type="submit" enCurso={mutacion.isPending}>
            Iniciar sesión
          </Boton>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          ¿No tenés cuenta?{' '}
          <Link to="/registro" className="font-medium text-emerald-700 hover:underline">
            Registrate
          </Link>
        </p>
      </div>
    </div>
  )
}
