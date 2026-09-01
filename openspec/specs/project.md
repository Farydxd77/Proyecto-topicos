# Project: Cuentas Claras

## Overview
App web para dividir gastos entre participantes de un viaje o evento.
Monorepo con backend (Spring Boot + PostgreSQL) y frontend (React + Vite).
Fase actual: backend únicamente.

## Main Features
- Registro y login de usuarios con JWT
- Perfil de participante vinculado al usuario
- Crear grupos de viaje y agregar miembros
- Registrar gastos indicando quién pagó y entre quiénes se divide
- Editar y eliminar gastos
- Calcular balances de cada participante
- Calcular liquidación mínima (quién le paga cuánto a quién)

## Tech Stack
### Backend (fase actual)
- Spring Boot 4.1.1, Java 21, Maven
- PostgreSQL 17, Spring Data JPA, Hibernate
- Spring Security + JWT (jjwt 0.12.6)
- Lombok, Validation, Actuator

### Frontend (fase 2)
- React + Vite + TypeScript

## Scope Limitations
- Sin email ni notificaciones
- Sin múltiples monedas
- Sin pagos reales
- Sin roles de administrador global
- Frontend es fase 2 — no implementar aún