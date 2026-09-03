## Why

El backend ya expone sus seis capacidades completas (auth, perfil, gestión general,
grupos, gastos con conversión de moneda, y balances con liquidación), pero el
frontend sigue hablando con la API simulada de MSW: ninguna pantalla ha tocado
nunca el backend real. Mientras ese canal no se abra, cada pantalla nueva que se
construya se valida contra mocks escritos a mano, y la divergencia entre lo
simulado y lo real solo se descubre al final, cuando ya hay cinco pantallas que
corregir.

Este cambio abre el canal real y lo deja verificado con las dos capacidades que
el frontend ya tiene construidas (auth y perfil), de modo que todo lo que venga
después —grupos, miembros, gastos, balances— se construya directamente contra el
backend real.

## What Changes

- El arranque de la aplicación (`src/main.tsx`) deja de iniciar MSW de forma
  incondicional en desarrollo: los mocks quedan detrás de la variable de entorno
  `VITE_USAR_MOCKS`, apagada por defecto. Sin esa variable, toda petición sale por
  el proxy de Vite hacia el backend real.
- Se documenta y verifica el arranque conjunto: PostgreSQL en `5432`, backend
  Spring Boot en `8080`, Vite en `5173`. El proxy `/api → http://localhost:8080`
  ya existe en `vite.config.ts` y no se modifica.
- Se verifica end-to-end contra el backend real el flujo completo que el frontend
  ya implementa: registro, inicio de sesión, persistencia de la sesión entre
  recargas, consulta y edición del perfil, cambio de username, cambio de
  contraseña, cierre de sesión y expiración del token.
- Se corrige cualquier divergencia que aparezca entre lo que devuelve el backend
  real y lo que asumían los mocks (formato de fechas, forma del cuerpo de error,
  respuestas con cuerpo vacío).
- Se añade a `frontend/README.md` la sección de puesta en marcha con los tres
  procesos y el orden en que deben arrancar.
- Se actualiza `CLAUDE.md`: el frontend deja de desarrollarse contra MSW y pasa a
  desarrollarse contra el backend real; grupos, gastos y balances salen de la
  lista de "fuera de alcance" del frontend.
- Los mocks (`src/mocks/`) **no se borran**: quedan como herramienta opcional para
  trabajar sin base de datos, activables con `VITE_USAR_MOCKS=true`.

## Non-Goals

- No se construye ninguna pantalla nueva: este cambio solo abre y verifica el
  canal. Grupos, miembros, gastos y balances son los cambios siguientes.
- No se configura CORS en el backend: el proxy de Vite hace que el navegador vea
  un único origen (`localhost:5173`), así que en desarrollo no hay petición
  cruzada. CORS será necesario solo si en producción el frontend y el backend se
  sirven desde dominios distintos, y eso es un cambio aparte.
- No se toca ningún archivo de `backend/`.
- No se despliega a producción ni se configura build de producción.
- No se añaden pruebas automatizadas de frontend (no hay runner de tests
  configurado); la verificación de este cambio es manual y está descrita en cada
  tarea.
- No se amplía `src/api/types.ts` con los contratos de grupos, gastos ni balances:
  cada cambio posterior añade los suyos.

## Capabilities

### New Capabilities

- `frontend-integracion`: el frontend consume la API real del backend a través del
  proxy de desarrollo, con los mocks desactivados por defecto y disponibles
  únicamente como modo opcional explícito. Cubre la selección de origen de datos,
  el arranque conjunto de los tres procesos y el comportamiento de la aplicación
  cuando el backend no está disponible.

### Modified Capabilities

<!-- Ninguna: las capacidades de auth y perfil no cambian de comportamiento, solo
     pasan a ejercitarse contra el backend real. -->

## Impact

- **Código modificado**:
  - `frontend/src/main.tsx`: el arranque de MSW pasa a depender de
    `import.meta.env.VITE_USAR_MOCKS === 'true'` además de `DEV`.
  - `frontend/src/api/types.ts` y `frontend/src/api/client.ts`: solo si la
    verificación end-to-end revela una divergencia real con el backend.
  - `frontend/README.md`: sección de puesta en marcha.
  - `CLAUDE.md`: fase actual, convenciones del frontend y fuera de alcance.
- **Código nuevo**: `frontend/.env.example` documentando `VITE_USAR_MOCKS`.
- **Sin cambios**: `vite.config.ts` (el proxy ya está), `src/mocks/**` (se
  conservan), y todo `backend/`.
- **Dependencias**: ninguna nueva. `msw` sigue como dependencia de desarrollo.
- **Requisito de entorno**: PostgreSQL corriendo en `localhost:5432` con la base
  `cuentas_claras`, y el backend arrancado con `mvnw.cmd spring-boot:run`. Sin
  eso este cambio no se puede verificar ni ejecutar.
