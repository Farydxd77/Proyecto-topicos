package com.cuentasclaras.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cuentasclaras.backend.entity.Gasto;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    List<Gasto> findByGrupoIdOrderByFechaDesc(Long grupoId);

    Optional<Gasto> findByIdAndGrupoId(Long id, Long grupoId);
}
