import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    /**
     * El frontend habla siempre con el backend real. Las peticiones salen a `/api`
     * relativo, el navegador las dirige a este mismo servidor y Vite las reenvía a
     * Spring Boot del lado del servidor.
     *
     * Por eso no hace falta configurar CORS en desarrollo: el navegador solo ve un
     * origen (`localhost:5173`), así que no hay petición cruzada que negociar. CORS
     * será necesario únicamente si en producción el frontend y el backend se sirven
     * desde dominios distintos.
     */
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
