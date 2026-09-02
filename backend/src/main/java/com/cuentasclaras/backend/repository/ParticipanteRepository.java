package com.cuentasclaras.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cuentasclaras.backend.entity.Participante;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {

    Optional<Participante> findByUsuarioId(Long usuarioId);
}
