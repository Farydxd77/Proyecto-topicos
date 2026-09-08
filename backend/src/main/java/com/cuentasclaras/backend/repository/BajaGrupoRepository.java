package com.cuentasclaras.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cuentasclaras.backend.entity.BajaGrupo;
import com.cuentasclaras.backend.entity.EstadoBaja;

public interface BajaGrupoRepository extends JpaRepository<BajaGrupo, Long> {

    List<BajaGrupo> findByGrupoIdOrderByFechaDesc(Long grupoId);

    Optional<BajaGrupo> findByIdAndGrupoId(Long id, Long grupoId);

    List<BajaGrupo> findByGrupoIdAndEstado(Long grupoId, EstadoBaja estado);

    void deleteByGrupoId(Long grupoId);
}
