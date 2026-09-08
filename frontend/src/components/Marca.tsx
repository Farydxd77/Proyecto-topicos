/**
 * La marca: un isotipo simple más el nombre.
 *
 * Las dos barras desiguales que se emparejan son la idea de la aplicación en una
 * imagen: cuentas que se equilibran. Es SVG inline y no un archivo porque toma el
 * color de marca del sistema en lugar de traerlo horneado.
 */
export function Marca({ tamano = 'normal' }: { tamano?: 'normal' | 'grande' }) {
  const grande = tamano === 'grande'

  return (
    <span className="inline-flex items-center gap-2">
      <svg
        viewBox="0 0 24 24"
        aria-hidden
        className={grande ? 'h-8 w-8' : 'h-6 w-6'}
        fill="none"
      >
        <rect x="3" y="4" width="18" height="4.5" rx="2.25" className="fill-marca-600" />
        <rect x="3" y="11" width="11" height="4.5" rx="2.25" className="fill-marca-400" />
        <rect x="3" y="18" width="18" height="3" rx="1.5" className="fill-marca-200" />
      </svg>
      <span
        className={`font-semibold tracking-tight text-tinta-900 ${
          grande ? 'text-2xl' : 'text-base'
        }`}
      >
        Cuentas Claras
      </span>
    </span>
  )
}
