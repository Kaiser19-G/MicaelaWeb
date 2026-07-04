package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.CursoAsignado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CursoAsignadoRepository extends JpaRepository<CursoAsignado, Long> {
    
    @Query("SELECT c FROM CursoAsignado c JOIN FETCH c.aula JOIN FETCH c.docente WHERE c.docente.id = :docenteId")
    List<CursoAsignado> findByDocenteId(@Param("docenteId") Long docenteId);
    
    @Query("SELECT c FROM CursoAsignado c JOIN FETCH c.aula JOIN FETCH c.docente WHERE c.aula.id = :aulaId")
    List<CursoAsignado> findByAulaId(@Param("aulaId") Long aulaId);
}
