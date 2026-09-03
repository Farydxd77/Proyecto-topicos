interface CampoProps {
  id: string
  etiqueta: string
  valor: string
  onChange: (valor: string) => void
  tipo?: 'text' | 'password'
  error?: string
  autoComplete?: string
  soloLectura?: boolean
  ayuda?: string
}

export function Campo({
  id,
  etiqueta,
  valor,
  onChange,
  tipo = 'text',
  error,
  autoComplete,
  soloLectura = false,
  ayuda,
}: CampoProps) {
  const idError = `${id}-error`
  const idAyuda = `${id}-ayuda`

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-slate-700">
        {etiqueta}
      </label>

      <input
        id={id}
        type={tipo}
        value={valor}
        onChange={(e) => onChange(e.target.value)}
        readOnly={soloLectura}
        autoComplete={autoComplete}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? idError : ayuda ? idAyuda : undefined}
        className={`rounded-md border px-3 py-2 text-slate-900 outline-none transition focus:ring-2 ${
          soloLectura
            ? 'border-slate-200 bg-slate-100 text-slate-500'
            : error
              ? 'border-red-400 bg-white focus:ring-red-200'
              : 'border-slate-300 bg-white focus:border-emerald-500 focus:ring-emerald-200'
        }`}
      />

      {error ? (
        <p id={idError} role="alert" className="text-sm text-red-600">
          {error}
        </p>
      ) : ayuda ? (
        <p id={idAyuda} className="text-sm text-slate-500">
          {ayuda}
        </p>
      ) : null}
    </div>
  )
}
