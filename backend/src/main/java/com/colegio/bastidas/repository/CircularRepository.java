package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.Circular;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CircularRepository extends JpaRepository<Circular, Long> {

    List<Circular> findAllByOrderByCreatedAtDesc();

    long countByPublicadaTrue();
}
