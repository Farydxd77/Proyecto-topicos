import { Outlet } from 'react-router'
import { Navegacion } from './Navegacion'

/** Marco común de las pantallas privadas. Login y registro no lo llevan. */
export function Layout() {
  return (
    <div className="min-h-screen">
      <Navegacion />
      <main className="mx-auto max-w-3xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
