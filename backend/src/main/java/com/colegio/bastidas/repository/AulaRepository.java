package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.Aula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AulaRepository extends JpaRepository<Aula, Long> {
    List<Aula> findByNivel(String nivel);
}
