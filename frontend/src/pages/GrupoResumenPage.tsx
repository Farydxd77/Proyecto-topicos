import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { listarBajas } from '../api/bajas'
import { obtenerBalances, obtenerResumen } from '../api/balances'
import { Boton } from '../components/Boton'
import { Card } from '../components/Card'
import { EstadoVacio } from '../components/EstadoVacio'
import { MensajeError } from '../components/MensajeError'
import { Monto, tonoDeSaldo } from '../components/Monto'
import { claveBajas, claveBalances, claveResumen } from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { useGrupo } from './GrupoLayout'

/** Traduce el signo del balance a una frase: «−200» obliga a recordar la convención. */
function leerBalance(balance: number): string {
  if (balance > 0) return 'Te deben'
  if (balance < 0) return 'Debés'
  return 'Estás a mano'
}

function AccesoRapido({
  to,
  titulo,
  detalle,
}: {
  to: string
  titulo: string
  detalle: string
}) {
  return (
    <Link
      to={to}
      className="flex items-center justify-between gap-3 rounded-lg border border-tinta-200 bg-white px-4 py-3 shadow-tarjeta transition-colors duration-150 hover:border-marca-300 hover:bg-marca-50/40"
    >
      <span className="min-w-0">
        <span className="block font-medium text-tinta-900">{titulo}</span>
        <span className="block text-sm text-tinta-500">{detalle}</span>
      </span>
      <span aria-hidden className="shrink-0 text-tinta-400">
        →
      </span>
    </Link>
  )
}

export function GrupoResumenPage() {
  const { grupo, participanteId } = useGrupo()
  const base = `/grupos/${grupo.id}`

  const cResumen = useQuery({
    queryKey: claveResumen(grupo.id),
    queryFn: () => obtenerResumen(grupo.id),
  })
  const cBalances = useQuery({
    queryKey: claveBalances(grupo.id),
    queryFn: () => obtenerBalances(grupo.id),
  })
  // La trae el layout: acá sale del caché, sin petición nueva.
  const { data: bajas } = useQuery({
    queryKey: claveBajas(grupo.id),
    queryFn: () => listarBajas(grupo.id),
  })

  const eResumen = estadoDe(cResumen)
  const eBalances = estadoDe(cBalances)

  if (eResumen.cargando) {
    return <p className="py-10 text-center text-tinta-500">Calculando…</p>
  }
  if (eResumen.error || !eResumen.datos) {
    return (
      <Card>
        <div className="flex flex-col items-start gap-3">
          <MensajeError error={eResumen.error} />
          <Boton variante="secundario" onClick={() => cResumen.refetch()}>
            Reintentar
          </Boton>
        </div>
      </Card>
    )
  }

  const r = eResumen.datos
  const miBalance =
    eBalances.datos?.find((b) => b.participante.id === participanteId)?.balance ?? 0
  const pendientes = (bajas ?? []).filter((b) => b.estado === 'PENDIENTE')

  // Un grupo recién creado no gana nada mostrando seis ceros: gana sabiendo qué hacer.
  if (r.cantidadGastos === 0) {
    return (
      <div className="flex flex-col gap-4">
        <EstadoVacio
          titulo="Todavía no hay nada gastado"
          descripcion={
            grupo.miembros.length === 1
              ? 'Empezá agregando a la gente del viaje, y después cargá el primer gasto. Nosotros nos encargamos de repartirlo.'
              : 'Cargá el primer gasto y la aplicación calcula sola quién le debe cuánto a quién.'
          }
          accion={
            <div className="flex flex-wrap justify-center gap-3">
              {grupo.miembros.length === 1 ? (
                <Link to={`${base}/miembros`}>
                  <Boton>Agregar integrantes</Boton>
                </Link>
              ) : null}
              <Link to={`${base}/gastos`}>
                <Boton variante={grupo.miembros.length === 1 ? 'secundario' : 'primario'}>
                  Registrar el primer gasto
                </Boton>
              </Link>
            </div>
          }
        />
      </div>
    )
  }

  const deudaTotal = r.totalPagado + r.pendientePorSaldar
  const saldado = r.pendientePorSaldar === 0
  const porcentaje = deudaTotal > 0 ? Math.round((r.totalPagado / deudaTotal) * 100) : 0

  return (
    <div className="flex flex-col gap-5">
      {/* Avisos que piden acción. Se gestionan en Miembros, pero si solo se ven ahí
          nadie se entera: acá se duplica la SEÑAL, no la función. */}
      {pendientes.length > 0 ? (
        <Card tono="aviso">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="min-w-0">
              <p className="font-medium text-aviso-900">
                {pendientes.length === 1
                  ? 'Hay una baja sin resolver'
                  : `Hay ${pendientes.length} bajas sin resolver`}
              </p>
              <p className="mt-0.5 text-sm text-aviso-900/80">
                {pendientes.map((b) => b.participante.nombre).join(', ')} dejó el grupo
                con cuentas pendientes. Hay que decidir si el grupo se hace cargo.
              </p>
            </div>
            <Link to={`${base}/miembros`} className="shrink-0">
              <Boton tamano="chico">Resolver</Boton>
            </Link>
          </div>
        </Card>
      ) : null}

      {/* La cifra héroe: la única de la pantalla en ese tamaño. */}
      <Card>
        <div className="flex flex-wrap items-end justify-between gap-6">
          <div>
            <p className="text-sm text-tinta-600">Total del viaje</p>
            <p className="mt-1">
              <Monto valor={r.totalGastado} tamano="heroe" />
            </p>
            <p className="mt-1.5 text-sm text-tinta-500">
              {r.cantidadGastos} {r.cantidadGastos === 1 ? 'gasto' : 'gastos'} · todo
              convertido a USDT
            </p>
          </div>

          <div className="text-right">
            <p className="text-sm text-tinta-600">Tu parte</p>
            <p className="mt-1">
              <Monto valor={r.miParte} tamano="destacado" />
            </p>
          </div>
        </div>
      </Card>

      <div className="grid gap-4 sm:grid-cols-2">
        {/* Mi situación. El color acompaña, pero el sentido lo lleva el texto. */}
        <Card titulo="Tu situación">
          {eBalances.cargando ? (
            <p className="text-sm text-tinta-500">Calculando…</p>
          ) : (
            <>
              <p className="text-sm text-tinta-600">{leerBalance(miBalance)}</p>
              <p className="mt-1">
                <Monto
                  valor={miBalance}
                  tamano="destacado"
                  tono={tonoDeSaldo(miBalance)}
                  absoluto
                  moneda={miBalance === 0 ? null : 'USDT'}
                />
              </p>
              <Link
                to={`${base}/balances`}
                className="mt-3 inline-block text-sm font-medium text-marca-700 hover:underline"
              >
                Ver todos los balances →
              </Link>
            </>
          )}
        </Card>

        {/* Avance de la liquidación. Se mide sobre lo que falta saldar, NO sobre el
            total gastado: quien paga un gasto ya cubre su propia parte. */}
        <Card titulo="Cómo va la liquidación">
          <div className="flex items-baseline justify-between gap-3">
            <span className="text-sm text-tinta-600">Ya saldado</span>
            <Monto valor={r.totalPagado} tamano="chico" />
          </div>
          <div className="mt-1 flex items-baseline justify-between gap-3">
            <span className="text-sm text-tinta-600">Falta saldar</span>
            <Monto valor={r.pendientePorSaldar} tamano="chico" />
          </div>

          {deudaTotal > 0 ? (
            <>
              <div
                className="mt-3 h-2 w-full overflow-hidden rounded-full bg-tinta-200"
                role="progressbar"
                aria-valuenow={porcentaje}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-label="Avance de la liquidación"
              >
                <div
                  className="h-full rounded-full bg-marca-500 transition-all duration-300"
                  style={{ width: `${porcentaje}%` }}
                />
              </div>
              <p className="mt-2 text-sm text-tinta-700">
                {saldado
                  ? 'Están todos a mano.'
                  : `Se saldó el ${porcentaje}% de lo que hay que transferir.`}
              </p>
            </>
          ) : (
            <p className="mt-3 text-sm text-tinta-600">Nadie le debe nada a nadie.</p>
          )}
        </Card>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <AccesoRapido
          to={`${base}/gastos`}
          titulo="Gastos"
          detalle={`${r.cantidadGastos} ${r.cantidadGastos === 1 ? 'registrado' : 'registrados'}`}
        />
        <AccesoRapido
          to={`${base}/pagos`}
          titulo="Pagos"
          detalle={`${r.cantidadPagos} ${r.cantidadPagos === 1 ? 'registrado' : 'registrados'}`}
        />
        <AccesoRapido
          to={`${base}/miembros`}
          titulo="Integrantes"
          detalle={`${grupo.miembros.length} en el grupo`}
        />
      </div>

      <p className="text-xs text-tinta-500">
        «Ya saldado» cuenta solo las transferencias entre integrantes, así que no llega
        al total del viaje: quien paga un gasto ya cubre su propia parte en ese momento.
      </p>
    </div>
  )
}
