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
  /** Texto de ejemplo dentro del campo. */
  ejemplo?: string
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
  ejemplo,
}: CampoProps) {
  const idError = `${id}-error`
  const idAyuda = `${id}-ayuda`

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium text-tinta-800">
        {etiqueta}
      </label>

      <input
        id={id}
        type={tipo}
        value={valor}
        onChange={(e) => onChange(e.target.value)}
        readOnly={soloLectura}
        autoComplete={autoComplete}
        placeholder={ejemplo}
        aria-invalid={error ? true : undefined}
        // El mensaje queda asociado al campo: un lector de pantalla lo anuncia al
        // enfocarlo, no solo al llegar visualmente.
        aria-describedby={error ? idError : ayuda ? idAyuda : undefined}
        className={`rounded-lg border px-3 py-2 text-tinta-900 transition-colors duration-150 placeholder:text-tinta-400 ${
          soloLectura
            ? 'border-tinta-200 bg-tinta-100 text-tinta-500'
            : error
              ? 'border-contra-200 bg-white'
              : 'border-tinta-300 bg-white hover:border-tinta-400'
        }`}
      />

      {error ? (
        <p id={idError} role="alert" className="text-sm text-contra-700">
          {error}
        </p>
      ) : ayuda ? (
        <p id={idAyuda} className="text-xs text-tinta-500">
          {ayuda}
        </p>
      ) : null}
    </div>
  )
}
