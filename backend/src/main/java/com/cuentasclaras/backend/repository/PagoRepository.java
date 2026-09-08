package com.cuentasclaras.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cuentasclaras.backend.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByGrupoIdOrderByFechaDesc(Long grupoId);

    Optional<Pago> findByIdAndGrupoId(Long id, Long grupoId);
}
