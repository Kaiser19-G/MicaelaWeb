package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.Aula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AulaRepository extends JpaRepository<Aula, Long> {

    List<Aula> findByAnioAcademico(Integer anioAcademico);

    List<Aula> findByNivelAndAnioAcademico(Aula.Nivel nivel, Integer anioAcademico);

    Optional<Aula> findByGradoAndSeccionAndNivelAndAnioAcademico(
        String grado, String seccion, Aula.Nivel nivel, Integer anioAcademico);

    long countByAnioAcademico(Integer anioAcademico);
}
