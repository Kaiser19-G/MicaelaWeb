package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.AuditoriaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, UUID> {
}
