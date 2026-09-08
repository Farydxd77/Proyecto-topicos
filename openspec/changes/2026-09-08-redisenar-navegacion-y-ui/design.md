## Decisión 1: rutas anidadas con un layout de grupo, no pestañas por estado local

`/grupos/:id` pasa a ser un `GrupoLayout` con `<Outlet />`, y cada pestaña es una ruta
hija real.

**Alternativa descartada: un `useState` con la pestaña activa.** Es más simple de
escribir y peor en todo lo demás: no se puede compartir un enlace a los balances de un
grupo, el botón «atrás» del navegador no vuelve a la pestaña anterior, y recargar
siempre cae en la primera. Con rutas reales, cada pantalla tiene URL, y eso es lo que
convierte «una pantalla con secciones» en «una aplicación».

La consulta del grupo vive en el layout. Como las pestañas son hijas, cambiar de
pestaña **no desmonta el layout** y la consulta no se repite. Es la razón principal
para anidar en vez de tener cinco rutas hermanas que cada una pida el grupo.

## Decisión 2: el Resumen es una pantalla, no un índice

Podría haber sido una lista de accesos a las otras cuatro. En cambio responde de una
las tres preguntas con las que alguien abre la aplicación:

1. **¿Cuánto llevamos gastado?** → el total del viaje, en el tamaño más grande de la
   pantalla.
2. **¿Cómo estoy yo?** → mi balance, interpretado en palabras, y mi parte.
3. **¿Qué falta?** → el avance de la liquidación y, si hay, los avisos que piden
   acción.

Más los últimos movimientos, que es lo que da la sensación de que el grupo está vivo.

**Los avisos que requieren acción aparecen acá aunque su lugar sea otra pestaña.** Una
baja pendiente de decidir se gestiona en Miembros, pero si solo se ve ahí, nadie se
entera. El Resumen la muestra como aviso con enlace; la gestión completa vive en su
pestaña. Es duplicación deliberada de la *señal*, no de la *función*.

## Decisión 3: cada pantalla pide solo lo suyo

Hoy entrar al grupo dispara seis consultas. Con el split:

| Pantalla | Consultas |
|----------|-----------|
| Resumen | grupo (del layout) + resumen + bajas |
| Gastos | grupo + gastos |
| Pagos | grupo + pagos |
| Balances | grupo + balances + liquidación + gastos |
| Miembros | grupo + bajas |

No hace falta tocar `clavesDerivadasDelGrupo`: sigue invalidando todo lo derivado, y
TanStack Query solo re-pide lo que hay montado. Invalidar una clave que nadie observa
no cuesta una petición.

## Decisión 4: los tokens se definen con `@theme`, no con clases arbitrarias

Tailwind 4 genera utilidades a partir de variables CSS declaradas en `@theme`. Definir
`--color-marca-600` produce `bg-marca-600`, `text-marca-600`, etc.

**Alternativa descartada: seguir usando `emerald-600` y `slate-700` directamente.** Es
lo que hay hoy y es el motivo de que no haya sistema: cuando el color de marca es el
nombre de una paleta genérica, cambiarlo obliga a un buscar-y-reemplazar por todo el
código, y nada impide que mañana alguien use `green-600` porque «quedaba mejor».

Nombrar por rol (`marca`, `tinta`, `lienzo`, `favor`, `contra`) es además lo que deja
el modo oscuro a un `@media` de distancia, aunque este cambio no lo entregue.

**La paleta:** se mantiene el verde esmeralda como color de marca —es el que la
aplicación ya tiene y funciona para «cuentas claras»— pero el neutro pasa de `slate`
(frío, azulado) a un gris con una pizca de calidez. El contraste entre el verde y un
neutro cálido se siente menos «plantilla de framework» que verde sobre azul-gris.

## Decisión 5: los montos tienen su propio componente

`<Monto valor={900} />` en vez de `{formatearMonto(900)} USDT` repetido en once
archivos.

Centraliza tres cosas que hoy se resuelven caso por caso: las **cifras tabulares**
(sin ellas los números bailan al actualizarse), el **color semántico** según el signo,
y el **tamaño** dentro de una escala fija. Un monto que representa dinero a favor se
ve igual en el Resumen, en Balances y en la fila de un pago, porque sale del mismo
lugar.

Sigue sin hacer aritmética: `lib/formato.ts` formatea, nunca opera. La convención del
proyecto no cambia.

## Decisión 6: login y registro con composición partida

Dos columnas: a la izquierda un panel de marca con el nombre, una frase y una prueba
visual de lo que hace la aplicación; a la derecha el formulario. En pantallas chicas
el panel se colapsa a una cabecera.

Es la pantalla que más rinde por trabajo invertido —es lo primero que ve cualquiera
que abra la aplicación— y hoy es un formulario centrado sobre gris plano.

## Decisión 7: las secciones existentes pierden su cabecera

`SeccionGastos`, `SeccionPagos`, `SeccionBalances`, `SeccionBajas`, `GestionMiembros` y
`PanelTotales` hoy renderizan cada una su propio `<section>` con `<h2>` y descripción,
porque convivían apiladas. Ahora la cabecera la pone la **pantalla**, y el componente
entrega solo su contenido.

Sin esto quedarían dos títulos por pantalla, uno dentro del otro. El cambio es
mecánico pero toca los seis componentes.

## Jerarquía tipográfica

| Rol | Tamaño | Uso |
|-----|--------|-----|
| Cifra héroe | `text-5xl` tabular | El total del viaje en el Resumen |
| Cifra destacada | `text-2xl` tabular | Mi balance, totales secundarios |
| Título de pantalla | `text-2xl` | Nombre del grupo |
| Título de bloque | `text-base` semibold | Cabecera de una tarjeta |
| Cuerpo | `text-sm` | Todo lo demás |
| Auxiliar | `text-xs` | Fechas, notas al pie, aclaraciones |

La regla que evita que se degrade: **una sola cifra héroe por pantalla**. Si todo se
destaca, no se destaca nada.

## Riesgos

- Es el cambio con más superficie de todo el proyecto en archivos tocados, y ninguno
  está cubierto por tests automatizados: el frontend no tiene runner. Se acota
  verificando cada pantalla en el navegador antes de dar el cambio por bueno.
- Partir en cinco pantallas agrega clics para quien quería ver dos cosas a la vez. Se
  mitiga con el Resumen, que trae lo que se mira junto —el total, mi situación y el
  avance— sin obligar a recorrer las pestañas.
