package com.cuentasclaras.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cuentasclaras.backend.entity.Participante;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {
}
