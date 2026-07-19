package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.ExpedienteDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedienteDocumentoRepository extends JpaRepository<ExpedienteDocumento, Long> {

    List<ExpedienteDocumento> findByAlumnoId(Long alumnoId);

    List<ExpedienteDocumento> findByAlumnoIdIn(List<Long> alumnoIds);

    List<ExpedienteDocumento> findByAlumnoIdAndAnioMatricula(Long alumnoId, Integer anio);

    List<ExpedienteDocumento> findByAlumnoIdAndTipoDocumento(
        Long alumnoId, ExpedienteDocumento.TipoDocumento tipo);

    Optional<ExpedienteDocumento> findFirstByAlumnoIdAndTipoDocumento(
        Long alumnoId, ExpedienteDocumento.TipoDocumento tipo);

    boolean existsByAlumnoIdAndTipoDocumentoAndEstadoVerificacion(
        Long alumnoId,
        ExpedienteDocumento.TipoDocumento tipo,
        ExpedienteDocumento.EstadoVerificacion estado);
}
