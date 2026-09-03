package com.cuentasclaras.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cuentasclaras.backend.entity.Participante;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {

    Optional<Participante> findByUsuarioId(Long usuarioId);

    List<Participante> findByNombreContainingIgnoreCase(String nombre);

    List<Participante> findByApellidoContainingIgnoreCase(String apellido);

    Optional<Participante> findByCi(String ci);
}
