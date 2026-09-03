## Context

Ver `proposal.md` — Why. Estado actual relevante:

- `vite.config.ts` **ya tiene** el proxy `'/api' → http://localhost:8080` con
  `changeOrigin: true`, y su comentario dice explícitamente que quedó preparado
  "para el día que se apaguen los mocks y haya backend real". Ese día es este
  cambio.
- `src/main.tsx` arranca MSW dentro de `if (import.meta.env.DEV)` con import
  dinámico, de modo que el bundler lo elimina del build de producción. El guard es
  incondicional: hoy, en desarrollo, siempre gana la simulación.
- `src/api/client.ts` ya usa rutas relativas (`const BASE = '/api'`), normaliza
  todo fallo a `ApiError`, distingue el 401 de `/auth/**` (credenciales) del 401 de
  ruta protegida (sesión expirada), tolera cuerpos vacíos y traduce el fallo de red
  a `ApiError(0, ...)`. No hay ninguna URL absoluta en el código.
- `src/api/types.ts` espeja los records de `dto/` del backend y es la única
  definición de contratos, compartida por mocks y producción.
- El backend expone hoy `/api/auth/**`, `/api/perfil/**`, `/api/usuarios/**`,
  `/api/participantes/**`, `/api/grupos/**`, `/api/grupos/{id}/gastos/**`,
  `/api/grupos/{id}/balances` y `/api/grupos/{id}/liquidacion`.
- `SecurityConfig` protege todo salvo `/api/auth/**` y `/actuator/health/**`.
- El backend usa `ddl-auto=update` contra PostgreSQL real: no hay base embebida.

## Goals / Non-Goals

**Goals:**

- Cambiar el origen de datos por defecto con la mínima superficie de código
  posible, y de forma reversible.
- Descubrir ahora, con dos capacidades ya construidas y estables, cualquier
  divergencia entre los mocks y el backend real — antes de que haya cinco
  pantallas apoyadas en supuestos equivocados.
- Dejar el arranque del sistema documentado y diagnosticable.

**Non-Goals (nivel diseño):**

- No se reescribe la capa de API: `apiFetch` ya está diseñada para esto.
- No se introduce un cliente HTTP nuevo (axios u otro).
- No se añade configuración de entorno más allá de una variable booleana.
- No se toca el backend, ni siquiera para CORS (ver decisión 2).

## Decisions

### 1. Los mocks se apagan con una bandera, no se borran

`main.tsx` pasa de `if (import.meta.env.DEV)` a
`if (import.meta.env.DEV && import.meta.env.VITE_USAR_MOCKS === 'true')`. Todo
`src/mocks/` se conserva intacto.

- **Por qué:** conservar el modo simulado tiene valor real y comprobado en este
  proyecto: durante toda esta fase la base de datos estuvo caída más de una vez, y
  poder seguir trabajando en la interfaz sin PostgreSQL es la diferencia entre
  avanzar y quedarse bloqueado. La comparación con la simulación también sirve de
  documentación viva del contrato. Además, el import dinámico dentro del guard
  sigue garantizando que msw no viaje al build de producción.
- **Por qué la comparación explícita con la cadena `'true'`:** las variables de
  Vite llegan siempre como `string`; `VITE_USAR_MOCKS=false` es la cadena
  `"false"`, que es *truthy*. Comparar contra `'true'` evita el error clásico de
  que apagar la bandera no apague nada.
- **Alternativas descartadas:**
  - Borrar `src/mocks/` y el service worker: irreversible, y tira una simulación
    fiel del contrato que costó construir.
  - Invertir la bandera (`VITE_USAR_BACKEND_REAL`): dejaría el modo simulado como
    comportamiento por defecto, que es justo lo que este cambio quiere terminar.
  - Elegir por modo de Vite (`--mode mock`): más ceremonia y un archivo de
    configuración extra para un booleano.

### 2. No se configura CORS: el proxy de Vite lo vuelve innecesario en desarrollo

Las peticiones salen a `/api/...` relativo, el navegador las dirige a
`localhost:5173`, y Vite las reenvía a `localhost:8080` **del lado del servidor**.
La política de origen cruzado es una regla del navegador; como el navegador solo
ve un origen, no hay preflight ni cabeceras `Access-Control-*` que negociar.

- **Por qué importa dejarlo escrito:** `CLAUDE.md` lista CORS como pendiente y es
  fácil concluir que bloquea la integración. No la bloquea en desarrollo. Confundir
  ambas cosas lleva a tocar `SecurityConfig` sin necesidad.
- **Cuándo sí hará falta:** cuando frontend y backend se sirvan desde dominios
  distintos en producción. Eso es un cambio de despliegue, con su propio origen
  permitido, y no pertenece acá.
- **Alternativa descartada:** configurar `CorsConfig` igual "por las dudas" —
  añade superficie de seguridad (un origen permitido más) sin resolver ningún
  problema presente.

### 3. La verificación es manual, guiada y sobre las capacidades ya construidas

No hay runner de tests en el frontend (`package.json` no define `test`). La
verificación de cada tarea es un recorrido manual descrito en la propia tarea,
sobre auth y perfil.

- **Por qué sobre auth y perfil:** son las dos capacidades que el frontend ya tiene
  terminadas y cuyo comportamiento esperado está especificado en
  `changes/init-frontend/specs/`. Sirven de banco de pruebas del canal real sin
  mezclar el riesgo de código nuevo con el riesgo de integración. Si algo falla,
  la causa es la integración.
- **Alternativa descartada:** montar Vitest y Testing Library en este cambio —
  es una decisión de infraestructura de pruebas con peso propio; mezclarla acá
  hace que un cambio de integración arrastre una elección de stack.

### 4. Las divergencias se resuelven siempre a favor del backend real

Si el backend real devuelve algo distinto de lo que asumían los mocks, se corrige
el mock y, si hace falta, `types.ts` — nunca al revés.

Los puntos donde la divergencia es más probable, y que las tareas verifican de
forma explícita:

- **`createdAt` de `PerfilResponse`:** Jackson serializa `LocalDateTime` sin zona
  horaria; los mocks emiten una cadena ISO con `Z`. Si el frontend la formatea,
  puede desplazarse por huso.
- **`PUT /api/perfil/me/password`:** el backend responde `200` con cuerpo vacío.
  `apiFetch` ya lo tolera, pero es el caso que más fácilmente rompe un cliente.
- **El mapa `errors` de los 400:** lo produce `MethodArgumentNotValidException`
  con los mensajes en español definidos en las anotaciones de los DTOs. Los
  nombres de campo deben coincidir con los del formulario.
- **El 401 sin token:** lo emite `JwtAuthenticationEntryPoint`, no
  `GlobalExceptionHandler`. Su cuerpo tiene la misma forma, pero el `message` sale
  de la excepción de Spring Security y puede venir en inglés.

- **Por qué esta regla:** el backend es el sistema que va a producción; el mock es
  una conveniencia de desarrollo. Un mock que miente es peor que no tener mock.

### 5. `types.ts` no se amplía en este cambio

Los contratos de grupos, gastos y balances se añaden en sus cambios respectivos,
junto con las pantallas que los usan.

- **Por qué:** añadir ahora quince interfaces que nadie consume produce código
  muerto y obliga a revisarlas dos veces. Cada cambio posterior traerá las suyas
  con su pantalla, y `types.ts` seguirá siendo la única fuente de contratos.

## Risks / Trade-offs

- **PostgreSQL debe estar corriendo para ejecutar y verificar este cambio** → sin
  base no hay backend, y sin backend no hay nada que verificar. Mitigación: es un
  requisito explícito del cambio, documentado en el README y en la primera tarea;
  y el modo simulado que este cambio conserva permite seguir trabajando en
  interfaz mientras tanto.
- **La primera conexión real puede destapar varias divergencias a la vez** y
  convertir un cambio corto en uno largo → Mitigación: se acota a auth y perfil,
  que son dos capacidades pequeñas y ya estables; cualquier divergencia aparece en
  un recorrido de diez minutos, no repartida entre cinco pantallas.
- **`ddl-auto=update` sobre una base con datos previos** puede dejar el esquema en
  un estado intermedio si alguna entidad cambió desde el último arranque →
  Mitigación: la primera tarea verifica el arranque limpio del backend y que
  Hibernate no reporte errores de esquema.
- **Los datos dejan de ser desechables:** con los mocks, recargar reiniciaba todo;
  contra el backend real las cuentas de prueba persisten y el username es único →
  Mitigación: las tareas de verificación usan usernames con sufijo variable, igual
  que hacen los tests de integración del backend.
- **La bandera puede quedar activada por accidente** en el entorno de alguien y dar
  la falsa impresión de que la integración funciona → Mitigación: `.env.example`
  la documenta apagada, y la aplicación es distinguible por comportamiento (con
  mocks, el usuario semilla existe siempre; contra el backend real, no).

## Migration Plan

No hay migración de datos. El cambio es reversible en un archivo: volver a activar
`VITE_USAR_MOCKS=true` restituye el comportamiento anterior sin tocar código. No
se elimina ningún artefacto, así que el rollback es revertir el commit y no queda
estado que limpiar.

Orden de arranque para ejecutar y verificar:

1. PostgreSQL en `5432` con la base `cuentas_claras`.
2. Backend: `mvnw.cmd spring-boot:run` desde `backend/` (puerto `8080`).
3. Frontend: `npm run dev` desde `frontend/` (puerto `5173`).
