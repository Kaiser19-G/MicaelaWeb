package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Long> {

    Optional<Docente> findByDni(String dni);

    Optional<Docente> findByCodigoDocente(String codigoDocente);

    Optional<Docente> findByUsuarioId(Long usuarioId);

    List<Docente> findByCondicion(Docente.Condicion condicion);

    boolean existsByDni(String dni);
}
