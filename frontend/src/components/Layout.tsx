import { Outlet } from 'react-router'
import { Navegacion } from './Navegacion'

/** Marco común de las pantallas privadas. Login y registro no lo llevan. */
export function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Navegacion />
      <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
