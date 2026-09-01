## Purpose

Defines the persisted domain model — user accounts, participants, groups, memberships, expenses, and expense splits — that the rest of the backend is built on.

## ADDED Requirements

### Requirement: Usuario persists core account data
The system SHALL persist a Usuario record with an auto-generated identifier, a unique username, a hashed password, and creation/update timestamps.

#### Scenario: Successful persistence
- **WHEN** a Usuario is saved with a unique username and password
- **THEN** it is assigned a numeric identifier and its created/updated timestamps are set

### Requirement: Usuario has exactly one linked Participante
The system SHALL represent a one-to-one relationship between a Usuario and a Participante, where each Participante references exactly one Usuario and no Usuario is referenced by more than one Participante.

#### Scenario: Unique usuario reference
- **WHEN** a Participante is saved referencing a Usuario
- **THEN** no other Participante can reference that same Usuario

### Requirement: Participante identifies a person
The system SHALL persist a Participante record with a name (nombre), last name (apellido), identity document (ci), its Usuario reference, and creation/update timestamps.

#### Scenario: Successful persistence
- **WHEN** a Participante is saved with nombre, apellido, ci, and a usuario reference
- **THEN** all fields persist and the usuario reference is required

### Requirement: Grupo groups participantes
The system SHALL persist a Grupo record with a name, an optional description, an optional creator reference (a Participante), and creation/update timestamps.

#### Scenario: Successful persistence
- **WHEN** a Grupo is saved with a name
- **THEN** it persists successfully with or without a description or creator

### Requirement: Participante membership in Grupo is tracked
The system SHALL record a Participante's membership in a Grupo, including the date joined, uniquely identified by the (grupo, participante) pair.

#### Scenario: Membership recorded
- **WHEN** a Participante is linked to a Grupo
- **THEN** a membership record exists with the join date, keyed uniquely by that grupo/participante pair

### Requirement: Gasto records an expense within a Grupo
The system SHALL persist a Gasto with a description, an amount greater than zero, a payer reference (a Participante), a date, a Grupo reference, and creation/update timestamps.

#### Scenario: Valid amount accepted
- **WHEN** a Gasto is saved with an amount greater than zero
- **THEN** it persists successfully

#### Scenario: Non-positive amount rejected
- **WHEN** a Gasto is saved with an amount of zero or less
- **THEN** the system rejects the record

### Requirement: GastoParticipante records each participant's share of a Gasto
The system SHALL record, for each Participante involved in a Gasto, the amount they owe, uniquely identified by the (gasto, participante) pair.

#### Scenario: Share recorded
- **WHEN** a Gasto is split among participantes
- **THEN** a record exists for each participante with the amount they owe, keyed uniquely by that gasto/participante pair

### Requirement: Schema is generated automatically from the entity model
The system SHALL cause the six underlying tables (and their constraints) to exist in PostgreSQL, generated automatically from the entity model without manual DDL.

#### Scenario: Tables created on startup
- **WHEN** the application starts against a database that does not yet have these tables
- **THEN** the usuarios, participantes, grupos, grupo_participantes, gastos, and gasto_participantes tables exist afterward, matching the documented columns and constraints
