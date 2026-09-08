# Modelo de Datos — Cuentas Claras

## Convenciones generales
- PKs: BIGINT autoincremental (BIGSERIAL en PostgreSQL, @GeneratedValue IDENTITY en JPA)
- Todas las tablas tienen created_at y updated_at (TIMESTAMP NOT NULL DEFAULT now())
- Tablas: snake_case plural
- Columnas: snake_case

## Tabla: usuarios
| Columna    | Tipo         | Restricciones          |
|------------|--------------|------------------------|
| id         | BIGSERIAL    | PK                     |
| username   | VARCHAR(50)  | NOT NULL, UNIQUE       |
| password   | VARCHAR(255) | NOT NULL (BCrypt)      |
| created_at | TIMESTAMP    | NOT NULL, DEFAULT now()|
| updated_at | TIMESTAMP    | NOT NULL, DEFAULT now()|

## Tabla: participantes
| Columna    | Tipo         | Restricciones                    |
|------------|--------------|----------------------------------|
| id         | BIGSERIAL    | PK                               |
| usuario_id | BIGINT       | NOT NULL, UNIQUE, FK→usuarios.id |
| nombre     | VARCHAR(100) | NOT NULL                         |
| apellido   | VARCHAR(100) | NOT NULL                         |
| ci         | VARCHAR(20)  | NOT NULL                         |
| created_at | TIMESTAMP    | NOT NULL                         |
| updated_at | TIMESTAMP    | NOT NULL                         |

> Relación 1 a 1 con usuarios. Se crean juntos al registrarse.

## Tabla: grupos
| Columna     | Tipo         | Restricciones              |
|-------------|--------------|----------------------------|
| id          | BIGSERIAL    | PK                         |
| nombre      | VARCHAR(100) | NOT NULL                   |
| descripcion | TEXT         | nullable                   |
| creador_id  | BIGINT       | FK→participantes.id        |
| created_at  | TIMESTAMP    | NOT NULL                   |
| updated_at  | TIMESTAMP    | NOT NULL                   |

## Tabla: grupo_participantes
| Columna         | Tipo      | Restricciones              |
|-----------------|-----------|----------------------------|
| grupo_id        | BIGINT    | FK→grupos.id               |
| participante_id | BIGINT    | FK→participantes.id        |
| joined_at       | TIMESTAMP | NOT NULL, DEFAULT now()    |

> PK compuesta: (grupo_id, participante_id)

## Tabla: gastos
| Columna           | Tipo              | Restricciones              |
|-------------------|-------------------|----------------------------|
| id                | BIGSERIAL         | PK                         |
| grupo_id          | BIGINT            | NOT NULL, FK→grupos.id     |
| descripcion       | VARCHAR(255)      | NOT NULL                   |
| monto             | DECIMAL(10,2)     | NOT NULL, CHECK (monto > 0) — monto original en la moneda del gasto, NO es USDT |
| moneda            | VARCHAR(10)       | NOT NULL, DEFAULT 'USDT'    |
| moneda_nombre     | VARCHAR(50)       | NOT NULL, DEFAULT 'Tether'  |
| monto_usdt        | DECIMAL(10,6)     | NOT NULL, DEFAULT 0         |
| tasa_cambio       | DECIMAL(10,6)     | NOT NULL, DEFAULT 1         |
| pagador_id        | BIGINT            | FK→participantes.id        |
| fecha             | DATE              | NOT NULL                   |
| created_at        | TIMESTAMP         | NOT NULL                   |
| updated_at        | TIMESTAMP         | NOT NULL                   |

## Tabla: gasto_participantes
| Columna         | Tipo          | Restricciones              |
|-----------------|---------------|----------------------------|
| gasto_id        | BIGINT        | FK→gastos.id               |
| participante_id | BIGINT        | FK→participantes.id        |
| monto_adeudado  | DECIMAL(10,2) | NOT NULL                   |
| peso            | INTEGER       | NOT NULL DEFAULT 1         |

> PK compuesta: (gasto_id, participante_id)
> `peso` son las partes que le tocaron a ese participante en el reparto: 1 para
> todos en un reparto equitativo. Se persiste porque deducirlo de `monto_adeudado`
> sería ambiguo.
> Absorbe los centavos sobrantes del redondeo el pagador si participa del gasto;
> si no participa, el de mayor peso, y a igualdad de peso el de menor id.

## Tabla: pagos
| Columna         | Tipo          | Restricciones              |
|-----------------|---------------|----------------------------|
| id              | BIGSERIAL     | PK                         |
| grupo_id        | BIGINT        | NOT NULL, FK→grupos.id     |
| pagador_id      | BIGINT        | NOT NULL, FK→participantes.id |
| receptor_id     | BIGINT        | NOT NULL, FK→participantes.id |
| monto           | DECIMAL(10,2) | NOT NULL — siempre en USDT |
| fecha           | DATE          | NOT NULL                   |
| tx_id           | VARCHAR(100)  | nullable                   |
| created_at      | TIMESTAMP     | NOT NULL                   |
| updated_at      | TIMESTAMP     | NOT NULL                   |

> Pagos siempre en USDT. tx_id es el hash de transacción blockchain (opcional, solo referencia).
> El sistema no verifica la transacción en blockchain.
## Tabla: bajas_grupo
| Columna         | Tipo          | Restricciones                          |
|-----------------|---------------|----------------------------------------|
| id              | BIGSERIAL     | PK                                     |
| grupo_id        | BIGINT        | NOT NULL, FK→grupos.id                 |
| participante_id | BIGINT        | NOT NULL, FK→participantes.id          |
| saldo           | DECIMAL(10,2) | NOT NULL — en USDT, negativo si debía   |
| estado          | VARCHAR(20)   | NOT NULL — PENDIENTE/ASUMIDA/NO_ASUMIDA |
| fecha           | DATE          | NOT NULL                               |
| created_at      | TIMESTAMP     | NOT NULL                               |
| updated_at      | TIMESTAMP     | NOT NULL                               |

> Se registra solo cuando quien deja el grupo tiene saldo distinto de cero.
> `saldo` queda CONGELADO al momento de la baja: el de un ex-miembro todavía puede
> moverse (un pago hacia él, la edición de un gasto viejo), y el reparto tiene que
> corresponder con lo que el creador decidió.
> Solo las bajas ASUMIDA entran en el cálculo de balances.

## Tabla: baja_participantes
| Columna         | Tipo          | Restricciones                 |
|-----------------|---------------|-------------------------------|
| baja_id         | BIGINT        | FK→bajas_grupo.id             |
| participante_id | BIGINT        | FK→participantes.id           |
| monto           | DECIMAL(10,2) | NOT NULL — delta CON SIGNO     |

> PK compuesta: (baja_id, participante_id)
> `monto` es el delta que la baja le aplica al balance de ese participante, ya listo
> para sumar. Negativo si el que se fue debía (los demás bajan), positivo si le
> debían a él (los demás suben). La suma de las filas es exactamente el saldo.
> El reparto queda CONGELADO: quien entra después no absorbe nada.
