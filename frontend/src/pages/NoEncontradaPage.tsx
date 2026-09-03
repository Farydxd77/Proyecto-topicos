import { Link } from 'react-router'

export function NoEncontradaPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 px-4">
      <p className="text-4xl font-semibold text-slate-300">404</p>
      <h1 className="text-xl font-semibold text-slate-900">Esta página no existe</h1>
      <p className="text-sm text-slate-600">
        Puede que el enlace esté mal escrito o que la página se haya movido.
      </p>
      <Link
        to="/"
        className="mt-2 rounded-md bg-emerald-600 px-4 py-2 font-medium text-white transition hover:bg-emerald-700"
      >
        Volver al inicio
      </Link>
    </div>
  )
}
