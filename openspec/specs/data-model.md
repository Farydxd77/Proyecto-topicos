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

> PK compuesta: (gasto_id, participante_id)
> El pagador absorbe los centavos sobrantes del redondeo.