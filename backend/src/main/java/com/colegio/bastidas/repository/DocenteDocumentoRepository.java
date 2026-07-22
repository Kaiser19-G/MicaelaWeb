package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.DocenteDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocenteDocumentoRepository extends JpaRepository<DocenteDocumento, Long> {

    Optional<DocenteDocumento> findFirstByDocenteId(Long docenteId);

    boolean existsByDocenteId(Long docenteId);

    void deleteByDocenteId(Long docenteId);
}
